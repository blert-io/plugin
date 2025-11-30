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
    // Vanguards share the same HP varbit, but we can track which one was last damaged
    private static final int VANGUARD_HP_VARBIT = 6099;
    
    // TODO: update when you finalize Vanguard animations from logging
    private static final int VANGUARD_ANVIL_ANIMATION = 7475;
    private static final int VANGUARD_STOMP_ANIMATION = 7491;
    private static final int VANGUARD_AUTO_ANIMATION = 7492;
    // Additional discovered animations
    private static final int VANGUARD_UNKNOWN_7483 = 7483;
    private static final int VANGUARD_UNKNOWN_7487 = 7487;
    private static final int VANGUARD_UNKNOWN_7488 = 7488;
    private static final int VANGUARD_UNKNOWN_7493 = 7493;
    private static final int VANGUARD_UNKNOWN_7494 = 7494;
    private static final int VANGUARD_UNKNOWN_7481 = 7481;
    private static final int VANGUARD_UNKNOWN_7482 = 7482;
    private static final int VANGUARD_UNKNOWN_7484 = 7484;

    // Track multiple vanguards using their NPC hash codes as keys
    private final Map<Integer, BasicTrackedNpc> vanguards = new HashMap<>();
    private @Nullable NpcAttack attackThisTick = null;
    
    // Track which vanguard was last damaged to attribute varbit changes
    private @Nullable BasicTrackedNpc lastDamagedVanguard = null;
    private int lastVarbitValue = -1;
    private int lastHitsplatTick = -1;

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
        
        // Debug: Log all NPC spawns in this room to help identify Vanguards
        log.info("[Vanguards] NPC spawned: id={}, name='{}'", npc.getId(), npcName);
        
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
            int scaledHp = coxChallenge.getScaledHitpoints(coxNpc);
            
            BasicTrackedNpc newVanguard = new BasicTrackedNpc(
                npc,
                coxNpc,
                generateRoomId(npc),
                new Hitpoints(scaledHp)
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
                "✓ Vanguard {} tracked: id={}, base HP {} (scaled={}){} - Total Vanguards: {}",
                coxNpc.name(),
                npc.getId(),
                coxNpc.getOriginalBaseHitpoints(),
                scaledHp,
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
                        return vanguard;
                    }
                }
            }
        }
        
        return null;
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
            if (vanguards.size() == 0)
            {
                int crystalAnimation = 4;
                log.info("[Vanguards] All Vanguards despawned expected room end {} animation detected", crystalAnimation + getTick());
            }
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
                    case VANGUARD_ANVIL_ANIMATION:
                        // attackThisTick = NpcAttack.COX_VANGUARD_ANVIL;
                        log.info("[Vanguard {}] Anvil animation detected", npc.getId());
                        break;
                    case VANGUARD_STOMP_ANIMATION:
                        // attackThisTick = NpcAttack.COX_VANGUARD_STOMP;
                        // log.info("[Vanguard {}] Stomp animation detected", npc.getId());
                        break;
                    case VANGUARD_AUTO_ANIMATION:
                        // attackThisTick = NpcAttack.COX_VANGUARD_AUTO;
                        // log.info("[Vanguard {}] Auto attack animation detected", npc.getId());
                        break;
                    // Discovered animations - TODO: determine which attacks these represent
                    case VANGUARD_UNKNOWN_7483:
                    case VANGUARD_UNKNOWN_7487:
                    case VANGUARD_UNKNOWN_7488:
                    case VANGUARD_UNKNOWN_7493:
                    case VANGUARD_UNKNOWN_7494:
                    case VANGUARD_UNKNOWN_7481:
                    case VANGUARD_UNKNOWN_7482:
                    case VANGUARD_UNKNOWN_7484:
                        // log.debug("[Vanguard {}] Known animation: {} at tick {}", npc.getId(), animation, getTick());
                        // For now, treat as auto attacks until we identify specific attacks
                        // attackThisTick = NpcAttack.COX_VANGUARD_AUTO;
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