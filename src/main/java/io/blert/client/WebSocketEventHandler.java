/*
 * Copyright (c) 2024 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.blert.client;

import com.google.protobuf.InvalidProtocolBufferException;
import io.blert.BlertPlugin;
import io.blert.BlertPluginPanel;
import io.blert.core.Challenge;
import io.blert.core.ChallengeMode;
import io.blert.core.RecordableChallenge;
import io.blert.core.Stage;
import io.blert.events.*;
import io.blert.events.Event;
import io.blert.proto.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * An {@code EventHandler} implementation that transmits received events to a Blert server through a websocket.
 */
@Slf4j
public class WebSocketEventHandler implements EventHandler {
    public enum Status {
        IDLE,
        CHALLENGE_STARTING,
        CHALLENGE_ACTIVE,
        CHALLENGE_ENDING,
    }

    /**
     * Tracks the state of a challenge start request, including any events that need to be queued
     * until the server responds with a challenge ID.
     */
    @AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    private static class ChallengeStartAttempt {
        private static final int INITIAL_RETRIES = 3;

        final int currentRequestId;
        final ChallengeStartRequest request;
        final Challenge challenge;
        final int remainingRetries;

        // Queue of events received during CHALLENGE_STARTING, before the challenge ID is known.
        final List<QueuedEvent> queuedEvents;

        // If the challenge ends before the start response arrives, store the end event here.
        @Nullable
        ChallengeEndEvent pendingEndEvent;

        ChallengeStartAttempt(int requestId, ChallengeStartRequest request, Challenge challenge) {
            this(requestId, request, challenge, INITIAL_RETRIES, new ArrayList<>(), null);
        }

        /**
         * Creates a new retry attempt with the same request and queued events but a new request ID
         * and decremented retry count.
         */
        ChallengeStartAttempt retry(int newRequestId) {
            return new ChallengeStartAttempt(
                    newRequestId,
                    request,
                    challenge,
                    remainingRetries - 1,
                    queuedEvents,
                    pendingEndEvent
            );
        }

        boolean canRetry() {
            return remainingRetries > 0;
        }

        /**
         * Returns the timeout duration for this attempt, using linear backoff.
         * Initial attempt: 5s, then 10s, 15s for subsequent retries.
         */
        int getTimeoutMs() {
            return (INITIAL_RETRIES - remainingRetries + 1) * DEFAULT_REQUEST_TIMEOUT_MS;
        }
    }

    /**
     * An event that was received during CHALLENGE_STARTING and needs to be processed once the
     * challenge ID is known.
     */
    @AllArgsConstructor
    private static class QueuedEvent {
        final int clientTick;
        final Event event;
    }

    private static final int DEFAULT_REQUEST_TIMEOUT_MS = 5000;

    private final BlertPlugin plugin;
    private final WebSocketClient webSocketClient;
    private final ProtoEventHandler protoEventHandler;
    private final Client runeliteClient;
    private final ClientThread runeliteThread;

    private int nextRequestId = 1;
    private int lastRequestId = -1;
    private final Timer requestTimeout = new Timer();
    private Status status = Status.IDLE;

    private Challenge currentChallenge = null;
    private @Nullable String challengeId = null;
    private @Nullable ChallengeStartAttempt currentStartAttempt = null;
    private Instant serverShutdownTime = null;
    private boolean apiKeyUsernameMismatch = false;

    private int currentTick = 0;

    /**
     * Constructs an event handler which will send and receive events over the provided websocket client.
     *
     * @param webSocketClient Websocket client connected and authenticated to the Blert server.
     */
    public WebSocketEventHandler(BlertPlugin plugin, WebSocketClient webSocketClient,
                                 Client client, ClientThread runeliteThread) {
        this.plugin = plugin;
        this.webSocketClient = webSocketClient;
        this.webSocketClient.setBinaryMessageCallback(this::handleProtoMessage);
        this.webSocketClient.setDisconnectCallback(this::handleDisconnect);
        this.protoEventHandler = new ProtoEventHandler();
        this.runeliteClient = client;
        this.runeliteThread = runeliteThread;
    }

