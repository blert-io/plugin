package com.blert;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Example"
)
public class BlertPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private BlertConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Example started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		// config.greeting();
		Player player = client.getLocalPlayer();
		WorldPoint point = player.getWorldLocation();
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[blert] Position: (" + point.getX() + "," + point.getY() + ")", null);
	}

	@Provides
	BlertConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BlertConfig.class);
	}
}
