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

import io.blert.BuildProperties;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.internal.annotations.EverythingIsNonNull;

@Slf4j
public class WebSocketClient extends WebSocketListener {
    private static final String PROTOCOL_JSON = "blert-json";
    private static final String PROTOCOL_JSON_DEFLATE = "blert-json-deflate";

    public enum State {
        CLOSED,
        OPEN,
        CONNECTING,
        CLOSING,
        REJECTED,
    }

    public enum DisconnectReason {
        CLOSED_SUCCESSFULLY,
        UNSUPPORTED_VERSION,
        ERROR,
    }

    @NonNull
    private final String hostname;

    private final byte[] apiKey;
    private final String runeliteVersion;
    private final OkHttpClient client;
    private WebSocket socket;
    private State state = State.CLOSED;

    // Set when the server accepts blert-json-deflate. Every message sent on the connection shares its stream.
    private @Nullable Deflater deflater = null;
    private final byte[] deflateBuffer = new byte[8192]; // arbitrary size

    private final List<CompletableFuture<Boolean>> openFutures = new ArrayList<>();
    private final List<CompletableFuture<Void>> closeFutures = new ArrayList<>();

    @Setter
    private @Nullable Consumer<byte[]> binaryMessageCallback = null;

    @Setter
    private @Nullable Consumer<String> textMessageCallback = null;

    @Setter
    private @Nullable Consumer<DisconnectReason> disconnectCallback = null;

    public WebSocketClient(
            @NonNull String hostname, @NonNull String apiKey, @NonNull String runeliteVersion, OkHttpClient client) {
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
        this.hostname = hostname;
        this.runeliteVersion = runeliteVersion;
        this.client = client;
    }

    /**
     * Checks if the socket is connected.
     *
     * @return True if the websocket is connected and messages can be sent or received.
     */
    public synchronized boolean isOpen() {
        return state == State.OPEN;
    }

    /**
     * Gets the current state of the websocket connection.
     *
     * @return Connection state.
     */
    public synchronized State getState() {
        return state;
    }

    /**
     * Opens the websocket connection to the configured server.
     *
     * @return A future that resolves to true if the connection was successful, or false if not.
     */
    public Future<Boolean> open() {
        if (state == State.REJECTED) {
            // The WebSocketClient is tied to a client/API key configuration, so if the connection was rejected,
            // all subsequent attempts will also be rejected.
            log.warn("Ignoring websocket reconnection attempt after rejection");
            return CompletableFuture.completedFuture(false);
        }

        Request.Builder request = new Request.Builder()
                .url(hostname)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(apiKey))
                .header("Sec-WebSocket-Protocol", PROTOCOL_JSON_DEFLATE + ", " + PROTOCOL_JSON)
                .header("Blert-Version", BuildProperties.VERSION)
                .header("Blert-Revision", BuildProperties.revision())
                .header("Blert-Jar-Hash", BuildProperties.jarHash())
                .header("Blert-Runelite-Version", runeliteVersion);

        for (String header : BuildProperties.customHeaders()) {
            String[] parts = header.split("=", 2);
            if (parts.length == 2) {
                request.header(parts[0], parts[1]);
            }
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        synchronized (this) {
            openFutures.add(future);
            socket = client.newWebSocket(request.build(), this);
            state = State.CONNECTING;
        }

        return future;
    }

    /**
     * Sends a text message through the open websocket.
     *
     * @param message The text to send.
     */
    public synchronized void sendTextMessage(String message) {
        if (!isOpen()) {
            return;
        }
        if (deflater == null) {
            socket.send(message);
            return;
        }
        socket.send(okio.ByteString.of(deflate(deflater, deflateBuffer, message)));
    }

    @Override
    @EverythingIsNonNull
    public synchronized void onOpen(WebSocket webSocket, Response response) {
        String protocol = response.header("Sec-WebSocket-Protocol");
        log.info("Blert websocket {} opened with protocol {}", webSocket, protocol);
        if (PROTOCOL_JSON_DEFLATE.equals(protocol)) {
            deflater = new Deflater(Deflater.BEST_SPEED, true);
        }
        state = State.OPEN;
        openFutures.forEach(future -> future.complete(true));
        openFutures.clear();
    }

    @Override
    @EverythingIsNonNull
    public void onMessage(WebSocket webSocket, okio.ByteString bytes) {
        if (!isOpen()) {
            return;
        }
        if (this.binaryMessageCallback != null) {
            this.binaryMessageCallback.accept(bytes.toByteArray());
        }
    }

    @Override
    @EverythingIsNonNull
    public void onMessage(WebSocket webSocket, String text) {
        if (!isOpen()) {
            return;
        }
        log.debug("Blert websocket {} received message {}", webSocket, text);
        if (this.textMessageCallback != null) {
            this.textMessageCallback.accept(text);
        }
    }

    @Override
    @EverythingIsNonNull
    public synchronized void onClosed(WebSocket webSocket, int status, String reason) {
        log.info("Blert websocket {} closed: {} ({})", webSocket, status, reason);
        state = State.CLOSED;
        socket = null;
        endDeflater();
        closeFutures.forEach(future -> future.complete(null));
        closeFutures.clear();
        onDisconnect(DisconnectReason.CLOSED_SUCCESSFULLY);
    }

    @Override
    @EverythingIsNonNull
    public void onClosing(WebSocket webSocket, int code, String reason) {
        log.info("Blert websocket {} closed by server: {} ({})", webSocket, code, reason);
        webSocket.close(code, reason);
    }

    @Override
    public synchronized void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
        // Capture previous state before modifying to properly notify listeners.
        State previousState = state;

        if (state == State.CONNECTING) {
            openFutures.forEach(future -> future.complete(false));
            openFutures.clear();
        }

        state = State.CLOSED;
        socket = null;
        endDeflater();

        if (response != null) {
            switch (response.code()) {
                case 403:
                    state = State.REJECTED;
                    log.warn("Blert websocket {} rejected: unsupported version", webSocket);
                    onDisconnect(DisconnectReason.UNSUPPORTED_VERSION);
                    break;
                case 401:
                    state = State.REJECTED;
                    log.warn("Blert websocket {} rejected: (unauthorized)", webSocket);
                    onDisconnect(DisconnectReason.ERROR);
                    break;
                default:
                    log.error("Blert websocket {} failed: {}", webSocket, response, t);
                    if (previousState == State.OPEN || previousState == State.CONNECTING) {
                        onDisconnect(DisconnectReason.ERROR);
                    }
                    break;
            }
        } else {
            log.error("Blert websocket {} failed", webSocket, t);
            if (previousState == State.OPEN || previousState == State.CONNECTING) {
                onDisconnect(DisconnectReason.ERROR);
            }
        }
    }

    public Future<Void> close() {
        if (socket != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            closeFutures.add(future);
            socket.close(1000, null);
            state = State.CLOSING;
            return future;
        }

        return CompletableFuture.completedFuture(null);
    }

    private void onDisconnect(DisconnectReason reason) {
        if (this.disconnectCallback != null) {
            this.disconnectCallback.accept(reason);
        }
    }

    private void endDeflater() {
        if (deflater != null) {
            deflater.end();
            deflater = null;
        }
    }

    static byte[] deflate(Deflater deflater, byte[] buffer, String message) {
        deflater.setInput(message.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        int n;
        do {
            n = deflater.deflate(buffer, 0, buffer.length, Deflater.SYNC_FLUSH);
            compressed.write(buffer, 0, n);
        } while (n == buffer.length);
        return compressed.toByteArray();
    }
}
