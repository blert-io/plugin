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
    MAIDEN_BLOOD_SPAWN_REGULAR(8367, 1, Mode.REGULAR, new int[]{90, 105, 120}),
    MAIDEN_BLOOD_SPAWN_HARD(10829, 1, Mode.HARD, new int[]{0, 0, 0}),

    // Bloat.
    BLOAT_ENTRY(10812, 1, Mode.ENTRY, 320),
    BLOAT_REGULAR(8359, 1, Mode.REGULAR, new int[]{1500, 1750, 2000}),
    BLOAT_HARD(10813, 1, Mode.HARD, new int[]{1800, 2100, 2400}),

    // Melee small nylos.
    NYLOCAS_ISCHYROS_SMALL_ENTRY(10774, 1, Mode.ENTRY, 2),
    NYLOCAS_ISCHYROS_SMALL_REGULAR(8342, 1, Mode.REGULAR, new int[]{8, 9, 11}),
    NYLOCAS_ISCHYROS_SMALL_HARD(10791, 1, Mode.HARD, new int[]{8, 9, 11}),
    NYLOCAS_ISCHYROS_SMALL_AGGRO_ENTRY(10780, 1, Mode.ENTRY, 2),
    NYLOCAS_ISCHYROS_SMALL_AGRRO_REGULAR(8348, 1, Mode.REGULAR, new int[]{8, 9, 11}),
    NYLOCAS_ISCHYROS_SMALL_AGGRO_HARD(10797, 1, Mode.HARD, new int[]{8, 9, 11}),

    // Melee big nylos.
    NYLOCAS_ISCHYROS_BIG_ENTRY(10777, 1, Mode.ENTRY, 3),
    NYLOCAS_ISCHYROS_BIG_REGULAR(8345, 1, Mode.REGULAR, new int[]{16, 19, 22}),
    NYLOCAS_ISCHYROS_BIG_HARD(10794, 1, Mode.HARD, new int[]{16, 19, 22}),
    NYLOCAS_ISCHYROS_BIG_AGGRO_ENTRY(10783, 1, Mode.ENTRY, 3),
    NYLOCAS_ISCHYROS_BIG_AGGRO_REGULAR(8351, 1, Mode.REGULAR, new int[]{16, 19, 22}),
    NYLOCAS_ISCHYROS_BIG_AGGRO_HARD(10800, 1, Mode.HARD, new int[]{16, 19, 22}),

    // Range small nylos.
    NYLOCAS_TOXOBOLOS_SMALL_ENTRY(10775, 1, Mode.ENTRY, 2),
    NYLOCAS_TOXOBOLOS_SMALL_REGULAR(8343, 1, Mode.REGULAR, new int[]{8, 9, 11}),
    NYLOCAS_TOXOBOLOS_SMALL_HARD(10792, 1, Mode.HARD, new int[]{8, 9, 11}),
    NYLOCAS_TOXOBOLOS_SMALL_AGGRO_ENTRY(10781, 1, Mode.ENTRY, 2),
    NYLOCAS_TOXOBOLOS_SMALL_AGRRO_REGULAR(8349, 1, Mode.REGULAR, new int[]{8, 9, 11}),
    NYLOCAS_TOXOBOLOS_SMALL_AGGRO_HARD(10798, 1, Mode.HARD, new int[]{8, 9, 11}),

    // Range big nylos.
    NYLOCAS_TOXOBOLOS_BIG_ENTRY(10778, 1, Mode.ENTRY, 3),
    NYLOCAS_TOXOBOLOS_BIG_REGULAR(8346, 1, Mode.REGULAR, new int[]{16, 19, 22}),
    NYLOCAS_TOXOBOLOS_BIG_HARD(10795, 1, Mode.HARD, new int[]{16, 19, 22}),
    NYLOCAS_TOXOBOLOS_BIG_AGGRO_ENTRY(10784, 1, Mode.ENTRY, 3),
    NYLOCAS_TOXOBOLOS_BIG_AGGRO_REGULAR(8352, 1, Mode.REGULAR, new int[]{16, 19, 22}),
    NYLOCAS_TOXOBOLOS_BIG_AGGRO_HARD(10801, 1, Mode.HARD, new int[]{16, 19, 22}),

    // Mage small nylos.
    NYLOCAS_HAGIOS_SMALL_ENTRY(10776, 1, Mode.ENTRY, 2),
    NYLOCAS_HAGIOS_SMALL_REGULAR(8344, 1, Mode.REGULAR, new int[]{8, 9, 11}),
    NYLOCAS_HAGIOS_SMALL_HARD(10793, 1, Mode.HARD, new int[]{8, 9, 11}),
    NYLOCAS_HAGIOS_SMALL_AGGRO_ENTRY(10782, 1, Mode.ENTRY, 2),
    NYLOCAS_HAGIOS_SMALL_AGRRO_REGULAR(8350, 1, Mode.REGULAR, new int[]{8, 9, 11}),
    NYLOCAS_HAGIOS_SMALL_AGGRO_HARD(10799, 1, Mode.HARD, new int[]{8, 9, 11}),

    // Mage big nylos.
    NYLOCAS_HAGIOS_BIG_ENTRY(10779, 1, Mode.ENTRY, 3),
    NYLOCAS_HAGIOS_BIG_REGULAR(8347, 1, Mode.REGULAR, new int[]{16, 19, 22}),
    NYLOCAS_HAGIOS_BIG_HARD(10796, 1, Mode.HARD, new int[]{16, 19, 22}),
    NYLOCAS_HAGIOS_BIG_AGGRO_ENTRY(10785, 1, Mode.ENTRY, 3),
    NYLOCAS_HAGIOS_BIG_AGGRO_REGULAR(8353, 1, Mode.REGULAR, new int[]{16, 19, 22}),
    NYLOCAS_HAGIOS_BIG_AGGRO_HARD(10802, 1, Mode.HARD, new int[]{16, 19, 22}),

    // HMT nylo prince.
    NYLOCAS_PRINKIPAS(10803, 4, Mode.HARD, new int[]{300, 350, 400}),

    // Nylo king.
    NYLOCAS_VASILIAS_ENTRY(10787, 4, Mode.ENTRY, 360),
    NYLOCAS_VASILIAS_REGULAR(8354, 4, Mode.REGULAR, new int[]{1875, 2187, 2500}),
    NYLOCAS_VASILIAS_HARD(10807, 4, Mode.HARD, new int[]{1875, 2187, 2500}),

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

    public boolean hasId(int id) {
        return id >= this.id && id < this.id + this.idRange;
    }

    private static boolean idMatches(int id, TobNpc entry, TobNpc regular, TobNpc hard) {
        return entry.hasId(id) || regular.hasId(id) || hard.hasId(id);
    }

    public static boolean isMaiden(int id) {
        return idMatches(id, MAIDEN_ENTRY, MAIDEN_REGULAR, MAIDEN_HARD);
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
        return idMatches(id, MAIDEN_MATOMENOS_ENTRY, MAIDEN_MATOMENOS_REGULAR, MAIDEN_MATOMENOS_HARD);
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
        return idMatches(id, MAIDEN_BLOOD_SPAWN_ENTRY, MAIDEN_BLOOD_SPAWN_REGULAR, MAIDEN_BLOOD_SPAWN_HARD);
    }

    public static boolean isBloat(int id) {
        return idMatches(id, BLOAT_ENTRY, BLOAT_REGULAR, BLOAT_HARD);
    }

    public static boolean isNylocasIschyrosSmall(int id) {
        return idMatches(id, NYLOCAS_ISCHYROS_SMALL_ENTRY, NYLOCAS_ISCHYROS_SMALL_REGULAR, NYLOCAS_ISCHYROS_SMALL_HARD)
                || idMatches(id, NYLOCAS_ISCHYROS_SMALL_AGGRO_ENTRY, NYLOCAS_ISCHYROS_SMALL_AGRRO_REGULAR, NYLOCAS_ISCHYROS_SMALL_AGGRO_HARD);
    }

    public static boolean isNylocasIschyrosBig(int id) {
        return idMatches(id, NYLOCAS_ISCHYROS_BIG_ENTRY, NYLOCAS_ISCHYROS_BIG_REGULAR, NYLOCAS_ISCHYROS_BIG_HARD)
                || idMatches(id, NYLOCAS_ISCHYROS_BIG_AGGRO_ENTRY, NYLOCAS_ISCHYROS_BIG_AGGRO_REGULAR, NYLOCAS_ISCHYROS_BIG_AGGRO_HARD);
    }

    public static boolean isNylocasIschyros(int id) {
        return isNylocasIschyrosSmall(id) || isNylocasIschyrosBig(id);
    }

    public static boolean isNylocasToxobolosSmall(int id) {
        return idMatches(id, NYLOCAS_TOXOBOLOS_SMALL_ENTRY, NYLOCAS_TOXOBOLOS_SMALL_REGULAR, NYLOCAS_TOXOBOLOS_SMALL_HARD)
                || idMatches(id, NYLOCAS_TOXOBOLOS_SMALL_AGGRO_ENTRY, NYLOCAS_TOXOBOLOS_SMALL_AGRRO_REGULAR, NYLOCAS_TOXOBOLOS_SMALL_AGGRO_HARD);
    }

    public static boolean isNylocasToxobolosBig(int id) {
        return idMatches(id, NYLOCAS_TOXOBOLOS_BIG_ENTRY, NYLOCAS_TOXOBOLOS_BIG_REGULAR, NYLOCAS_TOXOBOLOS_BIG_HARD)
                || idMatches(id, NYLOCAS_TOXOBOLOS_BIG_AGGRO_ENTRY, NYLOCAS_TOXOBOLOS_BIG_AGGRO_REGULAR, NYLOCAS_TOXOBOLOS_BIG_AGGRO_HARD);
    }

    public static boolean isNylocasToxobolos(int id) {
        return isNylocasToxobolosSmall(id) || isNylocasToxobolosBig(id);
    }

    public static boolean isNylocasHagiosSmall(int id) {
        return idMatches(id, NYLOCAS_HAGIOS_SMALL_ENTRY, NYLOCAS_HAGIOS_SMALL_REGULAR, NYLOCAS_HAGIOS_SMALL_HARD)
                || idMatches(id, NYLOCAS_HAGIOS_SMALL_AGGRO_ENTRY, NYLOCAS_HAGIOS_SMALL_AGRRO_REGULAR, NYLOCAS_HAGIOS_SMALL_AGGRO_HARD);
    }

    public static boolean isNylocasHagiosBig(int id) {
        return idMatches(id, NYLOCAS_HAGIOS_BIG_ENTRY, NYLOCAS_HAGIOS_BIG_REGULAR, NYLOCAS_HAGIOS_BIG_HARD)
                || idMatches(id, NYLOCAS_HAGIOS_BIG_AGGRO_ENTRY, NYLOCAS_HAGIOS_BIG_AGGRO_REGULAR, NYLOCAS_HAGIOS_BIG_AGGRO_HARD);
    }

    public static boolean isNylocasHagios(int id) {
        return isNylocasHagiosSmall(id) || isNylocasHagiosBig(id);
    }

    public static boolean isNylocas(int id) {
        return isNylocasIschyros(id) || isNylocasToxobolos(id) || isNylocasHagios(id);
    }

    public static boolean isNylocasPrinkipas(int id) {
        return NYLOCAS_PRINKIPAS.hasId(id);
    }

    public static boolean isNylocasVasilias(int id) {
        return idMatches(id, NYLOCAS_VASILIAS_ENTRY, NYLOCAS_VASILIAS_REGULAR, NYLOCAS_VASILIAS_HARD);
    }

    public static boolean isSotetsegIdle(int id) {
        return idMatches(id, SOTETSEG_IDLE_ENTRY, SOTETSEG_IDLE_REGULAR, SOTETSEG_IDLE_HARD);
    }

    public static boolean isSotetseg(int id) {
        return idMatches(id, SOTETSEG_ENTRY, SOTETSEG_REGULAR, SOTETSEG_HARD);
    }

    public static boolean isXarpusIdle(int id) {
        return idMatches(id, XARPUS_IDLE_ENTRY, XARPUS_IDLE_REGULAR, XARPUS_IDLE_HARD);
    }

    public static boolean isXarpus(int id) {
        return idMatches(id, XARPUS_P1_ENTRY, XARPUS_P1_REGULAR, XARPUS_P1_HARD);
    }

    public static boolean isVerzikIdle(int id) {
        return idMatches(id, VERZIK_IDLE_ENTRY, VERZIK_IDLE_REGULAR, VERZIK_IDLE_HARD);
    }

    public static boolean isVerzikP1(int id) {
        return idMatches(id, VERZIK_P1_ENTRY, VERZIK_P1_REGULAR, VERZIK_P1_HARD);
    }

    public int getBaseHitpoints(int scale) {
        if (scale < 1 || scale > 5) {
            return -1;
        }

        if (mode == Mode.ENTRY) {
            if (isNylocas(this.id)) {
                return hitpointsByScale[0];
            }
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
