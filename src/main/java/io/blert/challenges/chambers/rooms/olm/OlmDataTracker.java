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

package io.blert.challenges.chambers.rooms.olm;

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
public class OlmDataTracker extends RoomDataTracker
{
    // Olm share the same HP varbit, but we can track which one was last damaged
    private static final int OLM_HP_VARBIT = 6099;
    
    // TODO: update when you finalize Olm animations from logging
    private static final int OLM_ANVIL_ANIMATION = 7475;
    private static final int OLM_STOMP_ANIMATION = 7491;
    private static final int OLM_AUTO_ANIMATION = 7492;
    // Additional discovered animations
    private static final int OLM_UNKNOWN_7483 = 7483;
    private static final int OLM_UNKNOWN_7487 = 7487;
    private static final int OLM_UNKNOWN_7488 = 7488;
    private static final int OLM_UNKNOWN_7493 = 7493;
    private static final int OLM_UNKNOWN_7494 = 7494;
    private static final int OLM_UNKNOWN_7481 = 7481;
    private static final int OLM_UNKNOWN_7482 = 7482;
    private static final int OLM_UNKNOWN_7484 = 7484;

    private @Nullable BasicTrackedNpc olmHead = null;
    private @Nullable BasicTrackedNpc olmMeleeHand = null;
    private @Nullable BasicTrackedNpc olmMageHand = null;
    
    private @Nullable NpcAttack attackThisTick = null;
    private @Nullable BasicTrackedNpc attackingOlm = null;
    
    // Track which olm was last damaged to attribute varbit changes
    private @Nullable BasicTrackedNpc lastDamagedOlm = null;
    private int lastVarbitValue = -1;
    private int lastHitsplatTick = -1;
    private boolean olmDeath = false;

    public OlmDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[OlmDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();

        // Check varbit for HP changes and attribute to last damaged olm if recent
        int currentVarbitValue = client.getVarbitValue(OLM_HP_VARBIT);
        if (lastVarbitValue != -1 && currentVarbitValue != lastVarbitValue)
        {
            // If we have a recently damaged olm, attribute the change to them
            if (lastDamagedOlm != null && (tick - lastHitsplatTick) <= 3)
            {
                lastDamagedOlm.setHitpoints(lastDamagedOlm.getHitpoints().update(currentVarbitValue));
                
                log.info(
                    "[Olm {}] HP updated via varbit: {} -> {} ({}{}) at tick {}/{}",
                    lastDamagedOlm.getNpc().getId(),
                    lastVarbitValue,
                    currentVarbitValue,
                    currentVarbitValue > lastVarbitValue ? "+" : "",
                    currentVarbitValue - lastVarbitValue,
                    tick,
                    getStartTick() + tick
                );
                // Detect Olm head death (HP reaches 0)
                Optional<CoxNpc> olmType = CoxNpc.withId(lastDamagedOlm.getNpc().getId());
                if (olmType.isPresent() && olmType.get() == CoxNpc.OLM_HEAD && currentVarbitValue == 0 && !olmDeath)
                {
                    olmDeath = true;
                    log.info("[Olm {}] Detected death at tick {}/{}, expected raid end {}", 
                                lastDamagedOlm.getNpc().getId(), tick, tick + getStartTick(), getStartTick() + tick + 4);
                }
            }
            else
            {
                log.debug(
                    "[Olm] Varbit changed {} -> {} but no recent damage target (last damage {} ticks ago)",
                    lastVarbitValue,
                    currentVarbitValue,
                    lastDamagedOlm != null ? (tick - lastHitsplatTick) : "never"
                );
            }
        }
        lastVarbitValue = currentVarbitValue;

        // Process any attacks
        if (attackThisTick != null && attackingOlm != null)
        {
            dispatchEvent(new NpcAttackEvent(
                getStage(),
                tick,
                getWorldLocation(attackingOlm.getNpc()),
                attackThisTick,
                attackingOlm
            ));
        }

        attackThisTick = null;
        attackingOlm = null;
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned spawned)
    {
        NPC npc = spawned.getNpc();
        // String npcName = npc.getName();
        
        // Debug: Log all NPC spawns in this room to help identify Olm
        // log.info("[Olm] NPC spawned: id={}, name='{}'", npc.getId(), npcName);
        
        // First try to match by CoxNpc enum
        Optional<CoxNpc> coxNpcOpt = CoxNpc.withId(npc.getId());
        if (coxNpcOpt.isPresent())
        {
            CoxNpc coxNpc = coxNpcOpt.get();
            // Match any of the Olm types (including initial spawn)
            if (coxNpc == CoxNpc.OLM_MELEE_HAND || 
                coxNpc == CoxNpc.OLM_MAGE_HAND || coxNpc == CoxNpc.OLM_HEAD)
            {
                log.info("[Olm] Detected Olm spawn: id={}, type={}", npc.getId(), coxNpc);
                return createOlmTracker(npc, coxNpc);
            }
        }
        
        // Fallback: Try to detect Olm by name if ID lookup failed
        // if (npcName != null && (npcName.toLowerCase().contains("head") ||
        //                         npcName.toLowerCase().contains("left") ||
        //                         npcName.toLowerCase().contains("right")))
        // {
        //     log.warn("[Olm] Detected Olm by name: id={}, name='{}' (not in CoxNpc enum - please add this ID)", npc.getId(), npcName);
            
        //     // Determine type from name for fallback tracking
        //     CoxNpc fallbackType = CoxNpc.OLM_HEAD; // Default
        //     if (npcName.toLowerCase().contains("left"))
        //     {
        //         fallbackType = CoxNpc.OLM_MELEE_HAND;
        //     }
        //     else if (npcName.toLowerCase().contains("right"))
        //     {
        //         fallbackType = CoxNpc.OLM_MAGE_HAND;
        //     }
            
        //     return createOlmTracker(npc, fallbackType);
        // }
        
        return Optional.empty();
    }
    
