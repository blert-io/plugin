package io.blert.challenges.inferno;

import io.blert.core.BasicTrackedNpc;
import io.blert.core.Hitpoints;
import lombok.Getter;
import lombok.NonNull;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

@Getter
public class Pillar extends BasicTrackedNpc {
    enum Location {
        WEST,
        EAST,
        SOUTH,
    }

    private final Location location;

    private final WorldPoint WEST_PILLAR_LOCATION = new WorldPoint(2257, 5349, 0);
    private final WorldPoint EAST_PILLAR_LOCATION = new WorldPoint(2274, 5351, 0);
    private final WorldPoint SOUTH_PILLAR_LOCATION = new WorldPoint(2267, 5335, 0);

    public Pillar(@NonNull NPC npc, long roomId, WorldPoint point) {
        super(npc, roomId, new Hitpoints(InfernoNpc.ROCKY_SUPPORT.getHitpoints()));
        if (point.equals(WEST_PILLAR_LOCATION)) {
            this.location = Location.WEST;
        } else if (point.equals(EAST_PILLAR_LOCATION)) {
            this.location = Location.EAST;
        } else {
            this.location = Location.SOUTH;
        }
    }
}
