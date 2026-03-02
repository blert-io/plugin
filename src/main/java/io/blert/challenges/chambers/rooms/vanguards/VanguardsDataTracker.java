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

package io.blert.challenges.chambers.rooms.vanguards;

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

@Slf4j
public class VanguardsDataTracker extends RoomDataTracker
{
    // Map from initial spawn NPC hash to transformed NPC hash
    private final Map<Integer, Integer> initialToTransformedHash = new HashMap<>();
    // Map from NPC hash to their spawn tick
    private final Map<Integer, Integer> npcSpawnTicks = new HashMap<>();
    // Vanguards share the same HP varbit, but we can track which one was last damaged
    private static final int VANGUARD_HP_VARBIT = 6099;
    
    private static final int VANGUARD_SPAWN_ANIMATION = 7428;
    private static final int VANGUARD_MAGE_ANIMATION = 7436;
    private static final int VANGUARD_MELEE_ANIMATION = 7441;
    private static final int VANGUARD_RANGED_ANIMATION = 7446;
    private static final int VANGUARD_HEAL_ANIMATION = 7431;

    // Track multiple vanguards using their NPC hash codes as keys
    private final Map<Integer, BasicTrackedNpc> vanguards = new HashMap<>();
    private @Nullable NpcAttack attackThisTick = null;
    
    // Track which vanguard was last damaged to attribute varbit changes
    private @Nullable BasicTrackedNpc lastDamagedVanguard = null;
    private int lastVarbitValue = -1;
    private int lastHitsplatTick = -1;
    // Add this field to your class
    private final Map<Integer, Integer> vanguardSpawnAnimationTicks = new HashMap<>();

    public VanguardsDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[VanguardsDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();

        // Check varbit for HP changes and attribute to last damaged vanguard if recent
        int currentVarbitValue = client.getVarbitValue(VANGUARD_HP_VARBIT);
        if (lastVarbitValue != -1 && currentVarbitValue != lastVarbitValue)
        {
            // If we have a recently damaged vanguard, attribute the change to them
            if (lastDamagedVanguard != null && (tick - lastHitsplatTick) <= 3)
            {
                lastDamagedVanguard.setHitpoints(lastDamagedVanguard.getHitpoints().update(currentVarbitValue));
                
                log.info(
                    "[Vanguard {}] HP updated via varbit: {} -> {} ({}{}) at tick {}/{}",
                    lastDamagedVanguard.getNpc().getId(),
                    lastVarbitValue,
                    currentVarbitValue,
                    currentVarbitValue > lastVarbitValue ? "+" : "",
                    currentVarbitValue - lastVarbitValue,
                    tick,
                    getStartTick() + tick
                );
            }
            else
            {
                log.debug(
                    "[Vanguards] Varbit changed {} -> {} but no recent damage target (last damage {} ticks ago)",
                    lastVarbitValue,
                    currentVarbitValue,
                    lastDamagedVanguard != null ? (tick - lastHitsplatTick) : "never"
                );
            }
        }
        lastVarbitValue = currentVarbitValue;

        // Process any attacks
        if (attackThisTick != null)
        {
            // Find the vanguard that performed the attack (for now, just use the first one)
            // TODO: Improve attack attribution when we have better animation data
            for (BasicTrackedNpc vanguard : vanguards.values())
            {
                dispatchEvent(new NpcAttackEvent(
                    getStage(),
                    tick,
                    getWorldLocation(vanguard.getNpc()),
                    attackThisTick,
                    vanguard
                ));
                break; // Only dispatch once per tick
            }
        }

        attackThisTick = null;
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned spawned)
    {
        NPC npc = spawned.getNpc();
        String npcName = npc.getName();

        // Track spawn tick for this NPC hash
        npcSpawnTicks.put(npc.hashCode(), getTick());

        // Debug: Log all NPC spawns in this room to help identify Vanguards
        log.info("[Vanguards] NPC spawned: id={}, tick={}/{}", npc.getId(), getTick(), getStartTick() + getTick());

        // First try to match by CoxNpc enum
        Optional<CoxNpc> coxNpcOpt = CoxNpc.withId(npc.getId());
        if (coxNpcOpt.isPresent())
        {
            CoxNpc coxNpc = coxNpcOpt.get();
            // Match any of the Vanguard types (including initial spawn)
            if (coxNpc == CoxNpc.VANGUARD_INITIAL || coxNpc == CoxNpc.VANGUARD_MELEE || 
                coxNpc == CoxNpc.VANGUARD_RANGED || coxNpc == CoxNpc.VANGUARD_MAGIC)
            {
                return createVanguardTracker(npc, coxNpc);
            }
        }

        // Fallback: Try to detect Vanguards by name if ID lookup failed
        if (npcName != null && npcName.toLowerCase().contains("vanguard"))
        {
            log.warn("[Vanguards] Detected Vanguard by name: id={}, name='{}' (not in CoxNpc enum - please add this ID)", npc.getId(), npcName);

            // Determine type from name for fallback tracking
            CoxNpc fallbackType = CoxNpc.VANGUARD_MELEE; // Default
            if (npcName.toLowerCase().contains("ranged"))
            {
                fallbackType = CoxNpc.VANGUARD_RANGED;
            }
            else if (npcName.toLowerCase().contains("magic") || npcName.toLowerCase().contains("mage"))
            {
                fallbackType = CoxNpc.VANGUARD_MAGIC;
            }

            return createVanguardTracker(npc, fallbackType);
        }

        return Optional.empty();
    }
    
