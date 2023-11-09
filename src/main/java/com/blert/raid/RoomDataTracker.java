package com.blert.raid;

import com.blert.events.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;

@Slf4j

public abstract class RoomDataTracker {
    private final Client client;

    private @Nullable EventHandler eventHandler = null;

    private final Room room;

    protected int tick = 0;

    RoomDataTracker(Client client, EventHandler eventHandler, Room room) {
        this.client = client;
        this.eventHandler = eventHandler;
        this.room = room;

        dispatchEvent(new RoomStartEvent(room));
    }

    /**
     * Checks whether the room has been completed.
     * @return true if the room is over.
     */
    abstract boolean roomCompleted();

    /**
     * Collects data about the raid room in the current game tick.
     * Implementations should override this method to gather any relevant information. They *MUST* call `super.tick()`
     * to gather all common room data.
     */
    void tick() {
        tick++;

        Player player = client.getLocalPlayer();
        if (player != null) {
            dispatchEvent(new LocalPlayerUpdateEvent(tick, client));
        }
    }

    protected void dispatchEvent(Event event) {
        if (eventHandler != null) {
            eventHandler.handleEvent(event);
        }
    }
}
