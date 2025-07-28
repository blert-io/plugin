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

package io.blert.events.mokhaiotl;

import io.blert.core.Stage;
import io.blert.events.Event;
import io.blert.events.EventType;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MokhaiotlObjectsEvent extends Event {
    private final List<WorldPoint> rocksSpawned;
    private final List<WorldPoint> rocksDespawned;
    private final List<WorldPoint> splatsSpawned;
    private final List<WorldPoint> splatsDespawned;

    public MokhaiotlObjectsEvent(
            Stage stage,
            int tick,
            List<WorldPoint> rocksSpawned,
            List<WorldPoint> rocksDespawned,
            List<WorldPoint> splatsSpawned,
            List<WorldPoint> splatsDespawned
    ) {
        super(EventType.MOKHAIOTL_OBJECTS, stage, tick, null);
        this.rocksSpawned = rocksSpawned;
        this.rocksDespawned = rocksDespawned;
        this.splatsSpawned = splatsSpawned;
        this.splatsDespawned = splatsDespawned;
    }

    @Override
    protected String eventDataString() {
        return String.format("rocks_spawned=%s, rocks_despawned=%s, splats_spawned=%s, splats_despawned=%s",
                rocksSpawned.stream().map(WorldPoint::toString).collect(Collectors.joining(", ")),
                rocksDespawned.stream().map(WorldPoint::toString).collect(Collectors.joining(", ")),
                splatsSpawned.stream().map(WorldPoint::toString).collect(Collectors.joining(", ")),
                splatsDespawned.stream().map(WorldPoint::toString).collect(Collectors.joining(", "))
        );
    }
}
