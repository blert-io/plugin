package io.blert.challenges.chambers;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

public enum CoxLocation {
    LOBBY,
    ROOM_1,
    ROOM_2,
    ROOM_3,
    ROOM_4,
    ROOM_5;
    public static final WorldArea LOBBY_AREA = new WorldArea(12686, 11971, 1, 1, 3);
    public static CoxLocation fromWorldPoint(WorldPoint point) {
        switch (point.getRegionID()) {
            case 12345: // Replace with actual region IDs
                return LOBBY_AREA.contains(point) ? LOBBY : null;
            case 12346:
                return ROOM_1;
            case 12347:
                return ROOM_2;
            case 12348:
                return ROOM_3;
            case 12349:
                return ROOM_4;
            case 12350:
                return ROOM_5;
            default:
                return null;
        }
    }
}