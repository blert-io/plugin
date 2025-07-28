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