    @Override
    public void handleEvent(int clientTick, Event event) {
        switch (event.getType()) {
            case CHALLENGE_START:
                // Starting a new challenge. Discard any buffered events and abandon any
                // pending start attempt.
                protoEventHandler.flushEventsUpTo(clientTick);
                if (currentStartAttempt != null) {
                    log.warn("Abandoning previous challenge start attempt due to new challenge");
                    abandonChallengeStart();
                }
                startChallenge((ChallengeStartEvent) event);
                break;

            case CHALLENGE_END:
                // If we're still waiting for a challenge start response, queue the end event.
                if (currentStartAttempt != null) {
                    log.debug("Queueing challenge end event until start response is received");
                    currentStartAttempt.pendingEndEvent = (ChallengeEndEvent) event;
                    break;
                }

                // Flush any pending events, then indicate that the challenge has ended.
                if (protoEventHandler.hasEvents()) {
                    sendEvents(protoEventHandler.flushEventsUpTo(clientTick));
                }
                endChallenge((ChallengeEndEvent) event);
                break;

            case CHALLENGE_UPDATE:
                // Queue if waiting for challenge start response.
                if (currentStartAttempt != null) {
                    currentStartAttempt.queuedEvents.add(new QueuedEvent(clientTick, event));
                    break;
                }
                updateChallenge((ChallengeUpdateEvent) event, null);
                break;

            case STAGE_UPDATE:
                // Queue if waiting for challenge start response.
                if (currentStartAttempt != null) {
                    currentStartAttempt.queuedEvents.add(new QueuedEvent(clientTick, event));
                    break;
                }

                // Flush any pending events prior to updating the stage.
                if (protoEventHandler.hasEvents()) {
                    sendEvents(protoEventHandler.flushEventsUpTo(clientTick));
                }
                updateChallenge(null, (StageUpdateEvent) event);
                break;

            default:
                // Forward other events to the protobuf handler to be serialized and sent to the server.
                protoEventHandler.handleEvent(clientTick, event);

                // Only send events if we have an active challenge ID.
                // During CHALLENGE_STARTING, events are buffered until the ID is received.
                if (status == Status.CHALLENGE_ACTIVE && currentStartAttempt == null) {
                    if (currentTick != clientTick) {
                        // Events are collected and sent in a single batch at the end of a tick.
                        sendEvents(protoEventHandler.flushEventsUpTo(clientTick));
                    }
                }

                break;
        }

        currentTick = clientTick;
    }

    private void startChallenge(ChallengeStartEvent event) {
        if (pendingServerShutdown()) {
            sendGameMessage(
                    "<col=ef1020>This challenge will not be recorded due to scheduled Blert maintenance.</col>"
            );
            return;
        }

        if (apiKeyUsernameMismatch) {
            sendGameMessage(
                    "<col=ef1020>This challenge will not be recorded as this API key is linked to a different OSRS account. " +
                            "If you changed your display name, please update it on the Blert website.</col>"
            );
            return;
        }

        if (event.getMode() == ChallengeMode.TOB_ENTRY) {
            log.warn("Recording of Theatre of Blood entry raids is disabled");
            return;
        }

        if (!webSocketClient.isOpen()) {
            return;
        }

        ChallengeStartRequest.Builder challengeStartRequestBuilder =
                ChallengeStartRequest.newBuilder()
                        .setChallenge(event.getChallenge().toProto())
                        .setMode(event.getMode().toProto())
                        .addAllParty(event.getParty())
                        .setSpectator(event.isSpectator());
        event.getStage().map(Stage::toProto).ifPresent(challengeStartRequestBuilder::setStage);

        ChallengeStartRequest challengeStartRequest = challengeStartRequestBuilder.build();

        // Create a new attempt to track the challenge start request and queued events.
        currentStartAttempt = new ChallengeStartAttempt(
                getRequestId(), challengeStartRequest, event.getChallenge());
        this.currentChallenge = event.getChallenge();

        setStatus(Status.CHALLENGE_STARTING);
        sendChallengeStartRequest(currentStartAttempt);
    }

    /**
     * Sends a challenge start request to the server and schedules a timeout for retry.
     */
    private void sendChallengeStartRequest(ChallengeStartAttempt attempt) {
        ServerMessage message = ServerMessage.newBuilder()
                .setType(ServerMessage.Type.CHALLENGE_START_REQUEST)
                .setRequestId(attempt.currentRequestId)
                .setChallengeStartRequest(attempt.request)
                .build();

        lastRequestId = attempt.currentRequestId;
        webSocketClient.sendMessage(message.toByteArray());

        // Schedule timeout with linear backoff.
        requestTimeout.schedule(new TimerTask() {
            @Override
            public void run() {
                handleChallengeStartTimeout(attempt);
            }
        }, attempt.getTimeoutMs());
    }

