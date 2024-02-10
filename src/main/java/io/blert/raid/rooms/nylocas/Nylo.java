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
import io.blert.raid.TobNpc;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Slf4j
public class Nylo {
    public enum Style {
        MELEE,
        RANGE,
        MAGE;

        private static Style fromNpcId(int id) {
            if (TobNpc.isNylocasIschyros(id)) {
                return MELEE;
            }
            if (TobNpc.isNylocasToxobolos(id)) {
                return RANGE;
            }
            return MAGE;
        }
    }

    @Getter
    private final @NotNull NPC npc;
    @Setter
    private @Nullable Nylo parent;
    @Getter
    private final long roomId;

    @Getter
    private final SpawnType spawnType;
    @Getter
    private final WorldPoint spawnPoint;
    @Getter
    private final int spawnTick;
    @Getter
    private final Style style;
    @Getter
    private final int wave;
    @Getter
    private final boolean big;

    @Getter
    private WorldPoint deathPoint;
    private int deathTick;

    @Getter
    private final Hitpoints hitpoints;

    public Nylo(@NotNull NPC npc, long roomId, WorldPoint spawnPoint, int spawnTick, int wave, int baseHitpoints) {
        this.npc = npc;
        this.parent = null;
        this.roomId = roomId;
        this.spawnType = SpawnType.fromWorldPoint(spawnPoint);
        this.spawnPoint = spawnPoint;
        this.spawnTick = spawnTick;
        this.style = Style.fromNpcId(npc.getId());
        this.wave = wave;
        this.big = npc.getComposition().getSize() > 1;
        this.deathTick = -1;
        this.hitpoints = new Hitpoints(baseHitpoints);
    }

    public boolean isSplit() {
        return spawnType == SpawnType.SPLIT;
    }

    public Optional<Nylo> getParent() {
        return Optional.ofNullable(parent);
    }

    /**
     * Marks this nylo as having died.
     *
     * @param tick     Room tick on which the nylo died.
     * @param location Point at which the nylo died.
     */
    public void recordDeath(int tick, WorldPoint location) {
        deathTick = tick;
        deathPoint = location;
    }

    /**
     * Checks if this nylo may be a parent of {@code other}. Only possible after calling {@link #recordDeath} and if
     * this nylo is a big and {@code other} is a split.
     *
     * @param other Potential child to check.
     * @return {@code true} if this could be a parent of {@code other}.
     */
    public boolean isPossibleParentOf(Nylo other) {
        if (!big || !other.isSplit() || deathTick != other.spawnTick) {
            return false;
        }

        // Splits appear to spawn within a box of -1 to +2 tiles (inclusive) in both x and y directions around the
        // southwest tile of their parent.
        WorldArea areaToConsider = new WorldArea(deathPoint.getX() - 1, deathPoint.getY() - 1, 4, 4, 0);
        return areaToConsider.contains(other.spawnPoint);
    }
}
