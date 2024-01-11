/*
 * Copyright (c) 2023 Alexei Frolov
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

package com.blert.raid;

import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.LocalPoint;

public class Utils {
    /**
     * Returns the southwest tile of an NPC's area in the local scene.
     */
    public static LocalPoint getNpcSouthwestTile(NPC npc) {
        LocalPoint point = npc.getLocalLocation();
        NPCComposition comp = npc.getComposition();
        int size = comp != null ? comp.getSize() : 1;
        return new LocalPoint(correctForSize(point.getX(), size), correctForSize(point.getY(), size));
    }

    private static int correctForSize(int coord, int size) {
        // Southwest correction from Better NPC Highlight:
        // https://github.com/MoreBuchus/buchus-plugins/blob/609c887477749b5329a6a3bcb6f3265e3a2eca5f/src/main/java/com/betternpchighlight/BetterNpcHighlightOverlay.java#L406
        return coord - ((size - 1) * 128) / 2;
    }
}
