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
import io.blert.client.WebsocketManager;
import io.blert.core.RecordableChallenge;
import io.blert.util.DeferredTask;
import io.blert.util.Location;
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
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

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
    private WebsocketManager websocketManager;

    @Getter
    private BlertPluginPanel sidePanel;
    private NavigationButton sidePanelButton;

    private final List<RecordableChallenge> challenges = new ArrayList<>();
    @Getter
    private @Nullable RecordableChallenge activeChallenge = null;

    private GameState previousGameState = null;
    private boolean isLoggedIn = false;

    private DeferredTask deferredTask;

    @Override
    protected void startUp() throws Exception {
        eventBus.register(websocketManager);
        websocketManager.open();

        sidePanel = new BlertPluginPanel(config, websocketManager);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/blert.png");
        sidePanelButton = NavigationButton.builder().tooltip("Blert").priority(6).icon(icon).panel(sidePanel).build();
        clientToolbar.addNavigation(sidePanelButton);
        sidePanel.startPanel();

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

        websocketManager.close();
        eventBus.unregister(websocketManager);
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
            if (config.apiKey() != null && !websocketManager.isOpen()) {
                try {
                    websocketManager.open().get();
                } catch (InterruptedException | ExecutionException e) {
                    isLoggedIn = true;
                }
            }

            // If the player was not already logged in, notify the server that they have.
            if (!isLoggedIn) {
                deferredTask = new DeferredTask(() -> {
                    if (websocketManager.getEventHandler() != null) {
                        websocketManager.getEventHandler().updateGameState(GameState.LOGGED_IN);
                    }
                }, 3);
            }

            isLoggedIn = true;
        } else if (gameState == GameState.LOGIN_SCREEN) {
            if (isLoggedIn && websocketManager.getEventHandler() != null) {
                websocketManager.getEventHandler().updateGameState(GameState.LOGIN_SCREEN);
            }

            isLoggedIn = false;
        }

        previousGameState = gameState;
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
            activeChallenge.initialize(websocketManager.getEventHandler());

            log.info("Entered challenge \"{}\"", activeChallenge.getName());
        } else if (activeChallenge != null) {
            log.info("Exited challenge \"{}\"", activeChallenge.getName());

            activeChallenge.terminate();
            activeChallenge = null;
        }
    }
}
