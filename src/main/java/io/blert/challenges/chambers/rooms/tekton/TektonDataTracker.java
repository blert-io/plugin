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

package io.blert.challenges.chambers.rooms.tekton;

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
public class TektonDataTracker extends RoomDataTracker
{
    // UPDATED: correct Tekton HP varbit based on dev-shell logging
    private static final int TEKTON_HP_VARBIT = 6099;

    // TODO: update when you finalize Tekton animations from logging
    private static final int TEKTON_ANVIL_ANIMATION = 7475;
    private static final int TEKTON_STOMP_ANIMATION = 7491;
    private static final int TEKTON_AUTO_ANIMATION = 7492;
    // Additional discovered animations
    private static final int TEKTON_UNKNOWN_7483 = 7483;
    private static final int TEKTON_UNKNOWN_7487 = 7487;
    private static final int TEKTON_UNKNOWN_7488 = 7488;
    private static final int TEKTON_UNKNOWN_7493 = 7493;
    private static final int TEKTON_UNKNOWN_7494 = 7494;
    private static final int TEKTON_UNKNOWN_7481 = 7481;
    private static final int TEKTON_UNKNOWN_7482 = 7482;
    private static final int TEKTON_UNKNOWN_7484 = 7484;

    private @Nullable HpVarbitTrackedNpc tekton;
    private @Nullable NpcAttack attackThisTick = null;
    private boolean tektonAtAnvil = false; // Track when Tekton is at anvil (ID 7545)

    // Track previous varbit value to detect heals
    private int previousVarbitValue = -1;

