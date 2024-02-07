/*
 * Copyright (c) 2023-2024 Alexei Frolov
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

package io.blert.raid;

import io.blert.events.*;
import io.blert.raid.rooms.RoomDataTracker;
import io.blert.raid.rooms.bloat.BloatDataTracker;
import io.blert.raid.rooms.maiden.MaidenDataTracker;
import io.blert.raid.rooms.nylocas.NylocasDataTracker;
import io.blert.raid.rooms.sotetseg.SotetsegDataTracker;
import io.blert.raid.rooms.verzik.VerzikDataTracker;
import io.blert.raid.rooms.xarpus.XarpusDataTracker;
import joptsimple.internal.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j

public class RaidManager {
    private static final int TOB_ROOM_STATUS_VARBIT = 6447;

    private static final Pattern RAID_ENTRY_REGEX =
            Pattern.compile("You enter the Theatre of Blood \\((\\w+) Mode\\)\\.\\.\\.");

    @Inject
    private Client client;

    @Inject
    private EventBus eventBus;

    @Inject
    @Getter
    private ClientThread clientThread;

    @Setter
    private @Nullable EventHandler eventHandler = null;

    private Location location = Location.ELSEWHERE;
    private RaidState state = RaidState.INACTIVE;

    @Getter
    private Mode raidMode = null;

    /**
     * Players in the raid party, stored in orb order.
     */
    private final Map<String, Raider> party = new LinkedHashMap<>();

    private RoomState roomState = RoomState.INACTIVE;
    @Nullable
    RoomDataTracker roomDataTracker = null;

    public void initialize(EventHandler handler) {
        this.eventHandler = handler;
        eventBus.register(this);
    }

    public boolean inRaid() {
        return state.isActive();
    }

    public int getRaidScale() {
        return inRaid() ? party.size() : 0;
    }

    public boolean playerIsInRaid(@Nullable String username) {
        return username != null && party.containsKey(username.toLowerCase());
    }

    public Raider getRaider(@Nullable String username) {
        return username != null ? party.get(username.toLowerCase()) : null;
    }

    public Collection<Raider> getRaiders() {
        return party.values();
    }

    public void updateRaidMode(Mode mode) {
        if (raidMode != mode) {
            log.debug("Raid mode set to " + mode);
            raidMode = mode;
            dispatchEvent(new RaidUpdateEvent(mode));
        }
    }

    public void tick() {
        updateLocation();

        if (!inRaid()) {
            return;
        }

        updatePartyInformation();

        if (roomDataTracker != null) {
            roomDataTracker.checkEntry();

            if (roomDataTracker.notStarted() && roomState.isActive() && roomDataTracker.playersAreInRoom()) {
                // The room may already be active when entered (e.g. as a spectator); start its tracker.
                roomDataTracker.startRoomInaccurate();
                log.debug("Room " + roomDataTracker.getRoom() + " started via activity check");
            }

            roomDataTracker.tick();
        }
    }

    public void dispatchEvent(Event event) {
        if (eventHandler != null) {
            eventHandler.handleEvent(client.getTickCount(), event);
        }
    }

    /**
     * Invokes a callback for each username displayed in the ToB orb list.
     *
     * @param callback Function to run on each user.
     */
    public void forEachOrb(BiConsumer<Integer, String> callback) {
        // ID of the client string containing the username of the first member in a ToB party. Subsequent party members'
        // usernames (if present) occupy the following four IDs.
        final int TOB_P1_VARCSTR_ID = 330;
        for (int player = 0; player < 5; player++) {
            String username = Text.standardize(client.getVarcStrValue(TOB_P1_VARCSTR_ID + player));
            if (!Strings.isNullOrEmpty(username)) {
                callback.accept(player, username);
            }
        }
    }

    private void startRaid(@Nullable Mode mode) {
        log.debug("Starting new raid");
        state = RaidState.ACTIVE;
        raidMode = mode;

        // Defer sending the raid start event as party information is not always immediately available.
        clientThread.invokeLater(() -> {
            initializeParty();

            List<String> names = party.values().stream().map(Raider::getUsername).collect(Collectors.toList());
            dispatchEvent(new RaidStartEvent(names, raidMode));
        });
    }

    private void endRaid() {
        log.debug("Raid completed");

        dispatchEvent(new RaidEndEvent());

        clearRoomDataTracker();
        state = RaidState.INACTIVE;
        raidMode = null;
        party.clear();
    }

    private void updateLocation() {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return;
        }

        Location loc = Location.fromWorldPoint(WorldPoint.fromLocalInstance(client, player.getLocalLocation()));
        if (location == loc) {
            return;
        }

        location = loc;
        log.debug("Location changed to " + loc);

        clearRoomDataTracker();

        if (location.inMaidenInstance()) {
            roomDataTracker = new MaidenDataTracker(this, client);
        } else if (location.inBloatInstance()) {
            roomDataTracker = new BloatDataTracker(this, client);
        } else if (location.inNylocasInstance()) {
            roomDataTracker = new NylocasDataTracker(this, client);
        } else if (location.inSotetsegInstance()) {
            roomDataTracker = new SotetsegDataTracker(this, client);
        } else if (location.inXarpusInstance()) {
            roomDataTracker = new XarpusDataTracker(this, client);
        } else if (location.inVerzikInstance()) {
            roomDataTracker = new VerzikDataTracker(this, client);
        }

        if (roomDataTracker != null) {
            eventBus.register(roomDataTracker);
        }
    }

    @Subscribe(priority = 10)
    private void onVarbitChanged(VarbitChanged varbit) {
        updateLocation();

        if (varbit.getVarbitId() == Varbits.THEATRE_OF_BLOOD) {
            if (state.isInactive() && varbit.getValue() == 2) {
                startRaid(null);
            } else if (state.isActive() && varbit.getValue() < 2) {
                // The raid has finished; clean up.
                endRaid();
            }
        }

        int roomStatus = client.getVarbitValue(TOB_ROOM_STATUS_VARBIT);
        var state = RoomState.fromVarbit(roomStatus);
        if (state.isEmpty()) {
            log.error("Unknown value for room status varbit: " + varbit.getValue());
            return;
        }

        RoomState previousState = roomState;
        roomState = state.get();
        if (previousState != roomState) {
            log.debug("Room status changed from " + previousState + " to " + roomState);
        }
    }

    @Subscribe
    private void onChatMessage(ChatMessage message) {
        if (message.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        if (state.isInactive()) {
            Matcher matcher = RAID_ENTRY_REGEX.matcher(message.getMessage());
            if (matcher.matches()) {
                startRaid(Mode.parse(matcher.group(1)).orElse(null));
            }
        }
    }

    private void initializeParty() {
        String localPlayer = client.getLocalPlayer().getName();
        forEachOrb((orb, username) -> party.put(username.toLowerCase(), new Raider(username, username.equals(localPlayer))));
    }

    private void updatePartyInformation() {
        // Toggle the players who are currently connected to the raid.
        party.values().forEach(r -> r.setActive(false));
        forEachOrb((orb, username) -> {
            Raider raider = party.get(username.toLowerCase());
            if (raider != null) {
                raider.setActive(true);
            }
        });
    }

    private void clearRoomDataTracker() {
        if (roomDataTracker != null) {
            roomDataTracker.finishRoom();
            eventBus.unregister(roomDataTracker);
            roomDataTracker = null;
        }
    }
}