    private Optional<BasicTrackedNpc> createVanguardTracker(NPC npc, CoxNpc coxNpc)
    {
        log.info("[Vanguards] Creating tracker for Vanguard: id={}, type={}", npc.getId(), coxNpc);
        
        // Check if we already have this specific NPC tracked
        int npcHash = npc.hashCode();
        
        // Check if this might be an ID transition from an existing Vanguard
        BasicTrackedNpc existingVanguard = findVanguardByTransition(npc, coxNpc);
        if (existingVanguard != null)
        {
            log.info("[Vanguards] Detected ID transition: {} -> {} for existing Vanguard", 
                        existingVanguard.getNpc().getId(), npc.getId());
            
            // Update the existing tracker with new NPC reference and type
            vanguards.remove(existingVanguard.getNpc().hashCode());
            
            BasicTrackedNpc updatedVanguard = new BasicTrackedNpc(
                npc,
                coxNpc,
                existingVanguard.getRoomId(), // Keep same room ID
                existingVanguard.getHitpoints() // Keep current HP
            );
            
            vanguards.put(npcHash, updatedVanguard);
            
            log.info("[Vanguards] Updated Vanguard tracker: old_id={}, new_id={}, type={}, hp={}",
                        existingVanguard.getNpc().getId(), npc.getId(), coxNpc, 
                        updatedVanguard.getHitpoints().getCurrent());
            
            return Optional.of(updatedVanguard);
        }
        
        if (!vanguards.containsKey(npcHash))
        {
            CoxChallenge coxChallenge = (CoxChallenge) getChallenge();
            
            BasicTrackedNpc newVanguard = new BasicTrackedNpc(
                npc,
                coxNpc,
                generateRoomId(npc),
                new Hitpoints(coxNpc.getBaseHitpoints())
            );
            
            vanguards.put(npcHash, newVanguard);
            String modeStatus = coxChallenge.isChallengeMode() ? " [Challenge Mode]" : " [Normal Mode]";
            
            // Initialize varbit tracking on first vanguard spawn
            if (vanguards.size() == 1)
            {
                lastVarbitValue = client.getVarbitValue(VANGUARD_HP_VARBIT);
                log.info("[Vanguards] Initialized varbit tracking with value: {}", lastVarbitValue);
            }
            
            log.info(
                "✓ Vanguard {} tracked: id={}, base HP {} (scale={}){} - Total Vanguards: {}",
                coxNpc.name(),
                npc.getId(),
                newVanguard.getHitpoints().getBase(),
                getChallenge().getScale(),
                modeStatus,
                vanguards.size()
            );
            
            return Optional.of(newVanguard);
        }
        else
        {
            log.debug("[Vanguards] Vanguard {} already tracked", coxNpc.name());
            return Optional.of(vanguards.get(npcHash));
        }
    }
    
