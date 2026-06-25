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
import net.runelite.client.events.ConfigChanged;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.*;

@Slf4j
public class WebSocketManager {
    public static final String DEFAULT_BLERT_HOST = "https://blert.io";
    public static final String DEFAULT_SERVER_HOST = "wss://wave32.blert.io";

    // Drain reconnects are spread over a short random delay so a draining
    // instance's clients don't all reconnect at once.
    private static final long RECONNECT_MIN_DELAY_MS = 500;
    private static final long RECONNECT_MAX_DELAY_MS = 2500;

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
    private ScheduledExecutorService executor;

    @Inject
    @Named("developerMode")
    boolean developerMode;

    @Getter(AccessLevel.MODULE)
    private volatile WebSocketClient wsClient;
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
        if (eventHandler != null) {
            eventHandler.shutdown();
        }

        if (wsClient != null && wsClient.isOpen()) {
            var result = wsClient.close();
            wsClient = null;
            return result;
        }

        return CompletableFuture.completedFuture(null);
    }

    private void initializeWebSocketClient() {
        if (eventHandler != null) {
            eventHandler.shutdown();
        }

        if (wsClient != null) {
            // Detach the retiring client so it can no longer drive the outgoing handler.
            wsClient.setTextMessageCallback(null);
            wsClient.setDisconnectCallback(null);

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
        WebSocketEventHandler newEventHandler = new WebSocketEventHandler(
                plugin, wsClient, runeliteClient, runeLiteClientThread, this::reconnect);

        if (plugin.getActiveChallenge() != null) {
            plugin.getActiveChallenge().removeEventHandler(eventHandler);
            plugin.getActiveChallenge().addEventHandler(newEventHandler);
        }

        eventHandler = newEventHandler;
    }

    private void reconnect() {
        WebSocketClient draining = wsClient;
        long delayMs = ThreadLocalRandom.current().nextLong(RECONNECT_MIN_DELAY_MS, RECONNECT_MAX_DELAY_MS);
        executor.schedule(() -> {
            // Only reconnect if the draining connection is still the live one.
            if (draining != null && wsClient == draining && draining.isOpen()) {
                log.info("Reconnecting to Blert in response to drain request");
                open();
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    public void onConfigChanged(ConfigChanged changed) {
        if (!changed.getGroup().equals("blert")) {
            return;
        }

        String key = changed.getKey();
        if (key.equals("apiKey")) {
            plugin.getSidePanel().updateConnectionState(BlertPluginPanel.ConnectionState.DISCONNECTED, null);
            open();
        }
    }
}
