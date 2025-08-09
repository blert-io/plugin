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

package io.blert.challenges.inferno;

import io.blert.core.NpcAttack;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

public enum InfernoNpc {
    NIBBLER(7691, 10),
    BAT(7692, 25),
    BLOB(7693, 40),
    BLOB_MAGER(	7694, 15),
    BLOB_RANGER(7695, 15),
    BLOB_MELEER(7696, 15),
    MELEER(	7697, 75),
    RANGER(7698, 125),
    MAGER(7699, 220),
    JAD(7700, 350),
    JAD_HEALER(7701, 90),
    ZUK_RANGER(7702, 125),
    ZUK_MAGER(7703, 220),
    ZUK_JAD(7704, 350),
    ZUK_JAD_HEALER(7705, 90),
    ZUK(7706,1200),
    ZUK_MOVING_SAFESPOT(7707, 600),
    ZUK_HEALER(7708,75),
    ;

    @Getter
    private final int id;
    @Getter
    private final int hitpoints;

    public static Optional<InfernoNpc> withId(int id) {
        for (InfernoNpc npc : values()) {
            if (npc.id == id) {
                return Optional.of(npc);
            }
        }
        return Optional.empty();
    }

    @SafeVarargs
    InfernoNpc(int id, int hitpoints, Pair<Integer, NpcAttack>... attacks) {
        this.id = id;
        this.hitpoints = hitpoints;
    }
}
