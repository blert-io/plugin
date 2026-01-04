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
import com.google.gson.reflect.TypeToken;
import lombok.Getter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Definition of a player spell (utility spells, not combat).
 */
@Getter
public class SpellDefinition {
    @Getter
    public static class Graphic {
        private final int id;
        private final int durationTicks;
        private final int maxFrame;

        public Graphic(int id, int durationTicks, int maxFrame) {
            this.id = id;
            this.durationTicks = durationTicks;
            this.maxFrame = maxFrame;
        }
    }

    private final int id;
    private final String name;
    private final int[] animationIds;
    private final List<Graphic> graphics;
    private final List<Graphic> targetGraphics;
    private final int stallTicks;

    private final int defaultCooldown;

    public SpellDefinition(int id, String name, int[] animationIds, List<Graphic> graphics,
                           List<Graphic> targetGraphics, int stallTicks) {
        this.id = id;
        this.name = name;
        this.animationIds = animationIds != null ? animationIds : new int[0];
        this.graphics = graphics != null ? graphics : Collections.emptyList();
        this.targetGraphics = targetGraphics != null ? targetGraphics : Collections.emptyList();
        this.stallTicks = stallTicks;

        int maxCasterCooldown = this.graphics.stream()
                .mapToInt(Graphic::getDurationTicks)
                .max()
                .orElse(0);
        int maxTargetCooldown = this.targetGraphics.stream()
                .mapToInt(Graphic::getDurationTicks)
                .max()
                .orElse(0);
        this.defaultCooldown = Math.max(Math.max(maxCasterCooldown, maxTargetCooldown), 1);
    }

    public boolean isTargeted() {
        return !targetGraphics.isEmpty();
    }

    public boolean isStall() {
        return stallTicks > 0;
    }

    public boolean hasAnimation(int animationId) {
        for (int aid : animationIds) {
            if (aid == animationId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the caster graphic definition for the given graphic ID, if any.
     */
    @Nullable
    public Graphic getGraphic(int graphicId) {
        for (Graphic g : graphics) {
            if (g.getId() == graphicId) {
                return g;
            }
        }
        return null;
    }

    /**
     * Returns the cooldown (deduplication interval) for the spell.
     * Optionally, if the graphicId is provided, returns the cooldown for that specific graphic.
     *
     * @param graphicId The graphic ID to get the cooldown for, or null for the default.
     * @return The cooldown in ticks.
     */
    public int getCooldown(@Nullable Integer graphicId) {
        if (graphicId == null) {
            return defaultCooldown;
        }
        Graphic g = getGraphic(graphicId);
        return g != null ? g.getDurationTicks() : defaultCooldown;
    }

    /**
     * Loads spell definitions from a JSON input stream.
     */
    public static List<SpellDefinition> loadFromJson(Gson gson,
                                                     InputStream inputStream) throws IOException {
        Type listType = new TypeToken<List<io.blert.json.SpellDefinition>>() {
        }.getType();
        try (Reader r = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            List<io.blert.json.SpellDefinition> jsonDefs = gson.fromJson(r, listType);
            return jsonDefs.stream().map(io.blert.json.SpellDefinition::toCore).collect(Collectors.toList());
        }
    }
}