    /**
     * Handles a timeout for a challenge start request. Retries if possible, otherwise abandons.
     */
    private void handleChallengeStartTimeout(ChallengeStartAttempt attempt) {
        // Verify this is still the current attempt and we're still waiting for a response.
        if (currentStartAttempt != attempt || status != Status.CHALLENGE_STARTING) {
            return;
        }

        if (attempt.canRetry()) {
            ChallengeStartAttempt retryAttempt = attempt.retry(getRequestId());
            currentStartAttempt = retryAttempt;
            log.warn("Challenge start request timed out; retrying ({} retries remaining)",
                    retryAttempt.remainingRetries);
            sendChallengeStartRequest(retryAttempt);
        } else {
            log.error("Challenge start request failed after all retries");
            abandonChallengeStart();
        }
    }

    /**
     * Abandons any pending challenge start attempt, clearing state and returning to IDLE.
     */
    private void abandonChallengeStart() {
        currentStartAttempt = null;
        currentChallenge = null;
        setStatus(Status.IDLE);
    }

    void endChallenge(ChallengeEndEvent event) {
        if (challengeId == null) {
            log.warn("Attempted to end challenge without an active challenge ID");
            return;
        }

        int requestId = getRequestId();

        ServerMessage message = ServerMessage.newBuilder()
                .setType(ServerMessage.Type.CHALLENGE_END_REQUEST)
                .setRequestId(requestId)
                .setActiveChallengeId(challengeId)
                .setChallengeEndRequest(ChallengeEndRequest.newBuilder()
                        .setChallengeTimeTicks(event.getChallengeTime())
                        .setOverallTimeTicks(event.getOverallTime()))
                .build();

        lastRequestId = requestId;

        setStatus(Status.CHALLENGE_ENDING);
        webSocketClient.sendMessage(message.toByteArray());

        requestTimeout.schedule(new TimerTask() {
            @Override
            public void run() {
                if (status == Status.CHALLENGE_ENDING && lastRequestId == requestId) {
                    resetChallenge();
                }
            }
        }, DEFAULT_REQUEST_TIMEOUT_MS);
    }

    void updateChallenge(@Nullable ChallengeUpdateEvent challenge, @Nullable StageUpdateEvent stage) {
        if (challengeId == null) {
            log.warn("Attempted to update challenge without an active challenge ID");
            return;
        }

        ChallengeUpdate.Builder challengeUpdateBuilder = ChallengeUpdate.newBuilder();

        if (challenge != null) {
            challengeUpdateBuilder.setMode(challenge.getMode().toProto());
        }

        if (stage != null) {
            if (stage.getStage().isEmpty()) {
                log.error("Attempted to update stage without a stage value set");
                return;
            }

            var stageUpdate = ChallengeUpdate.StageUpdate.newBuilder()
                    .setStage(stage.getStage().get().toProto())
                    .setStatus(translateStageStatus(stage.getStatus()))
                    .setAccurate(stage.isAccurate())
                    .setRecordedTicks(stage.getTick())
                    .setGameTicksPrecise(stage.isGameTicksPrecise());

            stage.getInGameTicks().ifPresent(stageUpdate::setGameServerTicks);
            challengeUpdateBuilder.setStageUpdate(stageUpdate);
        }

        ServerMessage message = ServerMessage.newBuilder()
                .setActiveChallengeId(challengeId)
                .setType(ServerMessage.Type.CHALLENGE_UPDATE)
                .setChallengeUpdate(challengeUpdateBuilder)
                .build();

        webSocketClient.sendMessage(message.toByteArray());
    }

