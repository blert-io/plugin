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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * Recording config resolved for a challenge type.
 */
public class ChallengeRecordingConfig {
    public enum ObjectCapture {
        NONE,
        /** Recorded on every tick that the client has the object. */
        PRESENCE,
        /** Recorded only on the tick the object is created. Only applies to graphics. */
        ON_CREATION,
    }

    @Getter
    public static class HitpointsVarbit {
        private final int id;
        /** Varbit value that represents full hitpoints. */
        private final int scale;

        HitpointsVarbit(int id, int scale) {
            this.id = id;
            this.scale = scale;
        }
    }

    public static class NpcDefinition {
        @Getter
        private final ChallengeMode mode;

        @Getter
        private final @Nullable HitpointsVarbit hitpointsVarbit;

        private final int[] hitpoints;

        NpcDefinition(io.blert.json.RecordingConfig.NpcDefinition json) {
            ChallengeMode mode = json.mode != null ? ChallengeMode.fromId(json.mode) : null;
            this.mode = mode != null ? mode : ChallengeMode.NO_MODE;
            this.hitpointsVarbit = json.hitpointsVarbit != null && json.hitpointsVarbit.scale > 0
                    ? new HitpointsVarbit(json.hitpointsVarbit.id, json.hitpointsVarbit.scale)
                    : null;
            this.hitpoints = json.hitpoints != null
                    ? json.hitpoints.stream()
                            .mapToInt(hp -> hp != null ? hp : -1)
                            .toArray()
                    : new int[0];
        }

        public int getBaseHitpoints(int scale) {
            if (scale < 1 || scale > hitpoints.length) {
                return -1;
            }
            return hitpoints[scale - 1];
        }
    }

    @Getter
    private final int version;

    private final Map<ObjectType, Map<Integer, ObjectCapture>> objectCaptures = new EnumMap<>(ObjectType.class);
    private final Map<Integer, NpcDefinition> npcDefinitions = new HashMap<>();

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
     * Returns how objects of a type and ID should be recorded.
     */
    public ObjectCapture objectCapture(ObjectType type, int id) {
        Map<Integer, ObjectCapture> captures = objectCaptures.get(type);
        return captures != null ? captures.getOrDefault(id, ObjectCapture.NONE) : ObjectCapture.NONE;
    }

    /**
     * Returns whether any objects are configured to be recorded.
     */
    public boolean recordsObjects() {
        return objectCaptures.values().stream().anyMatch(captures -> !captures.isEmpty());
    }

    /**
     * Returns the recording configuration for the NPC with the given ID.
     */
    public Optional<NpcDefinition> getNpcDefinition(int npcId) {
        return Optional.ofNullable(npcDefinitions.get(npcId));
    }

    private void addScope(@Nullable io.blert.json.RecordingConfig.Scope scope) {
        if (scope == null) {
            return;
        }

        if (scope.objects != null) {
            for (io.blert.json.RecordingConfig.ObjectCapture capture : scope.objects) {
                ObjectType type = ObjectType.fromId(capture.type);
                if (type != null && capture.ids != null) {
                    // Creation mode is only meaningful for graphics.
                    ObjectCapture mode = type == ObjectType.GRAPHICS && Boolean.TRUE.equals(capture.onCreation)
                            ? ObjectCapture.ON_CREATION
                            : ObjectCapture.PRESENCE;
                    Map<Integer, ObjectCapture> captures = objectCaptures.computeIfAbsent(type, t -> new HashMap<>());
                    for (int id : capture.ids) {
                        captures.put(id, mode);
                    }
                }
            }
        }

        if (scope.npcDefinitions != null) {
            for (io.blert.json.RecordingConfig.NpcDefinition json : scope.npcDefinitions) {
                if (json.ids != null) {
                    NpcDefinition definition = new NpcDefinition(json);
                    for (int id : json.ids) {
                        npcDefinitions.put(id, definition);
                    }
                }
            }
        }
    }
}
