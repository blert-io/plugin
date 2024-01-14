/*
 * Copyright (c) 2023 Alexei Frolov
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

package com.blert.raid;

import com.blert.events.Event;
import com.blert.events.EventHandler;
import com.blert.events.RaidStartEvent;
import com.blert.raid.rooms.RoomDataTracker;
import com.blert.raid.rooms.maiden.MaidenDataTracker;
import joptsimple.internal.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j

public class RaidManager {
    private static final int TOB_ROOM_STATUS_VARBIT = 6447;

    private static final Pattern RAID_ENTRY_REGEX =
            Pattern.compile("You enter the Theatre of Blood \\((\\w+) Mode\\)\\.\\.\\.");

    @Inject
    private Client client;

    @Inject
    private EventBus eventBus;

    private @Nullable EventHandler eventHandler = null;

    private RaidStatus status = RaidStatus.INACTIVE;

    @Getter
    @Setter
    private Mode raidMode = null;

    private final List<String> raiders = new ArrayList<>();

    @Nullable
    RoomDataTracker roomDataTracker = null;

    public void initialize(EventHandler handler) {
        this.eventHandler = handler;
        eventBus.register(this);
    }

    public boolean inRaid() {
        return status.isActive();
    }

    public int getRaidScale() {
        return inRaid() ? raiders.size() : 0;
    }

    public void updateRaidMode(Mode mode) {
        if (raidMode != mode) {
            log.debug("Raid mode set to " + mode);
            raidMode = mode;
        }
    }

    public void tick() {
        if (roomDataTracker != null) {
            roomDataTracker.tick();
        }
    }

    public void dispatchEvent(Event event) {
        if (eventHandler != null) {
            eventHandler.handleEvent(client.getTickCount(), event);
        }
    }

    private void startRaid() {
        status = RaidStatus.ACTIVE;
        dispatchEvent(new RaidStartEvent());
    }

    private void endRaid() {
        clearRoomDataTracker();
        status = RaidStatus.INACTIVE;
        raidMode = null;
        raiders.clear();
    }

    @Subscribe
    private void onVarbitChanged(VarbitChanged varbit) {
        if (varbit.getVarbitId() == Varbits.THEATRE_OF_BLOOD) {
            if (status == RaidStatus.INACTIVE && varbit.getValue() == 2) {
                startRaid();
            } else if (status == RaidStatus.ACTIVE && varbit.getValue() < 2) {
                // The raid has finished; clean up.
                endRaid();
            }
            return;
        }

        if (varbit.getVarbitId() == TOB_ROOM_STATUS_VARBIT) {
            if (roomDataTracker != null && varbit.getValue() == 0) {
                // Room has been completed.
                clearRoomDataTracker();
                return;
            }

            if (varbit.getValue() == 1) {
                // A new room has been started; initialize its data tracker.
                Player player = client.getLocalPlayer();
                if (player == null) {
                    return;
                }

                WorldPoint point = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
                Location location = Location.fromWorldPoint(point);

                if (location.inMaidenInstance()) {
                    // When Maiden is started, players can no longer enter the raid. Grab final information about
                    // raiders and scale.
                    finalizePartyInformation();
                    roomDataTracker = new MaidenDataTracker(this, client);
                }

                if (roomDataTracker != null) {
                    eventBus.register(roomDataTracker);
                    roomDataTracker.startRoom();
                }
            }
        }
    }

    @Subscribe
    private void onChatMessage(ChatMessage message) {
        Matcher matcher = RAID_ENTRY_REGEX.matcher(message.getMessage());
        if (matcher.matches()) {
            Mode.parse(matcher.group(1)).ifPresent(this::updateRaidMode);
        }
    }

    private void finalizePartyInformation() {
        // ID of the client string containing the username of the first member in a ToB party. Subsequent party members'
        // usernames (if present) occupy the following four IDs.
        final int TOB_P1_VARCSTR_ID = 330;

        for (int player = 0; player < 5; player++) {
            String username = client.getVarcStrValue(TOB_P1_VARCSTR_ID + player);
            if (!Strings.isNullOrEmpty(username)) {
                raiders.add(username);
            }
        }
    }

    private void clearRoomDataTracker() {
        if (roomDataTracker != null) {
            roomDataTracker.finishRoom();
            eventBus.unregister(roomDataTracker);
            roomDataTracker = null;
        }
    }
}
