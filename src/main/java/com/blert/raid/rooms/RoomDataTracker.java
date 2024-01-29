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

package com.blert.raid.rooms;

import com.blert.events.*;
import com.blert.raid.*;
import joptsimple.internal.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j

public abstract class RoomDataTracker {
    protected final RaidManager raidManager;

    protected final Client client;

    private final ClientThread clientThread;

    @Getter
    private final Room room;
    private final Pattern waveEndRegex;

    @Getter(AccessLevel.PROTECTED)
    private State state;

    private final boolean startOnEntry;
    private int startClientTick;
    private boolean startingTickAccurate;

    private final Set<String> deadPlayers = new HashSet<>();

    /**
     * Set of NPC IDs for which an NpcUpdateEvent has been sent on the current tick.
     */
    private final Set<Integer> npcUpdatesThisTick = new HashSet<>();

    private final SpecialAttackTracker specialAttackTracker = new SpecialAttackTracker(this::onSpecialAttack);

    protected enum State {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
    }

    protected RoomDataTracker(RaidManager raidManager, Client client, Room room, boolean startOnEntry) {
        this.raidManager = raidManager;
        this.client = client;
        this.clientThread = raidManager.getClientThread();
        this.room = room;
        this.waveEndRegex = Pattern.compile(
                "Wave '" + room.waveName() + "' \\(\\w+ Mode\\) complete!Duration: ([0-9:.]+)"
        );
        this.state = State.NOT_STARTED;
        this.startOnEntry = startOnEntry;
    }

    protected RoomDataTracker(RaidManager raidManager, Client client, Room room) {
        this(raidManager, client, room, false);
    }

    /**
     * Begins tracking data for the room.
     */
    public void startRoom() {
        startRoom(0, true);
    }

    /**
     * Begins tracking data for the room, assuming that the starting tick is not correct (possibly because the room was
     * already in progress).
     */
    public void startRoomInaccurate() {
        startRoom(0, false);
    }

    private void startRoom(int tickOffset, boolean accurate) {
        if (state != State.NOT_STARTED) {
            return;
        }

        state = State.IN_PROGRESS;
        startClientTick = client.getTickCount() + tickOffset;
        startingTickAccurate = accurate;

        dispatchEvent(new RoomStatusEvent(room, 0, RoomStatusEvent.Status.STARTED));

        onRoomStart();
    }

    /**
     * Finishes tracking data for the room and performs any necessary cleanup.
     */
    public void finishRoom() {
        if (state != State.IN_PROGRESS) {
            return;
        }

        state = State.COMPLETED;
        int finalRoomTick = getRoomTick();

        log.debug("Room {} finished in {} ticks ({})", room, finalRoomTick, formattedRoomTime());

        var roomStatus = deadPlayers.size() == raidManager.getRaidScale()
                ? RoomStatusEvent.Status.WIPED
                : RoomStatusEvent.Status.COMPLETED;
        dispatchEvent(new RoomStatusEvent(room, finalRoomTick, roomStatus));
    }

    public void checkEntry() {
        if (state == State.NOT_STARTED && this.startOnEntry) {
            if (playersAreInRoom()) {
                log.debug("Room " + room + " started because player entered");
                // Players seem to enter the room boundary one tick after the in-game room timer starts.
                startRoom(-1, true);
            }
        }
    }

    /**
     * Collects data about the raid room in the current game tick.
     */
    public void tick() {
        if (state != State.IN_PROGRESS) {
            return;
        }

        npcUpdatesThisTick.clear();

        updatePartyStatus();
        updatePlayers();

        specialAttackTracker.processPendingSpecial();

        // Run implementation-specific behavior.
        onTick();

        // Send simple updates for any NPCs not already reported by the implementation.
        client.getNpcs()
                .stream()
                .filter(npc -> !npcUpdatesThisTick.contains(npc.getId()))
                .forEach(this::sendBasicNpcUpdate);
    }

    /**
     * Returns number of ticks the room has been active.
     *
     * @return The current room tick.
     */
    public int getRoomTick() {
        return client.getTickCount() - this.startClientTick;
    }

    public boolean notStarted() {
        return state == State.NOT_STARTED;
    }

    /**
     * Checks if any players are located within the boundaries of the boss room.
     *
     * @return True if there is at least one player within the room.
     */
    public boolean playersAreInRoom() {
        return client.getPlayers().stream()
                .filter(player -> raidManager.playerIsInRaid(Objects.requireNonNull(player.getName())))
                .anyMatch(player -> {
                    WorldPoint position = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
                    return Location.fromWorldPoint(position).inRoom(room);
                });
    }

    /**
     * Initialization method invoked when the room is first started.
     */
    protected abstract void onRoomStart();

    /**
     * Gathers information about the room and dispatches appropriate events. Invoked every tick.
     */
    protected abstract void onTick();

    /**
     * Implementation-specific equivalent of the `onHitsplatApplied` Runelite event handler.
     * Should be overriden by implementations which require special hitsplat tracking.
     *
     * @param hitsplatApplied The hitsplat event.
     */
    protected void onHitsplat(HitsplatApplied hitsplatApplied) {
    }

    /**
     * Sends an event to the registered event handler, if any.
     */
    protected void dispatchEvent(Event event) {
        raidManager.dispatchEvent(event);
    }

    @Subscribe
    private void onChatMessage(ChatMessage chatMessage) {
        if (state != State.IN_PROGRESS || chatMessage.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        String stripped = Text.removeTags(chatMessage.getMessage());
        Matcher matcher = waveEndRegex.matcher(stripped);
        if (matcher.find()) {
            finishRoom();
        }
    }

    @Subscribe
    private void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
        if (state != State.IN_PROGRESS) {
            return;
        }

        Actor target = hitsplatApplied.getActor();
        Hitsplat hitsplat = hitsplatApplied.getHitsplat();
        if (hitsplat.isMine() && target != client.getLocalPlayer()) {
            specialAttackTracker.recordHitsplat((NPC) target, hitsplat, client.getTickCount());
        }

        onHitsplat(hitsplatApplied);
    }

