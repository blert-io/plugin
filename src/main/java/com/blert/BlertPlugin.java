package com.blert;

import com.blert.events.EventHandler;
import com.blert.events.LoggingEventHandler;
import com.blert.raid.RaidManager;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Blert"
)
public class BlertPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private BlertConfig config;

	@Inject
	private RaidManager raidManager;

	@Override
	protected void startUp() throws Exception
	{
		EventHandler handler = new LoggingEventHandler();
		raidManager.setEventHandler(handler);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		raidManager.updateState();
	}

	@Provides
	BlertConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BlertConfig.class);
	}
}
