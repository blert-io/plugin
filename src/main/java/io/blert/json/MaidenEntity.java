/*
 * Copyright (c) 2023-2024 Alexei Frolov
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

import io.blert.raid.Hitpoints;
import io.blert.raid.rooms.maiden.CrabSpawn;
import io.blert.raid.rooms.maiden.MaidenCrab;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JSON object grouping all types of trackable maiden entities to avoid top-level `Event` clutter.
 */
@Getter
public class MaidenEntity {
    private @Nullable Crab crab;
    private List<Coords> bloodSplats;

    @AllArgsConstructor
    private static class Crab {
        final CrabSpawn spawn;
        final MaidenCrab.Position position;
        boolean scuffed;
        @Nullable
        Hitpoints hitpoints;
    }

    public static MaidenEntity fromCrab(CrabSpawn spawn, MaidenCrab crab) {
        MaidenEntity entity = new MaidenEntity();
        entity.crab = new Crab(spawn, crab.getPosition(), crab.isScuffed(), null);
        return entity;
    }

    public static MaidenEntity crabLeak(CrabSpawn spawn, MaidenCrab.Position position, Hitpoints hitpoints) {
        MaidenEntity entity = new MaidenEntity();
        entity.crab = new Crab(spawn, position, false, hitpoints);
        return entity;
    }

    public static MaidenEntity bloodSplats(List<WorldPoint> points) {
        MaidenEntity entity = new MaidenEntity();
        entity.bloodSplats = points.stream().map(Coords::fromWorldPoint).collect(Collectors.toList());
        return entity;
    }
}
