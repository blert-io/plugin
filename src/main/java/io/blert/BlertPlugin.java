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
import io.blert.client.WebSocketClient;
import io.blert.client.WebsocketEventHandler;
import io.blert.raid.RaidManager;
import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
        name = "Blert"
)
public class BlertPlugin extends Plugin {
    static final String DEFAULT_BLERT_HOSTNAME = "blert.io";
    static final String DEFAULT_SERVER_HOSTNAME = "socket.blert.io";

    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private BlertConfig config;

    private BlertPluginPanel sidePanel;
    private NavigationButton sidePanelButton;

    @Inject
    private RaidManager raidManager;

    private WebsocketEventHandler handler;

    private WebSocketClient wsClient;

    @Override
    protected void startUp() throws Exception {
        sidePanel = new BlertPluginPanel(this, config);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/blert.png");
        sidePanelButton = NavigationButton.builder().tooltip("Blert").priority(6).icon(icon).panel(sidePanel).build();
        clientToolbar.addNavigation(sidePanelButton);
        sidePanel.startPanel();

        if (config.apiKey() != null) {
            initializeWebSocketClient();
        }

        raidManager.initialize(handler);
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolbar.removeNavigation(sidePanelButton);
        sidePanel = null;

        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
    }

    @Subscribe(priority = 10)
    public void onGameTick(GameTick gameTick) {
        raidManager.tick();
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (wsClient == null || config.dontConnect()) {
            return;
        }

        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            if (config.apiKey() != null && !wsClient.isOpen()) {
                wsClient.open();
            }
        }
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged changed) {
        String key = changed.getKey();
        if (key.equals("apiKey") || key.equals("serverUrl") || key.equals("dontConnect")) {
            initializeWebSocketClient();
        }
    }

    public void initializeWebSocketClient() {
        if (wsClient != null && wsClient.isOpen()) {
            sidePanel.updateUser(null);
            try {
                wsClient.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (config.dontConnect() || config.apiKey() == null) {
            return;
        }

        String hostname = config.serverUrl();
        if (Strings.isNullOrEmpty(hostname)) {
            hostname = DEFAULT_SERVER_HOSTNAME;
        }

        if (!hostname.startsWith("http://") && !hostname.startsWith("https://")) {
            hostname = "https://" + hostname;
        }

        wsClient = new WebSocketClient(hostname, config.apiKey());
        handler = new WebsocketEventHandler(wsClient, sidePanel);
        raidManager.setEventHandler(handler);

        wsClient.open();
    }

    @Provides
    BlertConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BlertConfig.class);
    }
}
