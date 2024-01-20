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

import com.blert.raid.RaidManager;
import com.blert.raid.rooms.Room;
import com.blert.raid.rooms.RoomDataTracker;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NullNpcID;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.Subscribe;

@Slf4j

public class NylocasDataTracker extends RoomDataTracker {
    private static final ImmutableSet<Integer> NYLOCAS_PILLAR_NPC_IDS = ImmutableSet.of(
            NullNpcID.NULL_10790,
            NullNpcID.NULL_8358,
            NullNpcID.NULL_10811);

    public NylocasDataTracker(RaidManager manager, Client client) {
        super(manager, client, Room.NYLOCAS);
    }

    @Override
    protected void onRoomStart() {
    }

    @Override
    protected void onTick() {
    }

    @Subscribe
    private void onNpcSpawned(NpcSpawned spawned) {
        NPC npc = spawned.getNpc();

        if (NYLOCAS_PILLAR_NPC_IDS.contains(npc.getId())) {
            startRoom();
        }
    }
}
