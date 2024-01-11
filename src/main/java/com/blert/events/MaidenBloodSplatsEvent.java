/*
 * Copyright (c) 2023 Alexei Frolov
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

package com.blert.events;

import com.blert.raid.rooms.Room;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MaidenBloodSplatsEvent extends Event {
    private final List<WorldPoint> bloodSplats;

    public MaidenBloodSplatsEvent(int tick, List<WorldPoint> bloodSplats) {
        super(EventType.MAIDEN_BLOOD_SPLATS, Room.MAIDEN, tick, null);
        this.bloodSplats = bloodSplats;
    }

    @Override
    protected String eventDataString() {
        String splats = bloodSplats
                .stream()
                .map(wp -> "(" + wp.getX() + "," + wp.getY() + ")")
                .collect(Collectors.joining(","));
        return "blood_splats=[" + splats + ']';
    }
}
