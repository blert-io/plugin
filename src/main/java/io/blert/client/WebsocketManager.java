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
import lombok.AccessLevel;
import lombok.Getter;
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

public class WebsocketManager {
    public static final String DEFAULT_BLERT_HOST = "https://blert.io";
    public static final String DEFAULT_SERVER_HOST = "wss://wave32.blert.io";

    @Inject
    private BlertPlugin plugin;

    @Inject
    private BlertConfig config;

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private Client runeLiteClient;

    @Inject
    private ClientThread runeLiteClientThread;

    @Inject
    @Named("developerMode")
    boolean developerMode;

    @Getter(AccessLevel.MODULE)
    private WebSocketClient wsClient;
    @Getter
    private WebsocketEventHandler eventHandler;

    public Future<Boolean> open() {
        if (config.apiKey() == null) {
            return CompletableFuture.completedFuture(false);
        }

        initializeWebSocketClient();
        return wsClient.open();
    }

    public boolean isOpen() {
        return wsClient != null && wsClient.isOpen();
    }

    public void close() {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
    }

    private void initializeWebSocketClient() {
        if (wsClient != null) {
            plugin.getSidePanel().updateUser(null);

            if (wsClient.isOpen()) {
                try {
                    wsClient.close().get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (config.apiKey() == null) {
            return;
        }

        String runeliteVersion = String.format(
                "runelite-%s%s", RuneLiteProperties.getVersion(), developerMode ? "-dev" : "");
        wsClient = new WebSocketClient(DEFAULT_SERVER_HOST, config.apiKey(), runeliteVersion, httpClient);
        eventHandler = new WebsocketEventHandler(plugin, wsClient, runeLiteClient, runeLiteClientThread);

        if (plugin.getActiveChallenge() != null) {
            plugin.getActiveChallenge().setEventHandler(eventHandler);
        }
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged changed) {
        String key = changed.getKey();
        if (key.equals("apiKey")) {
            new Thread(this::initializeWebSocketClient).start();
        }
    }
}
