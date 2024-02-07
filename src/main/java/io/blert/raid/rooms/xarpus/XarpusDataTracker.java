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

package io.blert.raid.rooms.xarpus;

import io.blert.events.XarpusPhaseEvent;
import io.blert.raid.RaidManager;
import io.blert.raid.TobNpc;
import io.blert.raid.rooms.Room;
import io.blert.raid.rooms.RoomDataTracker;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.NpcChanged;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
public class XarpusDataTracker extends RoomDataTracker {

    private XarpusPhase phase;
    private NPC xarpus = null;

    public XarpusDataTracker(RaidManager manager, Client client) {
        super(manager, client, Room.XARPUS);
    }

    @Override
    protected void onRoomStart() {
        phase = XarpusPhase.P1;
    }

    @Override
    protected void onTick() {
        final int tick = getRoomTick();

        if (xarpus != null && xarpus.getOverheadText() != null && phase != XarpusPhase.P3) {
            phase = XarpusPhase.P3;
            dispatchEvent(new XarpusPhaseEvent(tick, getWorldLocation(xarpus), phase));
            log.debug("Screech: {} ({})", tick, formattedRoomTime());
        }

    }

    @Subscribe
    private void onNpcChanged(NpcChanged changed) {
        int idBefore = changed.getOld().getId();
        int idAfter = changed.getNpc().getId();

        if (TobNpc.isXarpusIdle(idBefore) && TobNpc.isXarpusP1(idAfter)) {
            startRoom();
            return;
        }

        final int tick = getRoomTick();

        if (TobNpc.isXarpusP1(idBefore) && TobNpc.isXarpus(idAfter)) {
            xarpus = changed.getNpc();
            phase = XarpusPhase.P2;
            dispatchEvent(new XarpusPhaseEvent(tick, getWorldLocation(xarpus), phase));
            log.debug("Exhumes: {} ({})", tick, formattedRoomTime());
        }
    }
}
