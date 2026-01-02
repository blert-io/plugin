/*
 * Copyright (c) 2024 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.blert.client;

import com.google.gson.Gson;
import io.blert.BlertPlugin;
import io.blert.BlertPluginPanel;
import io.blert.core.*;
import io.blert.events.*;
import io.blert.events.Event;
import io.blert.json.*;
import joptsimple.internal.Strings;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
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
    private static final Gson gson = new Gson();

    private final BlertPlugin plugin;
    private final WebSocketClient webSocketClient;
    private final EventBuffer eventBuffer;
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
        this.webSocketClient.setTextMessageCallback(this::handleJsonMessage);
        this.webSocketClient.setDisconnectCallback(this::handleDisconnect);
        this.eventBuffer = new EventBuffer();
        this.runeliteClient = client;
        this.runeliteThread = runeliteThread;
    }

    @Override
    public void handleEvent(int clientTick, Event event) {
        switch (event.getType()) {
            case CHALLENGE_START:
                // Starting a new challenge. Discard any buffered events and abandon any
                // pending start attempt.
                eventBuffer.flushEventsUpTo(clientTick);
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
                if (eventBuffer.hasEvents()) {
                    sendEvents(eventBuffer.flushEventsUpTo(clientTick));
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
                if (eventBuffer.hasEvents()) {
                    sendEvents(eventBuffer.flushEventsUpTo(clientTick));
                }
                updateChallenge(null, (StageUpdateEvent) event);
                break;

            default:
                // Forward other events to the event buffer to be serialized and sent to the server.
                eventBuffer.handleEvent(clientTick, event);

                // Only send events if we have an active challenge ID.
                // During CHALLENGE_STARTING, events are buffered until the ID is received.
                if (status == Status.CHALLENGE_ACTIVE && currentStartAttempt == null) {
                    if (currentTick != clientTick) {
                        // Events are collected and sent in a single batch at the end of a tick.
                        sendEvents(eventBuffer.flushEventsUpTo(clientTick));
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

        ChallengeStartRequest challengeStartRequest = new ChallengeStartRequest();
        challengeStartRequest.challenge = event.getChallenge().getId();
        challengeStartRequest.mode = event.getMode().getId();
        challengeStartRequest.party = new ArrayList<>(event.getParty());
        challengeStartRequest.spectator = event.isSpectator();
        event.getStage().map(Stage::getId).ifPresent(s -> challengeStartRequest.stage = s);

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
        ServerMessage message = new ServerMessage();
        message.type = ServerMessage.TYPE_CHALLENGE_START_REQUEST;
        message.requestId = attempt.currentRequestId;
        message.challengeStartRequest = attempt.request;

        lastRequestId = attempt.currentRequestId;
        webSocketClient.sendTextMessage(gson.toJson(message));

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

        ServerMessage message = new ServerMessage();
        message.type = ServerMessage.TYPE_CHALLENGE_END_REQUEST;
        message.requestId = requestId;
        message.activeChallengeId = challengeId;
        message.challengeEndRequest = new io.blert.json.ChallengeEndRequest();
        message.challengeEndRequest.challengeTimeTicks = event.getChallengeTime();
        message.challengeEndRequest.overallTimeTicks = event.getOverallTime();

        lastRequestId = requestId;

        setStatus(Status.CHALLENGE_ENDING);
        webSocketClient.sendTextMessage(gson.toJson(message));

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

        ChallengeUpdate challengeUpdate = new ChallengeUpdate();

        if (challenge != null) {
            challengeUpdate.mode = challenge.getMode().getId();
        }

        if (stage != null) {
            if (stage.getStage().isEmpty()) {
                log.error("Attempted to update stage without a stage value set");
                return;
            }

            ChallengeUpdate.StageUpdate stageUpdate = new ChallengeUpdate.StageUpdate();
            stageUpdate.stage = stage.getStage().get().getId();
            stageUpdate.status = translateStageStatus(stage.getStatus());
            stageUpdate.accurate = stage.isAccurate();
            stageUpdate.recordedTicks = stage.getTick();
            stageUpdate.gameTicksPrecise = stage.isGameTicksPrecise();
            stage.getInGameTicks().ifPresent(t -> stageUpdate.gameServerTicks = t);
            challengeUpdate.stageUpdate = stageUpdate;
        }

        ServerMessage message = new ServerMessage();
        message.activeChallengeId = challengeId;
        message.type = ServerMessage.TYPE_CHALLENGE_UPDATE;
        message.challengeUpdate = challengeUpdate;

        webSocketClient.sendTextMessage(gson.toJson(message));
    }

    public void updateGameState(GameState gameState) {
        int state;
        if (gameState == GameState.LOGGED_IN) {
            state = io.blert.json.GameState.STATE_LOGGED_IN;
        } else if (gameState == GameState.LOGIN_SCREEN) {
            state = io.blert.json.GameState.STATE_LOGGED_OUT;
        } else {
            return;
        }

        io.blert.json.GameState gameStateJson = new io.blert.json.GameState();
        gameStateJson.state = state;

        if (gameState == GameState.LOGGED_IN) {
            Player localPlayer = runeliteClient.getLocalPlayer();
            if (localPlayer == null || localPlayer.getName() == null) {
                return;
            }

            io.blert.json.GameState.PlayerInfo playerInfo = new io.blert.json.GameState.PlayerInfo();
            playerInfo.username = localPlayer.getName();
            playerInfo.accountHash = Long.toString(runeliteClient.getAccountHash());
            playerInfo.overallExperience = runeliteClient.getOverallExperience();
            playerInfo.attackExperience = runeliteClient.getSkillExperience(Skill.ATTACK);
            playerInfo.defenceExperience = runeliteClient.getSkillExperience(Skill.DEFENCE);
            playerInfo.strengthExperience = runeliteClient.getSkillExperience(Skill.STRENGTH);
            playerInfo.hitpointsExperience = runeliteClient.getSkillExperience(Skill.HITPOINTS);
            playerInfo.rangedExperience = runeliteClient.getSkillExperience(Skill.RANGED);
            playerInfo.prayerExperience = runeliteClient.getSkillExperience(Skill.PRAYER);
            playerInfo.magicExperience = runeliteClient.getSkillExperience(Skill.MAGIC);
            gameStateJson.playerInfo = playerInfo;
        }

        ServerMessage message = new ServerMessage();
        message.type = ServerMessage.TYPE_GAME_STATE;
        message.gameState = gameStateJson;
        webSocketClient.sendTextMessage(gson.toJson(message));

        apiKeyUsernameMismatch = false;
    }

    private void handleJsonMessage(String messageText) {
        ServerMessage serverMessage;
        try {
            serverMessage = gson.fromJson(messageText, ServerMessage.class);
        } catch (Exception e) {
            log.error("Failed to parse JSON message", e);
            return;
        }

        switch (serverMessage.type) {
            case ServerMessage.TYPE_PING:
                sendPong();
                log.debug("Received heartbeat ping from server; responding with pong");
                break;

            case ServerMessage.TYPE_ERROR:
                handleServerError(serverMessage);
                break;

            case ServerMessage.TYPE_CONNECTION_RESPONSE:
                serverShutdownTime = null;
                plugin.getSidePanel().setShutdownTime(null);

                if (serverMessage.user != null) {
                    plugin.getSidePanel().updateConnectionState(
                            BlertPluginPanel.ConnectionState.CONNECTED,
                            serverMessage.user.name
                    );
                    sendRaidHistoryRequest();
                } else {
                    log.warn("Received invalid connection response from server");
                    closeWebsocketClient();
                }
                break;

            case ServerMessage.TYPE_HISTORY_RESPONSE:
                plugin.getSidePanel().setRecentRecordings(serverMessage.recentRecordings);
                break;

            case ServerMessage.TYPE_CHALLENGE_START_RESPONSE:
                handleChallengeStartResponse(serverMessage);
                break;

            case ServerMessage.TYPE_CHALLENGE_END_RESPONSE:
                if (status != Status.CHALLENGE_ENDING || serverMessage.requestId == null ||
                        serverMessage.requestId != lastRequestId) {
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

            case ServerMessage.TYPE_SERVER_STATUS: {
                var serverStatus = serverMessage.serverStatus;
                if (serverStatus != null) {
                    handleServerStatus(serverStatus);
                }
                break;
            }

            case ServerMessage.TYPE_PLAYER_STATE:
                break;

            case ServerMessage.TYPE_GAME_STATE_REQUEST:
                updateGameState(runeliteClient.getGameState());
                break;

            case ServerMessage.TYPE_ATTACK_DEFINITIONS:
                if (serverMessage.attackDefinitions != null) {
                    plugin.getAttackRegistry().updateFromServer(
                            serverMessage.attackDefinitions.stream()
                                    .map(io.blert.json.AttackDefinition::toCore)
                                    .collect(Collectors.toList())
                    );
                }
                break;

            case ServerMessage.TYPE_SPELL_DEFINITIONS:
                if (serverMessage.spellDefinitions != null) {
                    plugin.getSpellRegistry().updateFromServer(
                            serverMessage.spellDefinitions.stream()
                                    .map(io.blert.json.SpellDefinition::toCore)
                                    .collect(Collectors.toList())
                    );
                }
                break;

            case ServerMessage.TYPE_CHALLENGE_STATE_CONFIRMATION:
                handleChallengeStateConfirmation(serverMessage);
                break;

            case ServerMessage.TYPE_PONG:
            case ServerMessage.TYPE_HISTORY_REQUEST:
            case ServerMessage.TYPE_EVENT_STREAM:
            case ServerMessage.TYPE_GAME_STATE:
                log.warn("Received unexpected message from server: type={}", serverMessage.type);
                break;

            default:
                log.warn("Received unrecognized message from server: type={}", serverMessage.type);
                break;
        }
    }

    private synchronized void handleServerError(ServerMessage message) {
        ErrorData error = message.error;
        if (error == null) {
            return;
        }

        switch (error.type) {
            case ErrorData.TYPE_BAD_REQUEST:
                // TODO(frolv): Implement.
                break;

            case ErrorData.TYPE_UNIMPLEMENTED:
                // TODO(frolv): Implement.
                break;

            case ErrorData.TYPE_USERNAME_MISMATCH:
                sendGameMessage(
                        ChatMessageType.GAMEMESSAGE,
                        "<col=ef1020>This Blert API key is linked to the OSRS account " + error.username +
                                ". If you changed your display name, please go update it on the Blert website.</col>");
                apiKeyUsernameMismatch = true;
                // Abandon any pending challenge start since this account can't record.
                if (currentStartAttempt != null) {
                    abandonChallengeStart();
                }
                break;

            case ErrorData.TYPE_UNAUTHENTICATED:
                log.info("Disconnected from server due to authentication failure");
                closeWebsocketClient();
                break;

            case ErrorData.TYPE_CHALLENGE_RECORDING_ENDED:
                if (challengeId == null || !challengeId.equals(message.activeChallengeId)) {
                    break;
                }

                if (status == Status.CHALLENGE_STARTING || status == Status.CHALLENGE_ACTIVE) {
                    log.error("Server ended recording for challenge {}", challengeId);
                    resetChallenge();
                }
                break;

            case ErrorData.TYPE_UNKNOWN:
            default:
                log.error("Received unrecognized server error type={}", error.type);
                break;
        }
    }

    private void handleChallengeStartResponse(ServerMessage serverMessage) {
        if (status != Status.CHALLENGE_STARTING ||
                serverMessage.requestId == null ||
                serverMessage.requestId != lastRequestId) {
            log.warn("Received unexpected CHALLENGE_START_RESPONSE from server");
            return;
        }

        if (Strings.isNullOrEmpty(serverMessage.activeChallengeId)) {
            log.error("Failed to start challenge");
            eventBuffer.setChallengeId(null);
            currentChallenge = null;
            challengeId = null;
            currentStartAttempt = null;
            setStatus(Status.IDLE);

            if (serverMessage.error != null && serverMessage.error.message != null) {
                sendGameMessage(ChatMessageType.GAMEMESSAGE, "<col=ef1020>[Blert] " + serverMessage.error.message + "</col>");
            }
            return;
        }

        challengeId = serverMessage.activeChallengeId;

        // Stamp all buffered events with the challenge ID.
        eventBuffer.setChallengeId(challengeId);

        // Capture and clear the start attempt before processing queued events.
        ChallengeStartAttempt attempt = currentStartAttempt;
        currentStartAttempt = null;

        setStatus(Status.CHALLENGE_ACTIVE);

        // Flush any buffered events (now that they have the challenge ID).
        if (eventBuffer.hasEvents()) {
            sendEvents(eventBuffer.flushEventsUpTo(currentTick));
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
                        // Other event types were already buffered in eventBuffer.
                        break;
                }
            }

            // If the challenge ended while waiting for the start response, process it now.
            if (attempt.pendingEndEvent != null) {
                log.debug("Processing queued challenge end event");
                if (eventBuffer.hasEvents()) {
                    sendEvents(eventBuffer.flushEventsUpTo(currentTick));
                }
                endChallenge(attempt.pendingEndEvent);
            }
        }
    }

    private void handleServerStatus(@NonNull ServerStatus serverStatus) {
        switch (serverStatus.status) {
            case ServerStatus.STATUS_SHUTDOWN_PENDING: {
                if (serverStatus.shutdownTime != null) {
                    serverShutdownTime = Instant.ofEpochSecond(
                            serverStatus.shutdownTime.seconds,
                            serverStatus.shutdownTime.nanos
                    );
                    Duration timeUntilShutdown = Duration.between(Instant.now(), serverShutdownTime);
                    plugin.getSidePanel().setShutdownTime(serverShutdownTime);

                    String shutdownMessage = String.format(
                            "Blert's servers will go offline for maintenance in %s." +
                            "<br>Visit Blert's Discord server for status updates.",
                            DurationFormatUtils.formatDuration(timeUntilShutdown.toMillis(), "HH:mm:ss")
                    );

                    sendGameMessage(ChatMessageType.BROADCAST, shutdownMessage);
                }
                break;
            }

            case ServerStatus.STATUS_SHUTDOWN_IMMINENT: {
                reset();
                eventBuffer.flushEventsUpTo(currentTick);
                closeWebsocketClient();
                break;
            }

            case ServerStatus.STATUS_SHUTDOWN_CANCELED: {
                serverShutdownTime = null;
                plugin.getSidePanel().setShutdownTime(null);
                sendGameMessage(
                        ChatMessageType.BROADCAST,
                        "The scheduled Blert maintenance has been canceled. You may continue to record PvM challenges!"
                );
                break;
            }

            default:
                break;
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

    private void sendEvents(List<io.blert.json.Event> events) {
        if (!webSocketClient.isOpen() || events.isEmpty()) {
            return;
        }

        ServerMessage message = new ServerMessage();
        message.type = ServerMessage.TYPE_EVENT_STREAM;
        message.activeChallengeId = events.get(0).challengeId;
        message.challengeEvents = new ArrayList<>();

        int ignoredEvents = 0;

        for (io.blert.json.Event event : events) {
            if (Strings.isNullOrEmpty(event.challengeId)) {
                ignoredEvents++;
                continue;
            }

            if (!event.challengeId.equals(message.activeChallengeId)) {
                if (!message.challengeEvents.isEmpty()) {
                    webSocketClient.sendTextMessage(gson.toJson(message));
                }

                message = new ServerMessage();
                message.type = ServerMessage.TYPE_EVENT_STREAM;
                message.activeChallengeId = event.challengeId;
                message.challengeEvents = new ArrayList<>();
            }

            // Clear the challengeId from individual events since it's set at the message level.
            event.challengeId = null;
            message.challengeEvents.add(event);
        }

        if (!message.challengeEvents.isEmpty()) {
            webSocketClient.sendTextMessage(gson.toJson(message));
        }

        if (ignoredEvents > 0) {
            log.debug("Ignored {} events without a challenge ID", ignoredEvents);
        }
    }

    private void sendPong() {
        ServerMessage message = new ServerMessage();
        message.type = ServerMessage.TYPE_PONG;
        webSocketClient.sendTextMessage(gson.toJson(message));
    }

    private void sendRaidHistoryRequest() {
        if (webSocketClient.isOpen()) {
            ServerMessage message = new ServerMessage();
            message.type = ServerMessage.TYPE_HISTORY_REQUEST;
            webSocketClient.sendTextMessage(gson.toJson(message));
        }
    }

    private void resetChallenge() {
        currentChallenge = null;
        challengeId = null;
        currentStartAttempt = null;
        eventBuffer.setChallengeId(null);
        setStatus(Status.IDLE);
    }

    private void reset() {
        resetChallenge();
        plugin.getSidePanel().updateConnectionState(BlertPluginPanel.ConnectionState.DISCONNECTED, null);
        plugin.getSidePanel().setRecentRecordings(null);
    }

    private void setStatus(Status status) {
        this.status = status;
        plugin.getSidePanel().updateChallengeStatus(status, currentChallenge, challengeId);
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
        ChallengeStateConfirmation stateToConfirm = message.challengeStateConfirmation;
        if (stateToConfirm == null) {
            return;
        }

        Player player = runeliteClient.getLocalPlayer();
        if (player == null) {
            return;
        }

        if (Strings.isNullOrEmpty(message.activeChallengeId)) {
            log.warn("Received confirmation request with empty challenge ID");
            return;
        }

        String username = player.getName() != null ? player.getName().toLowerCase() : null;
        if (!Objects.equals(stateToConfirm.username, username)) {
            log.warn("Received confirmation request for {} but current player is {}",
                    stateToConfirm.username, username);
            return;
        }

        if (plugin.getActiveChallenge() == null) {
            ServerMessage response = new ServerMessage();
            response.type = ServerMessage.TYPE_CHALLENGE_STATE_CONFIRMATION;
            response.activeChallengeId = message.activeChallengeId;
            response.challengeStateConfirmation = new ChallengeStateConfirmation();
            response.challengeStateConfirmation.isValid = false;
            webSocketClient.sendTextMessage(gson.toJson(response));
            return;
        }

        final WebSocketEventHandler self = this;

        // Getting the challenge status is a blocking operation, so run it in a separate thread.
        new Thread(() -> {
            ServerMessage response = new ServerMessage();
            response.type = ServerMessage.TYPE_CHALLENGE_STATE_CONFIRMATION;
            response.activeChallengeId = message.activeChallengeId;

            RecordableChallenge activeChallenge = plugin.getActiveChallenge();
            if (activeChallenge == null) {
                response.challengeStateConfirmation = new ChallengeStateConfirmation();
                response.challengeStateConfirmation.isValid = false;
                webSocketClient.sendTextMessage(gson.toJson(response));
                return;
            }

            RecordableChallenge.Status status = null;

            try {
                status = activeChallenge.getStatus().get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Failed to get challenge status", e);
            }

            ChallengeStateConfirmation confirmationBuilder = new ChallengeStateConfirmation();
            confirmationBuilder.username = username;

            boolean isValid = false;

            if (status != null) {
                Set<String> party = status.getParty().stream().map(String::toLowerCase).collect(Collectors.toSet());
                Set<String> partyToConfirm = stateToConfirm.party != null
                        ? stateToConfirm.party.stream().map(String::toLowerCase).collect(Collectors.toSet())
                        : Collections.emptySet();

                isValid = status.getChallenge().getId() == stateToConfirm.challenge &&
                        status.getStage() != null &&
                        status.getStage().getId() >= stateToConfirm.stage &&
                        party.equals(partyToConfirm);

                if (!party.contains(username)) {
                    confirmationBuilder.spectator = true;
                }
            }

            confirmationBuilder.isValid = isValid;
            response.challengeStateConfirmation = confirmationBuilder;

            synchronized (self) {
                self.webSocketClient.sendTextMessage(gson.toJson(response));

                if (isValid) {
                    self.challengeId = message.activeChallengeId;
                    self.eventBuffer.setChallengeId(self.challengeId);
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

    private static int translateStageStatus(StageUpdateEvent.Status status) {
        switch (status) {
            case ENTERED:
                return ChallengeUpdate.StageUpdate.STATUS_ENTERED;
            case STARTED:
                return ChallengeUpdate.StageUpdate.STATUS_STARTED;
            case COMPLETED:
                return ChallengeUpdate.StageUpdate.STATUS_COMPLETED;
            case WIPED:
                return ChallengeUpdate.StageUpdate.STATUS_WIPED;
            default:
                throw new NotImplementedException("Stage status translation not implemented for " + status);
        }
    }
}