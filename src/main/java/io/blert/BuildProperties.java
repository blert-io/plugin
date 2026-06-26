/*
 * Copyright (c) 2026 Alexei Frolov
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.externalplugins.PluginHubManifest;

/**
 * Build and distribution metadata reported to the Blert server when connecting.
 */
public final class BuildProperties {
    /** Protocol/feature version of the plugin. */
    public static final String VERSION = "0.9.12-RUNELITE";

    /** Reported in place of build identity when not known. */
    public static final String DEV = "dev";

    private static final Properties LOCAL_BUILD = loadLocalBuild();

    private BuildProperties() {
    }

    /** Short hash of the commit from which this jar was built. */
    public static String revision() {
        PluginHubManifest.DisplayData displayData = ExternalPluginManager.getDisplayData(BlertPlugin.class);
        if (displayData != null) {
            // When no explicit version is set, the plugin hub packager sets
            // version to the short commit hash.
            return displayData.getVersion() + ":0";
        }
        return LOCAL_BUILD.getProperty("revision", DEV);
    }

    /** Content hash of the exact jar the plugin hub built and distributed. */
    public static String jarHash() {
        PluginHubManifest.JarData jarData = ExternalPluginManager.getJarData(BlertPlugin.class);
        if (jarData != null) {
            return jarData.getJarHash();
        }
        return LOCAL_BUILD.getProperty("jarHash", DEV);
    }

    public static String[] customHeaders() {
        String raw = LOCAL_BUILD.getProperty("customHeaders", "");
        return raw.isEmpty() ? new String[0] : raw.split(";");
    }

    public static String summary() {
        return String.format("version=%s revision=%s jarHash=%s", VERSION, revision(), jarHash());
    }

    private static Properties loadLocalBuild() {
        Properties props = new Properties();
        try (InputStream in = BuildProperties.class.getResourceAsStream("/blert-build.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // No build metadata; treat as an unknown build.
        }
        return props;
    }
}
