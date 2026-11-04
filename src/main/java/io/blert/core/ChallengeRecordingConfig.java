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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * Recording config resolved for a challenge type.
 */
public class ChallengeRecordingConfig {
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

    private final Map<ObjectType, Set<Integer>> objectIds = new EnumMap<>(ObjectType.class);
    private final Map<Integer, NpcDefinition> npcDefinitions = new HashMap<>();

    /** Varbits whose values are sent every tick. */
    @Getter
    private final Set<Integer> varbits = new LinkedHashSet<>();

    /** Varps whose values are sent every tick. */
    @Getter
    private final Set<Integer> varps = new LinkedHashSet<>();

    /** IDs of projectiles to record on first sight. */
    @Getter
    private final Set<Integer> projectiles = new HashSet<>();

    private final Map<Integer, Integer> spotAnimLengths = new HashMap<>();

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
    public boolean recordsObject(ObjectType type, int id) {
        Set<Integer> ids = objectIds.get(type);
        return ids != null && ids.contains(id);
    }

    /**
     * Returns whether any objects are configured to be recorded.
     */
    public boolean recordsObjects() {
        return objectIds.values().stream().anyMatch(ids -> !ids.isEmpty());
    }

    /**
     * Returns whether spot anims with the given ID should be recorded.
     */
    public boolean recordsSpotAnim(int id) {
        return spotAnimLengths.containsKey(id);
    }

    /**
     * Returns whether any spot anims are configured to be recorded.
     */
    public boolean recordsSpotAnims() {
        return !spotAnimLengths.isEmpty();
    }

    /**
     * Returns the length of a recorded spot anim's animation in client cycles.
     */
    public int getSpotAnimLength(int id) {
        return spotAnimLengths.get(id);
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
                    objectIds.computeIfAbsent(type, t -> new HashSet<>()).addAll(capture.ids);
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

        if (scope.varbits != null) {
            varbits.addAll(scope.varbits);
        }
        if (scope.varps != null) {
            varps.addAll(scope.varps);
        }
        if (scope.projectiles != null) {
            projectiles.addAll(scope.projectiles);
        }
        if (scope.spotAnims != null) {
            for (io.blert.json.RecordingConfig.SpotAnim spotAnim : scope.spotAnims) {
                spotAnimLengths.put(spotAnim.id, spotAnim.length);
            }
        }
    }
}
