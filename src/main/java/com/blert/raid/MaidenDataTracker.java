package com.blert.raid;

import com.blert.events.Event;
import com.blert.events.EventHandler;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

@Slf4j

public class MaidenDataTracker extends RoomDataTracker {
    MaidenDataTracker(Client client, EventHandler eventHandler) {
        super(client, eventHandler, Room.MAIDEN);
    }

    @Override
    boolean roomCompleted() {
        return false;
    }

    @Override
    void tick() {
        super.tick();
    }
}
