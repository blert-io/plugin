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

import lombok.Getter;
import net.runelite.api.Projectile;

@Getter
public class Orb {
    public static final int MELEE_PROJECTILE_ID = 3378;
    public static final int MAGE_PROJECTILE_ID = 3379;
    public static final int RANGED_PROJECTILE_ID = 3380;

    public static boolean isOrb(int projectileId) {
        return projectileId == MELEE_PROJECTILE_ID
                || projectileId == RANGED_PROJECTILE_ID
                || projectileId == MAGE_PROJECTILE_ID;
    }

    public enum Source {
        MOKHAIOTL(1),
        BALL(2);

        @Getter
        private final int id;

        Source(int id) {
            this.id = id;
        }
    }

    public enum Style {
        MELEE(0),
        RANGE(1),
        MAGE(2);

        @Getter
        private final int id;

        Style(int id) {
            this.id = id;
        }
    }

    private final Projectile projectile;
    private final Source source;
    private final int spawnTick;
    private int landedTick;

    public Orb(Projectile projectile, Source source, int spawnTick) {
        if (!isOrb(projectile.getId())) {
            throw new IllegalArgumentException("Invalid projectile ID for Orb: " + projectile.getId());
        }
        this.projectile = projectile;
        this.source = source;
        this.spawnTick = spawnTick;
        this.landedTick = -1;
    }

    public boolean isActive() {
        return landedTick == -1;
    }

    public void setLanded(int tick) {
        if (isActive()) {
            this.landedTick = tick;
        }
    }

    public Style getStyle() {
        switch (projectile.getId()) {
            case MELEE_PROJECTILE_ID:
                return Style.MELEE;
            case RANGED_PROJECTILE_ID:
                return Style.RANGE;
            case MAGE_PROJECTILE_ID:
                return Style.MAGE;
            default:
                throw new IllegalArgumentException("Unknown projectile ID: " + projectile.getId());
        }
    }
}
