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

    private @Nullable BasicTrackedNpc vanguardMelee = null;
    private @Nullable BasicTrackedNpc vanguardMage = null;
    private @Nullable BasicTrackedNpc vanguardRange = null;
    
    private @Nullable NpcAttack attackThisTick = null;
    private @Nullable BasicTrackedNpc attackingVanguard = null;
    
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
        if (attackThisTick != null && attackingVanguard != null)
        {
            dispatchEvent(new NpcAttackEvent(
                getStage(),
                tick,
                getWorldLocation(attackingVanguard.getNpc()),
                attackThisTick,
                attackingVanguard
            ));
        }

        attackThisTick = null;
        attackingVanguard = null;
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
        CoxChallenge coxChallenge = (CoxChallenge) getChallenge();
        String modeStatus = coxChallenge.isChallengeMode() ? " [Challenge Mode]" : " [Normal Mode]";
        
        // Get existing tracker and roomId if available (reuse roomId like Verzik/Olm does)
        BasicTrackedNpc existingVanguard = getVanguardTracker(coxNpc);
        long roomId = existingVanguard != null ? existingVanguard.getRoomId() : generateRoomId(npc);
        
        BasicTrackedNpc newVanguard = new BasicTrackedNpc(
            npc,
            coxNpc,
            roomId,
            new Hitpoints(coxNpc.getBaseHitpoints())
        );
        
        // Set the tracker based on type
        switch (coxNpc)
        {
            case VANGUARD_MELEE:
                vanguardMelee = newVanguard;
                break;
            case VANGUARD_MAGIC:
                vanguardMage = newVanguard;
                break;
            case VANGUARD_RANGED:
                vanguardRange = newVanguard;
                break;
            case VANGUARD_INITIAL:
                // For initial spawn, we'll update when it transforms
                log.info("[Vanguards] Initial spawn detected, waiting for transformation");
                break;
        }
        
        // Initialize varbit tracking on first vanguard spawn
        if (existingVanguard == null && vanguardMelee == null && vanguardMage == null && vanguardRange == null)
        {
            lastVarbitValue = client.getVarbitValue(VANGUARD_HP_VARBIT);
            log.info("[Vanguards] Initialized varbit tracking with value: {}", lastVarbitValue);
        }
        
        log.info(
            "✓ Vanguard {} {}: id={}, HP {}{}",
            coxNpc.name(),
            existingVanguard != null ? "respawned" : "tracked",
            npc.getId(),
            newVanguard.getHitpoints().getBase(),
            modeStatus
        );
        
        return Optional.of(newVanguard);
    }
    
    private BasicTrackedNpc getVanguardTracker(CoxNpc coxNpc)
    {
        switch (coxNpc)
        {
            case VANGUARD_MELEE:
                return vanguardMelee;
            case VANGUARD_MAGIC:
                return vanguardMage;
            case VANGUARD_RANGED:
                return vanguardRange;
            default:
                return null;
        }
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
        
        if (vanguardMelee != null && npc == vanguardMelee.getNpc())
        {
            log.info("[Vanguards] Melee vanguard despawned (id={}) at tick {}/{}",
                     npc.getId(), getTick(), getStartTick() + getTick());
            return true;
        }
        
        if (vanguardMage != null && npc == vanguardMage.getNpc())
        {
            log.info("[Vanguards] Mage vanguard despawned (id={}) at tick {}/{}",
                     npc.getId(), getTick(), getStartTick() + getTick());
            return true;
        }
        
        if (vanguardRange != null && npc == vanguardRange.getNpc())
        {
            log.info("[Vanguards] Range vanguard despawned (id={}) at tick {}/{}",
                     npc.getId(), getTick(), getStartTick() + getTick());
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
            BasicTrackedNpc vanguard = findVanguardByNpc(npc);
            
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
            BasicTrackedNpc vanguard = findVanguardByNpc(npc);
            
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
    
    /**
     * Find a Vanguard tracker by its NPC reference
     */
    private BasicTrackedNpc findVanguardByNpc(NPC npc)
    {
        if (vanguardMelee != null && vanguardMelee.getNpc() == npc)
        {
            return vanguardMelee;
        }
        if (vanguardMage != null && vanguardMage.getNpc() == npc)
        {
            return vanguardMage;
        }
        if (vanguardRange != null && vanguardRange.getNpc() == npc)
        {
            return vanguardRange;
        }
        return null;
    }
}