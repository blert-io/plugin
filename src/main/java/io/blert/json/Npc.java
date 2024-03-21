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

package io.blert.json;

import io.blert.events.NpcEvent;
import io.blert.raid.Hitpoints;
import io.blert.raid.rooms.TrackedNpc;
import io.blert.raid.rooms.maiden.CrabSpawn;
import io.blert.raid.rooms.maiden.MaidenCrab;
import io.blert.raid.rooms.nylocas.Nylo;
import io.blert.raid.rooms.nylocas.SpawnType;
import io.blert.raid.rooms.verzik.VerzikCrab;
import io.blert.raid.rooms.verzik.VerzikPhase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

@Getter
public class Npc {
    private enum Type {
        BASIC,
        MAIDEN_CRAB,
        NYLO,
        VERZIK_CRAB,
    }

    @AllArgsConstructor
    @Getter
    public static class MaidenRedCrab {
        private final CrabSpawn spawn;
        private final MaidenCrab.Position position;
        private boolean scuffed;
    }

    @AllArgsConstructor
    @Getter
    private static class Nylocas {
        private final long parentRoomId;
        private final int wave;
        private final Nylo.Style style;
        private final SpawnType spawnType;
    }

    @AllArgsConstructor
    @Getter
    private static class VerzikNylo {
        private final VerzikPhase phase;
        private final VerzikCrab.Spawn spawn;
    }

    private final Type type;
    private final int id;
    private final long roomId;
    @Setter
    private @Nullable Hitpoints hitpoints = null;

    // Optional NPC properties.
    private @Nullable MaidenRedCrab maidenCrab = null;
    private @Nullable Nylocas nylo = null;
    private @Nullable VerzikNylo verzikCrab = null;

    static Npc fromNpcEvent(NpcEvent event) {
        TrackedNpc.Properties properties = event.getProperties();

        if (properties instanceof MaidenCrab.Properties) {
            var crab = (MaidenCrab.Properties) properties;
            return new Npc(event.getNpcId(), event.getRoomId(), event.getHitpoints(), crab);
        }

        if (properties instanceof Nylo.Properties) {
            var nylo = (Nylo.Properties) properties;
            return new Npc(event.getNpcId(), event.getRoomId(), event.getHitpoints(), nylo);
        }

        if (properties instanceof VerzikCrab.Properties) {
            var crab = (VerzikCrab.Properties) properties;
            return new Npc(event.getNpcId(), event.getRoomId(), event.getHitpoints(), crab);
        }

        return new Npc(event.getNpcId(), event.getRoomId(), event.getHitpoints());
    }

    Npc(int id, long roomId) {
        this.type = Type.BASIC;
        this.id = id;
        this.roomId = roomId;
        this.hitpoints = null;
    }

    Npc(int id, long roomId, @NotNull Hitpoints hitpoints) {
        this.type = Type.BASIC;
        this.id = id;
        this.roomId = roomId;
        this.hitpoints = hitpoints;
    }

    Npc(int id, long roomId, @NotNull Hitpoints hitpoints, MaidenCrab.Properties properties) {
        this.type = Type.MAIDEN_CRAB;
        this.id = id;
        this.roomId = roomId;
        this.hitpoints = hitpoints;
        this.maidenCrab = new MaidenRedCrab(properties.getSpawn(), properties.getPosition(), properties.isScuffed());
    }

    Npc(int id, long roomId, @NotNull Hitpoints hitpoints, Nylo.Properties properties) {
        this.type = Type.NYLO;
        this.id = id;
        this.roomId = roomId;
        this.hitpoints = hitpoints;
        this.nylo = new Nylocas(properties.getParentRoomId(), properties.getWave(),
                properties.getStyle(), properties.getSpawnType());
    }

    Npc(int id, long roomId, @NotNull Hitpoints hitpoints, VerzikCrab.Properties properties) {
        this.type = Type.VERZIK_CRAB;
        this.id = id;
        this.roomId = roomId;
        this.hitpoints = hitpoints;
        this.verzikCrab = new VerzikNylo(properties.getPhase(), properties.getSpawn());
    }
}