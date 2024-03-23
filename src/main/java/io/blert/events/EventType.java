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

package io.blert.events;

import io.blert.proto.Event;

public enum EventType {
    RAID_START(Event.Type.CHALLENGE_START),
    RAID_END(Event.Type.CHALLENGE_END),
    RAID_UPDATE(Event.Type.CHALLENGE_UPDATE),
    ROOM_STATUS(Event.Type.STAGE_UPDATE),
    PLAYER_UPDATE(Event.Type.PLAYER_UPDATE),
    PLAYER_ATTACK(Event.Type.PLAYER_ATTACK),
    PLAYER_DEATH(Event.Type.PLAYER_DEATH),
    NPC_SPAWN(Event.Type.NPC_SPAWN),
    NPC_UPDATE(Event.Type.NPC_UPDATE),
    NPC_DEATH(Event.Type.NPC_DEATH),
    NPC_ATTACK(Event.Type.NPC_ATTACK),

    MAIDEN_CRAB_LEAK(Event.Type.TOB_MAIDEN_CRAB_LEAK),
    MAIDEN_BLOOD_SPLATS(Event.Type.TOB_MAIDEN_BLOOD_SPLATS),

    BLOAT_DOWN(Event.Type.TOB_BLOAT_DOWN),
    BLOAT_UP(Event.Type.TOB_BLOAT_UP),

    NYLO_WAVE_SPAWN(Event.Type.TOB_NYLO_WAVE_SPAWN),
    NYLO_WAVE_STALL(Event.Type.TOB_NYLO_WAVE_STALL),
    NYLO_CLEANUP_END(Event.Type.TOB_NYLO_CLEANUP_END),
    NYLO_BOSS_SPAWN(Event.Type.TOB_NYLO_BOSS_SPAWN),

    SOTE_MAZE_PROC(Event.Type.TOB_SOTE_MAZE_PROC),
    SOTE_MAZE_PATH(Event.Type.TOB_SOTE_MAZE_PATH),

    XARPUS_PHASE(Event.Type.TOB_XARPUS_PHASE),

    VERZIK_PHASE(Event.Type.TOB_VERZIK_PHASE),
    VERZIK_REDS_SPAWN(Event.Type.TOB_VERZIK_REDS_SPAWN),
    VERZIK_ATTACK_STYLE(Event.Type.TOB_VERZIK_ATTACK_STYLE),

    ;

    private final Event.Type protoValue;

    EventType(Event.Type protoValue) {
        this.protoValue = protoValue;
    }

    public Event.Type toProto() {
        return protoValue;
    }
}
