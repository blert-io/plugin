/*
 * Copyright (c) 2025 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
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
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.*;

@Slf4j
public class AttackRegistry {
    private static final String BUNDLED_ATTACKS_RESOURCE = "/attacks.json";

    private static final int UNKNOWN_PROTO_ID = 0;

    @AllArgsConstructor
    private static class State {
        private volatile Map<Integer, List<AttackDefinition>> byAnimationId;

        private volatile Set<Integer> continuousAnimationIds;

        /**
         * Attacks that can be suppressed by continuous animations.
         */
        private volatile List<AttackDefinition> suppressableAttacks;
    }

    @Setter
    private Gson gson;

    private volatile State state = new State(Map.of(), Set.of(), List.of());

    /**
     * The UNKNOWN attack definition.
     */
    private AttackDefinition unknownAttack;

    public AttackRegistry() {
        // Initialize with a default UNKNOWN attack.
        unknownAttack = new AttackDefinition(
                UNKNOWN_PROTO_ID,
                "UNKNOWN",
                new int[]{-1},
                new int[]{-1},
                0,
                null,
                false,
                AttackDefinition.Category.MELEE
        );
    }

    /**
     * Loads bundled attack definitions from the JAR resources.
     */
    public void loadDefaults() {
        InputStream inputStream = getClass().getResourceAsStream(BUNDLED_ATTACKS_RESOURCE);
        if (inputStream == null) {
            log.warn("Bundled attacks.json not found; initializing empty attack registry");
            return;
        }

        try {
            List<AttackDefinition> definitions = AttackDefinition.loadFromJson(gson, inputStream);
            updateDefinitions(definitions);
            log.info("Loaded {} bundled attack definitions", definitions.size());
        } catch (Exception e) {
            log.error("Failed to load bundled attack definitions", e);
        }
    }

    /**
     * Updates the registry with new attack definitions from the server, replacing existing ones.
     *
     * @param definitions The new definitions.
     */
    public void updateFromServer(List<AttackDefinition> definitions) {
        updateDefinitions(definitions);
        log.info("Updated attack registry with {} definitions from server", definitions.size());
    }

    private void updateDefinitions(List<AttackDefinition> definitions) {
        Map<Integer, List<AttackDefinition>> newByAnimationId = new HashMap<>();
        Set<Integer> newContinuousAnimationIds = new HashSet<>();
        List<AttackDefinition> newSuppressableAttacks = new ArrayList<>();

        for (AttackDefinition def : definitions) {
            for (int animationId : def.getAnimationIds()) {
                newByAnimationId.computeIfAbsent(animationId, k -> new ArrayList<>()).add(def);
            }

            if (def.isContinuousAnimation()) {
                for (int animationId : def.getAnimationIds()) {
                    newContinuousAnimationIds.add(animationId);
                }
            }

            // Throwing chins can be suppressed by continuous animations.
            // TODO(frolv): Barrages can also be suppressed, but it requires
            //   checking for applied graphics.
            if (def.getName() != null && def.getName().startsWith("CHIN_")) {
                newSuppressableAttacks.add(def);
            }

            if (def.getProtoId() == UNKNOWN_PROTO_ID) {
                unknownAttack = def;
            }
        }

        this.state = new State(newByAnimationId, newContinuousAnimationIds, newSuppressableAttacks);
    }

    /**
     * Finds a player attack based on a weapon and animation.
     *
     * @param weaponId    ID of the weapon used.
     * @param animationId ID of the animation played with the attack.
     * @return If the animation ID does not map to any known attack, returns {@code Optional.empty()}.
     * Otherwise, returns the attack if the weapon ID is consistent with the animation,
     * or the UNKNOWN attack if not.
     */
    public Optional<AttackDefinition> find(int weaponId, int animationId) {
        List<AttackDefinition> candidates = state.byAnimationId.get(animationId);
        if (candidates == null) {
            return Optional.empty();
        }

        for (AttackDefinition attack : candidates) {
            if (attack.hasWeapon(weaponId)) {
                return Optional.of(attack);
            }
        }

        for (AttackDefinition attack : candidates) {
            if (attack.isUnknown()) {
                return Optional.of(attack);
            }
        }

        return Optional.of(unknownAttack);
    }

    /**
     * Finds an attack that may be hidden after a continuous animation.
     *
     * @param weaponId ID of the weapon used.
     * @return A player attack matching the weapon that can be suppressed, or
     * empty if none.
     */
    public Optional<AttackDefinition> findSuppressedAttack(int weaponId) {
        for (AttackDefinition attack : state.suppressableAttacks) {
            if (attack.hasWeapon(weaponId)) {
                return Optional.of(attack);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns whether the given animation ID is continuous.
     *
     * @param animationId ID of the animation to check.
     * @return true if the animation is a continuous animation, false otherwise.
     */
    public boolean isContinuousAnimation(int animationId) {
        return state.continuousAnimationIds.contains(animationId);
    }

    /**
     * Returns all attacks that share any of the given animation IDs.
     *
     * @param animationIds Animation IDs to include.
     * @return All matching attack definitions.
     */
    public List<AttackDefinition> allWithAnimations(int[] animationIds) {
        Set<AttackDefinition> result = new LinkedHashSet<>();
        for (int animationId : animationIds) {
            List<AttackDefinition> candidates = state.byAnimationId.get(animationId);
            if (candidates != null) {
                result.addAll(candidates);
            }
        }
        return new ArrayList<>(result);
    }
}