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

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * Recording config resolved for a challenge type.
 */
public class ChallengeRecordingConfig {
    @Getter
    private final int version;

    private final Map<ObjectType, Set<Integer>> objectIds = new EnumMap<>(ObjectType.class);

    ChallengeRecordingConfig(io.blert.json.RecordingConfig json, Challenge challenge) {
        this.version = json.version;

        addScope(json.global);
        if (json.challenges != null) {
            for (io.blert.json.RecordingConfig.ChallengeScope challengeScope : json.challenges) {
                if (challengeScope.challenge == challenge.getId()) {
                    addScope(challengeScope.scope);
                }
            }
        }
    }

    /**
     * Returns whether objects of a type and ID should be recorded.
     */
    public boolean shouldRecordObject(ObjectType type, int id) {
        Set<Integer> ids = objectIds.get(type);
        return ids != null && ids.contains(id);
    }

    private void addScope(@Nullable io.blert.json.RecordingConfig.Scope scope) {
        if (scope == null || scope.objects == null) {
            return;
        }

        for (io.blert.json.RecordingConfig.ObjectCapture capture : scope.objects) {
            ObjectType type = ObjectType.fromId(capture.type);
            if (type != null && capture.ids != null) {
                objectIds.computeIfAbsent(type, t -> new HashSet<>()).addAll(capture.ids);
            }
        }
    }
}
