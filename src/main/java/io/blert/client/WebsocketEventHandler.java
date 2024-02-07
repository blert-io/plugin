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

import io.blert.events.Event;
import io.blert.events.EventHandler;
import io.blert.events.EventType;
import io.blert.json.JsonEventHandler;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class WebsocketEventHandler implements EventHandler {
    private final WebSocketClient webSocketClient;
    private final JsonEventHandler jsonEventHandler;

    private String raidId = null;

    private int currentTick = 0;

    /**
     * An `EventHandler` implementation that transmits received events to a blert server through a websocket.
     *
     * @param webSocketClient Websocket client connected and authenticated to the blert server.
     */
    public WebsocketEventHandler(WebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
        this.webSocketClient.setMessageCallback(this::handleMessage);
        jsonEventHandler = new JsonEventHandler();
    }

    @Override
    public void handleEvent(int clientTick, Event event) {
        switch (event.getType()) {
            case RAID_START: {
                // Starting a new raid. Discard any buffered events.
                jsonEventHandler.flushEventsUpTo(clientTick);

                var json = io.blert.json.Event.fromBlert(event).encode();
                webSocketClient.sendMessage(json);
                break;
            }

            case RAID_END: {
                // Flush any pending events, then indicate that the raid has ended.
                if (jsonEventHandler.hasEvents()) {
                    webSocketClient.sendMessage(jsonEventHandler.flushEventsUpTo(clientTick));
                }

                var evt = io.blert.json.Event.fromBlert(event);
                evt.setRaidId(raidId);
                webSocketClient.sendMessage(evt.encode());

                jsonEventHandler.setRaidId(null);
                raidId = null;
                break;
            }

            default:
                // Forward other events to the JSON handler to be serialized and sent to the server.
                jsonEventHandler.handleEvent(clientTick, event);

                if (webSocketClient.isOpen()) {
                    if (event.getType() == EventType.ROOM_STATUS) {
                        // Room status events indicate the start or completion of a room, and should be sent to the
                        // server immediately.
                        webSocketClient.sendMessage(jsonEventHandler.flushEventsUpTo(clientTick));
                    } else if (currentTick != clientTick) {
                        // All other events are collected and sent in a single batch at the end of a tick.
                        webSocketClient.sendMessage(jsonEventHandler.flushEventsUpTo(currentTick));
                    }
                }

                break;
        }

        currentTick = clientTick;
    }

    /**
     * Processes a message arriving from the server.
     *
     * @param message The serialized JSON message.
     */
    private void handleMessage(String message) {
        Gson gson = new Gson();
        ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);
        if (serverMessage.getType() == ServerMessage.Type.RAID_START_RESPONSE) {
            raidId = serverMessage.getRaidId();
            jsonEventHandler.setRaidId(raidId);
        }
    }
}
