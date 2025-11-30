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

package io.blert.challenges.chambers.rooms.icedemon;

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
import java.util.Optional;

/**
 * Tracks Ice Demon room events, spawns, HP changes, and attacks with full lifecycle management.
 * Uses NPC health ratio/scale for HP tracking instead of varbits.
 * 
 * TODO: Add Ice Demon-specific attack animations when identified.
 */
@Slf4j
public class IceDemonDataTracker extends RoomDataTracker
{
    // private static final int ICE_DEMON_HP_VARBIT = 6100;
    // TODO: update when you finalize Ice Demon animations from logging
    private static final int ICE_DEMON_FREEZE_ANIMATION = 7596; // placeholder - needs verification
    // private static final int ICE_DEMON_STOMP_ANIMATION = ?;
    // private static final int ICE_DEMON_AUTO_ANIMATION = ?;

    private @Nullable BasicTrackedNpc iceDemon;
    private @Nullable NpcAttack attackThisTick = null;

    public IceDemonDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[IceDemonDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();

        // Update HP using the direct NPC reference but log more details like the script
        final var currentIceDemon = iceDemon; // Capture for null safety
        if (currentIceDemon != null)
        {
            NPC npc = currentIceDemon.getNpc();
            int ratio = npc.getHealthRatio();
            int scale = npc.getHealthScale();
            
            // Use script's exact condition check
            if (ratio > -1 && scale > 0)
            {
                // double hpPercent = (ratio * 100.0) / scale;
                int updatedHitpoints = (int) (currentIceDemon.getHitpoints().getBase() * (ratio / (double) scale));
                int currentHitpoints = currentIceDemon.getHitpoints().getCurrent();
                
                // Only update if there's a significant change (similar to varbit logic)
                if (Math.abs(currentHitpoints - updatedHitpoints) > 0)
                {
                    Hitpoints newHitpoints = currentIceDemon.getHitpoints().update(updatedHitpoints);
                    currentIceDemon.setHitpoints(newHitpoints);
                    log.info(
                        "[Ice Demon HP] Damaged: {} -> {} (-{}) at tick {}/{}", 
                        currentHitpoints,
                        updatedHitpoints, 
                        Math.abs(currentHitpoints - updatedHitpoints), 
                        tick,
                        getStartTick() + tick
                    );
                }
            }
        }

        if (attackThisTick != null)
        {
            if (currentIceDemon != null)
            {
                dispatchEvent(new NpcAttackEvent(
                    getStage(),
                    tick,
                    getWorldLocation(currentIceDemon.getNpc()),
                    attackThisTick,
                    currentIceDemon
                ));
            }
        }

        attackThisTick = null;
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned spawned)
    {
        // Only track NPCs if this room tracker is still active - check this FIRST
        if (terminating()) {
            log.debug("[Ice Demon] Ignoring NPC spawn {} - room tracker is terminating", spawned.getNpc().getId());
            return Optional.empty();
        }
        
        NPC npc = spawned.getNpc();
        
        // Log all NPC spawns in Ice Demon room for debugging
        // log.info("[Ice Demon Room] NPC spawned: id={}, name='{}'", npc.getId(), npc.getName());
        
        // Check if this NPC ID corresponds to Ice Demon
        Optional<CoxNpc> coxNpcOpt = CoxNpc.withId(npc.getId());
        if (coxNpcOpt.isPresent()) {
            CoxNpc coxNpc = coxNpcOpt.get();
            log.info("[Ice Demon Room] Found CoxNpc enum: {} for NPC id {}", coxNpc, npc.getId());
            
            if ((coxNpc == CoxNpc.ICE_DEMON_FROZEN || coxNpc == CoxNpc.ICE_DEMON_THAWED) && iceDemon == null) {
                CoxChallenge coxChallenge = (CoxChallenge) getChallenge();
                iceDemon = new BasicTrackedNpc(
                    npc,
                    coxNpc,
                    generateRoomId(npc),
                    new Hitpoints(coxNpc.getBaseHitpoints())
                );
                String modeStatus = coxChallenge.isChallengeMode() ? " [Challenge Mode]" : " [Normal Mode]";
                log.info(
                    "✓ Ice Demon tracked instance created: id={}, enum={}, base HP {} (scale={}){}",
                    npc.getId(),
                    coxNpc,
                    iceDemon.getHitpoints().getBase(),
                    getChallenge().getScale(),
                    modeStatus
                );
                return Optional.of(iceDemon);
            }
        }
        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    {
        NPC npc = despawned.getNpc();
        if (iceDemon != null && npc == iceDemon.getNpc())
        {
            log.info("[Ice Demon] Despawned NPC id={} tick={}", npc.getId(), getTick());
            iceDemon = null;
            return true;
        }
        return false;
    }

    @Override
    protected void onAnimation(AnimationChanged event)
    {
        Actor actor = event.getActor();
        final var currentIceDemon = iceDemon; // Capture for null safety
        if (currentIceDemon == null || actor != currentIceDemon.getNpc())
        {
            return;
        }

        switch (actor.getAnimation())
        {
            case ICE_DEMON_FREEZE_ANIMATION:
                attackThisTick = NpcAttack.COX_ICE_DEMON_FREEZE;
                break;
            // case ICE_DEMON_STOMP_ANIMATION:
            //     attackThisTick = NpcAttack.COX_ICE_DEMON_STOMP;
            //     break;
            // case ICE_DEMON_AUTO_ANIMATION:
            //     attackThisTick = NpcAttack.COX_ICE_DEMON_AUTO;
            //     break;
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
            final var currentIceDemon = iceDemon; // Capture for null safety
            if (currentIceDemon != null && event.getActor() == currentIceDemon.getNpc())
            {
                setHealTick(getTick());
                log.info("[Ice Demon HP] Heal hitsplat detected at tick {}", getHealTick());
            }
        }
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        // No longer using varbits for Ice Demon HP - using health ratio/scale instead
    }
}
