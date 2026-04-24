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

package io.blert.challenges.chambers.rooms.vespula;

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
public class VespulaDataTracker extends RoomDataTracker
{
    private static final int VESPULA_HP_VARBIT = 6099;

    private @Nullable HpVarbitTrackedNpc vespula;
    private @Nullable NpcAttack attackThisTick = null;
    private boolean vespulaAtAnvil = false; // Track when Vespula is at anvil (ID 7545)

    // Track previous varbit value to detect heals
    private int previousVarbitValue = -1;
    
    // Track player interactions
    private boolean targetedVespula = false; // Track if Vespula has been targeted
    private boolean attackedVespula = false; // Track if Vespula has been attacked
    private int lastPlayerAnimation = -1; // Track player's last animation

    public VespulaDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[VespulaDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();
        final var currentVespula = vespula; // Capture for null safety
        
        // Check player targeting
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer != null && localPlayer.getInteracting() instanceof NPC)
        {
            NPC targetedNpc = (NPC) localPlayer.getInteracting();
            
            // Check if this is Vespula and we haven't logged targeting yet
            if (currentVespula != null && targetedNpc == currentVespula.getNpc() && !targetedVespula)
            {
                targetedVespula = true;
                log.info("[Vespula Target] First time targeting Vespula id={} index={} at tick {}/{}",
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
                    
                    // Check if this is Vespula and we haven't logged attacking yet
                    if (currentVespula != null && attackedNpc == currentVespula.getNpc() && !attackedVespula)
                    {
                        attackedVespula = true;
                        log.info("[Vespula Attack] First time attacking Vespula id={} index={} with animation {} at tick {}/{}",
                            attackedNpc.getId(), attackedNpc.getIndex(), currentAnimation, tick, getStartTick() + tick);
                    }
                }
            }
            
            lastPlayerAnimation = currentAnimation;
        }
        
        // Check if Vespula's state changed (particularly if it's now at anvil)
        if (currentVespula != null)
        {
            int currentId = currentVespula.getNpc().getId();
            boolean wasAtAnvil = vespulaAtAnvil;
            vespulaAtAnvil = (currentId == 7545); // Vespula at anvil
            
            // Log state changes
            if (!wasAtAnvil && vespulaAtAnvil)
            {
                log.info("[Vespula] Moved to anvil (ID: 7545) - will heal");
                setHealTick(tick); // Set heal tick when arriving at anvil
            }
            else if (wasAtAnvil && !vespulaAtAnvil)
            {
                log.info("[Vespula] Left anvil (ID: {}) - healing stopped", currentId);
            }
        }

        // Poll the varbit every tick, only log and update if it changed
        if (currentVespula != null)
        {
            int varbitValue = client.getVarbitValue(VESPULA_HP_VARBIT);
            if (previousVarbitValue != -1 && varbitValue != previousVarbitValue) {
                if (varbitValue > previousVarbitValue) {
                    // log.info("[Vespula HP] Healed: {} -> {} (+{}) at tick {}", previousVarbitValue, varbitValue, (varbitValue - previousVarbitValue), tick);
                    // setHealTick(tick);
                } else {
                    log.info("[Vespula HP] Damaged: {} -> {} (-{}) at tick {}/{}", previousVarbitValue, varbitValue, (previousVarbitValue - varbitValue), tick, getStartTick() + tick);
                }
                currentVespula.updateHitpointsFromVarbit(varbitValue);
            }
            previousVarbitValue = varbitValue;
        }

        if (attackThisTick != null)
        {
            if (currentVespula != null)
            {
                dispatchEvent(new NpcAttackEvent(
                    getStage(),
                    tick,
                    getWorldLocation(currentVespula.getNpc()),
                    attackThisTick,
                    currentVespula
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
            // Only match Vespula or Vespula Enraged
            if (coxNpc == CoxNpc.ABYSSAL_PORTAL )
            {
                log.info("Detected Vespula NPC spawn: id={} (enum={})", npc.getId(), coxNpc);

                if (vespula == null)
                {
                    // Initialize previousVarbitValue on spawn
                    previousVarbitValue = client.getVarbitValue(VESPULA_HP_VARBIT);
                    HpVarbitTrackedNpc newVespula = new HpVarbitTrackedNpc(
                        npc,
                        coxNpc,
                        generateRoomId(npc),
                        new Hitpoints(coxNpc.getBaseHitpoints())
                    );
                    
                    vespula = newVespula;
                    
                    // Initialize anvil state based on spawn ID
                    vespulaAtAnvil = (npc.getId() == 7545);
                    targetedVespula = false; // Initialize targeting state
                    attackedVespula = false; // Initialize attacking state
                    String anvilStatus = vespulaAtAnvil ? " (at anvil)" : " (not at anvil)";

                    log.info(
                        "Vespula tracked instance created with base HP {} (scale={}){}", 
                        newVespula.getHitpoints().getBase(),
                        getChallenge().getScale(),
                        anvilStatus
                    );
                }

                return Optional.of(vespula);
            }

            // Handle other Vespula room NPCs (e.g., anvils) if needed
            return Optional.empty();
        });
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    {
        NPC npc = despawned.getNpc();
        
        // Capture vespula reference for comparison
        HpVarbitTrackedNpc currentVespula = vespula;
        
        // Only log and cleanup if matches the despawned NPC
        if (currentVespula != null && npc == currentVespula.getNpc())
        {
            // Clear vespula immediately to prevent duplicate processing
            vespula = null;
            previousVarbitValue = -1;
            targetedVespula = false; // Clean up targeting state
            attackedVespula = false; // Clean up attacking state
            
            log.info("[Vespula] Despawned NPC id={}, at tick {}/{}", npc.getId(), getTick(), getStartTick() + getTick());
            int tick_cycle = (4 - ((getStartTick() + getTick()) % 4)) % 4;
            log.info("[Vespula] 4 tick cycle offset: {}, RoomEnd: {}/{}", tick_cycle, getTick() + tick_cycle, getStartTick() + getTick() + tick_cycle);
            
            return true;
        }
        return false;
    }

    @Override
    protected void onAnimation(AnimationChanged event)
    {
        
    }

    @Override
    protected void onHitsplat(HitsplatApplied event)
    {
        final var currentVespula = vespula; // Capture for null safety
        if (currentVespula != null && event.getActor() == currentVespula.getNpc())
        {
            Hitsplat hitsplat = event.getHitsplat();
            int hitsplatType = hitsplat.getHitsplatType();
            int amount = hitsplat.getAmount();

            // Type 6 is the confirmed heal hitsplat type for Vespula
            if (hitsplatType == 6)
            {
                log.info("[Vespula HP] Healed: {} -> {} (+{}) at tick {}", previousVarbitValue, previousVarbitValue + amount, (amount), getTick());
                int newHp = previousVarbitValue + amount;
                currentVespula.setHitpoints(currentVespula.getHitpoints().update(newHp));
                previousVarbitValue = newHp;
            }
        }
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        if (event.getVarbitId() == VESPULA_HP_VARBIT)
        {
            int newValue = event.getValue();
            
            // Use event.getValue() instead of another client call
            log.debug(
                "[Vespula HP] Varbit {} changed to {} at tick {}",
                event.getVarbitId(),
                newValue,
                getTick()
            );
            setShouldUpdateHitpoints(true);
        }
    }
}