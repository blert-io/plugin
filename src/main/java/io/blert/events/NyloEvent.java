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

package io.blert.events;

import io.blert.raid.rooms.Room;
import io.blert.raid.rooms.nylocas.Nylo;
import io.blert.raid.rooms.nylocas.SpawnType;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * Base class for events containing information about a {@link Nylo}.
 */
@Getter
public abstract class NyloEvent extends Event {
    protected final long roomId;
    protected final long parentRoomId;
    protected final int wave;
    protected final Nylo.Style style;
    protected final SpawnType spawnType;
    protected final boolean big;

    protected NyloEvent(EventType type, int tick, WorldPoint point, Nylo nylo) {
        super(type, Room.NYLOCAS, tick, point);
        this.roomId = nylo.getRoomId();
        this.parentRoomId = nylo.getParent().map(Nylo::getRoomId).orElse(0L);
        this.wave = nylo.getWave();
        this.style = nylo.getStyle();
        this.spawnType = nylo.getSpawnType();
        this.big = nylo.isBig();
    }

    @Override
    protected String eventDataString() {
        StringBuilder sb = new StringBuilder();
        sb.append("roomId=").append(roomId);
        if (parentRoomId != 0) {
            sb.append(", parentRoomId=").append(parentRoomId);
        }
        sb.append(", wave=").append(wave);
        sb.append(", style=").append(style);
        sb.append(", spawnType=").append(spawnType);
        sb.append(", big=").append(big);
        return sb.toString();
    }
}
