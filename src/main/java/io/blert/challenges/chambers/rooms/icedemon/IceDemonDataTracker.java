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

/**
 * Tracks Ice Demon room events, spawns, HP changes, and attacks with full lifecycle management.
 * 
 * TODO: Verify these values through game testing:
 * - ICE_DEMON_HP_VARBIT: Currently set to 6100 (needs verification)
 * - ICE_DEMON_FREEZE_ANIMATION: Currently set to 7596 (placeholder)
 * - Ice Demon base HP: Currently set to 375 (needs verification)
 * 
 * Adapted from TektonDataTracker with Ice Demon-specific tracking.
 */
@Slf4j
public class IceDemonDataTracker extends RoomDataTracker
{
    // Ice Demon HP varbit - need to verify correct varbit ID
    private static final int ICE_DEMON_HP_VARBIT = 6100; // TODO: verify correct varbit

    // TODO: update when you finalize Ice Demon animations from logging
    private static final int ICE_DEMON_FREEZE_ANIMATION = 7596; // placeholder - needs verification
    // private static final int ICE_DEMON_STOMP_ANIMATION = ?;
    // private static final int ICE_DEMON_AUTO_ANIMATION = ?;

    private @Nullable HpVarbitTrackedNpc iceDemon;
    private @Nullable NpcAttack attackThisTick = null;

    public IceDemonDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[IceDemonDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
        log.info("[IceDemonDataTracker] Will track HP using varbit {}", ICE_DEMON_HP_VARBIT);
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();

        // The hitpoints varbit is delayed by up to 3 ticks, so don't update immediately following a heal
        if (getShouldUpdateHitpoints() && (getHealTick() == -1 || tick - getHealTick() > 3))
        {
            final var currentIceDemon = iceDemon; // Capture for null safety
            if (currentIceDemon != null)
            {
                int varbitValue = client.getVarbitValue(ICE_DEMON_HP_VARBIT);
                log.info(
                    "[Ice Demon HP] Updating hitpoints from varbit {} = {} at tick {}",
                    ICE_DEMON_HP_VARBIT,
                    varbitValue,
                    tick
                );

                currentIceDemon.updateHitpointsFromVarbit(varbitValue);
                setShouldUpdateHitpoints(false);
            }
            else
            {
                log.info(
                    "[Ice Demon HP] Ice Demon instance is null when trying to update HP at tick {}",
                    tick
                );
            }
        }

        if (attackThisTick != null)
        {
            final var currentIceDemon = iceDemon; // Capture for null safety
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
        NPC npc = spawned.getNpc();
        
        // Log all NPC spawns in Ice Demon room for debugging
        log.info("[Ice Demon Room] NPC spawned: id={}, name='{}'", npc.getId(), npc.getName());
        
        return CoxNpc.withId(npc.getId()).flatMap(coxNpc ->
        {
            // Only match Ice Demon
            if (coxNpc == CoxNpc.ICE_DEMON)
            {
                log.info("Detected Ice Demon NPC spawn: id={} (enum={})", npc.getId(), coxNpc);

                if (iceDemon == null)
                {
                    iceDemon = new HpVarbitTrackedNpc(
                        npc,
                        coxNpc,
                        generateRoomId(npc),
                        new Hitpoints(coxNpc.getBaseHitpoints(getChallenge().getScale()))
                    );

                    log.info(
                        "Ice Demon tracked instance created with base HP {} (scale={})",
                        iceDemon.getHitpoints().getBase(),
                        getChallenge().getScale()
                    );
                }

                return Optional.of(iceDemon);
            }

            // Handle other Ice Demon room NPCs if needed
            return Optional.empty();
        });
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    {
        NPC npc = despawned.getNpc();
        if (iceDemon != null && npc == iceDemon.getNpc())
        {
            log.info("[Ice Demon] Despawned NPC id={} – clearing tracked instance", npc.getId());
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
        if (event.getVarbitId() == ICE_DEMON_HP_VARBIT)
        {
            // Use event.getValue() instead of another client call
            log.info(
                "[Ice Demon HP] Varbit {} changed to {} at tick {}",
                event.getVarbitId(),
                event.getValue(),
                getTick()
            );
            setShouldUpdateHitpoints(true);
        }
    }
}