    public TektonDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[TektonDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();
        final var currentTekton = tekton; // Capture for null safety
        
        // Check if Tekton's state changed (particularly if it's now at anvil)
        if (currentTekton != null)
        {
            int currentId = currentTekton.getNpc().getId();
            boolean wasAtAnvil = tektonAtAnvil;
            tektonAtAnvil = (currentId == 7545); // Tekton at anvil
            
            // Log state changes
            if (!wasAtAnvil && tektonAtAnvil)
            {
                log.info("[Tekton] Moved to anvil (ID: 7545) - will heal");
                setHealTick(tick); // Set heal tick when arriving at anvil
            }
            else if (wasAtAnvil && !tektonAtAnvil)
            {
                log.info("[Tekton] Left anvil (ID: {}) - healing stopped", currentId);
            }
        }

        // Poll the varbit every tick, only log and update if it changed
        if (currentTekton != null)
        {
            int varbitValue = client.getVarbitValue(TEKTON_HP_VARBIT);
            if (previousVarbitValue != -1 && varbitValue != previousVarbitValue) {
                if (varbitValue > previousVarbitValue) {
                    // log.info("[Tekton HP] Healed: {} -> {} (+{}) at tick {}", previousVarbitValue, varbitValue, (varbitValue - previousVarbitValue), tick);
                    // setHealTick(tick);
                } else {
                    log.info("[Tekton HP] Damaged: {} -> {} (-{}) at tick {}/{}", previousVarbitValue, varbitValue, (previousVarbitValue - varbitValue), tick, getStartTick() + tick);
                }
                currentTekton.updateHitpointsFromVarbit(varbitValue);
            }
            previousVarbitValue = varbitValue;
        }

        if (attackThisTick != null)
        {
            if (currentTekton != null)
            {
                dispatchEvent(new NpcAttackEvent(
                    getStage(),
                    tick,
                    getWorldLocation(currentTekton.getNpc()),
                    attackThisTick,
                    currentTekton
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
            // Only match Tekton or Tekton Enraged
            if (coxNpc == CoxNpc.TEKTON || coxNpc == CoxNpc.TEKTON_ENRAGED)
            {
                log.info("Detected Tekton NPC spawn: id={} (enum={})", npc.getId(), coxNpc);

                if (tekton == null)
                {
                    // Initialize previousVarbitValue on spawn
                    previousVarbitValue = client.getVarbitValue(TEKTON_HP_VARBIT);
                    HpVarbitTrackedNpc newTekton = new HpVarbitTrackedNpc(
                        npc,
                        coxNpc,
                        generateRoomId(npc),
                        new Hitpoints(coxNpc.getBaseHitpoints())
                    );
                    
                    tekton = newTekton;
                    
                    // Initialize anvil state based on spawn ID
                    tektonAtAnvil = (npc.getId() == 7545);
                    String anvilStatus = tektonAtAnvil ? " (at anvil)" : " (not at anvil)";

                    log.info(
                        "Tekton tracked instance created with base HP {} (scale={}){}", 
                        newTekton.getHitpoints().getBase(),
                        getChallenge().getScale(),
                        anvilStatus
                    );
                }

                return Optional.of(tekton);
            }

            // Handle other Tekton room NPCs (e.g., anvils) if needed
            return Optional.empty();
        });
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    {
        NPC npc = despawned.getNpc();
        
        // Capture tekton reference for comparison
        HpVarbitTrackedNpc currentTekton = tekton;
        // Only log and cleanup if matches the despawned NPC
        if (npc == currentTekton.getNpc())
        {
            // Clear tekton immediately to prevent duplicate processing
            tekton = null;
            previousVarbitValue = -1;
            
            log.info("[Tekton] Despawned NPC id={}, at tick {}/{}", npc.getId(), getTick(), getStartTick() + getTick());
            int tick_cycle = (4 - ((getStartTick() + getTick()) % 4)) % 4;
            log.info("[Tekton] 4 tick cycle offset: {}, Anim Tick: {}, RoomEnd: {}", tick_cycle, getTick() + tick_cycle, getTick() + tick_cycle + 4);
            
            return true;
        }
        return false;
    }

    @Override
    protected void onAnimation(AnimationChanged event)
    {
        Actor actor = event.getActor();
        int animation = actor.getAnimation();
        
        // Log all animations for Tekton for debugging
        final var currentTekton = tekton; // Capture for null safety
        if (currentTekton != null && actor == currentTekton.getNpc())
        {
            // log.debug("[Tekton Animation] Animation: {} at tick {}", animation, getTick());
            
            switch (animation)
            {
                case TEKTON_ANVIL_ANIMATION:
                    attackThisTick = NpcAttack.COX_TEKTON_ANVIL;
                    log.info("[Tekton] Anvil animation detected");
                    // Note: Healing is now detected by NPC ID 7545, not animation
                    break;
                case TEKTON_STOMP_ANIMATION:
                    attackThisTick = NpcAttack.COX_TEKTON_STOMP;
                    // log.info("[Tekton] Stomp animation detected");
                    break;
                case TEKTON_AUTO_ANIMATION:
                    attackThisTick = NpcAttack.COX_TEKTON_AUTO;
                    // log.info("[Tekton] Auto attack animation detected");
                    break;
                // Discovered animations - TODO: determine which attacks these represent
                case TEKTON_UNKNOWN_7483:
                case TEKTON_UNKNOWN_7487:
                case TEKTON_UNKNOWN_7488:
                case TEKTON_UNKNOWN_7493:
                case TEKTON_UNKNOWN_7494:
                case TEKTON_UNKNOWN_7481:
                case TEKTON_UNKNOWN_7482:
                case TEKTON_UNKNOWN_7484:
                    // log.debug("[Tekton] Known animation: {} at tick {}", animation, getTick());
                    // For now, treat as auto attacks until we identify specific attacks
                    attackThisTick = NpcAttack.COX_TEKTON_AUTO;
                    break;
                default:
                    // Only log truly unknown animations
                    if (animation != -1 && animation != 0)
                    {
                        // log.info("[Tekton] Unknown animation: {} at tick {}", animation, getTick());
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
        final var currentTekton = tekton; // Capture for null safety
        if (currentTekton != null && event.getActor() == currentTekton.getNpc())
        {
            Hitsplat hitsplat = event.getHitsplat();
            int hitsplatType = hitsplat.getHitsplatType();
            int amount = hitsplat.getAmount();

            // Type 6 is the confirmed heal hitsplat type for Tekton
            if (hitsplatType == 6)
            {
                log.info("[Tekton HP] Healed: {} -> {} (+{}) at tick {}", previousVarbitValue, previousVarbitValue + amount, (amount), getTick());
                int newHp = previousVarbitValue + amount;
                currentTekton.setHitpoints(currentTekton.getHitpoints().update(newHp));
                previousVarbitValue = newHp;
            }
        }
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        if (event.getVarbitId() == TEKTON_HP_VARBIT)
        {
            int newValue = event.getValue();
            
            // Use event.getValue() instead of another client call
            log.debug(
                "[Tekton HP] Varbit {} changed to {} at tick {}",
                event.getVarbitId(),
                newValue,
                getTick()
            );
            setShouldUpdateHitpoints(true);
        }
    }
}