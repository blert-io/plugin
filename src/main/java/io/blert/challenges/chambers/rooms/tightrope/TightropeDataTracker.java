/*
 * Copyright (c) 2023-2024 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.blert.challenges.chambers.rooms.tightrope;

import io.blert.challenges.chambers.CoxNpc;
import io.blert.challenges.chambers.CoxChallenge;
import io.blert.challenges.chambers.RoomDataTracker;
import io.blert.core.BasicTrackedNpc;
import io.blert.core.Hitpoints;
import io.blert.core.NpcAttack;
import io.blert.core.RecordableChallenge;
import io.blert.core.Stage;
import io.blert.core.TrackedNpc;
import io.blert.events.NpcAttackEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks Tightrope room (Deathly Mage and Deathly Ranger) events, spawns, HP changes, and attacks.
 * Uses NPC health ratio/scale for HP tracking instead of varbits.
 */
@Slf4j
public class TightropeDataTracker extends RoomDataTracker
{
    private static final int DEATHLY_MAGE_ATTACK_ANIMATION = 1162;
    private static final int DEATHLY_RANGER_ATTACK_ANIMATION = 425;

    private final Map<Integer, BasicTrackedNpc> tightropeNpcs = new HashMap<>(); // Track multiple NPCs by NPC hash
    private final Map<Integer, Boolean> targetedNpcs = new HashMap<>(); // Track which NPC hashes have been targeted
    private final Map<Integer, Boolean> attackedNpcs = new HashMap<>(); // Track which NPC hashes have been attacked
    private @Nullable NpcAttack attackThisTick = null;
    private @Nullable BasicTrackedNpc attackingNpc = null;
    private int lastPlayerAnimation = -1; // Track player's last animation

    public TightropeDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[TightropeDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();
        
        // Check player targeting
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer != null && localPlayer.getInteracting() instanceof NPC)
        {
            NPC targetedNpc = (NPC) localPlayer.getInteracting();
            int npcHash = targetedNpc.hashCode();
            
            // Check if this is an NPC we're tracking and haven't logged targeting for yet
            if (tightropeNpcs.containsKey(npcHash) && !targetedNpcs.getOrDefault(npcHash, false))
            {
                targetedNpcs.put(npcHash, true);
                log.info("[Tightrope Target] First time targeting NPC id={} index={} at tick {}/{}",
                    targetedNpc.getId(), targetedNpc.getIndex(), tick, getStartTick() + tick);
            }
        }
        
        // Check player attacking (based on player animation)
        if (localPlayer != null)
        {
            int currentAnimation = localPlayer.getAnimation();
            
            // Detect if player is performing an attack animation (not idle/moving)
            if (currentAnimation != -1 && currentAnimation != lastPlayerAnimation)
            {
                Actor interacting = localPlayer.getInteracting();
                if (interacting instanceof NPC)
                {
                    NPC attackedNpc = (NPC) interacting;
                    int npcHash = attackedNpc.hashCode();
                    
                    // Check if this is an NPC we're tracking and haven't logged attacking for yet
                    if (tightropeNpcs.containsKey(npcHash) && !attackedNpcs.getOrDefault(npcHash, false))
                    {
                        attackedNpcs.put(npcHash, true);
                        log.info("[Tightrope Attack] First time attacking NPC id={} index={} with animation {} at tick {}/{}",
                            attackedNpc.getId(), attackedNpc.getIndex(), currentAnimation, tick, getStartTick() + tick);
                    }
                }
            }
            
            lastPlayerAnimation = currentAnimation;
        }

        // Update HP for all tracked NPCs
        for (Map.Entry<Integer, BasicTrackedNpc> entry : tightropeNpcs.entrySet())
        {
            BasicTrackedNpc currentNpc = entry.getValue();
            NPC npc = currentNpc.getNpc();
            int ratio = npc.getHealthRatio();
            int scale = npc.getHealthScale();
            
            if (ratio > -1 && scale > 0)
            {
                int updatedHitpoints = (int) (currentNpc.getHitpoints().getBase() * (ratio / (double) scale));
                int currentHitpoints = currentNpc.getHitpoints().getCurrent();
                
                // Only update if there's a change
                if (Math.abs(currentHitpoints - updatedHitpoints) > 0)
                {
                    Hitpoints newHitpoints = currentNpc.getHitpoints().update(updatedHitpoints);
                    currentNpc.setHitpoints(newHitpoints);
                    log.info(
                        "[Tightrope HP] NPC ID: {}, Damaged: {} -> {} (-{}), ratio {}/{} at tick {}/{}",
                        npc.getId(),
                        currentHitpoints, 
                        updatedHitpoints, 
                        Math.abs(currentHitpoints - updatedHitpoints),
                        ratio,
                        scale,
                        tick,
                        getStartTick() + tick
                    );
                }
            }
        }

        if (attackThisTick != null && attackingNpc != null)
        {
            dispatchEvent(new NpcAttackEvent(
                getStage(),
                tick,
                getWorldLocation(attackingNpc.getNpc()),
                attackThisTick,
                attackingNpc
            ));
        }

