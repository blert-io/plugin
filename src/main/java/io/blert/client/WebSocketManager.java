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

import io.blert.BlertConfig;
import io.blert.BlertPlugin;
import io.blert.BlertPluginPanel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Slf4j
public class WebSocketManager {
    public static final String DEFAULT_BLERT_HOST = "https://blert.io";
    public static final String DEFAULT_SERVER_HOST = "wss://wave32.blert.io";

    @Inject
    private BlertPlugin plugin;

    @Inject
    private BlertConfig config;

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private Client runeliteClient;

    @Inject
    private ClientThread runeLiteClientThread;

    @Inject
    @Named("developerMode")
    boolean developerMode;

    @Getter(AccessLevel.MODULE)
    private WebSocketClient wsClient;
    @Getter
    private WebSocketEventHandler eventHandler;

    public Future<Boolean> open() {
        if (config.apiKey() == null) {
            return CompletableFuture.completedFuture(false);
        }

        initializeWebSocketClient();
        return wsClient.open();
    }

    public boolean shouldTryToConnect() {
        if (wsClient == null) {
            return true;
        }

        return wsClient.getState() == WebSocketClient.State.CLOSED;
    }

    public Future<Void> close() {
        if (wsClient != null && wsClient.isOpen()) {
            var result = wsClient.close();
            wsClient = null;
            return result;
        }

        return CompletableFuture.completedFuture(null);
    }

    private void initializeWebSocketClient() {
        if (wsClient != null) {
            if (plugin != null && plugin.getSidePanel() != null) {
                plugin.getSidePanel().updateConnectionState(BlertPluginPanel.ConnectionState.DISCONNECTED, null);
            }

            if (wsClient.isOpen()) {
                wsClient.close();
            }

            wsClient = null;
        }

        if (config.apiKey() == null) {
            return;
        }

        String runeliteVersion = String.format(
                "runelite-%s%s", RuneLiteProperties.getVersion(), developerMode ? "-dev" : "");
        wsClient = new WebSocketClient(DEFAULT_SERVER_HOST, config.apiKey(), runeliteVersion, httpClient);
        eventHandler = new WebSocketEventHandler(plugin, wsClient, runeliteClient, runeLiteClientThread);

        if (plugin.getActiveChallenge() != null) {
            plugin.getActiveChallenge().setEventHandler(eventHandler);
        }
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged changed) {
        if (!changed.getGroup().equals("blert")) {
            return;
        }

        String key = changed.getKey();
        if (key.equals("apiKey")) {
            new Thread(() -> {
                plugin.getSidePanel().updateConnectionState(BlertPluginPanel.ConnectionState.DISCONNECTED, null);
                try {
                    open().get(10, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Failed to open WebSocket connection after API key change", e);
                }
            }).start();
        }
    }
}
