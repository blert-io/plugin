package com.blert.raid;

import com.blert.events.Event;
import com.blert.events.EventHandler;
import com.blert.events.RaidStartEvent;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

@Slf4j

public class RaidManager {
    @Inject
    private Client client;

    @Setter
    private @Nullable EventHandler eventHandler = null;

    private RaidStatus status = RaidStatus.INACTIVE;
    private Location location = Location.ELSEWHERE;

    private final Set<String> raidParty = new HashSet<>();

    @Nullable RoomDataTracker roomDataTracker = null;

    public boolean inRaid() {
        return status.isActive();
    }

    public void updateState() {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return;
        }

        WorldPoint point = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
        Location new_location = Location.fromWorldPoint(point);
        if (new_location.inRaid() && !location.inRaid()) {
            status = RaidStatus.ACTIVE;
            dispatchEvent(new RaidStartEvent());
        } else if (!new_location.inRaid() && location.inRaid()) {
            status = RaidStatus.INACTIVE;
            roomDataTracker = null;
        }

        if (new_location.inMaiden() && !location.inMaiden()) {
            roomDataTracker = new MaidenDataTracker(client, eventHandler);
        }

        location = new_location;

        if (roomDataTracker != null) {
            roomDataTracker.tick();
            if (roomDataTracker.roomCompleted()) {
                roomDataTracker = null;
            }
        }
    }

    private void dispatchEvent(Event event) {
        if (eventHandler != null) {
            eventHandler.handleEvent(event);
        }
    }
}
