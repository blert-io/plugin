package com.blert;

import com.blert.events.EventHandler;
import com.blert.json.JsonEventHandler;
import com.blert.raid.RaidManager;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
        name = "Blert"
)
public class BlertPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private BlertConfig config;

    @Inject
    private RaidManager raidManager;

    private EventHandler handler;

    @Override
    protected void startUp() throws Exception {
        handler = new JsonEventHandler();
        raidManager.initialize(handler);
    }

    @Override
    protected void shutDown() throws Exception {
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        raidManager.tick();
    }

    @Provides
    BlertConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BlertConfig.class);
    }
}
