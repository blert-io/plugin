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

package io.blert.challenges.chambers.rooms.muttadiles;

import io.blert.challenges.chambers.CoxNpc;
import io.blert.challenges.chambers.HpVarbitTrackedNpc;
import io.blert.challenges.chambers.RoomDataTracker;
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
import java.util.Optional;

@Slf4j
public class MuttadilesDataTracker extends RoomDataTracker
{
    // UPDATED: correct Muttadile HP varbit based on dev-shell logging
    private static final int MUTTADILE_HP_VARBIT = 6099;

    // TODO: update when you finalize Muttadile animations from logging
    private static final int MUTTADILE_ANVIL_ANIMATION = 7475;
    private static final int MUTTADILE_STOMP_ANIMATION = 7491;
    private static final int MUTTADILE_AUTO_ANIMATION = 7492;
    // Additional discovered animations
    private static final int MUTTADILE_UNKNOWN_7483 = 7483;
    private static final int MUTTADILE_UNKNOWN_7487 = 7487;
    private static final int MUTTADILE_UNKNOWN_7488 = 7488;
    private static final int MUTTADILE_UNKNOWN_7493 = 7493;
    private static final int MUTTADILE_UNKNOWN_7494 = 7494;
    private static final int MUTTADILE_UNKNOWN_7481 = 7481;
    private static final int MUTTADILE_UNKNOWN_7482 = 7482;
    private static final int MUTTADILE_UNKNOWN_7484 = 7484;

    private @Nullable HpVarbitTrackedNpc smallMuttadile;
    private @Nullable HpVarbitTrackedNpc largeMuttadile;
    private @Nullable NpcAttack attackThisTick = null;
    private boolean muttadileAtAnvil = false; // Track when Muttadile is at anvil (ID 7545)

    // Track previous varbit value to detect heals
    private int previousVarbitValue = -1;
    // Track which muttadile the varbit currently represents
    private @Nullable HpVarbitTrackedNpc activeVarbitMuttadile = null;

