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

package io.blert.json;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SpellDefinition {
    public int id;
    public String name;
    public List<Integer> animationIds;
    public List<Graphic> graphics;
    public List<Graphic> targetGraphics;
    public int stallTicks;

    public static class Graphic {
        public int id;
        public int durationTicks;
        public int maxFrame;
    }

    public io.blert.core.SpellDefinition toCore() {
        List<io.blert.core.SpellDefinition.Graphic> graphicList = graphics != null
                ? graphics.stream()
                    .map(g -> new io.blert.core.SpellDefinition.Graphic(g.id, g.durationTicks, g.maxFrame))
                    .collect(Collectors.toList())
                : Collections.emptyList();

        List<io.blert.core.SpellDefinition.Graphic> targetGraphicList = targetGraphics != null
                ? targetGraphics.stream()
                    .map(g -> new io.blert.core.SpellDefinition.Graphic(g.id, g.durationTicks, g.maxFrame))
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return new io.blert.core.SpellDefinition(
                id,
                name,
                animationIds != null ? animationIds.stream().mapToInt(Integer::intValue).toArray() : new int[0],
                graphicList,
                targetGraphicList,
                stallTicks
        );
    }
}