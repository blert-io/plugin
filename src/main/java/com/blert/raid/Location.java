package com.blert.raid;

import com.blert.raid.rooms.Room;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

public enum Location {
    ELSEWHERE,  // Anywhere but TOB (why?)
    LOBBY,
    MAIDEN_INSTANCE,
    MAIDEN_ROOM;

    private static final int LOBBY_REGION_ID = 14642;

    // The corridor leading up to Maiden.
    private static final int CORRIDOR_REGION_ID = 12869;
    private static final int MAIDEN_REGION_ID = 12613;

    private static final WorldArea MAIDEN_ROOM_AREA = new WorldArea(3159, 4434, 29, 25, 0);
    private static final WorldArea MAIDEN_STAIRCASE_AREA = new WorldArea(3185, 4444, 3, 6, 0);

    public static Location fromWorldPoint(WorldPoint worldPoint) {
        switch (worldPoint.getRegionID()) {
            case LOBBY_REGION_ID:
                return LOBBY;
            case CORRIDOR_REGION_ID:
            case MAIDEN_REGION_ID:
                if (MAIDEN_ROOM_AREA.contains(worldPoint) && !MAIDEN_STAIRCASE_AREA.contains(worldPoint)) {
                    return MAIDEN_ROOM;
                }
                return MAIDEN_INSTANCE;
            default:
                return ELSEWHERE;
        }
    }

    public boolean inRoom(Room room) {
        switch (room) {
            case MAIDEN:
                return inMaiden();
            case BLOAT:
                return false;
            case NYLOCAS:
                return false;
            case SOTETSEG:
                return false;
            case XARPUS:
                return false;
            case VERZIK:
                return false;
            default:
                return false;
        }
    }

    public boolean inRaid() {
        return this != ELSEWHERE && this != LOBBY;
    }

    public boolean inMaidenInstance() {
        return this == MAIDEN_INSTANCE || inMaiden();
    }

    public boolean inMaiden() {
        return this == MAIDEN_ROOM;
    }
}
