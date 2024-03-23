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

package io.blert.challenges.tob.rooms;

import io.blert.challenges.tob.Location;
import io.blert.challenges.tob.RaidManager;
import io.blert.challenges.tob.TobNpc;
import io.blert.core.Item;
import io.blert.core.*;
import io.blert.events.*;
import io.blert.util.Tick;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j

public abstract class RoomDataTracker extends DataTracker {
    protected final RaidManager raidManager;

    @Getter
    private final Room room;
    private final Pattern waveEndRegex;

    @Getter(AccessLevel.PROTECTED)
    private State state;

    private final boolean startOnEntry;
    private boolean startingTickAccurate;

    private final TrackedNpcCollection trackedNpcs = new TrackedNpcCollection();

    private final SpecialAttackTracker specialAttackTracker = new SpecialAttackTracker(this::onSpecialAttack);

    protected enum State {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        TERMINATING,
    }

    protected RoomDataTracker(RaidManager raidManager, Client client, Room room, boolean startOnEntry) {
        super(client, raidManager.getClientThread(), room.toStage());
        this.raidManager = raidManager;
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

        super.start();

        state = State.IN_PROGRESS;
        startingTickAccurate = accurate;

        client.getPlayers().forEach(player -> {
            Raider raider = raidManager.getRaider(player.getName());
            if (raider != null) {
                raider.resetForNewRoom();
                raider.setPlayer(player);
            }
        });

        dispatchEvent(new StageUpdateEvent(stage, 0, StageUpdateEvent.Status.STARTED));

        onRoomStart();
    }

    /**
     * Finishes tracking data for the room and performs any necessary cleanup.
     */
    public void finishRoom(boolean completion) {
        finishRoom(completion, -1);
    }

    /**
     * Finishes tracking data for the room and performs any necessary cleanup.
     *
     * @param inGameRoomTicks The number of in-game ticks the room took to complete, or -1 if the in-game timer is not
     *                        available. If provided, it is used to verify the accuracy of the recorded room time.
     */
    private void finishRoom(boolean completion, int inGameRoomTicks) {
        if (state != State.IN_PROGRESS) {
            return;
        }

        state = State.COMPLETED;
        int lastRecordedRoomTick = getTick();
        boolean accurate;

        if (inGameRoomTicks != -1 && inGameRoomTicks != lastRecordedRoomTick) {
            log.warn("Room {} completion time mismatch: in-game room ticks = {}, recorded ticks = {}",
                    room, inGameRoomTicks, lastRecordedRoomTick);
            accurate = false;
            lastRecordedRoomTick = inGameRoomTicks;
        } else {
            log.debug("Room {} finished in {} ticks ({})", room, lastRecordedRoomTick, formattedRoomTime());
            accurate = true;
        }

        var roomStatus = completion ? StageUpdateEvent.Status.COMPLETED : StageUpdateEvent.Status.WIPED;

        // Don't send the final room status immediately; allow other pending subscribers to run and dispatch their
        // own events first.
        final int finalRoomTick = lastRecordedRoomTick;
        clientThread.invokeLater(() -> dispatchEvent(new StageUpdateEvent(stage, finalRoomTick, roomStatus, accurate)));
    }

    /**
     * Prepares the room tracker for cleanup, preventing any further events from being processed.
     */
    public void terminate() {
        if (state == State.IN_PROGRESS) {
            finishRoom(false);
        }
        state = State.TERMINATING;
    }

