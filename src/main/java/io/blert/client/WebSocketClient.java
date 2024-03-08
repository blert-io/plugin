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

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.internal.annotations.EverythingIsNonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;

@Slf4j

public class WebSocketClient extends WebSocketListener implements AutoCloseable {
    @NotNull
    private final String hostname;
    private final byte[] apiKey;
    private final OkHttpClient client;
    private WebSocket socket;

    @Setter
    private @Nullable Consumer<String> messageCallback = null;

    @Setter
    private @Nullable Runnable disconnectCallback = null;

    public WebSocketClient(@NotNull String hostname, @NotNull String apiKey) {
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
        this.hostname = hostname;
        this.client = new OkHttpClient.Builder().build();
    }

    /**
     * Checks if the socket is connected.
     *
     * @return True if the websocket is connected and messages can be sent or received.
     */
    public boolean isOpen() {
        return socket != null;
    }

    public void open() {
        Request request = new Request.Builder()
                .url("ws://" + hostname + "/ws")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(apiKey))
                .build();
        socket = client.newWebSocket(request, this);
    }

    /**
     * Sends a text message through the open websocket.
     *
     * @param message The text to send.
     */
    public void sendMessage(String message) {
        if (isOpen()) {
            socket.send(message);
        }
    }

    @Override
    @EverythingIsNonNull
    public void onOpen(WebSocket webSocket, Response response) {
        log.info("Blert websocket {} opened", webSocket);
    }

    @Override
    @EverythingIsNonNull
    public void onMessage(WebSocket webSocket, String text) {
        log.debug("Blert websocket {} received message {}", webSocket, text);
        if (this.messageCallback != null) {
            this.messageCallback.accept(text);
        }
    }

    @Override
    @EverythingIsNonNull
    public void onClosed(WebSocket webSocket, int status, String reason) {
        log.info("Blert websocket {} closed: {} ({})", webSocket, status, reason);
        socket = null;
        onDisconnect();
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {
        log.warn("Blert websocket {} failed: {}", webSocket, response, t);
        socket = null;
        onDisconnect();
    }

    @Override
    public void close() throws Exception {
        if (socket != null) {
            socket.close(1000, null);
            onDisconnect();
        }
    }

    private void onDisconnect() {
        if (this.disconnectCallback != null) {
            this.disconnectCallback.run();
        }
    }
}
