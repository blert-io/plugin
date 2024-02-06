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

package com.blert.raid.rooms.nylocas;

import com.blert.events.*;
import com.blert.raid.Mode;
import com.blert.raid.RaidManager;
import com.blert.raid.TobNpc;
import com.blert.raid.Utils;
import com.blert.raid.rooms.Room;
import com.blert.raid.rooms.RoomDataTracker;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NullNpcID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.Subscribe;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j

public class NylocasDataTracker extends RoomDataTracker {
    private static final int CAP_INCREASE_WAVE = 20;
    private static final int LAST_NYLO_WAVE = 31;
    private static final int WAVE_TICK_CYCLE = 4;
    private static final int[] NATURAL_STALLS = new int[]{
            0, 4, 4, 4, 4, 16, 4, 12, 4, 12, 8, 8, 8, 8, 8, 8, 4, 12, 8, 12, 16, 8, 12, 8, 8, 8, 4, 8, 4, 4, 4,
    };

    private int currentWave;
    private int nextWaveSpawnCheckTick;
    private final int[] waveSpawnTicks = new int[LAST_NYLO_WAVE + 1];
    private int bossSpawnTick;

    private final Map<Integer, Nylo> nylosInRoom = new HashMap<>();
    private @Nullable NPC nyloPrince = null;

    private static final ImmutableSet<Integer> NYLOCAS_PILLAR_NPC_IDS = ImmutableSet.of(
            NullNpcID.NULL_10790,
            NullNpcID.NULL_8358,
            NullNpcID.NULL_10811);

    public NylocasDataTracker(RaidManager manager, Client client) {
        super(manager, client, Room.NYLOCAS);
        currentWave = 0;
        nextWaveSpawnCheckTick = -1;
        bossSpawnTick = -1;
    }

    private int roomNyloCount() {
        return nylosInRoom.size() + (nyloPrince != null ? 3 : 0);
    }

    private int waveCap() {
        if (raidManager.getRaidMode() == Mode.HARD) {
            return currentWave < CAP_INCREASE_WAVE ? 15 : 24;
        }
        return currentWave < CAP_INCREASE_WAVE ? 12 : 24;
    }

    private boolean isPrinceWave() {
        return raidManager.getRaidMode() == Mode.HARD && (currentWave == 10 || currentWave == 20 || currentWave == 30);
    }

    @Override
    protected void onRoomStart() {
    }

    @Override
    protected void onTick() {
        final int tick = getRoomTick();

        if (waveSpawnTicks[currentWave] == tick) {
            dispatchEvent(new NyloWaveSpawnEvent(tick, currentWave, roomNyloCount(), waveCap()));
        }

        if (currentWave < LAST_NYLO_WAVE && tick == nextWaveSpawnCheckTick) {
            // The spawn event handler runs before the on tick handler, so if `nextWaveSpawnCheckTick` is ever reached,
            // it means that the next wave did not spawn when expected, i.e. a stall occurred.
            nextWaveSpawnCheckTick += WAVE_TICK_CYCLE;

            log.debug("Stalled wave {} ({}/{})", currentWave, roomNyloCount(), waveCap());
            dispatchEvent(new NyloWaveStallEvent(tick, currentWave, roomNyloCount(), waveCap()));
        }

        nylosInRoom.forEach((roomId, nylo) -> {
            NPC npc = nylo.getNpc();
            WorldPoint point = WorldPoint.fromLocalInstance(client, npc.getLocalLocation());
            dispatchNpcUpdate(new NpcUpdateEvent(getRoom(), tick, point, roomId, npc.getId(), nylo.getHitpoints()));
        });
    }

    @Subscribe
    private void onNpcSpawned(NpcSpawned spawned) {
        NPC npc = spawned.getNpc();

        if (NYLOCAS_PILLAR_NPC_IDS.contains(npc.getId())) {
            startRoom();
            return;
        }

        if (TobNpc.isNylocas(npc.getId())) {
            handleNylocasSpawn(npc);
        } else if (TobNpc.isNylocasPrinkipas(npc.getId())) {
            nyloPrince = npc;
        } else if (TobNpc.isNylocasVasilias(npc.getId())) {
            handleBossSpawn(npc);
        }
    }

    @Subscribe
    private void onNpcDespawned(NpcDespawned despawned) {
        NPC npc = despawned.getNpc();
        if (TobNpc.isNylocasPrinkipas(npc.getId())) {
            nyloPrince = null;
        } else if (!TobNpc.isNylocas(npc.getId())) {
            return;
        }

        nylosInRoom.remove(npc.hashCode());

        int tick = getRoomTick();

        if (currentWave == LAST_NYLO_WAVE && nylosInRoom.isEmpty() && (nyloPrince == null || nyloPrince.isDead())) {
            dispatchEvent(new NyloCleanupEndEvent(tick));
            log.debug("Cleanup: {} ({})", tick, formattedRoomTime());
        }
    }

    @Override
    protected void onHitsplat(HitsplatApplied event) {
        Actor actor = event.getActor();
        if (!(actor instanceof NPC)) {
            return;
        }

        NPC npc = (NPC) actor;
        Nylo nylo = nylosInRoom.get(npc.hashCode());
        if (nylo != null) {
            nylo.getHitpoints().drain(event.getHitsplat().getAmount());
        }
    }

    private void handleNylocasSpawn(NPC npc) {
        Optional<TobNpc> tobNpc = TobNpc.withId(npc.getId());
        if (tobNpc.isEmpty()) {
            return;
        }

        final int tick = getRoomTick();

        WorldPoint point = WorldPoint.fromLocalInstance(client, Utils.getNpcSouthwestTile(npc));
        if (SpawnPoint.isNyloLaneSpawn(point)) {
            if (waveSpawnTicks[currentWave] != tick) {
                handleWaveSpawn(tick);
            }
        }

        Nylo nylo = new Nylo(npc, point, tick, currentWave, tobNpc.get().getBaseHitpoints(raidManager.getRaidScale()));
        nylosInRoom.put(npc.hashCode(), nylo);
    }

    private void handleWaveSpawn(int tick) {
        currentWave++;
        waveSpawnTicks[currentWave] = tick;
        if (currentWave < LAST_NYLO_WAVE) {
            if (isPrinceWave()) {
                nextWaveSpawnCheckTick = tick + 4 * WAVE_TICK_CYCLE;
            } else {
                nextWaveSpawnCheckTick = tick + NATURAL_STALLS[currentWave];
            }
        }

        if (currentWave == CAP_INCREASE_WAVE) {
            log.debug("Cap increase: {} ({})", tick, formattedRoomTime());
        } else if (currentWave == LAST_NYLO_WAVE) {
            log.debug("Waves: {} ({})", tick, formattedRoomTime());
        }
    }

    private void handleBossSpawn(NPC npc) {
        final int tick = getRoomTick();

        if (bossSpawnTick == -1) {
            bossSpawnTick = tick;
            WorldPoint spawnPoint = WorldPoint.fromLocalInstance(client, npc.getLocalLocation());
            dispatchEvent(new NyloBossSpawnEvent(tick, spawnPoint));
            log.debug("Boss: {} ({})", tick, formattedRoomTime());
        }
    }
}