    @Subscribe
    private void onVarbitChanged(VarbitChanged varbitChanged) {
        if (state != State.IN_PROGRESS) {
            return;
        }

        if (varbitChanged.getVarpId() != VarPlayer.SPECIAL_ATTACK_PERCENT) {
            return;
        }

        int percent = varbitChanged.getValue();
        int oldPercent = specialAttackTracker.updateSpecialPercent(percent);
        if (oldPercent != -1 && percent >= oldPercent) {
            // This is a special attack regen, not drain. Ignore it.
            return;
        }

        int specTick = client.getTickCount();
        clientThread.invokeLater(() -> {
            Actor target = client.getLocalPlayer().getInteracting();
            if (target instanceof NPC) {
                var equipment = client.getItemContainer(InventoryID.EQUIPMENT);
                if (equipment == null) {
                    return;
                }

                Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
                if (weapon != null) {
                    specialAttackTracker.recordSpecialUsed((NPC) target, weapon, specTick);
                }
            }
        });
    }

    private void onSpecialAttack(SpecialAttackTracker.SpecialAttack spec) {
        var weapon = client.getItemDefinition(spec.getWeapon().getId());
        log.debug("Hit a " + spec.getDamage() + " with " + weapon.getName() + " on " + spec.getTarget().getName());
    }

    private void updatePartyStatus() {
        // ID of the client string containing the username of the first member in a ToB party. Subsequent party members'
        // usernames (if present) occupy the following four IDs.
        final int TOB_P1_VARCSTR_ID = 330;

        for (int player = 0; player < 5; player++) {
            String username = Text.standardize(client.getVarcStrValue(TOB_P1_VARCSTR_ID + player));
            if (Strings.isNullOrEmpty(username)) {
                continue;
            }

            // ToB orb health. 0 = hide, 1-27 = health percentage (0-100%), 30 = dead.
            int orbHealth = client.getVarbitValue(Varbits.THEATRE_OF_BLOOD_ORB1 + player);
            if (orbHealth == 1) {
                if (!deadPlayers.add(username)) {
                    // Player was already dead.
                    continue;
                }

                for (Player p : client.getPlayers()) {
                    if (Objects.equals(Text.standardize(p.getName()), username)) {
                        WorldPoint point = WorldPoint.fromLocalInstance(client, p.getLocalLocation());
                        dispatchEvent(new PlayerDeathEvent(room, getRoomTick(), point, p.getName()));
                        break;
                    }
                }
            }
        }
    }

    private void updatePlayers() {
        Player player = client.getLocalPlayer();
        if (player != null && raidManager.playerIsInRaid(Objects.requireNonNull(player.getName()))) {
            Location location = Location.fromWorldPoint(WorldPoint.fromLocalInstance(client, player.getLocalLocation()));
            if (location.inRoom(room) && !deadPlayers.contains(Text.standardize(player.getName()))) {
                // Report detailed information about the logged-in player.
                dispatchEvent(PlayerUpdateEvent.fromLocalPlayer(room, getRoomTick(), client));
            }
        }

        // Report basic positional information about every other player in the room.
        for (Player other : client.getPlayers()) {
            if (other == player
                    || !raidManager.playerIsInRaid(Objects.requireNonNull(other.getName()))
                    || deadPlayers.contains(Text.standardize(other.getName()))) {
                continue;
            }

            WorldPoint point = WorldPoint.fromLocalInstance(client, other.getLocalLocation());
            if (Location.fromWorldPoint(point).inRoom(room)) {
                dispatchEvent(PlayerUpdateEvent.fromPlayer(room, getRoomTick(), client, other));
            }
        }
    }

    /**
     * Sends an NPC update event for the current tick, tracking which NPCs have already been updated to avoid duplicate
     * events.
     */
    protected void dispatchNpcUpdate(@NotNull NpcUpdateEvent update) {
        if (npcUpdatesThisTick.add(update.getNpcId())) {
            dispatchEvent(update);
        }
    }

    /**
     * Sends a generic update status about an NPC in the room. Implementations can choose to call this directly, or
     * track additional information about their room NPCs and send them through `dispatchNpcUpdate`.
     */
    protected void sendBasicNpcUpdate(NPC npc) {
        WorldPoint point = WorldPoint.fromLocalInstance(client, Utils.getNpcSouthwestTile(npc));
        if (!Location.fromWorldPoint(point).inRoom(room)) {
            return;
        }

        Optional<TobNpc> maybeNpc = TobNpc.withId(npc.getId());
        if (maybeNpc.isEmpty()) {
            return;
        }
        TobNpc tobNpc = maybeNpc.get();

        Hitpoints hitpoints = null;
        if (npc.getHealthScale() > 0 && npc.getHealthRatio() != -1) {
            double hpRatio = npc.getHealthRatio() / (double) npc.getHealthScale();
            hitpoints = Hitpoints.fromRatio(hpRatio, tobNpc.getBaseHitpoints(raidManager.getRaidScale()));
        }

        dispatchEvent(new NpcUpdateEvent(room, getRoomTick(), point, npc.hashCode(), npc.getId(), hitpoints));
    }

    protected String formattedRoomTime() {
        int milliseconds = getRoomTick() * 600;
        int seconds = (milliseconds / 1000) % 60;
        int minutes = milliseconds / 1000 / 60;
        int deciseconds = (milliseconds % 1000) / 100;

        return String.format("%d:%02d.%d", minutes, seconds, deciseconds);
    }
}
