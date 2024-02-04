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
import com.blert.raid.Item;
import com.blert.raid.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
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

        client.getPlayers().forEach(player -> {
            Raider raider = raidManager.getRaider(player.getName());
            if (raider != null) {
                raider.resetForNewRoom();
                raider.setPlayer(player);
            }
        });

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

        long deadRaiders = raidManager.getRaiders().stream().filter(Raider::isDead).count();
        var roomStatus = deadRaiders == raidManager.getRaidScale()
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

        raidManager.getRaiders().forEach(this::checkForPlayerAttack);

        specialAttackTracker.processPendingSpecial();

        // Run implementation-specific behavior.
        onTick();

        // Send simple updates for any NPCs not already reported by the implementation.
        client.getNpcs()
                .stream()
                .filter(npc -> !npcUpdatesThisTick.contains(npc.hashCode()))
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
                .filter(player -> raidManager.playerIsInRaid(player.getName()))
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
     * Implementation-specific equivalent of the {@code onAnimationChanged} Runelite event handler.
     * Should be overriden by implementations which require special animation tracking.
     *
     * @param animationChanged The animation event.
     */
    protected void onAnimation(AnimationChanged animationChanged) {
    }

    /**
     * Implementation-specific equivalent of the {@code onHitsplatApplied} Runelite event handler.
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

                net.runelite.api.Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
                if (weapon != null) {
                    specialAttackTracker.recordSpecialUsed((NPC) target, weapon, specTick);
                }
            }
        });
    }

    @Subscribe
    private void onAnimationChanged(AnimationChanged event) {
        if (state != State.IN_PROGRESS) {
            return;
        }

        Actor actor = event.getActor();
        if (actor instanceof Player) {
            Raider raider = raidManager.getRaider(actor.getName());
            if (raider != null) {
                raider.setAnimation(getRoomTick(), actor.getAnimation());
            }
        }

        onAnimation(event);
    }

    private void onSpecialAttack(SpecialAttackTracker.SpecialAttack spec) {
        var weapon = client.getItemDefinition(spec.getWeapon().getId());
        log.debug("Hit a " + spec.getDamage() + " with " + weapon.getName() + " on " + spec.getTarget().getName());
    }

    private void updatePartyStatus() {
        raidManager.forEachOrb((orb, username) -> {
            Raider raider = raidManager.getRaider(username);

            // ToB orb health. 0 = hide, 1-27 = health percentage (0-100%), 30 = dead.
            int orbHealth = client.getVarbitValue(Varbits.THEATRE_OF_BLOOD_ORB1 + orb);
            if (orbHealth == 1) {
                if (raider.isDead()) {
                    // Player was already dead.
                    return;
                }

                raider.setDead(true);
                if (raider.getPlayer() != null) {
                    WorldPoint point = WorldPoint.fromLocalInstance(client, raider.getPlayer().getLocalLocation());
                    dispatchEvent(new PlayerDeathEvent(room, getRoomTick(), point, raider.getUsername()));
                }
            }
        });
    }

    private void updatePlayers() {
        int tick = getRoomTick();

        client.getPlayers().forEach(player -> {
            Raider raider = raidManager.getRaider(player.getName());
            if (raider == null || raider.isDead()) {
                return;
            }

            raider.updateState(client, player, tick);

            dispatchEvent(PlayerUpdateEvent.fromRaider(room, tick, client, raider));
        });
    }

    private void checkForPlayerAttack(@NotNull Raider raider) {
        int animationId = raider.getAnimationId();
        if (animationId == -1) {
            return;
        }

        final int tick = getRoomTick();

        boolean mayHaveAttacked = raider.isOffCooldownOn(tick)
                && (raider.getAnimationTick() == tick || raider.isBlowpiping() || raider.stoppedBlowpiping());
        if (!mayHaveAttacked) {
            return;
        }

        Player player = raider.getPlayer();
        if (player == null) {
            return;
        }

        WorldPoint point = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
        if (!Location.fromWorldPoint(point).inRoom(room)) {
            return;
        }

        Optional<PlayerAttack> maybeAttack;
        Optional<NPC> target = raider.getTarget();
        Optional<Item> weapon = raider.getEquippedItem(EquipmentSlot.WEAPON);
        int weaponId = weapon.map(Item::getId).orElse(-1);

        if (raider.stoppedBlowpiping()) {
            // In some instances, the blowpipe animation overrides another weapon's attack animation when the player
            // attacks on blowpipe cooldown. If the player is still using the blowpipe animation but has just stopped
            // blowpiping and targeted another NPC, assume that they attacked it with the weapon they're holding.
            if (!PlayerAttack.BLOWPIPE.hasAnimation(animationId) || target.isEmpty()) {
                return;
            }
            maybeAttack = PlayerAttack.findByWeaponOnly(weaponId);
        } else {
            maybeAttack = PlayerAttack.find(weaponId, animationId);
        }

        maybeAttack.ifPresent(attack -> {
            raider.recordAttack(tick, attack);
            dispatchEvent(new PlayerAttackEvent(room, tick, point, attack,
                    weapon.orElse(null), player.getName(), target.orElse(null)));
        });
    }

    /**
     * Sends an NPC update event for the current tick, tracking which NPCs have already been updated to avoid duplicate
     * events.
     */
    protected void dispatchNpcUpdate(@NotNull NpcUpdateEvent update) {
        if (npcUpdatesThisTick.add(update.getRoomId())) {
            dispatchEvent(update);
        }
    }

    /**
     * Sends a generic update status about an NPC in the room. Implementations can choose to call this directly, or
     * track additional information about their room NPCs and send them through
     * {@link #dispatchNpcUpdate(NpcUpdateEvent)}.
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
        npcUpdatesThisTick.add(npc.hashCode());
    }

    protected String formattedRoomTime() {
        int milliseconds = getRoomTick() * 600;
        int seconds = (milliseconds / 1000) % 60;
        int minutes = milliseconds / 1000 / 60;
        int deciseconds = (milliseconds % 1000) / 100;

        return String.format("%d:%02d.%d", minutes, seconds, deciseconds);
    }
}
