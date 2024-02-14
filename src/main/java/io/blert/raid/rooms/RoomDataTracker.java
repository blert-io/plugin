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

package io.blert.raid.rooms;

import io.blert.events.*;
import io.blert.raid.Item;
import io.blert.raid.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
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

    private final RoomNpcCollection roomNpcs = new RoomNpcCollection();

    private final SpecialAttackTracker specialAttackTracker = new SpecialAttackTracker(this::onSpecialAttack);

    protected enum State {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        TERMINATING,
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

        // Don't send the final room status immediately; allow other pending subscribers to run and dispatch their
        // own events first.
        clientThread.invokeLater(() -> dispatchEvent(new RoomStatusEvent(room, finalRoomTick, roomStatus)));
    }

    public void terminate() {
        if (state == State.IN_PROGRESS) {
            finishRoom();
        }
        state = State.TERMINATING;
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

        updatePartyStatus();
        updatePlayers();

        raidManager.getRaiders().forEach(this::checkForPlayerAttack);

        specialAttackTracker.processPendingSpecial();

        // Run implementation-specific behavior.
        onTick();

        // Send out an update for every tracked NPC. This must be done after `onTick` to ensure any
        // implementation-specific changes to the NPC are complete.
        roomNpcs.forEach(this::sendNpcUpdate);
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

    public boolean terminating() {
        return state == State.TERMINATING;
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
     * Event handler invoked when a new NPC spawns. If the spawned NPC should be tracked and have its data reported,
     * returns a {@link RoomNpc} describing it. Otherwise, returns {@link Optional#empty()}, performing any desired
     * actions to handle the spawn.
     *
     * @param event The Runelite NPC spawn event.
     * @return The room NPC to track if desired.
     */
    protected abstract Optional<? extends RoomNpc> onNpcSpawn(NpcSpawned event);

    /**
     * Event handler invoked when an NPC in the room despawns. If the NPC was being tracked as a room NPC, and is now
     * completely dead or otherwise inactive, returns {@code true} to indicate that it should be removed from
     * tracking.
     *
     * @param event   The event.
     * @param roomNpc The room NPC corresponding to the despawned NPC, if it is tracked.
     * @return {@code true} if the NPC should no longer be tracked, {@code false} if it should not (e.g. this despawn
     * indicates a phase change).
     */
    protected abstract boolean onNpcDespawn(NpcDespawned event, @Nullable RoomNpc roomNpc);

    /**
     * Implementation-specific equivalent of the {@code onNpcChanged} Runelite event handler.
     * Should be overriden by implementations which require special handling.
     *
     * @param event The event.
     */
    protected void onNpcChange(NpcChanged event) {
    }

    /**
     * Implementation-specific equivalent of the {@code onGameObjectSpawned} Runelite event handler.
     * Should be overriden by implementations which require special handling.
     *
     * @param event The event.
     */
    protected void onGameObjectSpawn(GameObjectSpawned event) {
    }

    /**
     * Implementation-specific equivalent of the {@code onGameObjectDespawned} Runelite event handler.
     * Should be overriden by implementations which require special handling.
     *
     * @param event The event.
     */
    protected void onGameObjectDespawn(GameObjectDespawned event) {
    }

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

    protected WorldPoint getWorldLocation(@NotNull Actor actor) {
        LocalPoint local = actor instanceof NPC ? Utils.getNpcSouthwestTile((NPC) actor) : actor.getLocalLocation();
        return WorldPoint.fromLocalInstance(client, local);
    }

    protected WorldPoint getWorldLocation(@NotNull RoomNpc roomNpc) {
        return getWorldLocation(roomNpc.getNpc());
    }

    protected long generateRoomId(@NotNull NPC npc) {
        return RoomNpcCollection.npcRoomId(getRoomTick(), npc.getId(), getWorldLocation(npc));
    }

    @Subscribe
    public final void onNpcSpawned(NpcSpawned event) {
        if (terminating()) {
            return;
        }

        onNpcSpawn(event).ifPresent(roomNpc -> {
            boolean existing = roomNpcs.remove(roomNpc);
            if (!existing) {
                roomNpc.setSpawnTick(getRoomTick());
            }
            roomNpcs.add(roomNpc);

            if (state == State.NOT_STARTED) {
                // NPCs which spawn before the room begins must be reported immediately as the `onTick` handler
                // is not yet active.
                dispatchEvent(NpcEvent.spawn(room, 0, getWorldLocation(roomNpc), roomNpc));
            }

            // Update the raid mode on the first NPC seen in a room, in case the client joined the raid late, and it
            // has not been set.
            if (roomNpcs.size() == 1) {
                raidManager.updateRaidMode(roomNpc.getRaidMode());
            }
        });
    }

    @Subscribe
    protected final void onNpcDespawned(NpcDespawned event) {
        if (terminating()) {
            return;
        }

        Optional<RoomNpc> maybeRoomNpc = roomNpcs.getByNpc(event.getNpc());
        if (onNpcDespawn(event, maybeRoomNpc.orElse(null))) {
            maybeRoomNpc.ifPresent(roomNpc -> {
                dispatchEvent(NpcEvent.death(room, getRoomTick(), getWorldLocation(roomNpc), roomNpc));
                roomNpcs.remove(roomNpc);
            });
        }
    }

    @Subscribe
    protected final void onNpcChanged(NpcChanged event) {
        if (!terminating()) {
            onNpcChange(event);
        }
    }

    @Subscribe
    protected final void onGameObjectSpawned(GameObjectSpawned event) {
        if (!terminating()) {
            onGameObjectSpawn(event);
        }
    }

    @Subscribe
    protected final void onGameObjectDespawned(GameObjectDespawned event) {
        if (!terminating()) {
            onGameObjectDespawn(event);
        }
    }

    @Subscribe
    protected final void onChatMessage(ChatMessage chatMessage) {
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
    protected final void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
        if (state != State.IN_PROGRESS) {
            return;
        }

        Actor target = hitsplatApplied.getActor();
        Hitsplat hitsplat = hitsplatApplied.getHitsplat();
        if (hitsplat.isMine() && target != client.getLocalPlayer()) {
            specialAttackTracker.recordHitsplat((NPC) target, hitsplat, client.getTickCount());
        }

        if (target instanceof NPC) {
            roomNpcs.getByNpc((NPC) target).ifPresent(roomNpc -> {
                if (hitsplat.getHitsplatType() == HitsplatID.HEAL) {
                    roomNpc.getHitpoints().boost(hitsplat.getAmount());
                } else {
                    roomNpc.getHitpoints().drain(hitsplat.getAmount());
                }
            });
        }

        onHitsplat(hitsplatApplied);
    }

    @Subscribe
    protected final void onVarbitChanged(VarbitChanged varbitChanged) {
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
    protected final void onAnimationChanged(AnimationChanged event) {
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
            Raider raider = raidManager.getRaider(Text.standardize(username));

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

            RoomNpc roomTarget = target.flatMap(roomNpcs::getByNpc).orElse(null);
            dispatchEvent(new PlayerAttackEvent(room, tick, point, attack, weapon.orElse(null),
                    player.getName(), roomTarget));
        });
    }

    /**
     * Sends an {@link NpcEvent} about an NPC in the room.
     */
    protected void sendNpcUpdate(RoomNpc roomNpc) {
        final int tick = getRoomTick();
        WorldPoint point = getWorldLocation(roomNpc);

        if (roomNpc.getSpawnTick() == tick) {
            dispatchEvent(NpcEvent.spawn(room, tick, point, roomNpc));
        } else {
            dispatchEvent(NpcEvent.update(room, tick, point, roomNpc));
        }
    }

    protected String formattedRoomTime() {
        int milliseconds = getRoomTick() * 600;
        int seconds = (milliseconds / 1000) % 60;
        int minutes = milliseconds / 1000 / 60;
        int deciseconds = (milliseconds % 1000) / 100;

        return String.format("%d:%02d.%d", minutes, seconds, deciseconds);
    }
}
