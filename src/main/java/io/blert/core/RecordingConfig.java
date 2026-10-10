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

package io.blert.core;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RecordingConfig {
    private static final String BUNDLED_CONFIG_RESOURCE = "/recording_config.json";

    private volatile Map<Challenge, ChallengeRecordingConfig> challenges = resolve(new io.blert.json.RecordingConfig());

    public void loadDefaults(Gson gson) {
        InputStream stream = getClass().getResourceAsStream(BUNDLED_CONFIG_RESOURCE);
        if (stream == null) {
            log.warn("Bundled recording_config.json not found");
            return;
        }

        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            update(gson.fromJson(reader, io.blert.json.RecordingConfig.class));
        } catch (Exception e) {
            log.error("Failed to load bundled recording config", e);
        }
    }

    public void update(io.blert.json.RecordingConfig json) {
        challenges = resolve(json);
        log.info("Updated recording config to version {}", json.version);
    }

    /**
     * Returns a recording config for a specific challenge type.
     */
    public ChallengeRecordingConfig forChallenge(Challenge challenge) {
        return challenges.get(challenge);
    }

    private static Map<Challenge, ChallengeRecordingConfig> resolve(io.blert.json.RecordingConfig json) {
        Map<Challenge, ChallengeRecordingConfig> challenges = new EnumMap<>(Challenge.class);
        for (Challenge challenge : Challenge.values()) {
            challenges.put(challenge, new ChallengeRecordingConfig(json, challenge));
        }
        return challenges;
    }
}
