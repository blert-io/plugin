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

package io.blert.challenges.tob.rooms.bloat;

import io.blert.challenges.tob.TheatreChallenge;
import io.blert.challenges.tob.rooms.Room;
import io.blert.challenges.tob.rooms.RoomDataTracker;
import io.blert.core.ChallengeRecordingConfig;
import io.blert.events.tob.BloatHandsEvent;
import io.blert.util.Location;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GraphicsObjectCreated;

public class BloatDataTracker extends RoomDataTracker {
    private static final int BLOAT_HANDS_GRAPHICS_START_ID = 1570;
    private static final int BLOAT_HANDS_GRAPHICS_END_ID = 1573;

    private final List<WorldPoint> fallingHands = new ArrayList<>(16);

    public BloatDataTracker(TheatreChallenge manager, Client client, ChallengeRecordingConfig recordingConfig) {
        super(manager, client, recordingConfig, Room.BLOAT, true);
    }

    @Override
    protected void onTick() {
        super.onTick();

        if (!fallingHands.isEmpty()) {
            dispatchEvent(BloatHandsEvent.drop(getTick(), new ArrayList<>(fallingHands)));
            fallingHands.clear();
        }
    }

    @Override
    protected void onGraphicsObjectCreation(GraphicsObjectCreated event) {
        GraphicsObject object = event.getGraphicsObject();
        if (object.getId() >= BLOAT_HANDS_GRAPHICS_START_ID && object.getId() <= BLOAT_HANDS_GRAPHICS_END_ID) {
            WorldPoint point = Location.getWorldLocation(client, WorldPoint.fromLocal(client, object.getLocation()));
            fallingHands.add(point);
        }
    }
}
