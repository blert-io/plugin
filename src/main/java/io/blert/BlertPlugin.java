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

package io.blert;

import com.google.inject.Provides;
import io.blert.challenges.colosseum.ColosseumChallenge;
import io.blert.challenges.tob.TheatreChallenge;
import io.blert.client.WebSocketClient;
import io.blert.client.WebsocketEventHandler;
import io.blert.core.RecordableChallenge;
import io.blert.util.DeferredTask;
import io.blert.util.Location;
import joptsimple.internal.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@PluginDescriptor(
        name = "Blert"
)
public class BlertPlugin extends Plugin {
    static final String DEFAULT_BLERT_HOSTNAME = "blert.io";
    static final String DEFAULT_SERVER_HOSTNAME = "wave32.blert.io";

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private BlertConfig config;

    @Inject
    OkHttpClient httpClient;

    private BlertPluginPanel sidePanel;
    private NavigationButton sidePanelButton;

    private final List<RecordableChallenge> challenges = new ArrayList<>();
    @Getter
    private @Nullable RecordableChallenge activeChallenge = null;

    private WebsocketEventHandler handler;

    private GameState previousGameState = null;
    private boolean isLoggedIn = false;

    private DeferredTask deferredTask;

    @Getter(AccessLevel.MODULE)
    private WebSocketClient wsClient;

    @Override
    protected void startUp() throws Exception {
        sidePanel = new BlertPluginPanel(this, config);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/blert.png");
        sidePanelButton = NavigationButton.builder().tooltip("Blert").priority(6).icon(icon).panel(sidePanel).build();
        clientToolbar.addNavigation(sidePanelButton);
        sidePanel.startPanel();

        if (config.apiKey() != null && !config.dontConnect()) {
            initializeWebSocketClient();
            wsClient.open();
        }

        challenges.add(new TheatreChallenge(client, eventBus, clientThread));
        challenges.add(new ColosseumChallenge(client, eventBus, clientThread));

        previousGameState = client.getGameState();
        isLoggedIn = previousGameState == GameState.LOGGED_IN;
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolbar.removeNavigation(sidePanelButton);
        sidePanel.stopPanel();
        sidePanel = null;

        if (activeChallenge != null) {
            activeChallenge.terminate();
        }

        challenges.clear();

        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
    }

    @Subscribe(priority = 10)
    public void onGameTick(GameTick gameTick) {
        if (deferredTask != null) {
            deferredTask.tick();
        }

        updateActiveChallenge();

        if (activeChallenge != null) {
            activeChallenge.tick();
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged gameStateChanged) {
        GameState gameState = gameStateChanged.getGameState();
        if (gameState == previousGameState) {
            return;
        }

        if (gameState == GameState.LOGGED_IN) {
            if (!config.dontConnect() && config.apiKey() != null && !wsClient.isOpen()) {
                try {
                    wsClient.open().get();
                } catch (InterruptedException | ExecutionException e) {
                    isLoggedIn = true;
                }
            }

            // If the player was not already logged in, notify the server that they have.
            if (!isLoggedIn && handler != null) {
                deferredTask = new DeferredTask(() -> handler.updateGameState(GameState.LOGGED_IN), 3);
            }

            isLoggedIn = true;
        } else if (gameState == GameState.LOGIN_SCREEN) {
            if (isLoggedIn && handler != null) {
                handler.updateGameState(GameState.LOGIN_SCREEN);
            }

            isLoggedIn = false;
        }

        previousGameState = gameState;
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged changed) {
        String key = changed.getKey();
        if (key.equals("apiKey") || key.equals("serverUrl") || key.equals("dontConnect")) {
            new Thread(this::initializeWebSocketClient).start();
        }
    }

    private void initializeWebSocketClient() {
        if (wsClient != null) {
            sidePanel.updateUser(null);

            if (wsClient.isOpen()) {
                try {
                    wsClient.close().get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (config.dontConnect() || config.apiKey() == null) {
            return;
        }

        String hostname = config.serverUrl();
        if (Strings.isNullOrEmpty(hostname)) {
            hostname = DEFAULT_SERVER_HOSTNAME;
        }

        if (!hostname.startsWith("ws://") && !hostname.startsWith("wss://")) {
            hostname = "wss://" + hostname;
        }

        wsClient = new WebSocketClient(hostname, config.apiKey(), httpClient);
        handler = new WebsocketEventHandler(this, wsClient, sidePanel, client, clientThread);

        if (activeChallenge != null) {
            activeChallenge.setEventHandler(handler);
        }
    }

    @Provides
    BlertConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BlertConfig.class);
    }

    /**
     * Updates the active challenge based on the player's game location.
     */
    private void updateActiveChallenge() {
        WorldPoint playerLocation = Location.getWorldLocation(client, client.getLocalPlayer().getWorldLocation());

        var maybeChallenge = challenges.stream().filter(c -> c.containsLocation(playerLocation)).findFirst();
        if (maybeChallenge.isPresent()) {
            RecordableChallenge challenge = maybeChallenge.get();
            if (activeChallenge == challenge) {
                return;
            }

            if (activeChallenge != null) {
                activeChallenge.terminate();
            }

            activeChallenge = challenge;
            activeChallenge.initialize(handler);

            log.info("Entered challenge \"{}\"", activeChallenge.getName());
        } else if (activeChallenge != null) {
            log.info("Exited challenge \"{}\"", activeChallenge.getName());

            activeChallenge.terminate();
            activeChallenge = null;
        }
    }
}
