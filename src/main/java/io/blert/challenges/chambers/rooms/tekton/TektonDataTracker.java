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
    // private static final int TEKTON_STOMP_ANIMATION = 7491;
    // private static final int TEKTON_AUTO_ANIMATION = 7492;

    private @Nullable HpVarbitTrackedNpc tekton;
    private @Nullable NpcAttack attackThisTick = null;
    private boolean shouldUpdateHitpoints;
    private int healTick = -1;

    public TektonDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();

        // The hitpoints varbit is delayed by up to 3 ticks, so don't update immediately following a heal
        if (shouldUpdateHitpoints && (healTick == -1 || tick - healTick > 3))
        {
            final var currentTekton = tekton; // Capture for null safety
            if (currentTekton != null)
            {
                int varbitValue = client.getVarbitValue(TEKTON_HP_VARBIT);
                log.info(
                    "[Tekton HP] Updating hitpoints from varbit {} = {} at tick {}",
                    TEKTON_HP_VARBIT,
                    varbitValue,
                    tick
                );

                currentTekton.updateHitpointsFromVarbit(varbitValue);
                shouldUpdateHitpoints = false;
            }
            else
            {
                log.info(
                    "[Tekton HP] Tekton instance is null when trying to update HP at tick {}",
                    tick
                );
            }
        }

        if (attackThisTick != null)
        {
            final var currentTekton = tekton; // Capture for null safety
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
                    tekton = new HpVarbitTrackedNpc(
                        npc,
                        coxNpc,
                        generateRoomId(npc),
                        new Hitpoints(coxNpc.getBaseHitpoints(getChallenge().getScale()))
                    );

                    log.info(
                        "Tekton tracked instance created with base HP {} (scale={})",
                        tekton.getHitpoints().getBase(),
                        getChallenge().getScale()
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
        if (tekton != null && npc == tekton.getNpc())
        {
            log.info("[Tekton] Despawned NPC id={} – clearing tracked instance", npc.getId());
            tekton = null;
            return true;
        }
        return false;
    }

    @Override
    protected void onAnimation(AnimationChanged event)
    {
        Actor actor = event.getActor();
        final var currentTekton = tekton; // Capture for null safety
        if (currentTekton == null || actor != currentTekton.getNpc())
        {
            return;
        }

        switch (actor.getAnimation())
        {
            case TEKTON_ANVIL_ANIMATION:
                attackThisTick = NpcAttack.COX_TEKTON_ANVIL;
                break;
            // case TEKTON_STOMP_ANIMATION:
            //     attackThisTick = NpcAttack.COX_TEKTON_STOMP;
            //     break;
            // case TEKTON_AUTO_ANIMATION:
            //     attackThisTick = NpcAttack.COX_TEKTON_AUTO;
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
            final var currentTekton = tekton; // Capture for null safety
            if (currentTekton != null && event.getActor() == currentTekton.getNpc())
            {
                healTick = getTick();
                log.info("[Tekton HP] Heal hitsplat detected at tick {}", healTick);
            }
        }
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        if (event.getVarbitId() == TEKTON_HP_VARBIT)
        {
            // Use event.getValue() instead of another client call
            log.info(
                "[Tekton HP] Varbit {} changed to {} at tick {}",
                event.getVarbitId(),
                event.getValue(),
                getTick()
            );
            shouldUpdateHitpoints = true;
        }
    }
}