    private Optional<BasicTrackedNpc> createOlmTracker(NPC npc, CoxNpc coxNpc)
    {
        CoxChallenge coxChallenge = (CoxChallenge) getChallenge();
        String modeStatus = coxChallenge.isChallengeMode() ? " [Challenge Mode]" : " [Normal Mode]";
        
        // Get existing tracker and roomId if available (reuse roomId like Verzik does)
        BasicTrackedNpc existingOlm = getOlmTracker(coxNpc);
        long roomId = existingOlm != null ? existingOlm.getRoomId() : generateRoomId(npc);
        
        BasicTrackedNpc newOlm = new BasicTrackedNpc(
            npc,
            coxNpc,
            roomId,
            new Hitpoints(coxNpc.getBaseHitpoints())
        );
        
        // Set the tracker based on type
        switch (coxNpc)
        {
            case OLM_HEAD:
                olmHead = newOlm;
                break;
            case OLM_MELEE_HAND:
                olmMeleeHand = newOlm;
                break;
            case OLM_MAGE_HAND:
                olmMageHand = newOlm;
                break;
        }
        
        // Initialize varbit tracking on first olm spawn
        if (existingOlm == null && olmHead == null && olmMeleeHand == null && olmMageHand == null)
        {
            lastVarbitValue = client.getVarbitValue(OLM_HP_VARBIT);
            log.info("[Olm] Initialized varbit tracking with value: {}", lastVarbitValue);
        }
        
        log.info(
            "✓ Olm {} {}: id={}, HP {}{}",
            coxNpc.name(),
            existingOlm != null ? "respawned" : "tracked",
            npc.getId(),
            newOlm.getHitpoints().getBase(),
            modeStatus
        );
        
        return Optional.of(newOlm);
    }
    
