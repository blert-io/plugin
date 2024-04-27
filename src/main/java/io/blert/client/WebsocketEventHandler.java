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
import io.blert.BlertPluginPanel;
import io.blert.core.Challenge;
import io.blert.events.ChallengeStartEvent;
import io.blert.events.Event;
import io.blert.events.EventHandler;
import io.blert.events.EventType;
import io.blert.proto.EventTranslator;
import io.blert.proto.ProtoEventHandler;
import io.blert.proto.ServerMessage;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Slf4j
public class WebsocketEventHandler implements EventHandler {
    public enum Status {
        IDLE,
        CHALLENGE_STARTING,
        CHALLENGE_ACTIVE,
    }

    private final WebSocketClient webSocketClient;
    private final ProtoEventHandler protoEventHandler;
    private final BlertPluginPanel sidePanel;
    private final Client runeliteClient;
    private final ClientThread runeliteThread;

    private Status status = Status.IDLE;
    private Challenge currentChallenge = null;
    private @Nullable String challengeId = null;
    private Instant serverShutdownTime = null;
    private boolean apiKeyUsernameMismatch = false;

    private int currentTick = 0;

    /**
     * An `EventHandler` implementation that transmits received events to a blert server through a websocket.
     *
     * @param webSocketClient Websocket client connected and authenticated to the blert server.
     */
    public WebsocketEventHandler(WebSocketClient webSocketClient, BlertPluginPanel sidePanel,
                                 Client client, ClientThread runeliteThread) {
        this.webSocketClient = webSocketClient;
        this.webSocketClient.setBinaryMessageCallback(this::handleProtoMessage);
        this.webSocketClient.setDisconnectCallback(this::reset);
        this.protoEventHandler = new ProtoEventHandler();
        this.sidePanel = sidePanel;
        this.runeliteClient = client;
        this.runeliteThread = runeliteThread;
    }

    @Override
    public void handleEvent(int clientTick, Event event) {
        switch (event.getType()) {
            case CHALLENGE_START: {
                // Starting a new raid. Discard any buffered events.
                protoEventHandler.flushEventsUpTo(clientTick);

                if (pendingServerShutdown()) {
                    sendGameMessage(
                            "<col=ef1020>This challenge will not be recorded due to scheduled Blert maintenance.</col>"
                    );
                    break;
                }

                if (apiKeyUsernameMismatch) {
                    sendGameMessage(
                            "<col=ef1020>This challenge will not be recorded as this API key is linked to a different OSRS account. " +
                                    "If you changed your display name, please update it on the Blert website.</col>"
                    );
                    break;
                }

                if (webSocketClient.isOpen()) {
                    sendEvents(EventTranslator.toProto(event, null));
                    setStatus(Status.CHALLENGE_STARTING);
                    this.currentChallenge = ((ChallengeStartEvent) event).getChallenge();
                }
                break;
            }

            case CHALLENGE_END: {
                // Flush any pending events, then indicate that the raid has ended.
                if (protoEventHandler.hasEvents()) {
                    sendEvents(protoEventHandler.flushEventsUpTo(clientTick));
                }

                sendEvents(EventTranslator.toProto(event, challengeId));

                protoEventHandler.setChallengeId(null);
                challengeId = null;
                currentChallenge = null;
                setStatus(Status.IDLE);

                sendRaidHistoryRequest();
                break;
            }

            default:
                // Forward other events to the protobuf handler to be serialized and sent to the server.
                protoEventHandler.handleEvent(clientTick, event);

                if (status == Status.CHALLENGE_ACTIVE) {
                    if (event.getType() == EventType.STAGE_UPDATE) {
                        // Room status events indicate the start or completion of a room, and should be sent to the
                        // server immediately.
                        sendEvents(protoEventHandler.flushEventsUpTo(clientTick));
                    } else if (currentTick != clientTick) {
                        // All other events are collected and sent in a single batch at the end of a tick.
                        sendEvents(protoEventHandler.flushEventsUpTo(clientTick));
                    }
                }

                break;
        }

        currentTick = clientTick;
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
                    .setOverallExperience(runeliteClient.getOverallExperience());
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
                handleServerError(serverMessage.getError());
                break;

            case CONNECTION_RESPONSE:
                serverShutdownTime = null;
                sidePanel.setShutdownTime(null);

                if (serverMessage.hasUser()) {
                    sidePanel.updateUser(serverMessage.getUser().getName());
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
                sidePanel.setRecentRecordings(serverMessage.getRecentRecordingsList());
                break;

            case ACTIVE_CHALLENGE_INFO:
                if (status != Status.CHALLENGE_STARTING) {
                    log.warn("Received unexpected raid start response from server");
                    return;
                }

                if (!serverMessage.hasActiveChallengeId()) {
                    log.warn("Failed to start raid");
                    protoEventHandler.setChallengeId(null);
                    currentChallenge = null;
                    challengeId = null;
                    setStatus(Status.IDLE);
                    return;
                }

                challengeId = serverMessage.getActiveChallengeId();

                protoEventHandler.setChallengeId(challengeId);
                setStatus(Status.CHALLENGE_ACTIVE);

                if (protoEventHandler.hasEvents()) {
                    sendEvents(protoEventHandler.flushEventsUpTo(currentTick));
                }
                break;

            case SERVER_STATUS: {
                var serverStatus = serverMessage.getServerStatus();
                switch (serverStatus.getStatus()) {
                    case SHUTDOWN_PENDING: {
                        var shutdownTime = serverStatus.getShutdownTime();
                        serverShutdownTime = Instant.ofEpochSecond(shutdownTime.getSeconds(), shutdownTime.getNanos());
                        Duration timeUntilShutdown = Duration.between(Instant.now(), serverShutdownTime);
                        sidePanel.setShutdownTime(serverShutdownTime);

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
                        sidePanel.setShutdownTime(null);
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

            case PONG:
            case HISTORY_REQUEST:
            case EVENT_STREAM:
                log.warn("Received unexpected protobuf message from server: {}", serverMessage.getType());
                break;

            case UNRECOGNIZED:
                log.warn("Received unrecognized protobuf message from server");
                break;
        }
    }

    private void handleServerError(ServerMessage.Error error) {
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
                break;

            case UNAUTHENTICATED:
                log.info("Disconnected from server due to authentication failure");
                closeWebsocketClient();
                break;

            case UNKNOWN:
            case UNRECOGNIZED:
                log.error("Received unrecognized server errror {}", error.getTypeValue());
                break;
        }
    }

    private void sendEvents(io.blert.proto.Event event) {
        sendEvents(Collections.singletonList(event));
    }

    private void sendEvents(List<io.blert.proto.Event> events) {
        if (webSocketClient.isOpen()) {
            ServerMessage message = ServerMessage.newBuilder()
                    .setType(ServerMessage.Type.EVENT_STREAM)
                    .addAllChallengeEvents(events)
                    .build();
            webSocketClient.sendMessage(message.toByteArray());
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

    private void reset() {
        currentChallenge = null;
        challengeId = null;
        protoEventHandler.setChallengeId(null);
        setStatus(Status.IDLE);
        sidePanel.updateUser(null);
        sidePanel.setRecentRecordings(null);
    }

    private void setStatus(Status status) {
        this.status = status;
        var challenge = currentChallenge != null
                ? currentChallenge.toProto()
                : io.blert.proto.Challenge.UNKNOWN_CHALLENGE;
        sidePanel.updateChallengeStatus(status, challenge, challengeId);
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
        sidePanel.setShutdownTime(null);
    }
}