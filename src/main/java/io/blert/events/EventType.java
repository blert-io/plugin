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
    CHALLENGE_START(null),
    CHALLENGE_END(null),
    CHALLENGE_UPDATE(null),
    STAGE_UPDATE(null),

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
    BLOAT_HANDS_DROP(Event.Type.TOB_BLOAT_HANDS_DROP),
    BLOAT_HANDS_SPLAT(Event.Type.TOB_BLOAT_HANDS_SPLAT),

    NYLO_WAVE_SPAWN(Event.Type.TOB_NYLO_WAVE_SPAWN),
    NYLO_WAVE_STALL(Event.Type.TOB_NYLO_WAVE_STALL),
    NYLO_CLEANUP_END(Event.Type.TOB_NYLO_CLEANUP_END),
    NYLO_BOSS_SPAWN(Event.Type.TOB_NYLO_BOSS_SPAWN),

    SOTE_MAZE_PROC(Event.Type.TOB_SOTE_MAZE_PROC),
    SOTE_MAZE_PATH(Event.Type.TOB_SOTE_MAZE_PATH),
    SOTE_MAZE_END(Event.Type.TOB_SOTE_MAZE_END),

    XARPUS_PHASE(Event.Type.TOB_XARPUS_PHASE),
    XARPUS_EXHUMED(Event.Type.TOB_XARPUS_EXHUMED),
    XARPUS_SPLAT(Event.Type.TOB_XARPUS_SPLAT),

    VERZIK_PHASE(Event.Type.TOB_VERZIK_PHASE),
    VERZIK_BOUNCE(Event.Type.TOB_VERZIK_BOUNCE),
    VERZIK_REDS_SPAWN(Event.Type.TOB_VERZIK_REDS_SPAWN),
    VERZIK_ATTACK_STYLE(Event.Type.TOB_VERZIK_ATTACK_STYLE),
    VERZIK_YELLOWS(Event.Type.TOB_VERZIK_YELLOWS),
    VERZIK_HEAL(Event.Type.TOB_VERZIK_HEAL),
    VERZIK_DAWN(Event.Type.TOB_VERZIK_DAWN),

    COLOSSEUM_HANDICAP_CHOICE(Event.Type.COLOSSEUM_HANDICAP_CHOICE),

    MOKHAIOTL_ATTACK_STYLE(Event.Type.MOKHAIOTL_ATTACK_STYLE),
    MOKHAIOTL_ORB(Event.Type.MOKHAIOTL_ORB),
    MOKHAIOTL_OBJECTS(Event.Type.MOKHAIOTL_OBJECTS),
    MOKHAIOTL_LARVA_LEAK(Event.Type.MOKHAIOTL_LARVA_LEAK),
    MOKHAIOTL_SHOCKWAVE(Event.Type.MOKHAIOTL_SHOCKWAVE),

    INFERNO_WAVE_START(Event.Type.INFERNO_WAVE_START),
    ;

    private final Event.Type protoValue;

    EventType(Event.Type protoValue) {
        this.protoValue = protoValue;
    }

    public Event.Type toProto() {
        return protoValue;
    }
}