    private BasicTrackedNpc getOlmTracker(CoxNpc coxNpc)
    {
        switch (coxNpc)
        {
            case OLM_HEAD:
                return olmHead;
            case OLM_MELEE_HAND:
                return olmMeleeHand;
            case OLM_MAGE_HAND:
                return olmMageHand;
            default:
                return null;
        }
    }
    


    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    {
        NPC npc = despawned.getNpc();
        
        if (olmHead != null && npc == olmHead.getNpc())
        {
            log.info("[Olm] Head despawned (id={}) at tick {}/{}",
                     npc.getId(), getTick(), getStartTick() + getTick());
            return true;
        }
        
        if (olmMeleeHand != null && npc == olmMeleeHand.getNpc())
        {
            log.info("[Olm] Melee hand despawned (id={}) at tick {}/{}",
                     npc.getId(), getTick(), getStartTick() + getTick());
            return true;
        }
        
        if (olmMageHand != null && npc == olmMageHand.getNpc())
        {
            log.info("[Olm] Mage hand despawned (id={}) at tick {}/{}",
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
        
        // Check if the actor is one of our tracked olms
        if (actor instanceof NPC)
        {
            NPC npc = (NPC) actor;
            BasicTrackedNpc olm = findOlmByNpc(npc);
            
            if (olm != null)
            {
                // log.debug("[Olm Animation] Animation: {} at tick {}", animation, getTick());
                
                switch (animation)
                {
                    case OLM_ANVIL_ANIMATION:
                        // attackThisTick = NpcAttack.COX_OLM_ANVIL;
                        // attackingOlm = olm;
                        log.info("[Olm {}] Anvil animation detected", npc.getId());
                        break;
                    case OLM_STOMP_ANIMATION:
                        // attackThisTick = NpcAttack.COX_OLM_STOMP;
                        // attackingOlm = olm;
                        // log.info("[Olm {}] Stomp animation detected", npc.getId());
                        break;
                    case OLM_AUTO_ANIMATION:
                        // attackThisTick = NpcAttack.COX_OLM_AUTO;
                        // attackingOlm = olm;
                        // log.info("[Olm {}] Auto attack animation detected", npc.getId());
                        break;
                    // Discovered animations - TODO: determine which attacks these represent
                    case OLM_UNKNOWN_7483:
                    case OLM_UNKNOWN_7487:
                    case OLM_UNKNOWN_7488:
                    case OLM_UNKNOWN_7493:
                    case OLM_UNKNOWN_7494:
                    case OLM_UNKNOWN_7481:
                    case OLM_UNKNOWN_7482:
                    case OLM_UNKNOWN_7484:
                        // log.debug("[Olm {}] Known animation: {} at tick {}", npc.getId(), animation, getTick());
                        // For now, treat as auto attacks until we identify specific attacks
                        // attackThisTick = NpcAttack.COX_OLM_AUTO;
                        break;
                    default:
                        // Only log truly unknown animations
                        if (animation != -1 && animation != 0)
                        {
                            // log.info("[Olm {}] Unknown animation: {} at tick {}", npc.getId(), animation, getTick());
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
            BasicTrackedNpc olm = findOlmByNpc(npc);
            
            if (olm != null)
            {
                Hitsplat hitsplat = event.getHitsplat();
                int hitsplatType = hitsplat.getHitsplatType();
                
                if (hitsplatType == HitsplatID.HEAL)
                {
                    setHealTick(getTick());
                    log.info("[Olm {}] Heal hitsplat detected at tick {}", npc.getId(), getHealTick());
                }
                else if (hitsplat.getAmount() > 0)
                {
                    // Track this olm as the last one damaged for varbit attribution
                    lastDamagedOlm = olm;
                    lastHitsplatTick = getTick();
                    
                    log.debug("[Olm {}] Damage hitsplat: {} (type {}) - marked for varbit attribution", 
                              npc.getId(), hitsplat.getAmount(), hitsplatType);
                }
            }
        }
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        if (event.getVarbitId() == OLM_HP_VARBIT)
        {
            int newValue = event.getValue();
            
            log.debug(
                "[Olm] HP varbit {} changed to {} at tick {} (last damaged: {})",
                event.getVarbitId(),
                newValue,
                getTick(),
                lastDamagedOlm != null ? lastDamagedOlm.getNpc().getId() : "none"
            );
            
            setShouldUpdateHitpoints(true);
        }
    }
    
    /**
     * Find an Olm tracker by its NPC reference
     */
    private BasicTrackedNpc findOlmByNpc(NPC npc)
    {
        if (olmHead != null && olmHead.getNpc() == npc)
        {
            return olmHead;
        }
        if (olmMeleeHand != null && olmMeleeHand.getNpc() == npc)
        {
            return olmMeleeHand;
        }
        if (olmMageHand != null && olmMageHand.getNpc() == npc)
        {
            return olmMageHand;
        }
        return null;
    }
}