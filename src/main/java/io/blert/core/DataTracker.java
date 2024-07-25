/*
 * Copyright (c) 2024 Alexei Frolov
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

package io.blert.core;

import io.blert.events.*;
import io.blert.util.Location;
import io.blert.util.Tick;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class DataTracker {
    protected enum State {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        TERMINATING,
    }

    @Getter(AccessLevel.PROTECTED)
    private final RecordableChallenge challenge;

    protected final Client client;
    protected final ClientThread clientThread;

    @Getter
    private final Stage stage;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private State state;

    private final SpecialAttackTracker specialAttackTracker = new SpecialAttackTracker(this::onSpecialAttack);

    private int startClientTick;
    @Getter
    private int totalTicks;

    @Getter(AccessLevel.PROTECTED)
    private final TrackedNpcCollection trackedNpcs = new TrackedNpcCollection();

    public DataTracker(RecordableChallenge challenge, Client client, Stage stage) {
        this.challenge = challenge;
        this.client = client;
        this.clientThread = challenge.getClientThread();
        this.state = State.NOT_STARTED;
        this.stage = stage;
    }

    public boolean notStarted() {
        return state == State.NOT_STARTED;
    }

    public boolean inProgress() {
        return state == State.IN_PROGRESS;
    }

    public boolean completed() {
        return state == State.COMPLETED;
    }

    public boolean terminating() {
        return state == State.TERMINATING;
    }

    /**
     * Prepares the tracker for cleanup, preventing any further events from being processed.
     */
    public void terminate() {
        if (getState() == State.IN_PROGRESS) {
            finish(false);
        }
        state = State.TERMINATING;
    }

    public void tick() {
        if (state != State.IN_PROGRESS) {
            return;
        }

        updatePlayers();
        specialAttackTracker.processPendingSpecial();

        challenge.getParty().forEach(this::checkForPlayerAttack);

        // Run implementation-specific behavior.
        onTick();

        // Send out an update for every tracked NPC. This must be done after `onTick` to ensure any
        // implementation-specific changes to the NPC are complete.
        trackedNpcs.forEach(this::sendNpcUpdate);
    }

    /**
     * Returns number of ticks the tracker has been active.
     *
     * @return The current tick.
     */
    public int getTick() {
        if (notStarted()) {
            return 0;
        }
        return client.getTickCount() - this.startClientTick;
    }

    /**
     * Gathers information about the stage and dispatches appropriate events. Invoked every tick while the challenge
     * is in progress.
     */
    protected abstract void onTick();

    /**
     * Implementation-specific equivalent of the {@code onGameStateChanged} Runelite event handler.
     * Should be overriden by implementations which require special handling.
     *
     * @param event The event.
     */
    protected void onGameState(GameStateChanged event) {
    }

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
     * Implementation-specific equivalent of the {@code onAnimationChanged} Runelite event handler.
     * Should be overriden by implementations which require special animation tracking.
     *
     * @param event The animation event.
     */
    protected void onAnimation(AnimationChanged event) {
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
     * Implementation-specific equivalent of the {@code onChatMessage} Runelite event handler.
     * Should be overriden by implementations which require special animation tracking.
     *
     * @param event The event.
     */
    protected void onMessage(ChatMessage event) {
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
     * Implementation-specific equivalent of the {@code onGroundObjectSpawned} Runelite event handler.
     * Should be overriden by implementations which require special handling.
     *
     * @param event The event.
     */
    protected void onGroundObjectSpawn(GroundObjectSpawned event) {
    }

    /**
     * Implementation-specific equivalent of the {@code onGroundObjectDespawned} Runelite event handler.
     * Should be overriden by implementations which require special handling.
     *
     * @param event The event.
     */
    protected void onGroundObjectDespawn(GroundObjectDespawned event) {
    }

    /**
     * Implementation-specific equivalent of the {@code onActorDeath} Runelite event handler.
     * Should be overriden by implementations which require special handling.
     *
     * @param event The event.
     */
    protected void onDeath(ActorDeath event) {
    }

    /**
     * Implementation-specific equivalent of the {@code onVarbitChanged} Runelite event handler.
     * Should be overriden by implementations which require special handling.
     *
     * @param event The event.
     */
    protected void onVarbit(VarbitChanged event) {
    }

    protected WorldPoint getWorldLocation(@NonNull Actor actor) {
        return Location.getWorldLocation(client, actor.getWorldLocation());
    }

    protected WorldPoint getWorldLocation(@NonNull TrackedNpc trackedNpc) {
        return getWorldLocation(trackedNpc.getNpc());
    }

    protected WorldPoint getWorldLocation(@NonNull GameObject object) {
        return Location.getWorldLocation(client, object.getWorldLocation());
    }

    protected WorldPoint getWorldLocation(@NonNull GroundObject object) {
        return Location.getWorldLocation(client, object.getWorldLocation());
    }

    /**
     * Sends an event to the registered event handler, if any.
     */
    protected void dispatchEvent(Event event) {
        challenge.dispatchEvent(event);
    }

    protected void start(int tickOffset) {
        this.startClientTick = client.getTickCount() + tickOffset;
        setState(State.IN_PROGRESS);

        client.getPlayers().forEach(player -> {
            Raider raider = challenge.getRaider(player.getName());
            if (raider != null) {
                raider.setPlayer(player);
                raider.resetForNewRoom();
            }
        });

        dispatchEvent(new StageUpdateEvent(getStage(), 0, StageUpdateEvent.Status.STARTED));
    }

    /**
     * Starts the tracker.
     */
    protected void start() {
        start(0);
    }

    protected void finish(boolean completion) {
        finish(completion, -1);
    }

    /**
     * Finishes tracking data for the stage and performs any necessary cleanup.
     *
     * @param completion       Whether the stage was completed successfully.
     * @param inGameStageTicks The number of in-game ticks the stage took to complete, or -1 if the in-game timer is not
     *                         available. If provided, it is used to verify the accuracy of the recorded stage time.
     */
    protected void finish(boolean completion, int inGameStageTicks) {
        if (state != State.IN_PROGRESS) {
            return;
        }

        setState(State.COMPLETED);
        int lastRecordedRoomTick = getTick();
        boolean accurate;

        if (inGameStageTicks != -1) {
            if (inGameStageTicks != lastRecordedRoomTick) {
                log.warn("Stage {} completion time mismatch: in-game room ticks = {}, recorded ticks = {}",
                        stage, inGameStageTicks, lastRecordedRoomTick);
                accurate = false;
                lastRecordedRoomTick = inGameStageTicks;
            } else {
                accurate = true;
            }
        } else {
            log.debug("Stage {} finished in {} unconfirmed ticks ({})",
                    stage, lastRecordedRoomTick, Tick.asTimeString(lastRecordedRoomTick));
            accurate = false;
        }

        final int finalRoomTick = lastRecordedRoomTick;
        totalTicks = finalRoomTick;

        boolean spectator = !challenge.playerIsInChallenge(client.getLocalPlayer().getName());
        boolean isWipe = challenge.getParty().stream().allMatch(Raider::isDead);

        if (spectator && !completion && !isWipe) {
            log.info("Spectator left challenge at stage {}", stage);
        } else {
            var status = completion ? StageUpdateEvent.Status.COMPLETED : StageUpdateEvent.Status.WIPED;
            log.info("Stage {} finished, status: {}", stage, status);

            // Don't send the final room status immediately; allow other pending subscribers to run and dispatch their
            // own events first.
            clientThread.invokeLater(() ->
                    challenge.dispatchEvent(new StageUpdateEvent(getStage(), finalRoomTick, status, accurate)));
        }
    }

    private void updatePlayers() {
        int tick = getTick();

        client.getPlayers().forEach(player -> {
            Raider raider = challenge.getRaider(player.getName());
            if (raider == null || raider.isDead()) {
                return;
            }

            raider.updateState(client, player, tick);

            dispatchEvent(PlayerUpdateEvent.fromRaider(getStage(), tick, getWorldLocation(player), client, raider));
        });
    }

    private void checkForPlayerAttack(@NonNull Raider raider) {
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
            maybeAttack = PlayerAttack.findBlowpipeSuppressedAttack(weaponId);
        } else {
            maybeAttack = PlayerAttack.find(weaponId, animationId);
        }

        maybeAttack.ifPresent(attack -> {
            if (attack == PlayerAttack.BLOWPIPE) {
                if (checkForBlowpipeSpecial(raider)) {
                    attack = PlayerAttack.BLOWPIPE_SPEC;
                }
            } else if (attack.hasProjectile()) {
                attack = adjustForProjectile(attack, raider);
            }

            raider.recordAttack(tick, attack);

            TrackedNpc roomTarget = target.flatMap(trackedNpcs::getByNpc).orElse(null);
            int distanceToNpc = target.map(npc -> npc.getWorldArea().distanceTo2D(player.getWorldArea())).orElse(-1);
            dispatchEvent(new PlayerAttackEvent(getStage(), tick, point, attack, weapon.orElse(null),
                    raider, roomTarget, distanceToNpc));
        });
    }

    private PlayerAttack adjustForProjectile(PlayerAttack attack, Raider raider) {
        List<PlayerAttack.Projectile> possibleProjectiles = attack.projectilesForAnimation();

        for (Projectile p : client.getProjectiles()) {
            for (PlayerAttack.Projectile projectile : possibleProjectiles) {
                if (projectileMatches(p, projectile, raider.getPlayer())) {
                    return projectile.getAttack();
                }
            }
        }

        return attack;
    }

    private boolean checkForBlowpipeSpecial(Raider raider) {
        var weapon = raider.getEquippedItem(EquipmentSlot.WEAPON).orElse(null);
        if (weapon == null) {
            return false;
        }

        PlayerAttack.Projectile specProjectile;
        switch (weapon.getId()) {
            case ItemID.TOXIC_BLOWPIPE:
                specProjectile = PlayerAttack.Projectile.BLOWPIPE_SPEC;
                break;
            case ItemID.BLAZING_BLOWPIPE:
                specProjectile = PlayerAttack.Projectile.BLAZING_BLOWPIPE_SPEC;
                break;
            default:
                return false;
        }

        for (Projectile projectile : client.getProjectiles()) {
            if (projectileMatches(projectile, specProjectile, raider.getPlayer())) {
                return true;
            }
        }

        return false;
    }

    private boolean projectileMatches(Projectile p, PlayerAttack.Projectile projectile, Player player) {
        if (p.getId() != projectile.getId()) {
            return false;
        }

        WorldPoint origin = WorldPoint.fromLocalInstance(client, new LocalPoint(p.getX1(), p.getY1()));
        boolean originatesFromPlayer = origin.distanceTo2D(getWorldLocation(player)) == 0;
        boolean startCycleMatches = p.getStartCycle() - client.getGameCycle() == projectile.getStartCycleOffset();

        return originatesFromPlayer && startCycleMatches;
    }

    /**
     * Sends an {@link NpcEvent} about an NPC in the room.
     */
    protected void sendNpcUpdate(TrackedNpc trackedNpc) {
        final int tick = getTick();
        WorldPoint point = getWorldLocation(trackedNpc);

        if (trackedNpc.getSpawnTick() == tick) {
            dispatchEvent(NpcEvent.spawn(getStage(), tick, point, trackedNpc));
        } else {
            dispatchEvent(NpcEvent.update(getStage(), tick, point, trackedNpc));
        }

        trackedNpc.setUpdatedProperties(false);
    }

    protected void addTrackedNpc(TrackedNpc trackedNpc) {
        boolean existing = trackedNpcs.remove(trackedNpc);
        if (!existing) {
            trackedNpc.setSpawnTick(getTick());
        }
        trackedNpcs.add(trackedNpc);

        if (notStarted()) {
            // NPCs which spawn before the room begins must be reported immediately as the `onTick` handler
            // is not yet active.
            dispatchEvent(NpcEvent.spawn(getStage(), 0, getWorldLocation(trackedNpc), trackedNpc));
        }

        challenge.updateMode(trackedNpc.getMode());
    }

    protected void despawnTrackedNpc(TrackedNpc trackedNpc) {
        if (trackedNpcs.remove(trackedNpc)) {
            dispatchEvent(NpcEvent.death(getStage(), getTick(), getWorldLocation(trackedNpc), trackedNpc));
        }
    }

    protected long generateRoomId(@NonNull NPC npc) {
        return npc.getIndex();
    }

    private void onSpecialAttack(SpecialAttackTracker.SpecialAttack spec) {
        var weapon = client.getItemDefinition(spec.getWeapon().getId());
        log.debug("Hit a {} with {} on {}", spec.getDamage(), weapon.getName(), spec.getTarget().getName());
    }

    @Subscribe
    public final void onGameStateChanged(GameStateChanged event) {
        if (terminating()) {
            return;
        }

        onGameState(event);
    }

    @Subscribe
    public final void onNpcSpawned(NpcSpawned event) {
        if (terminating()) {
            return;
        }

        onNpcSpawn(event).ifPresent(this::addTrackedNpc);
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
    protected final void onAnimationChanged(AnimationChanged event) {
        if (getState() != State.IN_PROGRESS) {
            return;
        }

        Actor actor = event.getActor();
        if (actor instanceof Player) {
            Raider raider = challenge.getRaider(actor.getName());
            if (raider != null) {
                raider.setAnimation(getTick(), actor.getAnimation());
            }
        }

        onAnimation(event);
    }

    @Subscribe
    protected final void onProjectileMoved(ProjectileMoved event) {
        if (!terminating()) {
            onProjectile(event);
        }
    }

    @Subscribe(priority = 10)
    protected final void onChatMessage(ChatMessage event) {
        if (state == State.IN_PROGRESS || event.getType() == ChatMessageType.GAMEMESSAGE) {
            onMessage(event);
        }
    }

    @Subscribe
    protected final void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
        if (getState() != State.IN_PROGRESS) {
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
    protected final void onGroundObjectSpawned(GroundObjectSpawned event) {
        if (!terminating()) {
            onGroundObjectSpawn(event);
        }
    }

    @Subscribe
    protected final void onGroundObjectDespawned(GroundObjectDespawned event) {
        if (!terminating()) {
            onGroundObjectDespawn(event);
        }
    }

    @Subscribe
    protected final void onActorDeath(ActorDeath event) {
        if (terminating()) {
            return;
        }

        if (event.getActor() instanceof Player) {
            Raider raider = challenge.getRaider(event.getActor().getName());
            if (raider != null) {
                raider.setDead(true);
                dispatchEvent(new PlayerDeathEvent(
                        getStage(), getTick(), getWorldLocation(event.getActor()), raider.getUsername()));
            }
        }

        onDeath(event);
    }

    @Subscribe
    protected final void onVarbitChanged(VarbitChanged varbitChanged) {
        if (getState() != State.IN_PROGRESS) {
            return;
        }

        if (varbitChanged.getVarpId() == VarPlayer.SPECIAL_ATTACK_PERCENT) {
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

        onVarbit(varbitChanged);
    }
}
