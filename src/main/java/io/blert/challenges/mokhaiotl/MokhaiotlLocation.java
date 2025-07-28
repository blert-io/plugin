package io.blert.challenges.mokhaiotl;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

public enum MokhaiotlLocation {
    ELSEWHERE,
    CAVERN,
    LOBBY,
    DELVE_1,
    DELVE_MID,
    DELVE_DEEP;

    private static final int CAVERN_REGION_ID = 5268;
    private static final int DELVE_REGION_ID = 5269;  // Delve 1 and lobby area
    private static final int DELVE_MID_REGION_ID = 13668; // Delves 2-5
    private static final int DELVE_DEEP_REGION_ID = 14180; // Delves 6+

    private static final WorldArea DELVE_1_AREA = new WorldArea(1299, 9559, 24, 26, 0);

    public static MokhaiotlLocation fromWorldPoint(WorldPoint point) {
        switch (point.getRegionID()) {
            case CAVERN_REGION_ID:
                return CAVERN;
            case DELVE_REGION_ID:
                return DELVE_1_AREA.contains(point) ? DELVE_1 : LOBBY;
            case DELVE_MID_REGION_ID:
                return DELVE_MID;
            case DELVE_DEEP_REGION_ID:
                return DELVE_DEEP;
            default:
                return ELSEWHERE;
        }
    }

    public boolean inCavern() {
        return this == CAVERN || this == LOBBY;
    }

    public boolean inMokhaiotl() {
        return this == DELVE_1 || this == DELVE_MID || this == DELVE_DEEP;
    }
}