        attackThisTick = null;
        attackingNpc = null;
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned spawned)
    {
        // Only track NPCs if this room tracker is still active
        if (terminating()) {
            log.debug("[Tightrope] Ignoring NPC spawn {} - room tracker is terminating", spawned.getNpc().getId());
            return Optional.empty();
        }
        
        NPC npc = spawned.getNpc();
        int npcHash = npc.hashCode();

        // Check if this NPC ID corresponds to Deathly Mage or Deathly Ranger
        Optional<CoxNpc> coxNpcOpt = CoxNpc.withId(npc.getId());
        if (coxNpcOpt.isPresent()) {
            CoxNpc coxNpc = coxNpcOpt.get();
            log.info("[Tightrope Room] Found CoxNpc enum: {} for NPC id {}", coxNpc, npc.getId());
            
            // Check if it's a Deathly Mage or Deathly Ranger
            if ((coxNpc == CoxNpc.DEATHLY_MAGE || coxNpc == CoxNpc.DEATHLY_RANGER) 
                    && !tightropeNpcs.containsKey(npcHash)) {
                CoxChallenge coxChallenge = (CoxChallenge) getChallenge();
                BasicTrackedNpc newNpc = new BasicTrackedNpc(
                    npc,
                    coxNpc,
                    generateRoomId(npc),
                    new Hitpoints(coxNpc.getBaseHitpoints())
                );
                tightropeNpcs.put(npcHash, newNpc);
                targetedNpcs.put(npcHash, false); // Initialize targeting state
                attackedNpcs.put(npcHash, false); // Initialize attacking state
                String modeStatus = coxChallenge.isChallengeMode() ? " [Challenge Mode]" : " [Normal Mode]";
                log.info("✓ Tightrope NPC tracked: id={}, enum={}, base HP {} (scale={}) - Total NPCs: {}{}", 
                            npc.getId(), coxNpc, newNpc.getHitpoints().getBase(), getChallenge().getScale(), tightropeNpcs.size(), modeStatus);
                return Optional.of(newNpc);
            }
        } else {
            log.debug("[Tightrope Room] NPC id {} not found in CoxNpc enum", npc.getId());
        }
        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    {
        NPC npc = despawned.getNpc();
        int npcHash = npc.hashCode();
        BasicTrackedNpc removedNpc = tightropeNpcs.remove(npcHash);
        if (removedNpc != null)
        {
            targetedNpcs.remove(npcHash); // Clean up targeting state
            attackedNpcs.remove(npcHash); // Clean up attacking state
            log.info("[Tightrope] Despawned NPC id={} at tick {}/{}. Remaining NPCs: {}", 
                npc.getId(), getTick(), getTick() + getStartTick(), tightropeNpcs.size());
            if (tightropeNpcs.size() == 0)
            {
                log.info("[Tightrope] All NPCs dead, room finishing at tick {}/{}", 
                    getTick(), getStartTick() + getTick());
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onAnimation(AnimationChanged event)
    {
        Actor actor = event.getActor();
        
        // Check if the animation is from any of our tracked NPCs
        boolean isTrackedNpc = false;
        for (BasicTrackedNpc npc : tightropeNpcs.values())
        {
            if (actor == npc.getNpc())
            {
                isTrackedNpc = true;
                break;
            }
        }
        
        if (!isTrackedNpc)
        {
            return;
        }

        // Find which tracked NPC is performing the animation
        BasicTrackedNpc performingNpc = null;
        for (BasicTrackedNpc npc : tightropeNpcs.values())
        {
            if (actor == npc.getNpc())
            {
                performingNpc = npc;
                break;
            }
        }

        switch (actor.getAnimation())
        {
            case DEATHLY_MAGE_ATTACK_ANIMATION:
                attackThisTick = NpcAttack.COX_TIGHTROPE_MAGE;
                attackingNpc = performingNpc;
                log.info("[Tightrope] Deathly Mage attack animation detected ({})", DEATHLY_MAGE_ATTACK_ANIMATION);
                break;
            case DEATHLY_RANGER_ATTACK_ANIMATION:
                attackThisTick = NpcAttack.COX_TIGHTROPE_RANGE;
                attackingNpc = performingNpc;
                log.info("[Tightrope] Deathly Ranger attack animation detected ({})", DEATHLY_RANGER_ATTACK_ANIMATION);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onHitsplat(HitsplatApplied event)
    {
        if (event.getActor() instanceof NPC &&
            event.getHitsplat().getHitsplatType() == HitsplatID.HEAL)
        {
            // Check if heal is on any of our tracked NPCs
            for (BasicTrackedNpc npc : tightropeNpcs.values())
            {
                if (event.getActor() == npc.getNpc())
                {
                    setHealTick(getTick());
                    log.info("[Tightrope HP] Heal hitsplat detected on NPC id={} at tick {}", 
                        npc.getNpc().getId(), getHealTick());
                    break;
                }
            }
        }
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        // Not using varbits for Tightrope NPCs - using health ratio/scale instead
    }
}
