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

import com.google.gson.Gson;
import io.blert.BlertPluginPanel;
import io.blert.events.Event;
import io.blert.events.EventHandler;
import io.blert.events.EventType;
import io.blert.json.JsonEventHandler;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Slf4j

public class WebsocketEventHandler implements EventHandler {
    public enum Status {
        IDLE,
        RAID_STARTING,
        RAID_ACTIVE,
    }

    private final WebSocketClient webSocketClient;
    private final JsonEventHandler jsonEventHandler;
    private final BlertPluginPanel sidePanel;

    private Status status = Status.IDLE;
    private @Nullable String raidId = null;

    private int currentTick = 0;

    /**
     * An `EventHandler` implementation that transmits received events to a blert server through a websocket.
     *
     * @param webSocketClient Websocket client connected and authenticated to the blert server.
     */
    public WebsocketEventHandler(WebSocketClient webSocketClient, BlertPluginPanel sidePanel) {
        this.webSocketClient = webSocketClient;
        this.webSocketClient.setMessageCallback(this::handleMessage);
        this.webSocketClient.setDisconnectCallback(this::handleDisconnect);
        jsonEventHandler = new JsonEventHandler();
        this.sidePanel = sidePanel;
    }

    @Override
    public void handleEvent(int clientTick, Event event) {
        switch (event.getType()) {
            case RAID_START: {
                // Starting a new raid. Discard any buffered events.
                jsonEventHandler.flushEventsUpTo(clientTick);

                if (webSocketClient.isOpen()) {
                    var jsonEvent = io.blert.json.Event.fromBlert(event);
                    sendRaidEvents(jsonEvent);
                    setStatus(Status.RAID_STARTING);
                }
                break;
            }

            case RAID_END: {
                // Flush any pending events, then indicate that the raid has ended.
                if (jsonEventHandler.hasEvents()) {
                    sendRaidEvents(jsonEventHandler.flushEventsUpTo(clientTick));
                }

                var evt = io.blert.json.Event.fromBlert(event);
                evt.setRaidId(raidId);

                sendRaidEvents(evt);

                setStatus(Status.IDLE);
                jsonEventHandler.setRaidId(null);
                raidId = null;

                sendRaidHistoryRequest();
                break;
            }

            default:
                // Forward other events to the JSON handler to be serialized and sent to the server.
                jsonEventHandler.handleEvent(clientTick, event);

                if (status == Status.RAID_ACTIVE) {
                    if (event.getType() == EventType.ROOM_STATUS) {
                        // Room status events indicate the start or completion of a room, and should be sent to the
                        // server immediately.
                        sendRaidEvents(jsonEventHandler.flushEventsUpTo(clientTick));
                    } else if (currentTick != clientTick) {
                        // All other events are collected and sent in a single batch at the end of a tick.
                        sendRaidEvents(jsonEventHandler.flushEventsUpTo(clientTick));
                    }
                }

                break;
        }

        currentTick = clientTick;
    }

    private void sendRaidEvents(io.blert.json.Event event) {
        sendRaidEvents(Collections.singletonList(event));
    }

    private void sendRaidEvents(List<io.blert.json.Event> events) {
        if (webSocketClient.isOpen()) {
            ServerMessage message = new ServerMessage(ServerMessage.Type.RAID_EVENTS);
            message.setEvents(events);
            webSocketClient.sendMessage(message.encode());
        }
    }

    private void sendRaidHistoryRequest() {
        if (webSocketClient.isOpen()) {
            ServerMessage message = new ServerMessage(ServerMessage.Type.RAID_HISTORY_REQUEST);
            webSocketClient.sendMessage(message.encode());
        }
    }

    /**
     * Processes a message arriving from the server.
     *
     * @param message The serialized JSON message.
     */
    private void handleMessage(String message) {
        Gson gson = new Gson();
        ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);

        switch (serverMessage.getType()) {
            case CONNECTION_RESPONSE:
                ServerMessage.User user = serverMessage.getUser();
                if (user != null) {
                    sidePanel.updateUser(user.getName());
                    sendRaidHistoryRequest();
                } else {
                    log.warn("Received invalid connection response from server");
                    try {
                        webSocketClient.close();
                    } catch (Exception e) {
                        // Ignore.
                    }
                    sidePanel.updateUser(null);
                }
                break;

            case HEARTBEAT_PING:
                webSocketClient.sendMessage(new ServerMessage(ServerMessage.Type.HEARTBEAT_PONG).encode());
                log.debug("Received heartbeat ping from server; responding with pong");
                break;

            case RAID_HISTORY_RESPONSE:
                sidePanel.setRaidHistory(serverMessage.getHistory());
                break;

            case RAID_START_RESPONSE:
                if (status != Status.RAID_STARTING) {
                    log.warn("Received unexpected raid start response from server");
                    return;
                }

                raidId = serverMessage.getRaidId();
                jsonEventHandler.setRaidId(raidId);
                setStatus(Status.RAID_ACTIVE);

                if (jsonEventHandler.hasEvents()) {
                    sendRaidEvents(jsonEventHandler.flushEventsUpTo(currentTick));
                }
                break;

            default:
                log.warn("Received unexpected message from server: {}", serverMessage.getType());
                break;
        }
    }

    private void handleDisconnect() {
        raidId = null;
        jsonEventHandler.setRaidId(null);
        setStatus(Status.IDLE);
        sidePanel.updateUser(null);
        sidePanel.setRaidHistory(null);
    }

    private void setStatus(Status status) {
        this.status = status;
        sidePanel.updateRaidStatus(status, raidId);
    }
}
