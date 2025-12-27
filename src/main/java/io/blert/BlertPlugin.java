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
import io.blert.client.WebSocketManager;
import io.blert.core.AttackRegistry;
import io.blert.core.RecordableChallenge;
import io.blert.util.DeferredTask;
import io.blert.util.Location;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
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
    private WebSocketManager websocketManager;

    @Getter
    private BlertPluginPanel sidePanel;
    private NavigationButton sidePanelButton;

    @Getter
    private final AttackRegistry attackRegistry = new AttackRegistry();

    private final List<RecordableChallenge> challenges = new ArrayList<>();
    @Getter
    private @Nullable RecordableChallenge activeChallenge = null;

    private enum LoginState {
        LOGGED_IN,
        JUST_LOGGED_IN,
        LOGGED_OUT;

        LoginState logIn() {
            if (this == LOGGED_OUT) {
                log.info("LOGGED_OUT -> JUST_LOGGED_IN");
                return JUST_LOGGED_IN;
            }
            return LOGGED_IN;
        }

        LoginState update() {
            if (this == JUST_LOGGED_IN) {
                log.info("state JUST_LOGGED_IN -> LOGGED_IN");
                return LOGGED_IN;
            }
            return this;
        }

        boolean isLoggedIn() {
            return this != LOGGED_OUT;
        }
    }

    private GameState previousGameState = null;
    private LoginState loginState = LoginState.LOGGED_OUT;
    private boolean enabled = true;

    private DeferredTask deferredTask;

    @Override
    protected void startUp() throws Exception {
        attackRegistry.loadDefaults();

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
        loginState = previousGameState == GameState.LOGGED_IN ? LoginState.LOGGED_IN : LoginState.LOGGED_OUT;

        if (loginState.isLoggedIn()) {
            checkWorldType();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        websocketManager.close();
        eventBus.unregister(websocketManager);

        clientToolbar.removeNavigation(sidePanelButton);
        sidePanel.stopPanel();
        sidePanel = null;

        if (activeChallenge != null) {
            eventBus.unregister(activeChallenge);
            activeChallenge.terminate();
            activeChallenge = null;
        }

        challenges.clear();
    }

    @Subscribe(priority = 10)
    public void onGameTick(GameTick gameTick) {
        if (deferredTask != null) {
            deferredTask.tick();
        }

        if (loginState == LoginState.JUST_LOGGED_IN) {
            // Resume the active challenge at the end of the login tick.
            if (activeChallenge != null) {
                eventBus.register(activeChallenge);
            }
        } else {
            updateActiveChallenge();

            if (activeChallenge != null) {
                activeChallenge.tick();
            }
        }

        loginState = loginState.update();
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged gameStateChanged) {
        GameState gameState = gameStateChanged.getGameState();
        if (gameState == previousGameState) {
            return;
        }

        if (gameState == GameState.LOGGED_IN) {
            if (config.apiKey() != null && websocketManager.shouldTryToConnect()) {
                websocketManager.open();
            }

            checkWorldType();

            // If the player was not already logged in, notify the server that they have.
            if (!loginState.isLoggedIn()) {
                deferredTask = new DeferredTask(() -> {
                    if (websocketManager.getEventHandler() != null) {
                        websocketManager.getEventHandler().updateGameState(GameState.LOGGED_IN);
                    }
                }, 3);
            }

            loginState = loginState.logIn();
        } else if (gameState == GameState.LOGIN_SCREEN) {
            if (loginState.isLoggedIn() && websocketManager.getEventHandler() != null) {
                websocketManager.getEventHandler().updateGameState(GameState.LOGIN_SCREEN);
            }

            if (activeChallenge != null) {
                // Suspend the challenge while the player is logged out.
                eventBus.unregister(activeChallenge);
            }

            loginState = LoginState.LOGGED_OUT;
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
        if (enabled && maybeChallenge.isPresent()) {
            RecordableChallenge challenge = maybeChallenge.get();
            if (activeChallenge == challenge) {
                return;
            }

            if (activeChallenge != null) {
                activeChallenge.terminate();
            }

            activeChallenge = challenge;
            eventBus.register(activeChallenge);
            activeChallenge.initialize(websocketManager.getEventHandler(), attackRegistry);

            log.info("Entered challenge \"{}\"", activeChallenge.getName());
        } else if (activeChallenge != null) {
            log.info("Exited challenge \"{}\"", activeChallenge.getName());

            eventBus.unregister(activeChallenge);
            activeChallenge.terminate();
            activeChallenge = null;
        }
    }

    private void checkWorldType() {
        var worldTypes = client.getWorldType();
        enabled = !worldTypes.contains(WorldType.BETA_WORLD)
                && !worldTypes.contains(WorldType.DEADMAN)
                && !worldTypes.contains(WorldType.QUEST_SPEEDRUNNING)
                && !worldTypes.contains(WorldType.SEASONAL);
        if (!enabled) {
            log.info("Plugin is disabled due to world type: {}", worldTypes);
        }
    }
}
