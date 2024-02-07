/*
 * Copyright (c) 2024 Alexei Frolov
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

package io.blert.raid.rooms.nylocas;

import io.blert.raid.Hitpoints;
import lombok.Getter;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import org.jetbrains.annotations.NotNull;

public class Nylo {
    @Getter
    private final @NotNull NPC npc;
    private final WorldPoint spawnPoint;
    @Getter
    private final int spawnTick;
    @Getter
    private final int wave;
    @Getter
    private final boolean big;
    @Getter
    private final boolean split;

    @Getter
    private Hitpoints hitpoints;

    public Nylo(@NotNull NPC npc, WorldPoint spawnPoint, int spawnTick, int wave, int baseHitpoints) {
        this.spawnPoint = spawnPoint;
        this.npc = npc;
        this.spawnTick = spawnTick;
        this.wave = wave;
        this.big = npc.getComposition().getSize() > 1;
        this.split = !SpawnPoint.isNyloLaneSpawn(spawnPoint);
        this.hitpoints = new Hitpoints(baseHitpoints);
    }
}
