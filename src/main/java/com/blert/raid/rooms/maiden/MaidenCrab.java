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

package com.blert.raid.rooms.maiden;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.Optional;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MaidenCrab {
    private Position position;
    private boolean scuffed;

    public enum Position {
        S1,
        N1,
        S2,
        N2,
        S3,
        N3,
        S4_INNER,
        S4_OUTER,
        N4_INNER,
        N4_OUTER,
    }

    /**
     * Returns the crab that spawns at a specified location.
     *
     * @param location Spawn location corresponding to the southwest tile of the crab.
     * @return The type of crab which spawns at that location, if one exists.
     */
    public static Optional<MaidenCrab> fromSpawnLocation(WorldPoint location) {
        MaidenCrab crab = null;

        if (location.equals(N1_SPAWN)) {
            crab = new MaidenCrab(Position.N1, false);
        } else if (location.equals(N1_SCUFFED_SPAWN)) {
            crab = new MaidenCrab(Position.N1, true);
        } else if (location.equals(N2_SPAWN)) {
            crab = new MaidenCrab(Position.N2, false);
        } else if (location.equals(N2_SCUFFED_SPAWN)) {
            crab = new MaidenCrab(Position.N2, true);
        } else if (location.equals(N3_SPAWN)) {
            crab = new MaidenCrab(Position.N3, false);
        } else if (location.equals(N3_SCUFFED_SPAWN)) {
            crab = new MaidenCrab(Position.N3, true);
        } else if (location.equals(N4_INNER_SPAWN)) {
            crab = new MaidenCrab(Position.N4_INNER, false);
        } else if (location.equals(N4_INNER_SCUFFED_SPAWN)) {
            crab = new MaidenCrab(Position.N4_INNER, true);
        } else if (location.equals(N4_OUTER_SPAWN)) {
            crab = new MaidenCrab(Position.N4_OUTER, false);
        } else if (location.equals(N4_OUTER_SCUFFED_SPAWN)) {
            crab = new MaidenCrab(Position.N4_OUTER, true);
        } else if (location.equals(S1_SPAWN)) {
            crab = new MaidenCrab(Position.S1, false);
        } else if (location.equals(S1_SCUFFED_SPAWN)) {
            crab = new MaidenCrab(Position.S1, true);
        } else if (location.equals(S2_SPAWN)) {
            crab = new MaidenCrab(Position.S2, false);
        } else if (location.equals(S2_SCUFFED_SPAWN)) {
            crab = new MaidenCrab(Position.S2, true);
        } else if (location.equals(S3_SPAWN)) {
            crab = new MaidenCrab(Position.S3, false);
        } else if (location.equals(S3_SCUFFED_SPAWN)) {
            crab = new MaidenCrab(Position.S3, true);
        } else if (location.equals(S4_INNER_SPAWN)) {
            crab = new MaidenCrab(Position.S4_INNER, false);
        } else if (location.equals(S4_INNER_SCUFFED_SPAWN)) {
            crab = new MaidenCrab(Position.S4_INNER, true);
        } else if (location.equals(S4_OUTER_SPAWN)) {
            crab = new MaidenCrab(Position.S4_OUTER, false);
        } else if (location.equals(S4_OUTER_SCUFFED_SPAWN)) {
            crab = new MaidenCrab(Position.S4_OUTER, true);
        }

        return Optional.ofNullable(crab);
    }

    /**
     * Returns the location at which this crab spawns.
     */
    public WorldPoint getSpawnPoint() {
        switch (position) {
            case S1:
                return scuffed ? S1_SCUFFED_SPAWN : S1_SPAWN;
            case N1:
                return scuffed ? N1_SCUFFED_SPAWN : N1_SPAWN;
            case S2:
                return scuffed ? S2_SCUFFED_SPAWN : S2_SPAWN;
            case N2:
                return scuffed ? N2_SCUFFED_SPAWN : N2_SPAWN;
            case S3:
                return scuffed ? S3_SCUFFED_SPAWN : S3_SPAWN;
            case N3:
                return scuffed ? N3_SCUFFED_SPAWN : N3_SPAWN;
            case S4_INNER:
                return scuffed ? S4_INNER_SCUFFED_SPAWN : S4_INNER_SPAWN;
            case S4_OUTER:
                return scuffed ? S4_OUTER_SCUFFED_SPAWN : S4_OUTER_SPAWN;
            case N4_INNER:
                return scuffed ? N4_INNER_SCUFFED_SPAWN : N4_INNER_SPAWN;
            case N4_OUTER:
                return scuffed ? N4_OUTER_SCUFFED_SPAWN : N4_OUTER_SPAWN;
        }

        return null;
    }

    private static final WorldPoint N1_SPAWN = new WorldPoint(3173, 4456, 0);
    private static final WorldPoint N1_SCUFFED_SPAWN = new WorldPoint(3174, 4457, 0);
    private static final WorldPoint N2_SPAWN = new WorldPoint(3177, 4456, 0);
    private static final WorldPoint N2_SCUFFED_SPAWN = new WorldPoint(3178, 4457, 0);
    private static final WorldPoint N3_SPAWN = new WorldPoint(3181, 4456, 0);
    private static final WorldPoint N3_SCUFFED_SPAWN = new WorldPoint(3182, 4457, 0);
    private static final WorldPoint N4_INNER_SPAWN = new WorldPoint(3185, 4454, 0);
    private static final WorldPoint N4_INNER_SCUFFED_SPAWN = new WorldPoint(3186, 4455, 0);
    private static final WorldPoint N4_OUTER_SPAWN = new WorldPoint(3185, 4456, 0);
    private static final WorldPoint N4_OUTER_SCUFFED_SPAWN = new WorldPoint(3186, 4457, 0);

    private static final WorldPoint S1_SPAWN = new WorldPoint(3173, 4436, 0);
    private static final WorldPoint S1_SCUFFED_SPAWN = new WorldPoint(3174, 4435, 0);
    private static final WorldPoint S2_SPAWN = new WorldPoint(3177, 4436, 0);
    private static final WorldPoint S2_SCUFFED_SPAWN = new WorldPoint(3178, 4435, 0);
    private static final WorldPoint S3_SPAWN = new WorldPoint(3181, 4436, 0);
    private static final WorldPoint S3_SCUFFED_SPAWN = new WorldPoint(3182, 4435, 0);
    private static final WorldPoint S4_INNER_SPAWN = new WorldPoint(3185, 4438, 0);
    private static final WorldPoint S4_INNER_SCUFFED_SPAWN = new WorldPoint(3186, 4437, 0);
    private static final WorldPoint S4_OUTER_SPAWN = new WorldPoint(3185, 4436, 0);
    private static final WorldPoint S4_OUTER_SCUFFED_SPAWN = new WorldPoint(3186, 4435, 0);
}
