package com.blert;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

import java.util.Arrays;

public class BlertPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BlertPlugin.class);
		String[] debugArgs = Arrays.copyOf(args, args.length + 1);
		debugArgs[args.length] = "--debug";
		RuneLite.main(args);
	}
}