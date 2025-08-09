/*
 * Copyright (c) 2025 Alexei Frolov
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

package io.blert.challenges.mokhaiotl;

import lombok.Getter;

import java.util.Optional;

@Getter
public enum MokhaiotlNpc {
    MOKHAIOTL(14707),
    MOKHAIOTL_SHIELDED(14708),
    MOKHAIOTL_BURROWED(14709),
    DEMONIC_LARVA(14710, 2),
    DEMONIC_RANGE_LARVA(14711, 2),
    DEMONIC_MAGIC_LARVA(14712, 2),
    DEMONIC_MELEE_LARVA(14713, 2),
    VOLATILE_EARTH(14714, 1),
    EARTHEN_SHIELD(14715, 0),
    GIANT_DEMONIC_RANGE_LARVA(14788, 4),
    GIANT_DEMONIC_MAGIC_LARVA(14789, 4);

    private final int id;
    private final int hitpoints;

    public static Optional<MokhaiotlNpc> withId(int id) {
        for (MokhaiotlNpc npc : values()) {
            if (npc.id == id) {
                return Optional.of(npc);
            }
        }
        return Optional.empty();
    }

    public boolean isMokhaiotl() {
        return this == MOKHAIOTL || this == MOKHAIOTL_SHIELDED || this == MOKHAIOTL_BURROWED;
    }

    public boolean isLarva() {
        return this == DEMONIC_LARVA
                || this == DEMONIC_RANGE_LARVA
                || this == DEMONIC_MAGIC_LARVA
                || this == DEMONIC_MELEE_LARVA
                || this == GIANT_DEMONIC_RANGE_LARVA
                || this == GIANT_DEMONIC_MAGIC_LARVA;
    }

    MokhaiotlNpc(int id) {
        this(id, -1);
    }

    MokhaiotlNpc(int id, int hitpoints) {
        this.id = id;
        this.hitpoints = hitpoints;
    }
}
