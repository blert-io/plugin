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

package io.blert.challenges.tob;

import io.blert.challenges.tob.rooms.RoomDataTracker;
import io.blert.challenges.tob.rooms.bloat.BloatDataTracker;
import io.blert.challenges.tob.rooms.maiden.MaidenDataTracker;
import io.blert.challenges.tob.rooms.nylocas.NylocasDataTracker;
import io.blert.challenges.tob.rooms.sotetseg.SotetsegDataTracker;
import io.blert.challenges.tob.rooms.verzik.VerzikDataTracker;
import io.blert.challenges.tob.rooms.xarpus.XarpusDataTracker;
import io.blert.core.ChallengeMode;
import io.blert.core.Raider;
import io.blert.events.*;
import io.blert.util.DeferredTask;
import io.blert.util.Tick;
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
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j

public class RaidManager {
    private static final int TOB_ROOM_STATUS_VARBIT = 6447;
    private static final int TOB_PARTY_LIST_COMPONENT_ID = 1835020;

    private static final Pattern RAID_ENTRY_REGEX_1P =
            Pattern.compile("You enter the Theatre of Blood \\((\\w+) Mode\\)\\.\\.\\.");
    private static final Pattern RAID_ENTRY_REGEX_3P =
            Pattern.compile("(.+) has entered the Theatre of Blood \\((\\w+) Mode\\). Step inside to join (her|him|them)\\.\\.\\.");
    private static final Pattern RAID_COMPLETION_REGEX =
            Pattern.compile("Theatre of Blood total completion time: ([0-9:.]+)");

    @Inject
    private Client client;

    @Inject
    private EventBus eventBus;

    @Inject
    @Getter
    private ClientThread clientThread;

    @Setter
    private @Nullable EventHandler eventHandler = null;
    List<Event> pendingRaidEvents = new ArrayList<>();

    private Location location = Location.ELSEWHERE;
    private RaidState state = RaidState.INACTIVE;

    @Getter
    private ChallengeMode raidMode = null;

    /**
     * Players in the raid party, stored in orb order.
     */
    private final Map<String, Raider> party = new LinkedHashMap<>();

    private RoomState roomState = RoomState.INACTIVE;
    @Nullable
    RoomDataTracker roomDataTracker = null;

