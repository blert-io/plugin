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

package io.blert.challenges.chambers.rooms.vasa;

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
public class VasaDataTracker extends RoomDataTracker
{
    // UPDATED: correct Vasa HP varbit based on dev-shell logging
    private static final int VASA_HP_VARBIT = 6099;

    // TODO: update when you finalize Vasa animations from logging
    private static final int VASA_ANVIL_ANIMATION = 7475;
    private static final int VASA_STOMP_ANIMATION = 7491;
    private static final int VASA_AUTO_ANIMATION = 7492;
    // Additional discovered animations
    private static final int VASA_UNKNOWN_7483 = 7483;
    private static final int VASA_UNKNOWN_7487 = 7487;
    private static final int VASA_UNKNOWN_7488 = 7488;
    private static final int VASA_UNKNOWN_7493 = 7493;
    private static final int VASA_UNKNOWN_7494 = 7494;
    private static final int VASA_UNKNOWN_7481 = 7481;
    private static final int VASA_UNKNOWN_7482 = 7482;
    private static final int VASA_UNKNOWN_7484 = 7484;

    private @Nullable HpVarbitTrackedNpc vasa;
    private @Nullable NpcAttack attackThisTick = null;
    private boolean vasaAtAnvil = false; // Track when Vasa is at anvil (ID 7545)

    // Track previous varbit value to detect heals
    private int previousVarbitValue = -1;