    public MuttadilesDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[MuttadilesDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();
        
        // Determine which muttadile is currently being tracked by the varbit
        // Small muttadile gets priority while alive, then switch to large
        HpVarbitTrackedNpc currentMuttadile = smallMuttadile != null ? smallMuttadile : largeMuttadile;
        
        // Check if we need to switch varbit tracking
        if (currentMuttadile != activeVarbitMuttadile)
        {
            if (currentMuttadile != null)
            {
                log.info("[Muttadile] Switching varbit tracking to {} (id={}) at tick {}", 
                         currentMuttadile == smallMuttadile ? "SMALL" : "LARGE", 
                         currentMuttadile.getNpc().getId(), tick);
                activeVarbitMuttadile = currentMuttadile;
                previousVarbitValue = client.getVarbitValue(MUTTADILE_HP_VARBIT);
            }
        }
        
        // Check if Muttadile's state changed (particularly if it's now at anvil)
        if (currentMuttadile != null)
        {
            int currentId = currentMuttadile.getNpc().getId();
            boolean wasAtAnvil = muttadileAtAnvil;
            muttadileAtAnvil = (currentId == 7545); // Muttadile at anvil
            
            // Log state changes
            if (!wasAtAnvil && muttadileAtAnvil)
            {
                log.info("[Muttadile] Moved to anvil (ID: 7545) - will heal");
                setHealTick(tick); // Set heal tick when arriving at anvil
            }
            else if (wasAtAnvil && !muttadileAtAnvil)
            {
                log.info("[Muttadile] Left anvil (ID: {}) - healing stopped", currentId);
            }
        }

        // Poll the varbit every tick, only log and update if it changed
        if (currentMuttadile != null && currentMuttadile == activeVarbitMuttadile)
        {
            int varbitValue = client.getVarbitValue(MUTTADILE_HP_VARBIT);
            if (previousVarbitValue != -1 && varbitValue != previousVarbitValue) {
                String muttType = currentMuttadile == smallMuttadile ? "Small" : "Large";
                if (varbitValue > previousVarbitValue) {
                    log.info("[{} Muttadile HP] Healed: {} -> {} (+{}) at tick {}/{}", muttType, previousVarbitValue, varbitValue, (varbitValue - previousVarbitValue), tick, getStartTick() + tick);
                    // setHealTick(tick);
                } else {
                    log.info("[{} Muttadile HP] Damaged: {} -> {} (-{}) at tick {}/{}", muttType, previousVarbitValue, varbitValue, (previousVarbitValue - varbitValue), tick, getStartTick() + tick);
                    currentMuttadile.updateHitpointsFromVarbit(varbitValue);
                }
            }
            previousVarbitValue = varbitValue;
        }

        if (attackThisTick != null)
        {
            if (currentMuttadile != null)
            {
                dispatchEvent(new NpcAttackEvent(
                    getStage(),
                    tick,
                    getWorldLocation(currentMuttadile.getNpc()),
                    attackThisTick,
                    currentMuttadile
                ));
            }
        }

        attackThisTick = null;
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned spawned)
    {
        NPC npc = spawned.getNpc();
        
        return CoxNpc.withId(npc.getId()).flatMap(coxNpc ->
        {
            // Match both small and large muttadiles
            if (coxNpc == CoxNpc.MUTTADILE_SMALL)
            {
                log.info("Detected Small Muttadile NPC spawn: id={} (enum={}) tick={}", npc.getId(), coxNpc, getTick());

                if (smallMuttadile == null)
                {
                    // Initialize varbit tracking for small muttadile
                    if (activeVarbitMuttadile == null)
                    {
                        previousVarbitValue = client.getVarbitValue(MUTTADILE_HP_VARBIT);
                    }
                    
                    HpVarbitTrackedNpc newSmallMuttadile = new HpVarbitTrackedNpc(
                        npc,
                        coxNpc,
                        generateRoomId(npc),
                        new Hitpoints(coxNpc.getBaseHitpoints())
                    );
                    
                    smallMuttadile = newSmallMuttadile;
                    
                    log.info(
                        "Small Muttadile id={} tracked with base HP {} (scale={}) at tick {}", 
                        npc.getId(),
                        newSmallMuttadile.getHitpoints().getBase(),
                        getChallenge().getScale(),
                        getTick()
                    );
                    
                    return Optional.of(newSmallMuttadile);
                }
            }
            else if (coxNpc == CoxNpc.MUTTADILE_LARGE)
            {
                log.info("Detected Large Muttadile NPC spawn: id={} (enum={}) tick={}", npc.getId(), coxNpc, getTick());

                // Handle ID changes for large muttadile (7561 -> 7563)
                if (largeMuttadile != null)
                {
                    NPC oldNpc = largeMuttadile.getNpc();
                    if (oldNpc.getId() != npc.getId())
                    {
                        log.info(
                            "Large Muttadile ID changed from {} to {} at tick {} - updating tracker",
                            oldNpc.getId(),
                            npc.getId(),
                            getTick()
                        );
                        // Update the tracked NPC reference to the new one
                        largeMuttadile = new HpVarbitTrackedNpc(
                            npc,
                            coxNpc,
                            largeMuttadile.getRoomId(), // Keep same room ID
                            largeMuttadile.getHitpoints() // Keep current HP
                        );
                        return Optional.of(largeMuttadile);
                    }
                }
                else
                {
                    HpVarbitTrackedNpc newLargeMuttadile = new HpVarbitTrackedNpc(
                        npc,
                        coxNpc,
                        generateRoomId(npc),
                        new Hitpoints(coxNpc.getBaseHitpoints())
                    );
                    
                    largeMuttadile = newLargeMuttadile;
                    
                    log.info(
                        "Large Muttadile id={} tracked with base HP {} (scale={}) at tick {}", 
                        npc.getId(),
                        newLargeMuttadile.getHitpoints().getBase(),
                        getChallenge().getScale(),
                        getTick()
                    );
                    
                    return Optional.of(newLargeMuttadile);
                }
            }

            return Optional.empty();
        });
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    {
        NPC npc = despawned.getNpc();
        
        // Check if it's the small muttadile despawning
        if (smallMuttadile != null && npc == smallMuttadile.getNpc())
        {
            log.info("[Small Muttadile] Despawned NPC id={}, at tick {}", npc.getId(), getTick());
            smallMuttadile = null;
            
            // If this was the active varbit tracker, clear it so large muttadile can take over
            if (activeVarbitMuttadile == smallMuttadile)
            {
                activeVarbitMuttadile = null;
                log.info("[Muttadile] Small muttadile was varbit tracker - will switch to large if available");
            }
            
            int tick_cycle_lake = (4 - ((getStartTick() + getTick()) % 4)) % 4;
            log.info("[Small Muttadile] 4 tick cycle offset: {}, Anim Tick: {}, MuttaLake: {}/{}", tick_cycle_lake, getTick() + tick_cycle_lake, getTick() + tick_cycle_lake + 5, getStartTick() + getTick() + tick_cycle_lake + 5);
            
            return true;
        }
        
        // Check if it's the large muttadile despawning
        if (largeMuttadile != null && npc == largeMuttadile.getNpc())
        {
            log.info("[Large Muttadile] Despawned NPC id={}, at tick {}/{}", npc.getId(), getTick(), getStartTick() + getTick());
            largeMuttadile = null;
            
            // If this was the active varbit tracker, clear it
            if (activeVarbitMuttadile == largeMuttadile)
            {
                activeVarbitMuttadile = null;
                previousVarbitValue = -1;
            }
            
            int tick_cycle = (4 - ((getStartTick() + getTick()) % 4)) % 4;
            log.info("[Large Muttadile] 4 tick cycle offset: {}, Anim Tick: {}, RoomEnd: {}/{}", tick_cycle, getTick() + tick_cycle, getTick() + tick_cycle + 4, getStartTick() + getTick() + tick_cycle + 4);
            
            return true;
        }
        
        return false;
    }

    @Override
    protected void onAnimation(AnimationChanged event)
    {
        Actor actor = event.getActor();
        int animation = actor.getAnimation();
        
        // Check if it's one of our tracked muttadiles
        HpVarbitTrackedNpc currentMuttadile = null;
        String muttType = null;
        
        if (smallMuttadile != null && actor == smallMuttadile.getNpc())
        {
            currentMuttadile = smallMuttadile;
            muttType = "Small";
        }
        else if (largeMuttadile != null && actor == largeMuttadile.getNpc())
        {
            currentMuttadile = largeMuttadile;
            muttType = "Large";
        }
        
        if (currentMuttadile != null)
        {
            // log.debug("[{} Muttadile Animation] Animation: {} at tick {}", muttType, animation, getTick());
            
            switch (animation)
            {
                case MUTTADILE_ANVIL_ANIMATION:
                    // attackThisTick = NpcAttack.COX_MUTTADILE_ANVIL;
                    log.info("[{} Muttadile] Anvil animation detected", muttType);
                    break;
                case MUTTADILE_STOMP_ANIMATION:
                case MUTTADILE_AUTO_ANIMATION:
                case MUTTADILE_UNKNOWN_7483:
                case MUTTADILE_UNKNOWN_7487:
                case MUTTADILE_UNKNOWN_7488:
                case MUTTADILE_UNKNOWN_7493:
                case MUTTADILE_UNKNOWN_7494:
                case MUTTADILE_UNKNOWN_7481:
                case MUTTADILE_UNKNOWN_7482:
                case MUTTADILE_UNKNOWN_7484:
                    // log.debug("[{} Muttadile] Known animation: {} at tick {}", muttType, animation, getTick());
                    break;
                default:
                    // Only log truly unknown animations
                    if (animation != -1 && animation != 0)
                    {
                        // log.info("[{} Muttadile] Unknown animation: {} at tick {}", muttType, animation, getTick());
                    }
                    break;
            }
        }
        
        // Also log animations for any NPCs with "anvil" in the name
        if (actor instanceof NPC)
        {
            NPC npc = (NPC) actor;
            if (npc.getName() != null && npc.getName().toLowerCase().contains("anvil"))
            {
                log.info("[Anvil Animation] NPC: {}, Animation: {} at tick {}", npc.getName(), animation, getTick());
            }
        }
    }

    @Override
    protected void onHitsplat(HitsplatApplied event)
    {
        if (!(event.getActor() instanceof NPC))
        {
            return;
        }
        
        NPC npc = (NPC) event.getActor();
        HpVarbitTrackedNpc hitMuttadile = null;
        String muttType = null;
        
        if (smallMuttadile != null && npc == smallMuttadile.getNpc())
        {
            hitMuttadile = smallMuttadile;
            muttType = "Small";
        }
        else if (largeMuttadile != null && npc == largeMuttadile.getNpc())
        {
            hitMuttadile = largeMuttadile;
            muttType = "Large";
        }
        
        if (hitMuttadile != null)
        {
            Hitsplat hitsplat = event.getHitsplat();
            int hitsplatType = hitsplat.getHitsplatType();
            int amount = hitsplat.getAmount();

            // Type 6 is the confirmed heal hitsplat type for Muttadile
            if (hitsplatType == 6)
            {
                log.info("[{} Muttadile HP] Heal hitsplat: +{} at tick {}", muttType, amount, getTick());
                setHealTick(getTick());
            }
        }
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        if (event.getVarbitId() == MUTTADILE_HP_VARBIT)
        {
            int newValue = event.getValue();
            
            // Use event.getValue() instead of another client call
            log.debug(
                "[Muttadile HP] Varbit {} changed to {} at tick {}",
                event.getVarbitId(),
                newValue,
                getTick()
            );
            setShouldUpdateHitpoints(true);
        }
    }
}