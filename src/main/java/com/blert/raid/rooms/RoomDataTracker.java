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
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
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

@Slf4j

public abstract class RoomDataTracker {
    protected final RaidManager raidManager;

    protected final Client client;

    private final ClientThread clientThread;

    private final Room room;

    private int startClientTick = 0;

    private final Set<String> deadPlayers = new HashSet<>();

    /**
     * Set of NPC IDs for which an NpcUpdateEvent has been sent on the current tick.
     */
    private final Set<Integer> npcUpdatesThisTick = new HashSet<>();

    private final SpecialAttackTracker specialAttackTracker = new SpecialAttackTracker(this::onSpecialAttack);

    protected RoomDataTracker(RaidManager raidManager, Client client, Room room) {
        this.raidManager = raidManager;
        this.client = client;
        this.clientThread = raidManager.getClientThread();
        this.room = room;
    }

    /**
     * Begins tracking data for the room.
     */
    public void startRoom() {
        this.startClientTick = client.getTickCount();
        dispatchEvent(new RoomStatusEvent(room, 0, RoomStatusEvent.Status.STARTED));
        onRoomStart();
    }

    /**
     * Finishes tracking data for the room and performs any necessary cleanup.
     */
    public void finishRoom() {
        log.debug("Room " + room + " finished in " + getRoomTick() + " ticks");

        var roomStatus = deadPlayers.size() == raidManager.getRaidScale()
                ? RoomStatusEvent.Status.WIPED
                : RoomStatusEvent.Status.COMPLETED;
        dispatchEvent(new RoomStatusEvent(room, getRoomTick(), roomStatus));
    }

    /**
     * Collects data about the raid room in the current game tick.
     */
    public void tick() {
        npcUpdatesThisTick.clear();

        updatePartyStatus();
        updatePlayers();

        specialAttackTracker.processPendingSpecial();

        // Send simple updates for any NPCs not already reported by the implementation.
        client.getNpcs()
                .stream()
                .filter(npc -> !npcUpdatesThisTick.contains(npc.getId()))
                .forEach(this::sendBasicNpcUpdate);

        // Run implementation-specific behavior.
        onTick();
    }

    /**
     * Returns number of ticks the room has been active.
     *
     * @return The current room tick, starting at 1.
     */
    public int getRoomTick() {
        return client.getTickCount() - this.startClientTick + 1;
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
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
        Actor target = hitsplatApplied.getActor();
        Hitsplat hitsplat = hitsplatApplied.getHitsplat();
        if (hitsplat.isMine() && target != client.getLocalPlayer()) {
            specialAttackTracker.recordHitsplat((NPC) target, hitsplat, client.getTickCount());
        }

        onHitsplat(hitsplatApplied);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged varbitChanged) {
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
        if (player != null) {
            Location location = Location.fromWorldPoint(WorldPoint.fromLocalInstance(client, player.getLocalLocation()));
            if (location.inRoom(room) && !deadPlayers.contains(Text.standardize(player.getName()))) {
                // Report detailed information about the logged-in player.
                dispatchEvent(PlayerUpdateEvent.fromLocalPlayer(room, getRoomTick(), client));
            }
        }

        // Report basic positional information about every other player in the room.
        for (Player other : client.getPlayers()) {
            if (other == player || deadPlayers.contains(Text.standardize(other.getName()))) {
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
}
