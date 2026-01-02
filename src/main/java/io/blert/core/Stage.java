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

package io.blert.core;

import lombok.Getter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public enum Stage {
    TOB_MAIDEN(10),
    TOB_BLOAT(11),
    TOB_NYLOCAS(12),
    TOB_SOTETSEG(13),
    TOB_XARPUS(14),
    TOB_VERZIK(15),

    COX_TEKTON(20),
    COX_CRABS(21),
    COX_ICE_DEMON(22),
    COX_SHAMANS(23),
    COX_VANGUARDS(24),
    COX_THIEVING(25),
    COX_VESPULA(26),
    COX_TIGHTROPE(27),
    COX_GUARDIANS(28),
    COX_VASA(29),
    COX_MYSTICS(30),
    COX_MUTTADILE(31),
    COX_OLM(32),

    TOA_APMEKEN(40),
    TOA_BABA(41),
    TOA_SCABARAS(42),
    TOA_KEPHRI(43),
    TOA_HET(44),
    TOA_AKKHA(45),
    TOA_CRONDIS(46),
    TOA_ZEBAK(47),
    TOA_WARDENS(48),

    COLOSSEUM_WAVE_1(100),
    COLOSSEUM_WAVE_2(101),
    COLOSSEUM_WAVE_3(102),
    COLOSSEUM_WAVE_4(103),
    COLOSSEUM_WAVE_5(104),
    COLOSSEUM_WAVE_6(105),
    COLOSSEUM_WAVE_7(106),
    COLOSSEUM_WAVE_8(107),
    COLOSSEUM_WAVE_9(108),
    COLOSSEUM_WAVE_10(109),
    COLOSSEUM_WAVE_11(110),
    COLOSSEUM_WAVE_12(111),
    ;

    @Getter
    private final int id;

    private static final Map<Integer, Stage> ID_MAP = new HashMap<>();

    static {
        for (Stage stage : values()) {
            ID_MAP.put(stage.id, stage);
        }
    }

    Stage(int id) {
        this.id = id;
    }

    /**
     * Retrieves the Stage enum constant associated with a specific ID.
     *
     * @param stageId The integer ID to look up.
     * @return The matching Stage, or null if no match is found.
     */
    @Nullable
    public static Stage fromId(int stageId) {
        return ID_MAP.get(stageId);
    }
}
