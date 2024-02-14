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
import io.blert.raid.rooms.maiden.CrabSpawn;
import io.blert.raid.rooms.maiden.MaidenCrab;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class MaidenCrabLeakEvent extends NpcEvent {
    public MaidenCrabLeakEvent(int tick, WorldPoint point, MaidenCrab crab) {
        super(EventType.MAIDEN_CRAB_LEAK, Room.MAIDEN, tick, point, crab);
    }

    @Override
    public MaidenCrab.Properties getProperties() {
        return (MaidenCrab.Properties) super.getProperties();
    }

    public CrabSpawn getSpawn() {
        return getProperties().getSpawn();
    }

    public MaidenCrab.Position getPosition() {
        return getProperties().getPosition();
    }

    @Override
    protected String eventDataString() {
        return "crab_leak=(crab=" + getSpawn() + ' ' + getPosition() + ", hp=" + getHitpoints().getCurrent() + ')';
    }
}
