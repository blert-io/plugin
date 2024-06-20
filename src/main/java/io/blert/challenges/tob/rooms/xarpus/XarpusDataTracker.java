/*
 * Copyright (c) 2024 Alexei Frolov
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

package io.blert.challenges.tob.rooms.xarpus;

import io.blert.challenges.tob.HpVarbitTrackedNpc;
import io.blert.challenges.tob.TheatreChallenge;
import io.blert.challenges.tob.TobNpc;
import io.blert.challenges.tob.rooms.Room;
import io.blert.challenges.tob.rooms.RoomDataTracker;
import io.blert.core.ChallengeMode;
import io.blert.core.Hitpoints;
import io.blert.core.NpcAttack;
import io.blert.core.TrackedNpc;
import io.blert.events.NpcAttackEvent;
import io.blert.events.tob.XarpusPhaseEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;

import javax.annotation.Nullable;
import java.util.Optional;

@Slf4j
public class XarpusDataTracker extends RoomDataTracker {
    private static final int FIRST_P2_TURN_TICK = 7;
    private static final int TICKS_PER_TURN_P2 = 4;
    private static final int TICKS_PER_TURN_P3 = 8;

    private XarpusPhase phase;
    private @Nullable HpVarbitTrackedNpc xarpus = null;

    private int nextTurnTick = -1;

    public XarpusDataTracker(TheatreChallenge manager, Client client) {
        super(manager, client, Room.XARPUS);
    }

    @Override
    protected void onRoomStart() {
        if (xarpus == null) {
            client.getNpcs().stream().filter(npc -> TobNpc.isAnyXarpus(npc.getId())).findFirst().ifPresent(npc -> {
                TobNpc tobNpc = TobNpc.withId(npc.getId()).orElseThrow();
                xarpus = new HpVarbitTrackedNpc(npc, tobNpc, generateRoomId(npc),
                        new Hitpoints(tobNpc, theatreChallenge.getScale()));
                addTrackedNpc(xarpus);
            });
        }

        phase = XarpusPhase.P1;
    }

    @Override
    protected void onTick() {
        super.onTick();
        final int tick = getTick();

        if (xarpus != null && xarpus.getNpc().getOverheadText() != null && phase != XarpusPhase.P3) {
            phase = XarpusPhase.P3;
            nextTurnTick = tick + TICKS_PER_TURN_P3;
            dispatchEvent(new XarpusPhaseEvent(tick, getWorldLocation(xarpus.getNpc()), phase));
            log.debug("Screech: {} ({})", tick, formattedRoomTime());
        }

        if (tick == nextTurnTick && xarpus != null) {
            WorldPoint point = getWorldLocation(xarpus);
            if (phase == XarpusPhase.P2) {
                nextTurnTick += TICKS_PER_TURN_P2;
                dispatchEvent(new NpcAttackEvent(getStage(), tick, point, NpcAttack.TOB_XARPUS_SPIT, xarpus));
            } else if (phase == XarpusPhase.P3) {
                if (theatreChallenge.getChallengeMode() != ChallengeMode.TOB_HARD) {
                    nextTurnTick += TICKS_PER_TURN_P3;
                    dispatchEvent(new NpcAttackEvent(getStage(), tick, point, NpcAttack.TOB_XARPUS_TURN, xarpus));
                }
            }
        }
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned event) {
        NPC npc = event.getNpc();

        Optional<TobNpc> maybeXarpus = TobNpc.withId(npc.getId()).filter(TobNpc::isAnyXarpus);
        if (maybeXarpus.isPresent()) {
            if (xarpus == null) {
                xarpus = new HpVarbitTrackedNpc(npc, maybeXarpus.get(), generateRoomId(npc),
                        new Hitpoints(maybeXarpus.get(), theatreChallenge.getScale()));
            }
            return Optional.of(xarpus);
        }

        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned event, TrackedNpc trackedNpc) {
        if (trackedNpc == xarpus) {
            nextTurnTick = -1;
            return true;
        }
        return false;
    }

    @Override
    protected void onNpcChange(NpcChanged changed) {
        int idBefore = changed.getOld().getId();
        int idAfter = changed.getNpc().getId();

        if (TobNpc.isXarpusIdle(idBefore) && TobNpc.isXarpusP1(idAfter)) {
            startRoom();
            return;
        }

        final int tick = getTick();

        if (TobNpc.isXarpusP1(idBefore) && TobNpc.isXarpus(idAfter)) {
            phase = XarpusPhase.P2;
            nextTurnTick = tick + FIRST_P2_TURN_TICK;
            dispatchEvent(new XarpusPhaseEvent(tick, getWorldLocation(changed.getNpc()), phase));
            log.debug("Exhumes: {} ({})", tick, formattedRoomTime());
        }
    }
}