    private @Nullable DeferredTask deferredTask = null;

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
        return username != null && party.containsKey(Text.standardize(username));
    }

    public Raider getRaider(@Nullable String username) {
        return username != null ? party.get(Text.standardize(username)) : null;
    }

    public Collection<Raider> getRaiders() {
        return party.values();
    }

    public void updateRaidMode(ChallengeMode mode) {
        if (raidMode != mode) {
            log.debug("Raid mode set to " + mode);
            raidMode = mode;
            dispatchEvent(new ChallengeUpdateEvent(mode));
        }
    }

    public void tick() {
        updateLocation();

        if (location == Location.LOBBY && client.getVarbitValue(Varbits.THEATRE_OF_BLOOD) == 1) {
            // If the player is in a party in the raid lobby, grab the party information from the party widget.
            initializePartyFromLobby();
        }

        if (!inRaid()) {
            return;
        }

        updatePartyInformation();

        if (roomDataTracker != null) {
            roomDataTracker.checkEntry();

            if (roomDataTracker.notStarted() && roomState.isActive() && roomDataTracker.playersAreInRoom()) {
                // The room may already be active when entered (e.g. as a spectator); start its tracker.
                if (state == RaidState.STARTING) {
                    startRaid();
                }

                roomDataTracker.startRoomInaccurate();
                log.debug("Room " + roomDataTracker.getRoom() + " started via activity check");
            }

            roomDataTracker.tick();
        }

        if (deferredTask != null) {
            deferredTask.tick();
        }
    }

    public void dispatchEvent(Event event) {
        if (state == RaidState.INACTIVE || state == RaidState.STARTING) {
            if (event.getType() != EventType.RAID_START && event.getType() != EventType.RAID_END) {
                pendingRaidEvents.add(event);
                return;
            }
        }

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
            String username = client.getVarcStrValue(TOB_P1_VARCSTR_ID + player);
            if (!Strings.isNullOrEmpty(username)) {
                callback.accept(player, Text.sanitize(username));
            }
        }
    }

    private void queueRaidStart(@Nullable ChallengeMode mode, boolean reinitializeParty) {
        log.info("Starting new raid");
        state = RaidState.STARTING;
        raidMode = mode;

        if (reinitializeParty) {
            // When players join the raid, the orb list does not immediately update, nor does it update all at once.
            // (Some players may take longer to load in, thanks Jagex!) Therefore, wait a few ticks before starting.
            // This value is arbitrary and may need to be adjusted.
            final int TICKS_TO_DELAY_ORB_CHECK = 5;
            deferredTask = new DeferredTask(() -> {
                initializePartyFromOrbs();
                startRaid();
            }, TICKS_TO_DELAY_ORB_CHECK);
        } else {
            deferredTask = new DeferredTask(this::startRaid, 1);
        }
    }

    private void startRaid() {
        state = RaidState.ACTIVE;
        boolean spectator = !playerIsInRaid(client.getLocalPlayer().getName());

        List<String> names = party.values().stream().map(Raider::getUsername).collect(Collectors.toList());
        dispatchEvent(new ChallengeStartEvent(names, raidMode, spectator));

        if (this.roomDataTracker != null) {
            // Raid scale information is not immediately available when the raid starts, so if any NPCs have already
            // spawned, their hitpoints must be corrected after the scale is known.
            this.roomDataTracker.correctNpcHitpointsForScale(getRaidScale());
        }

        // Dispatch any pending events that were queued before the raid started.
        pendingRaidEvents.forEach(this::dispatchEvent);
        pendingRaidEvents.clear();
    }

    private void queueRaidEnd(RaidState state) {
        queueRaidEnd(state, -1);
    }

    private void queueRaidEnd(RaidState state, int overallTime) {
        deferredTask = new DeferredTask(() -> endRaid(state, overallTime), 5);
    }

    private void endRaid(RaidState state, int overallTime) {
        log.info("Raid completed; overall time {}", overallTime == -1 ? "unknown" : Tick.asTimeString(overallTime));

        clearRoomDataTracker();
        this.state = state;
        raidMode = null;
        party.clear();

        clientThread.invokeAtTickEnd(() -> dispatchEvent(new ChallengeEndEvent(overallTime)));
    }

    /**
     * Checks the location of the logged-in player, initializing the appropriate {@link RoomDataTracker} if a new room
     * has been entered.
     *
     * @return {@code true} if the player's location has changed from the last recorded one.
     */
    private boolean updateLocation() {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return false;
        }

        Location loc = Location.fromWorldPoint(WorldPoint.fromLocalInstance(client, player.getLocalLocation()));
        if (location == loc) {
            return false;
        }

        log.debug("Location changed to " + loc);
        location = loc;

        if (roomDataTracker == null || !location.inRoomInstance(roomDataTracker.getRoom())) {
            // When entering a new instance for the first time, its room data tracker must be initialized.
            initializeRoomDataTracker();
        }
        return true;
    }

    @Subscribe(priority = 10)
    private void onNpcSpawned(NpcSpawned npcSpawned) {
        if (updateLocation() && roomDataTracker != null) {
            // When the room changes, the event must be manually forwarded to the new `roomDataTracker`.
            roomDataTracker.onNpcSpawned(npcSpawned);
        }
    }

    @Subscribe(priority = 10)
    private void onVarbitChanged(VarbitChanged varbit) {
        updateLocation();

        if (varbit.getVarbitId() == Varbits.THEATRE_OF_BLOOD) {
            if (state.isInactive() && varbit.getValue() == 2) {
                // A raid start due to a varbit change usually means that the player is joining late, rejoining, or
                // spectating. Party and mode information is not immediately available.
                log.debug("Raid started via varbit change");
                queueRaidStart(null, true);
            } else if (state.isActive() && varbit.getValue() < 2) {
                if (state == RaidState.COMPLETE) {
                    state = RaidState.INACTIVE;
                } else {
                    // The raid has finished; clean up.
                    queueRaidEnd(RaidState.INACTIVE);
                }
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

    @Subscribe(priority = 5)
    private void onChatMessage(ChatMessage message) {
        updateLocation();

        if (message.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        String stripped = Text.removeTags(message.getMessage());

        if (state.isInactive()) {
            // Listen for a chat message indicating the start of a raid, and queue the start action immediately
            // instead of waiting to enter.
            Matcher matcher = RAID_ENTRY_REGEX_1P.matcher(stripped);
            if (matcher.matches()) {
                log.debug("Raid started via 1p chat message (mode: {})", matcher.group(1));
                queueRaidStart(ChallengeMode.parseTob(matcher.group(1)).orElse(null), party.isEmpty());
                return;
            }

            matcher = RAID_ENTRY_REGEX_3P.matcher(stripped);
            if (matcher.matches()) {
                log.debug("Raid started via 3p chat message (leader: {} mode: {})", matcher.group(1), matcher.group(2));
                queueRaidStart(ChallengeMode.parseTob(matcher.group(2)).orElse(null), party.isEmpty());
            }
            return;
        }

        Matcher matcher = RAID_COMPLETION_REGEX.matcher(stripped);
        if (matcher.matches()) {
            int overallTime = Tick.fromTimeString(matcher.group(1));
            queueRaidEnd(RaidState.COMPLETE, overallTime);
        }
    }

    /**
     * Collects party members' names from the lobby party widget.
     */
    private void initializePartyFromLobby() {
        var tobPartyWidget = client.getWidget(TOB_PARTY_LIST_COMPONENT_ID);
        if (tobPartyWidget == null) {
            return;
        }

        party.clear();

        Arrays.stream(tobPartyWidget.getText().split("<br>"))
                .filter(s -> !s.equals("-"))
                .map(Text::sanitize)
                .forEach(s -> party.put(Text.standardize(s),
                        new Raider(s, s.equals(client.getLocalPlayer().getName()))));
    }

    private void initializePartyFromOrbs() {
        party.clear();
        String localPlayer = client.getLocalPlayer().getName();
        forEachOrb((orb, username) -> {
            String normalized = Text.standardize(username);
            party.put(normalized, new Raider(username, username.equals(localPlayer)));
        });
    }

    private void updatePartyInformation() {
        // Toggle the players who are currently connected to the raid.
        party.values().forEach(r -> r.setActive(false));
        forEachOrb((orb, username) -> {
            Raider raider = party.get(Text.standardize(username));
            if (raider != null) {
                raider.setActive(true);
            }
        });
    }

    private void clearRoomDataTracker() {
        if (roomDataTracker != null) {
            roomDataTracker.terminate();
            eventBus.unregister(roomDataTracker);
            roomDataTracker = null;
        }
    }

    private void initializeRoomDataTracker() {
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
            log.info("Initialized room data tracker for {} from {}", roomDataTracker.getRoom(), location);
            eventBus.register(roomDataTracker);
            dispatchEvent(new StageUpdateEvent(roomDataTracker.getStage(), 0, StageUpdateEvent.Status.ENTERED));
        }
    }
}
