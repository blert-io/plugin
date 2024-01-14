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

package com.blert.raid;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public enum TobNpc {
    MAIDEN_ENTRY(10814, 6, Mode.ENTRY, new int[]{500, 0, 0}, true),
    MAIDEN_MATOMENOS_ENTRY(10820, 1, Mode.ENTRY, new int[]{16, 0, 0}, true),
    MAIDEN_BLOOD_SPAWN_ENTRY(10821, 1, Mode.ENTRY, new int[]{0, 0, 0}, true),

    MAIDEN_REGULAR(8360, 6, Mode.REGULAR, new int[]{2625, 3062, 3500}, false),
    MAIDEN_MATOMENOS_REGULAR(8366, 1, Mode.REGULAR, new int[]{75, 87, 100}, false),
    MAIDEN_BLOOD_SPAWN_REGULAR(8367, 1, Mode.REGULAR, new int[]{0, 0, 0}, false),

    MAIDEN_HARD(10822, 6, Mode.HARD, new int[]{2625, 3062, 3500}, false),
    MAIDEN_MATOMENOS_HARD(10828, 1, Mode.HARD, new int[]{75, 87, 100}, false),
    MAIDEN_BLOOD_SPAWN_HARD(10829, 1, Mode.HARD, new int[]{0, 0, 0}, false);

    @Getter
    private final int id;
    private final int idRange;
    @Getter
    private final Mode mode;
    private final int[] hitpointsByScale;
    private final boolean linearScaling;

    private static final ImmutableMap<Integer, TobNpc> npcsById;

    static {
        ImmutableMap.Builder<Integer, TobNpc> builder = ImmutableMap.builder();

        for (TobNpc npc : TobNpc.values()) {
            for (int i = 0; i < npc.idRange; i++) {
                builder.put(npc.id + i, npc);
            }
        }

        npcsById = builder.build();
    }

    public static Optional<TobNpc> withId(int id) {
        return Optional.ofNullable(npcsById.get(id));
    }

    public static boolean isMaiden(int id) {
        return id == MAIDEN_ENTRY.id || id == MAIDEN_REGULAR.id || id == MAIDEN_HARD.id;
    }

    @NotNull
    public static TobNpc maiden(Mode mode) {
        switch (mode) {
            case ENTRY:
                return MAIDEN_ENTRY;
            case REGULAR:
                return MAIDEN_REGULAR;
            default:
                return MAIDEN_HARD;
        }
    }

    public static boolean isMaidenMatomenos(int id) {
        return id == MAIDEN_MATOMENOS_ENTRY.id
                || id == MAIDEN_MATOMENOS_REGULAR.id
                || id == MAIDEN_MATOMENOS_HARD.id;
    }

    @NotNull
    public static TobNpc maidenMatomenos(Mode mode) {
        switch (mode) {
            case ENTRY:
                return MAIDEN_MATOMENOS_ENTRY;
            case REGULAR:
                return MAIDEN_MATOMENOS_REGULAR;
            default:
                return MAIDEN_MATOMENOS_HARD;
        }
    }

    public static boolean isMaidenBloodSpawn(int id) {
        return id == MAIDEN_BLOOD_SPAWN_ENTRY.id
                || id == MAIDEN_BLOOD_SPAWN_REGULAR.id
                || id == MAIDEN_BLOOD_SPAWN_HARD.id;
    }

    public int getBaseHitpoints(int scale) {
        if (scale < 1 || scale > 5) {
            return -1;
        }

        if (linearScaling) {
            return hitpointsByScale[0] * scale;
        }

        switch (scale) {
            case 5:
                return hitpointsByScale[2];
            case 4:
                return hitpointsByScale[1];
            default:
                return hitpointsByScale[0];
        }
    }

    TobNpc(int id, int idRange, Mode mode, int[] hitpoints, boolean linearScaling) {
        this.id = id;
        this.idRange = idRange;
        this.mode = mode;
        this.hitpointsByScale = hitpoints;
        this.linearScaling = linearScaling;
    }
}
