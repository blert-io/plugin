/*
 * Copyright (c) 2026 Alexei Frolov
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

package io.blert.challenges.tfa;

import io.blert.core.Stage;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

public enum Location {
    ELSEWHERE(null),
    LOBBY(null),
    HUB(null),
    GUARDIAN(Stage.TFA_GUARDIAN),
    EZEKIEL(Stage.TFA_EZEKIEL),
    BORK(Stage.TFA_BORK),
    MOIA(Stage.TFA_MOIA),
    DREADNOTH(Stage.TFA_DREADNOTH),
    XARTH(Stage.TFA_XARTH),
    DAGONHAI(Stage.TFA_DAGONHAI),
    OCELLUS(Stage.TFA_OCELLUS),
    ;

    // The hub is the Guardian's region, on a different plane. The party
    // returns to it after each boss from the first row onwards.
    private static final int GUARDIAN_REGION_ID = 16471;
    private static final int GUARDIAN_PLANE = 0;

    private static final Map<Integer, Location> REGIONS = Map.of(
            16470, LOBBY,
            16214, EZEKIEL,
            16727, BORK,
            16216, MOIA,
            16728, DREADNOTH,
            16726, XARTH,
            16215, DAGONHAI,
            16472, OCELLUS);

    @Getter
    @Nullable
    private final Stage stage;

    Location(@Nullable Stage stage) {
        this.stage = stage;
    }

    public static Location fromWorldPoint(WorldPoint worldPoint) {
        if (worldPoint.getRegionID() == GUARDIAN_REGION_ID) {
            return worldPoint.getPlane() == GUARDIAN_PLANE ? GUARDIAN : HUB;
        }
        return REGIONS.getOrDefault(worldPoint.getRegionID(), ELSEWHERE);
    }

    public boolean inRaid() {
        return this != ELSEWHERE && this != LOBBY;
    }
}
