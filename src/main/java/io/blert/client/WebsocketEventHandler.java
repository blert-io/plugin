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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    private Status status = Status.IDLE;
    private Challenge currentChallenge = null;
    private @Nullable String challengeId = null;

    private int currentTick = 0;

    /**
     * An `EventHandler` implementation that transmits received events to a blert server through a websocket.
     *
     * @param webSocketClient Websocket client connected and authenticated to the blert server.
     */
    public WebsocketEventHandler(WebSocketClient webSocketClient, BlertPluginPanel sidePanel) {
        this.webSocketClient = webSocketClient;
        this.webSocketClient.setBinaryMessageCallback(this::handleProtoMessage);
        this.webSocketClient.setDisconnectCallback(this::handleDisconnect);
        this.protoEventHandler = new ProtoEventHandler();
        this.sidePanel = sidePanel;
    }

    @Override
    public void handleEvent(int clientTick, Event event) {
        switch (event.getType()) {
            case CHALLENGE_START: {
                // Starting a new raid. Discard any buffered events.
                protoEventHandler.flushEventsUpTo(clientTick);

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
                // Currently unused.
                break;

            case CONNECTION_RESPONSE:
                if (serverMessage.hasUser()) {
                    sidePanel.updateUser(serverMessage.getUser().getName());
                    sendRaidHistoryRequest();
                } else {
                    log.warn("Received invalid connection response from server");
                    try {
                        webSocketClient.close().get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                    sidePanel.updateUser(null);
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

    private void handleDisconnect() {
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
}