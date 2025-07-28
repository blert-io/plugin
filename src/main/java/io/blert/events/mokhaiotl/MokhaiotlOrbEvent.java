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

import io.blert.challenges.mokhaiotl.Orb;
import io.blert.core.Stage;
import io.blert.events.Event;
import io.blert.events.EventType;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class MokhaiotlOrbEvent extends Event {
    private final Orb.Source source;
    private final WorldPoint sourcePoint;
    private final io.blert.proto.Event.AttackStyle.Style style;
    private final int startTick;
    private final int endTick;

    public MokhaiotlOrbEvent(Stage stage, int tick, Orb orb, WorldPoint sourcePoint) {
        super(EventType.MOKHAIOTL_ORB, stage, tick, null);
        this.source = orb.getSource();
        this.sourcePoint = sourcePoint;
        this.style = orb.getStyle();
        this.startTick = orb.getSpawnTick();
        this.endTick = tick;
    }

    @Override
    protected String eventDataString() {
        return String.format("source=%s, sourcePoint=%s, style=%s, startTick=%d, endTick=%d",
                source, sourcePoint, style, startTick, endTick);
    }
}