    public void updateGameState(GameState gameState) {
        ServerMessage.GameState.State state;
        if (gameState == GameState.LOGGED_IN) {
            state = ServerMessage.GameState.State.LOGGED_IN;
        } else if (gameState == GameState.LOGIN_SCREEN) {
            state = ServerMessage.GameState.State.LOGGED_OUT;
        } else {
            return;
        }

        ServerMessage.GameState.Builder gameStateBuilder = ServerMessage.GameState.newBuilder().setState(state);

        if (gameState == GameState.LOGGED_IN) {
            ServerMessage.GameState.PlayerInfo.Builder playerInfoBuilder = ServerMessage.GameState.PlayerInfo.newBuilder()
                    .setUsername(runeliteClient.getLocalPlayer().getName())
                    .setOverallExperience(runeliteClient.getOverallExperience())
                    .setAttackExperience(runeliteClient.getSkillExperience(Skill.ATTACK))
                    .setDefenceExperience(runeliteClient.getSkillExperience(Skill.DEFENCE))
                    .setStrengthExperience(runeliteClient.getSkillExperience(Skill.STRENGTH))
                    .setHitpointsExperience(runeliteClient.getSkillExperience(Skill.HITPOINTS))
                    .setRangedExperience(runeliteClient.getSkillExperience(Skill.RANGED))
                    .setPrayerExperience(runeliteClient.getSkillExperience(Skill.PRAYER))
                    .setMagicExperience(runeliteClient.getSkillExperience(Skill.MAGIC));
            gameStateBuilder.setPlayerInfo(playerInfoBuilder);
        }

        ServerMessage message = ServerMessage.newBuilder()
                .setType(ServerMessage.Type.GAME_STATE)
                .setGameState(gameStateBuilder)
                .build();
        webSocketClient.sendMessage(message.toByteArray());

        apiKeyUsernameMismatch = false;
    }

