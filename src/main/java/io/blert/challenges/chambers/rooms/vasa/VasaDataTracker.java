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

    private static final int VASA_SPAWN_ANIMATION = 7408;
    private static final int VASA_TELEPORT_ANIMATION = 7409;
    private static final int VASA_TELEPORT_BOMB_ANIMATION = 7410;
    private static final int VASA_AT_CRYSTAL_ANIMATION = 7412;
    private static final int VASA_LEAVES_CRYSTAL_ANIMATION = 7414;

    private @Nullable HpVarbitTrackedNpc vasa;
    private @Nullable NpcAttack attackThisTick = null;
    private boolean vasaAtAnvil = false; // Track when Vasa is at anvil (ID 7545)

    // Track previous varbit value to detect heals
    private int previousVarbitValue = -1;
    
    // Track player interactions
    private boolean targetedVasa = false; // Track if Vasa has been targeted
    private boolean attackedVasa = false; // Track if Vasa has been attacked
    private int lastPlayerAnimation = -1; // Track player's last animation

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
        
        // Check player targeting
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer != null && localPlayer.getInteracting() instanceof NPC)
        {
            NPC targetedNpc = (NPC) localPlayer.getInteracting();
            
            // Check if this is Vasa and we haven't logged targeting yet
            if (currentVasa != null && targetedNpc == currentVasa.getNpc() && !targetedVasa)
            {
                targetedVasa = true;
                log.info("[Vasa Target] First time targeting Vasa id={} index={} at tick {}/{}",
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
                    
                    // Check if this is Vasa and we haven't logged attacking yet
                    if (currentVasa != null && attackedNpc == currentVasa.getNpc() && !attackedVasa)
                    {
                        attackedVasa = true;
                        log.info("[Vasa Attack] First time attacking Vasa id={} index={} with animation {} at tick {}/{}",
                            attackedNpc.getId(), attackedNpc.getIndex(), currentAnimation, tick, getStartTick() + tick);
                    }
                }
            }
            
            lastPlayerAnimation = currentAnimation;
        }
        
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
                    log.info("[Vasa HP] Damaged: {} -> {} (-{}) at tick {}/{}", previousVarbitValue, varbitValue, (previousVarbitValue - varbitValue), tick, getStartTick() + tick);
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
                    targetedVasa = false; // Initialize targeting state
                    attackedVasa = false; // Initialize attacking state
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
            targetedVasa = false; // Clean up targeting state
            attackedVasa = false; // Clean up attacking state
            log.info("[Vasa] Despawned NPC id={}, at tick {}", npc.getId(), getTick());
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
                case VASA_SPAWN_ANIMATION:
                    attackThisTick = NpcAttack.COX_VASA_SPAWN;
                    log.info("[Vasa] Spawn animation detected");
                    break;
                case VASA_TELEPORT_ANIMATION:
                    attackThisTick = NpcAttack.COX_VASA_TELEPORT;
                    log.info("[Vasa] Teleport animation detected");
                    break;
                case VASA_TELEPORT_BOMB_ANIMATION:
                    attackThisTick = NpcAttack.COX_VASA_TELEPORT_BOMBS;
                    log.info("[Vasa] Teleport bomb animation detected");
                    break;
                case VASA_AT_CRYSTAL_ANIMATION:
                    attackThisTick = NpcAttack.COX_VASA_AT_CRYSTAL;
                    log.info("[Vasa] At crystal animation detected");
                    break;
                case VASA_LEAVES_CRYSTAL_ANIMATION:
                    attackThisTick = NpcAttack.COX_VASA_LEAVES_CRYSTAL;
                    log.info("[Vasa] Leaves crystal animation detected");
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