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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SpellRegistry {
    private static final String BUNDLED_SPELLS_RESOURCE = "/spells.json";

    @AllArgsConstructor
    private static class State {
        private final Map<Integer, SpellDefinition> byAnimationId;
        private final Map<Integer, SpellDefinition> byGraphicId;
        private final Map<Integer, List<SpellDefinition>> targetedByAnimationId;
    }

    private volatile State state = new State(Map.of(), Map.of(), Map.of());

    /**
     * Loads default spell definitions from the bundled JSON resource.
     */
    public void loadDefaults() {
        InputStream stream = getClass().getResourceAsStream(BUNDLED_SPELLS_RESOURCE);
        if (stream == null) {
            log.warn("Bundled spells.json not found; initializing empty spell registry");
            return;
        }

        try {
            List<SpellDefinition> loaded = SpellDefinition.loadFromJson(stream);
            updateDefinitions(loaded);
            log.info("Loaded {} bundled spell definitions", loaded.size());
        } catch (Exception e) {
            log.error("Failed to load bundled spell definitions", e);
        }
    }

    /**
     * Updates the registry with new spell definitions from the server,
     * replacing existing ones.
     *
     * @param definitions The new definitions.
     */
    public void updateFromServer(List<SpellDefinition> definitions) {
        updateDefinitions(definitions);
        log.info("Updated spell registry with {} definitions from server",
                definitions.size());
    }

    /**
     * Replaces all spell definitions with the provided list.
     */
    public void updateDefinitions(List<SpellDefinition> newDefinitions) {
        Map<Integer, SpellDefinition> newByAnimation = new HashMap<>();
        Map<Integer, SpellDefinition> newByGraphic = new HashMap<>();
        Map<Integer, List<SpellDefinition>> newTargetedByAnimation = new HashMap<>();

        for (SpellDefinition spell : newDefinitions) {
            for (int animId : spell.getAnimationIds()) {
                if (spell.isTargeted()) {
                    newTargetedByAnimation.computeIfAbsent(animId, k -> new ArrayList<>()).add(spell);
                } else {
                    newByAnimation.put(animId, spell);
                }
            }
            for (SpellDefinition.Graphic g : spell.getGraphics()) {
                newByGraphic.put(g.getId(), spell);
            }
        }

        this.state = new State(
                newByAnimation,
                newByGraphic,
                newTargetedByAnimation
        );
    }

    /**
     * Finds a spell by animation ID (for spells with unique animations).
     *
     * @param animationId The animation ID.
     * @return The spell definition, or null if not found.
     */
    @Nullable
    public SpellDefinition findByAnimation(int animationId) {
        return state.byAnimationId.get(animationId);
    }

    /**
     * Finds a spell by caster graphic ID.
     *
     * @param graphicId The graphic ID on the caster.
     * @return The spell definition, or null if not found.
     */
    @Nullable
    public SpellDefinition findByGraphic(int graphicId) {
        return state.byGraphicId.get(graphicId);
    }

    /**
     * Gets all targeted spells that use the given caster animation.
     * Used for disambiguating spells that share an animation but have different
     * target graphics.
     *
     * @param animationId The caster animation ID.
     * @return List of targeted spell candidates, or empty list if none found.
     */
    public List<SpellDefinition> getTargetedSpellsByAnimation(int animationId) {
        List<SpellDefinition> result = state.targetedByAnimationId.get(animationId);
        return result != null ? result : List.of();
    }
}