    /**
     * Find an existing Vanguard that might be transitioning to a new ID
     */
    private BasicTrackedNpc findVanguardByTransition(NPC newNpc, CoxNpc newType)
    {
        // If this is a transition from initial spawn (7525) to specific type
        if (newType != CoxNpc.VANGUARD_INITIAL)
        {
            for (BasicTrackedNpc vanguard : vanguards.values())
            {
                // Look for an initial spawn (7525) that should transition
                if (vanguard.getNpc().getId() == 7525)  // Check for initial spawn ID
                {
                    // Check if NPCs are at similar positions (indicating same entity)
                    var oldPos = vanguard.getNpc().getWorldLocation();
                    var newPos = newNpc.getWorldLocation();

                    if (oldPos != null && newPos != null && oldPos.distanceTo(newPos) <= 2)
                    {
                        // Map initial hash to transformed hash
                        initialToTransformedHash.put(vanguard.getNpc().hashCode(), newNpc.hashCode());
                        // Optionally, also map transformed hash to initial hash if needed
                        // initialToTransformedHash.put(newNpc.hashCode(), vanguard.getNpc().hashCode());
                        return vanguard;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get the spawn tick for a given NPC hash (initial or transformed)
     */
    public int getSpawnTickForNpcHash(int npcHash) {
        return npcSpawnTicks.getOrDefault(npcHash, -1);
    }

    /**
     * Get the transformed hash for an initial spawn hash
     */
    public Integer getTransformedHashForInitial(int initialHash) {
        return initialToTransformedHash.get(initialHash);
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    {
        NPC npc = despawned.getNpc();
        int npcHash = npc.hashCode();
        
        // Check if this NPC is one of our tracked vanguards
        BasicTrackedNpc vanguard = vanguards.get(npcHash);
        if (vanguard != null && npc == vanguard.getNpc())
        {
            // Remove the vanguard from our tracking
            vanguards.remove(npcHash);

            log.info("[Vanguards] Despawned Vanguard NPC id={} at tick {} – Remaining Vanguards: {}", 
                        npc.getId(), getTick(), vanguards.size());
            // if (vanguards.size() == 0)
            // {
            //     int crystalAnimation = 4;
            //     // Use the spawn tick of the last removed Vanguard for tick cycle calculation
            //     int lastSpawnTick = getSpawnTickForNpcHash(npcHash);
            //     int tick_cycle = (4 - ((lastSpawnTick + getStartTick()) % 4)) % 4;
            //     int tick_cycle_room = (4 - (lastSpawnTick % 4)) % 4;
            //     log.info("[Vanguards] Last Vang id={} spawn {}, tick cycle raid {}, tick cycle room {}", npc.getId(), lastSpawnTick, tick_cycle, tick_cycle_room);
            //     // if (crystalAnimation + getTick() + tick_cycle != crystalAnimation + getTick() + tick_cycle_room) {
            //     //     log.info("[Vanguards] One of the tick cycles are incorrect for Vanguard id={}", npc.getId());
            //     // }
            //     log.info("[Vanguards] Room end {}/{} (spawn tick)", crystalAnimation + getTick() + tick_cycle, getTick() + getStartTick() + crystalAnimation + tick_cycle);
            //     int spawnAnimTick = vanguardSpawnAnimationTicks.getOrDefault(npcHash, -1);
            //     int tick_cycle_anim = (4 - ((spawnAnimTick + getStartTick()) % 4)) % 4;
            //     int tick_cycle_room_anim = (4 - (spawnAnimTick % 4)) % 4;
            //     log.info("[Vanguards] Vanguard id={} had spawn animation tick {}, tick cycle raid {}, tick cycle room {}", npc.getId(), spawnAnimTick, tick_cycle_anim, tick_cycle_room_anim);
            //     // Keep the original logs for reference
            //     log.info("[Vanguards No Cycle] Room end {}/{}", crystalAnimation + getTick(), getTick() + getStartTick() + crystalAnimation);
            //     log.info("[Vanguards Test 4 tick cycle] Room end {}/{}", crystalAnimation + getTick() + tick_cycle, getTick() + getStartTick() + crystalAnimation + tick_cycle);
            // }
            return true;
        }
        
        return false;
    }

    @Override
    protected void onAnimation(AnimationChanged event)
    {
        Actor actor = event.getActor();
        int animation = actor.getAnimation();
        
        // Check if the actor is one of our tracked vanguards
        if (actor instanceof NPC)
        {
            NPC npc = (NPC) actor;
            BasicTrackedNpc vanguard = vanguards.get(npc.hashCode());
            
            if (vanguard != null)
            {
                // log.debug("[Vanguard Animation] Animation: {} at tick {}", animation, getTick());
                
                switch (animation)
                {
                    case VANGUARD_SPAWN_ANIMATION:
                        vanguardSpawnAnimationTicks.put(npc.hashCode(), getTick());
                        log.info("[Vanguard {}] Spawn animation detected at tick {}/{}", npc.getId(), getTick(), getTick() + getStartTick());
                        break;
                    case VANGUARD_MAGE_ANIMATION:
                        attackThisTick = NpcAttack.COX_VANGS_MAGE;
                        log.info("[Vanguard {}] Mage animation detected", npc.getId());
                        break;
                    case VANGUARD_MELEE_ANIMATION:
                        attackThisTick = NpcAttack.COX_VANGS_MELEE;
                        log.info("[Vanguard {}] Melee animation detected", npc.getId());
                        break;
                    case VANGUARD_RANGED_ANIMATION:
                        attackThisTick = NpcAttack.COX_VANGS_RANGED;
                        log.info("[Vanguard {}] Ranged animation detected", npc.getId());
                        break;
                    case VANGUARD_HEAL_ANIMATION:
                        attackThisTick = NpcAttack.COX_VANGS_HEAL;
                        log.info("[Vanguard {}] Heal animation detected", npc.getId());
                        break;
                    default:
                        // Only log truly unknown animations
                        if (animation != -1 && animation != 0)
                        {
                            // log.info("[Vanguard {}] Unknown animation: {} at tick {}", npc.getId(), animation, getTick());
                        }
                        break;
                }
            }
        }
    }

    @Override
    protected void onHitsplat(HitsplatApplied event)
    {
        if (event.getActor() instanceof NPC)
        {
            NPC npc = (NPC) event.getActor();
            BasicTrackedNpc vanguard = vanguards.get(npc.hashCode());
            
            if (vanguard != null)
            {
                Hitsplat hitsplat = event.getHitsplat();
                int hitsplatType = hitsplat.getHitsplatType();
                
                if (hitsplatType == HitsplatID.HEAL)
                {
                    setHealTick(getTick());
                    log.info("[Vanguard {}] Heal hitsplat detected at tick {}", npc.getId(), getHealTick());
                }
                else if (hitsplat.getAmount() > 0)
                {
                    // Track this vanguard as the last one damaged for varbit attribution
                    lastDamagedVanguard = vanguard;
                    lastHitsplatTick = getTick();
                    
                    log.debug("[Vanguard {}] Damage hitsplat: {} (type {}) - marked for varbit attribution", 
                                npc.getId(), hitsplat.getAmount(), hitsplatType);
                }
            }
        }
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        if (event.getVarbitId() == VANGUARD_HP_VARBIT)
        {
            int newValue = event.getValue();
            
            log.debug(
                "[Vanguards] HP varbit {} changed to {} at tick {} (last damaged: {})",
                event.getVarbitId(),
                newValue,
                getTick(),
                lastDamagedVanguard != null ? lastDamagedVanguard.getNpc().getId() : "none"
            );
            
            setShouldUpdateHitpoints(true);
        }
    }
}