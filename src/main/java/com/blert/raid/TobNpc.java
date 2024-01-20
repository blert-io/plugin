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

/**
 * All known NPCs within the Theatre of Blood.
 */
public enum TobNpc {
    // Maiden boss.
    MAIDEN_ENTRY(10814, 6, Mode.ENTRY, 500),
    MAIDEN_REGULAR(8360, 6, Mode.REGULAR, new int[]{2625, 3062, 3500}),
    MAIDEN_HARD(10822, 6, Mode.HARD, new int[]{2625, 3062, 3500}),

    // Maiden red crab.
    MAIDEN_MATOMENOS_ENTRY(10820, 1, Mode.ENTRY, 16),
    MAIDEN_MATOMENOS_REGULAR(8366, 1, Mode.REGULAR, new int[]{75, 87, 100}),
    MAIDEN_MATOMENOS_HARD(10828, 1, Mode.HARD, new int[]{75, 87, 100}),

    // Maiden heat-seeking blood spawn.
    MAIDEN_BLOOD_SPAWN_ENTRY(10821, 1, Mode.ENTRY, 0),
    MAIDEN_BLOOD_SPAWN_REGULAR(8367, 1, Mode.REGULAR, new int[]{0, 0, 0}),
    MAIDEN_BLOOD_SPAWN_HARD(10829, 1, Mode.HARD, new int[]{0, 0, 0}),

    // Inactive Sotetseg (prior to room entry or during maze).
    SOTETSEG_IDLE_ENTRY(10864, Mode.ENTRY),
    SOTETSEG_IDLE_REGULAR(8387, Mode.REGULAR),
    SOTETSEG_IDLE_HARD(10867, Mode.HARD),

    // Main Sotetseg boss.
    SOTETSEG_ENTRY(10865, 1, Mode.ENTRY, 560),
    SOTETSEG_REGULAR(8388, 1, Mode.REGULAR, new int[]{3000, 3500, 4000}),
    SOTETSEG_HARD(10868, 1, Mode.HARD, new int[]{3000, 3500, 4000}),

    // Inactive Xarpus prior to room entry.
    XARPUS_IDLE_ENTRY(10766, Mode.ENTRY),
    XARPUS_IDLE_REGULAR(8338, Mode.REGULAR),
    XARPUS_IDLE_HARD(10770, Mode.HARD),

    // Xarpus healing during P1 exhumes.
    XARPUS_P1_ENTRY(10767, Mode.ENTRY),
    XARPUS_P1_REGULAR(8339, Mode.REGULAR),
    XARPUS_P1_HARD(10771, Mode.HARD),

    // Verzik sitting at her throne prior to combat.
    VERZIK_IDLE_ENTRY(10830, Mode.ENTRY),
    VERZIK_IDLE_REGULAR(8369, Mode.REGULAR),
    VERZIK_IDLE_HARD(10847, Mode.HARD),

    // Verzik P1.
    VERZIK_P1_ENTRY(10831, 1, Mode.ENTRY, 300),
    VERZIK_P1_REGULAR(8370, 1, Mode.REGULAR, new int[]{0, 0, 0}),
    VERZIK_P1_HARD(10848, 1, Mode.HARD, new int[]{0, 0, 0});

    @Getter
    private final int id;
    private final int idRange;
    @Getter
    private final Mode mode;
    private final int[] hitpointsByScale;

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

    TobNpc(int id, int idRange, Mode mode, int[] hitpoints) {
        this.id = id;
        this.idRange = idRange;
        this.mode = mode;
        this.hitpointsByScale = hitpoints;
    }

    TobNpc(int id, int idRange, Mode mode, int hitpoints) {
        this.id = id;
        this.idRange = idRange;
        this.mode = mode;
        this.hitpointsByScale = new int[]{hitpoints, hitpoints, hitpoints};
    }

    TobNpc(int id, Mode mode) {
        this(id, 1, mode, new int[]{0, 0, 0});
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


    public static boolean isSotetsegIdle(int id) {
        return id == SOTETSEG_IDLE_ENTRY.id || id == SOTETSEG_IDLE_REGULAR.id || id == SOTETSEG_IDLE_HARD.id;
    }

    public static boolean isSotetseg(int id) {
        return id == SOTETSEG_ENTRY.id || id == SOTETSEG_REGULAR.id || id == SOTETSEG_HARD.id;
    }

    public static boolean isXarpusIdle(int id) {
        return id == XARPUS_IDLE_ENTRY.id || id == XARPUS_IDLE_REGULAR.id || id == XARPUS_IDLE_HARD.id;
    }

    public static boolean isXarpus(int id) {
        return id == XARPUS_P1_ENTRY.id || id == XARPUS_P1_REGULAR.id || id == XARPUS_P1_HARD.id;
    }

    public static boolean isVerzikIdle(int id) {
        return id == VERZIK_IDLE_ENTRY.id || id == VERZIK_IDLE_REGULAR.id || id == VERZIK_IDLE_HARD.id;
    }

    public static boolean isVerzikP1(int id) {
        return id == VERZIK_P1_ENTRY.id || id == VERZIK_P1_REGULAR.id || id == VERZIK_P1_HARD.id;
    }

    public int getBaseHitpoints(int scale) {
        if (scale < 1 || scale > 5) {
            return -1;
        }

        if (mode == Mode.ENTRY) {
            // Entry mode scales hitpoints linearly with party size.
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
}
