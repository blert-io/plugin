package com.blert.events;

import com.blert.raid.Hitpoints;
import net.runelite.api.coords.WorldPoint;

import java.util.Optional;

public abstract class PlayerUpdateEvent extends Event {
    protected PlayerUpdateEvent(int tick, WorldPoint point) {
        super(EventType.PLAYER_UPDATE, tick, point);
    }

    /**
     * Returns the username of the player whose data is being reported.
     */
    public abstract String getUsername();

    /**
     * Returns the player's hitpoints, if available.
     */
    public abstract Optional<Hitpoints> getHitpoints();

    @Override
    protected String eventDataString() {
        StringBuilder string = new StringBuilder("player=(");
        string.append("name=").append(getUsername());

        getHitpoints().ifPresent(hp -> {
            string.append(", hp=").append(hp);
        });

        string.append(')');
        return string.toString();
    }
}
