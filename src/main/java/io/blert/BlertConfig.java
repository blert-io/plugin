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
            description = "Overrides the default server hostname (wave32.blert.io)",
            position = 1,
            section = DEVELOPER_SECTION
    )
    default @Nullable String serverUrl() {
        return null;
    }

    @ConfigItem(
            keyName = "webUrl",
            name = "Custom Blert website URL",
            description = "Overrides the default website URL (blert.io)",
            position = 2,
            section = DEVELOPER_SECTION
    )
    default @Nullable String webUrl() {
        return null;
    }

    @ConfigItem(
            keyName = "dontConnect",
            name = "Don't connect to a Blert server",
            description = "If set, will not attempt to open a socket or stream events",
            position = 3,
            section = DEVELOPER_SECTION
    )
    default boolean dontConnect() {
        return false;
    }
}
