package com.blert;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import javax.annotation.Nullable;

@ConfigGroup("blert")
public interface BlertConfig extends Config {
    @ConfigSection(
            name = "General",
            description = "Blert plugin settings",
            position = 1
    )
    String GENERAL_SECTION = "general";

    @ConfigSection(
            name = "Developer Tools",
            description = "Settings applicable to Blert developers",
            position = 2,
            closedByDefault = true
    )
    String DEVELOPER_SECTION = "developer";


    @ConfigItem(
            keyName = "apiKey",
            name = "Blert API key",
            description = "Token to access the blert API",
            position = 1,
            section = GENERAL_SECTION
    )
    default @Nullable String apiKey() {
        return null;
    }

    @ConfigItem(
            keyName = "serverUrl",
            name = "Custom server URL",
            description = "Overrides the default server hostname (blert.io)",
            position = 1,
            section = DEVELOPER_SECTION
    )
    default @Nullable String serverUrl() {
        return null;
    }


    @ConfigItem(
            keyName = "dontConnect",
            name = "Don't connect to a Blert server",
            description = "If set, will not attempt to open a socket or stream events",
            position = 2,
            section = DEVELOPER_SECTION
    )
    default boolean dontConnect() {
        return false;
    }
}