    /**
     * Checks whether players are in the room and starts the room if they are.
     */
    public void checkEntry() {
        if (state == State.NOT_STARTED && this.startOnEntry) {
            if (playersAreInRoom()) {
                log.debug("Room " + room + " started because player entered");
                startRoom();
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
        trackedNpcs.forEach(this::sendNpcUpdate);
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
                .anyMatch(player -> Location.fromWorldPoint(getWorldLocation(player)).inRoom(room));
    }

    /**
     * Updates the base and current hitpoints of all NPCs in the room to match the given raid scale.
     *
     * @param scale The raid scale.
     */
    public void correctNpcHitpointsForScale(int scale) {
        trackedNpcs.forEach(trackedNpc -> {
            NPC npc = trackedNpc.getNpc();
            int healthRatio = npc.getHealthRatio();
            int healthScale = npc.getHealthScale();

            if (healthScale > 0 && healthRatio != -1) {
                double percent = healthRatio / (double) healthScale;
                TobNpc tobNpc = TobNpc.withId(npc.getId()).orElseThrow();
                trackedNpc.setHitpoints(Hitpoints.fromRatio(percent, tobNpc.getBaseHitpoints(scale)));
            }
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
     * returns a {@link TrackedNpc} describing it. Otherwise, returns {@link Optional#empty()}, performing any desired
     * actions to handle the spawn.
     *
     * @param event The Runelite NPC spawn event.
     * @return The tracked NPC to track if desired.
     */
    protected abstract Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned event);

    /**
     * Event handler invoked when an NPC in the room despawns. If the NPC was being tracked, and is now completely
     * dead or otherwise inactive, returns {@code true} to indicate that it should be removed from tracking.
     *
     * @param event      The event.
     * @param trackedNpc The tracked NPC corresponding to the despawned NPC, if it is tracked.
     * @return {@code true} if the NPC should no longer be tracked, {@code false} if it should not (e.g. this despawn
     * indicates a phase change).
     */
    protected abstract boolean onNpcDespawn(NpcDespawned event, @Nullable TrackedNpc trackedNpc);

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
     * Implementation-specific equivalent of the {@code onGraphicsObjectCreated} Runelite event handler.
     * Should be overriden by implementations which require special handling.
     *
     * @param event The event.
     */
    protected void onGraphicsObjectCreation(GraphicsObjectCreated event) {
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
     * Implementation-specific equivalent of the {@code onProjectileMoved} Runelite event handler.
     * Should be overriden by implementations which require special handling.
     *
     * @param event The event.
     */
    protected void onProjectile(ProjectileMoved event) {
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

    private WorldPoint getWorldLocation(WorldPoint instanceUnawareWorldPoint) {
        LocalPoint local = LocalPoint.fromWorld(client, instanceUnawareWorldPoint);
        return local != null ? WorldPoint.fromLocalInstance(client, local) : null;
    }

    protected WorldPoint getWorldLocation(@NotNull Actor actor) {
        return getWorldLocation(actor.getWorldLocation());
    }

    protected WorldPoint getWorldLocation(@NotNull TrackedNpc trackedNpc) {
        return getWorldLocation(trackedNpc.getNpc());
    }

    protected WorldPoint getWorldLocation(@NotNull GameObject object) {
        return getWorldLocation(object.getWorldLocation());
    }

    protected long generateRoomId(@NotNull NPC npc) {
        return TrackedNpcCollection.npcRoomId(getTick(), npc.getId(), getWorldLocation(npc));
    }

    @Subscribe
    public final void onNpcSpawned(NpcSpawned event) {
        if (terminating()) {
            return;
        }

        onNpcSpawn(event).ifPresent(trackedNpc -> {
            boolean existing = trackedNpcs.remove(trackedNpc);
            if (!existing) {
                trackedNpc.setSpawnTick(getTick());
            }
            trackedNpcs.add(trackedNpc);

            if (state == State.NOT_STARTED) {
                // NPCs which spawn before the room begins must be reported immediately as the `onTick` handler
                // is not yet active.
                dispatchEvent(NpcEvent.spawn(stage, 0, getWorldLocation(trackedNpc), trackedNpc));
            }

            // Update the raid mode on the first NPC seen in a room, in case the client joined the raid late, and it
            // has not been set.
            raidManager.updateRaidMode(trackedNpc.getMode());
        });
    }

    @Subscribe
    protected final void onNpcDespawned(NpcDespawned event) {
        if (terminating()) {
            return;
        }

        Optional<TrackedNpc> maybeTrackedNpc = trackedNpcs.getByNpc(event.getNpc());
        if (onNpcDespawn(event, maybeTrackedNpc.orElse(null))) {
            maybeTrackedNpc.ifPresent(this::despawnTrackedNpc);
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
    private void onGraphicsObjectCreated(GraphicsObjectCreated event) {
        if (!terminating()) {
            onGraphicsObjectCreation(event);
        }
    }

    @Subscribe
    protected final void onProjectileMoved(ProjectileMoved event) {
        if (!terminating()) {
            onProjectile(event);
        }
    }

    @Subscribe(priority = 10)
    protected final void onChatMessage(ChatMessage chatMessage) {
        if (state != State.IN_PROGRESS || chatMessage.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        String stripped = Text.removeTags(chatMessage.getMessage());
        Matcher matcher = waveEndRegex.matcher(stripped);
        if (matcher.find()) {
            try {
                String inGameTime = matcher.group(1);
                finishRoom(true, Tick.fromTimeString(inGameTime));
            } catch (IndexOutOfBoundsException e) {
                log.warn("Could not parse timestamp from wave end message: {}", stripped);
                finishRoom(true);
            }
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
            trackedNpcs.getByNpc((NPC) target).ifPresent(trackedNpc -> {
                if (hitsplat.getHitsplatType() == HitsplatID.HEAL) {
                    trackedNpc.getHitpoints().boost(hitsplat.getAmount());
                } else {
                    trackedNpc.getHitpoints().drain(hitsplat.getAmount());
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
                raider.setAnimation(getTick(), actor.getAnimation());
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
                    dispatchEvent(new PlayerDeathEvent(
                            stage, getTick(), getWorldLocation(raider.getPlayer()), raider.getUsername()));
                }
            }
        });
    }

    private void updatePlayers() {
        int tick = getTick();

        client.getPlayers().forEach(player -> {
            Raider raider = raidManager.getRaider(player.getName());
            if (raider == null || raider.isDead()) {
                return;
            }

            raider.updateState(client, player, tick);

            dispatchEvent(PlayerUpdateEvent.fromRaider(stage, tick, getWorldLocation(player), client, raider));
        });
    }

    private void checkForPlayerAttack(@NotNull Raider raider) {
        int animationId = raider.getAnimationId();
        if (animationId == -1) {
            return;
        }

        final int tick = getTick();

        boolean mayHaveAttacked = raider.isOffCooldownOn(tick)
                && (raider.getAnimationTick() == tick || raider.isBlowpiping() || raider.stoppedBlowpiping());
        if (!mayHaveAttacked) {
            return;
        }

        Player player = raider.getPlayer();
        if (player == null) {
            return;
        }

        WorldPoint point = getWorldLocation(player);
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

            TrackedNpc roomTarget = target.flatMap(trackedNpcs::getByNpc).orElse(null);
            int distanceToNpc = target.map(npc -> npc.getWorldArea().distanceTo2D(player.getWorldArea())).orElse(-1);
            dispatchEvent(new PlayerAttackEvent(stage, tick, point, attack, weapon.orElse(null),
                    raider, roomTarget, distanceToNpc));
        });
    }

    /**
     * Sends an {@link NpcEvent} about an NPC in the room.
     */
    protected void sendNpcUpdate(TrackedNpc trackedNpc) {
        final int tick = getTick();
        WorldPoint point = getWorldLocation(trackedNpc);

        if (trackedNpc.getSpawnTick() == tick) {
            dispatchEvent(NpcEvent.spawn(stage, tick, point, trackedNpc));
        } else {
            dispatchEvent(NpcEvent.update(stage, tick, point, trackedNpc));
        }
    }

    protected void despawnTrackedNpc(TrackedNpc trackedNpc) {
        dispatchEvent(NpcEvent.death(stage, getTick(), getWorldLocation(trackedNpc), trackedNpc));
        trackedNpcs.remove(trackedNpc);
    }

    protected String formattedRoomTime() {
        return Tick.asTimeString(getTick());
    }
}