    private void handleProtoMessage(byte[] message) {
        ServerMessage serverMessage;
        try {
            serverMessage = ServerMessage.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse protobuf message", e);
            return;
        }

        switch (serverMessage.getType()) {
            case PING:
                sendPong();
                log.debug("Received heartbeat ping from server; responding with pong");
                break;

            case ERROR:
                handleServerError(serverMessage);
                break;

            case CONNECTION_RESPONSE:
                serverShutdownTime = null;
                plugin.getSidePanel().setShutdownTime(null);

                if (serverMessage.hasUser()) {
                    plugin.getSidePanel().updateConnectionState(
                            BlertPluginPanel.ConnectionState.CONNECTED,
                            serverMessage.getUser().getName()
                    );
                    if (runeliteClient.getGameState() == GameState.LOGGED_IN) {
                        updateGameState(GameState.LOGGED_IN);
                    }
                    sendRaidHistoryRequest();
                } else {
                    log.warn("Received invalid connection response from server");
                    closeWebsocketClient();
                }
                break;

            case HISTORY_RESPONSE:
                plugin.getSidePanel().setRecentRecordings(serverMessage.getRecentRecordingsList());
                break;

            case CHALLENGE_START_RESPONSE:
                handleChallengeStartResponse(serverMessage);
                break;

            case CHALLENGE_END_RESPONSE:
                if (status != Status.CHALLENGE_ENDING || serverMessage.getRequestId() != lastRequestId) {
                    log.warn("Received unexpected CHALLENGE_END_RESPONSE from server");
                    return;
                }
                resetChallenge();
                // TODO Make proper fix https://github.com/blert-io/plugin/issues/9
                // delaying raid history request to allow backend update last challenge
                requestTimeout.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendRaidHistoryRequest();
                    }
                }, DEFAULT_REQUEST_TIMEOUT_MS);

                break;

            case SERVER_STATUS: {
                var serverStatus = serverMessage.getServerStatus();
                switch (serverStatus.getStatus()) {
                    case SHUTDOWN_PENDING: {
                        var shutdownTime = serverStatus.getShutdownTime();
                        serverShutdownTime = Instant.ofEpochSecond(shutdownTime.getSeconds(), shutdownTime.getNanos());
                        Duration timeUntilShutdown = Duration.between(Instant.now(), serverShutdownTime);
                        plugin.getSidePanel().setShutdownTime(serverShutdownTime);

                        String shutdownMessage = String.format(
                                "Blert's servers will go offline for maintenance in %s.<br>Visit Blert's Discord server for status updates.",
                                DurationFormatUtils.formatDuration(timeUntilShutdown.toMillis(), "HH:mm:ss")
                        );

                        sendGameMessage(ChatMessageType.BROADCAST, shutdownMessage);
                        break;
                    }

                    case SHUTDOWN_IMMINENT: {
                        reset();
                        protoEventHandler.flushEventsUpTo(currentTick);
                        closeWebsocketClient();
                        break;
                    }

                    case SHUTDOWN_CANCELED: {
                        serverShutdownTime = null;
                        plugin.getSidePanel().setShutdownTime(null);
                        sendGameMessage(
                                ChatMessageType.BROADCAST,
                                "The scheduled Blert maintenance has been canceled. You may continue to record PvM challenges!"
                        );
                        break;
                    }

                    case UNRECOGNIZED:
                        log.error("Received unrecognized server status from server: {}", serverStatus.getStatus());
                        break;

                    default:
                        break;
                }
                break;
            }

            case PLAYER_STATE:
                break;

            case CHALLENGE_STATE_CONFIRMATION:
                handleChallengeStateConfirmation(serverMessage);
                break;

            case PONG:
            case HISTORY_REQUEST:
            case EVENT_STREAM:
            case GAME_STATE:
                log.warn("Received unexpected protobuf message from server: {}", serverMessage.getType());
                break;

            case UNRECOGNIZED:
                log.warn("Received unrecognized protobuf message from server");
                break;
        }
    }

    private synchronized void handleServerError(ServerMessage message) {
        ServerMessage.Error error = message.getError();

        switch (error.getType()) {
            case BAD_REQUEST:
                // TODO(frolv): Implement.
                break;

            case UNIMPLEMENTED:
                // TODO(frolv): Implement.
                break;

            case USERNAME_MISMATCH:
                sendGameMessage(
                        ChatMessageType.GAMEMESSAGE,
                        "<col=ef1020>This Blert API key is linked to the account " + error.getUsername() +
                                ". If you changed your display name, please go update it on the Blert website.</col>");
                apiKeyUsernameMismatch = true;
                // Abandon any pending challenge start since this account can't record.
                if (currentStartAttempt != null) {
                    abandonChallengeStart();
                }
                break;

            case UNAUTHENTICATED:
                log.info("Disconnected from server due to authentication failure");
                closeWebsocketClient();
                break;

            case CHALLENGE_RECORDING_ENDED:
                if (challengeId == null || !challengeId.equals(message.getActiveChallengeId())) {
                    break;
                }

                if (status == Status.CHALLENGE_STARTING || status == Status.CHALLENGE_ACTIVE) {
                    log.error("Server ended recording for challenge {}", challengeId);
                    resetChallenge();
                }
                break;

            case UNKNOWN:
            case UNRECOGNIZED:
                log.error("Received unrecognized server error {}", error.getTypeValue());
                break;
        }
    }

    private void handleChallengeStartResponse(ServerMessage serverMessage) {
        if (status != Status.CHALLENGE_STARTING || serverMessage.getRequestId() != lastRequestId) {
            log.warn("Received unexpected CHALLENGE_START_RESPONSE from server");
            return;
        }

        if (!serverMessage.hasActiveChallengeId()) {
            log.error("Failed to start challenge");
            protoEventHandler.setChallengeId(null);
            currentChallenge = null;
            challengeId = null;
            currentStartAttempt = null;
            setStatus(Status.IDLE);

            if (serverMessage.hasError()) {
                var error = serverMessage.getError();
                if (error.hasMessage()) {
                    sendGameMessage(ChatMessageType.GAMEMESSAGE, "<col=ef1020>[Blert] " + error.getMessage() + "</col>");
                }
            }
            return;
        }

        challengeId = serverMessage.getActiveChallengeId();

        // Stamp all buffered proto events with the challenge ID.
        protoEventHandler.setChallengeId(challengeId);

        // Capture and clear the start attempt before processing queued events.
        ChallengeStartAttempt attempt = currentStartAttempt;
        currentStartAttempt = null;

        setStatus(Status.CHALLENGE_ACTIVE);

        // Flush any buffered proto events (now that they have the challenge ID).
        if (protoEventHandler.hasEvents()) {
            sendEvents(protoEventHandler.flushEventsUpTo(currentTick));
        }

        // Process any queued STAGE_UPDATE and CHALLENGE_UPDATE events.
        if (attempt != null) {
            for (QueuedEvent qe : attempt.queuedEvents) {
                switch (qe.event.getType()) {
                    case STAGE_UPDATE:
                        updateChallenge(null, (StageUpdateEvent) qe.event);
                        break;
                    case CHALLENGE_UPDATE:
                        updateChallenge((ChallengeUpdateEvent) qe.event, null);
                        break;
                    default:
                        // Other event types were already buffered in protoEventHandler.
                        break;
                }
            }

            // If the challenge ended while waiting for the start response, process it now.
            if (attempt.pendingEndEvent != null) {
                log.debug("Processing queued challenge end event");
                if (protoEventHandler.hasEvents()) {
                    sendEvents(protoEventHandler.flushEventsUpTo(currentTick));
                }
                endChallenge(attempt.pendingEndEvent);
            }
        }
    }

    private void handleDisconnect(WebSocketClient.DisconnectReason reason) {
        resetChallenge();

        BlertPluginPanel.ConnectionState connectionState;
        switch (reason) {
            case UNSUPPORTED_VERSION:
                connectionState = BlertPluginPanel.ConnectionState.UNSUPPORTED_VERSION;
                break;
            case ERROR:
                if (webSocketClient.getState() == WebSocketClient.State.REJECTED) {
                    connectionState = BlertPluginPanel.ConnectionState.REJECTED;
                } else {
                    connectionState = BlertPluginPanel.ConnectionState.DISCONNECTED;
                }
                break;
            case CLOSED_SUCCESSFULLY:
            default:
                connectionState = BlertPluginPanel.ConnectionState.DISCONNECTED;
                break;
        }

        plugin.getSidePanel().updateConnectionState(connectionState, null);
        plugin.getSidePanel().setRecentRecordings(null);
    }

    private void sendEvents(List<io.blert.proto.Event> events) {
        if (!webSocketClient.isOpen() || events.isEmpty()) {
            return;
        }

        ServerMessage.Builder message = ServerMessage.newBuilder()
                .setType(ServerMessage.Type.EVENT_STREAM)
                .setActiveChallengeId(events.get(0).getChallengeId());
        int ignoredEvents = 0;

        for (io.blert.proto.Event event : events) {
            if (event.getChallengeId().isEmpty()) {
                ignoredEvents++;
                continue;
            }

            if (event.getChallengeId().equals(message.getActiveChallengeId())) {
                message.addChallengeEvents(event.toBuilder().clearChallengeId().build());
            } else {
                if (message.getChallengeEventsCount() > 0) {
                    webSocketClient.sendMessage(message.build().toByteArray());
                }

                message = ServerMessage.newBuilder()
                        .setType(ServerMessage.Type.EVENT_STREAM)
                        .setActiveChallengeId(event.getChallengeId());
            }
        }

        if (message.getChallengeEventsCount() > 0) {
            webSocketClient.sendMessage(message.build().toByteArray());
        }

        if (ignoredEvents > 0) {
            log.debug("Ignored {} events without a challenge ID", ignoredEvents);
        }
    }

    private void sendPong() {
        ServerMessage message = ServerMessage.newBuilder().setType(ServerMessage.Type.PONG).build();
        webSocketClient.sendMessage(message.toByteArray());
    }

    private void sendRaidHistoryRequest() {
        if (webSocketClient.isOpen()) {
            ServerMessage message = ServerMessage.newBuilder().setType(ServerMessage.Type.HISTORY_REQUEST).build();
            webSocketClient.sendMessage(message.toByteArray());
        }
    }

    private void resetChallenge() {
        currentChallenge = null;
        challengeId = null;
        currentStartAttempt = null;
        protoEventHandler.setChallengeId(null);
        setStatus(Status.IDLE);
    }

    private void reset() {
        resetChallenge();
        plugin.getSidePanel().updateConnectionState(BlertPluginPanel.ConnectionState.DISCONNECTED, null);
        plugin.getSidePanel().setRecentRecordings(null);
    }

    private void setStatus(Status status) {
        this.status = status;
        var challenge = currentChallenge != null
                ? currentChallenge.toProto()
                : io.blert.proto.Challenge.UNKNOWN_CHALLENGE;
        plugin.getSidePanel().updateChallengeStatus(status, challenge, challengeId);
    }

    private boolean pendingServerShutdown() {
        return serverShutdownTime != null;
    }

    private void sendGameMessage(String message) {
        sendGameMessage(ChatMessageType.GAMEMESSAGE, message);
    }

    private void sendGameMessage(ChatMessageType type, String message) {
        runeliteThread.invoke(() -> {
            if (runeliteClient.getGameState() == GameState.LOGGED_IN) {
                runeliteClient.addChatMessage(type, "", message, null);
            }
        });
    }

    private void closeWebsocketClient() {
        try {
            webSocketClient.close().get();
        } catch (Exception e) {
            log.error("Failed to close websocket client", e);
        }
        plugin.getSidePanel().setShutdownTime(null);
    }

    private void handleChallengeStateConfirmation(ServerMessage message) {
        ServerMessage.ChallengeStateConfirmation stateToConfirm = message.getChallengeStateConfirmation();
        Player player = runeliteClient.getLocalPlayer();
        if (player == null) {
            return;
        }

        if (message.getActiveChallengeId().isEmpty()) {
            log.warn("Received confirmation request with empty challenge ID");
            return;
        }

        String username = player.getName() != null ? player.getName().toLowerCase() : null;
        if (!Objects.equals(stateToConfirm.getUsername(), username)) {
            log.warn("Received confirmation request for {} but current player is {}",
                    stateToConfirm.getUsername(), username);
            return;
        }

        if (plugin.getActiveChallenge() == null) {
            ServerMessage.Builder response = ServerMessage.newBuilder()
                    .setType(ServerMessage.Type.CHALLENGE_STATE_CONFIRMATION)
                    .setActiveChallengeId(message.getActiveChallengeId())
                    .setChallengeStateConfirmation(ServerMessage.ChallengeStateConfirmation.newBuilder().setIsValid(false));
            webSocketClient.sendMessage(response.build().toByteArray());
            return;
        }

        final WebSocketEventHandler self = this;

        // Getting the challenge status is a blocking operation, so run it in a separate thread.
        new Thread(() -> {
            ServerMessage.Builder response = ServerMessage.newBuilder()
                    .setType(ServerMessage.Type.CHALLENGE_STATE_CONFIRMATION)
                    .setActiveChallengeId(message.getActiveChallengeId());

            RecordableChallenge activeChallenge = plugin.getActiveChallenge();
            if (activeChallenge == null) {
                response.setChallengeStateConfirmation(
                        ServerMessage.ChallengeStateConfirmation.newBuilder().setIsValid(false));
                webSocketClient.sendMessage(response.build().toByteArray());
                return;
            }

            RecordableChallenge.Status status = null;

            try {
                status = activeChallenge.getStatus().get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Failed to get challenge status", e);
            }

            ServerMessage.ChallengeStateConfirmation.Builder confirmationBuilder =
                    ServerMessage.ChallengeStateConfirmation.newBuilder().setUsername(username);

            boolean isValid = false;

            if (status != null) {
                Set<String> party = status.getParty().stream().map(String::toLowerCase).collect(Collectors.toSet());
                Set<String> partyToConfirm = stateToConfirm.getPartyList()
                        .stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());

                isValid = status.getChallenge().toProto().equals(stateToConfirm.getChallenge()) &&
                        status.getStage() != null &&
                        status.getStage().toProto().getNumber() >= stateToConfirm.getStage().getNumber() &&
                        party.equals(partyToConfirm);

                if (!party.contains(username)) {
                    confirmationBuilder.setSpectator(true);
                }
            }

            confirmationBuilder.setIsValid(isValid);
            response.setChallengeStateConfirmation(confirmationBuilder);

            synchronized (self) {
                self.webSocketClient.sendMessage(response.build().toByteArray());

                if (isValid) {
                    self.challengeId = message.getActiveChallengeId();
                    self.protoEventHandler.setChallengeId(self.challengeId);
                    self.setStatus(Status.CHALLENGE_ACTIVE);
                    log.debug("Confirmed challenge state; rejoining challenge {}", self.challengeId);
                }

            }
        }).start();
    }

    private int getRequestId() {
        int id = nextRequestId;
        if (nextRequestId == Integer.MAX_VALUE) {
            nextRequestId = 1;
        } else {
            nextRequestId++;
        }
        return id;
    }

    private static ChallengeUpdate.StageUpdate.Status translateStageStatus(StageUpdateEvent.Status status) {
        switch (status) {
            case ENTERED:
                return ChallengeUpdate.StageUpdate.Status.ENTERED;
            case STARTED:
                return ChallengeUpdate.StageUpdate.Status.STARTED;
            case COMPLETED:
                return ChallengeUpdate.StageUpdate.Status.COMPLETED;
            case WIPED:
                return ChallengeUpdate.StageUpdate.Status.WIPED;
            default:
                throw new NotImplementedException("Stage status translation not implemented for " + status);
        }
    }
}