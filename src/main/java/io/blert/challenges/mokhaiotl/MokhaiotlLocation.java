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