    public VasaDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[VasaDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();
        final var currentVasa = vasa; // Capture for null safety
        
        // Check if Vasa's state changed (particularly if it's now at anvil)
        if (currentVasa != null)
        {
            int currentId = currentVasa.getNpc().getId();
            boolean wasAtAnvil = vasaAtAnvil;
            vasaAtAnvil = (currentId == 7545); // Vasa at anvil
            
            // Log state changes
            if (!wasAtAnvil && vasaAtAnvil)
            {
                log.info("[Vasa] Moved to anvil (ID: 7545) - will heal");
                setHealTick(tick); // Set heal tick when arriving at anvil
            }
            else if (wasAtAnvil && !vasaAtAnvil)
            {
                log.info("[Vasa] Left anvil (ID: {}) - healing stopped", currentId);
            }
        }

        // Poll the varbit every tick, only log and update if it changed
        if (currentVasa != null)
        {
            int varbitValue = client.getVarbitValue(VASA_HP_VARBIT);
            if (previousVarbitValue != -1 && varbitValue != previousVarbitValue) {
                if (varbitValue > previousVarbitValue) {
                    // log.info("[Vasa HP] Healed: {} -> {} (+{}) at tick {}", previousVarbitValue, varbitValue, (varbitValue - previousVarbitValue), tick);
                    // setHealTick(tick);
                } else {
                    log.info("[Vasa HP] Damaged: {} -> {} (-{}) at tick {}", previousVarbitValue, varbitValue, (previousVarbitValue - varbitValue), tick);
                }
                currentVasa.updateHitpointsFromVarbit(varbitValue);
            }
            previousVarbitValue = varbitValue;
        }

        if (attackThisTick != null)
        {
            if (currentVasa != null)
            {
                dispatchEvent(new NpcAttackEvent(
                    getStage(),
                    tick,
                    getWorldLocation(currentVasa.getNpc()),
                    attackThisTick,
                    currentVasa
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
            // Only match Vasa or Vasa Enraged
            if (coxNpc == CoxNpc.VASA_NISTIRIO)
            {
                log.info("Detected Vasa NPC spawn: id={} (enum={})", npc.getId(), coxNpc);

                if (vasa == null)
                {
                    // Initialize previousVarbitValue on spawn
                    previousVarbitValue = client.getVarbitValue(VASA_HP_VARBIT);
                    HpVarbitTrackedNpc newVasa = new HpVarbitTrackedNpc(
                        npc,
                        coxNpc,
                        generateRoomId(npc),
                        new Hitpoints(coxNpc.getBaseHitpoints())
                    );
                    
                    vasa = newVasa;
                    
                    // Initialize anvil state based on spawn ID
                    vasaAtAnvil = (npc.getId() == 7545);
                    String anvilStatus = vasaAtAnvil ? " (at anvil)" : " (not at anvil)";

                    log.info(
                        "Vasa tracked instance created with base HP {} (scale={}){}", 
                        newVasa.getHitpoints().getBase(),
                        getChallenge().getScale(),
                        anvilStatus
                    );
                }

                return Optional.of(vasa);
            }

            // Handle other Vasa room NPCs (e.g., anvils) if needed
            return Optional.empty();
        });
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    {
        NPC npc = despawned.getNpc();
        
        // Capture vasa reference for comparison
        HpVarbitTrackedNpc currentVasa = vasa;
        if (currentVasa == null)
        {
            log.debug("[Vasa] Vasa already null, ignoring despawn of NPC id={}", npc.getId());
            return false;
        }
        
        // Only log and cleanup if matches the despawned NPC
        if (npc == currentVasa.getNpc())
        {
            // Clear vasa immediately to prevent duplicate processing
            vasa = null;
            previousVarbitValue = -1;
            
            log.info("[Vasa] Despawned NPC id={}, at tick {} – clearing instance", npc.getId(), getTick());
            log.info("[Vasa] RoomEnd: {}", getTick() + 5);
            
            return true;
        }
        return false;
    }

    @Override
    protected void onAnimation(AnimationChanged event)
    {
        Actor actor = event.getActor();
        int animation = actor.getAnimation();
        
        // Log all animations for Vasa for debugging
        final var currentVasa = vasa; // Capture for null safety
        if (currentVasa != null && actor == currentVasa.getNpc())
        {
            // log.debug("[Vasa Animation] Animation: {} at tick {}", animation, getTick());
            
            switch (animation)
            {
                case VASA_ANVIL_ANIMATION:
                    // attackThisTick = NpcAttack.COX_VASA_ANVIL;
                    log.info("[Vasa] Anvil animation detected");
                    // Note: Healing is now detected by NPC ID 7545, not animation
                    break;
                case VASA_STOMP_ANIMATION:
                    // attackThisTick = NpcAttack.COX_VASA_STOMP;
                    // log.info("[Vasa] Stomp animation detected");
                    break;
                case VASA_AUTO_ANIMATION:
                    // attackThisTick = NpcAttack.COX_VASA_AUTO;
                    // log.info("[Vasa] Auto attack animation detected");
                    break;
                // Discovered animations - TODO: determine which attacks these represent
                case VASA_UNKNOWN_7483:
                case VASA_UNKNOWN_7487:
                case VASA_UNKNOWN_7488:
                case VASA_UNKNOWN_7493:
                case VASA_UNKNOWN_7494:
                case VASA_UNKNOWN_7481:
                case VASA_UNKNOWN_7482:
                case VASA_UNKNOWN_7484:
                    // log.debug("[Vasa] Known animation: {} at tick {}", animation, getTick());
                    // For now, treat as auto attacks until we identify specific attacks
                    // attackThisTick = NpcAttack.COX_VASA_AUTO;
                    break;
                default:
                    // Only log truly unknown animations
                    if (animation != -1 && animation != 0)
                    {
                        // log.info("[Vasa] Unknown animation: {} at tick {}", animation, getTick());
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
        final var currentVasa = vasa; // Capture for null safety
        if (currentVasa != null && event.getActor() == currentVasa.getNpc())
        {
            Hitsplat hitsplat = event.getHitsplat();
            int hitsplatType = hitsplat.getHitsplatType();
            int amount = hitsplat.getAmount();

            // Type 6 is the confirmed heal hitsplat type for Vasa
            if (hitsplatType == 6)
            {
                log.info("[Vasa HP] Healed: {} -> {} (+{}) at tick {}", previousVarbitValue, previousVarbitValue + amount, (amount), getTick());
                int newHp = previousVarbitValue + amount;
                currentVasa.setHitpoints(currentVasa.getHitpoints().update(newHp));
                previousVarbitValue = newHp;
            }
        }
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        if (event.getVarbitId() == VASA_HP_VARBIT)
        {
            int newValue = event.getValue();
            
            // Use event.getValue() instead of another client call
            log.debug(
                "[Vasa HP] Varbit {} changed to {} at tick {}",
                event.getVarbitId(),
                newValue,
                getTick()
            );
            setShouldUpdateHitpoints(true);
        }
    }
}