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

package io.blert.core;

public enum NpcAttack {
    TOB_MAIDEN_AUTO(io.blert.proto.NpcAttack.TOB_MAIDEN_AUTO),
    TOB_MAIDEN_BLOOD_THROW(io.blert.proto.NpcAttack.TOB_MAIDEN_BLOOD_THROW),
    TOB_BLOAT_STOMP(io.blert.proto.NpcAttack.TOB_BLOAT_STOMP),
    TOB_NYLO_BOSS_MELEE(io.blert.proto.NpcAttack.TOB_NYLO_BOSS_MELEE),
    TOB_NYLO_BOSS_RANGE(io.blert.proto.NpcAttack.TOB_NYLO_BOSS_RANGE),
    TOB_NYLO_BOSS_MAGE(io.blert.proto.NpcAttack.TOB_NYLO_BOSS_MAGE),
    TOB_SOTE_MELEE(io.blert.proto.NpcAttack.TOB_SOTE_MELEE),
    TOB_SOTE_BALL(io.blert.proto.NpcAttack.TOB_SOTE_BALL),
    TOB_SOTE_DEATH_BALL(io.blert.proto.NpcAttack.TOB_SOTE_DEATH_BALL),
    TOB_XARPUS_SPIT(io.blert.proto.NpcAttack.TOB_XARPUS_SPIT),
    TOB_XARPUS_TURN(io.blert.proto.NpcAttack.TOB_XARPUS_TURN),
    TOB_VERZIK_P1_AUTO(io.blert.proto.NpcAttack.TOB_VERZIK_P1_AUTO),
    TOB_VERZIK_P2_BOUNCE(io.blert.proto.NpcAttack.TOB_VERZIK_P2_BOUNCE),
    TOB_VERZIK_P2_CABBAGE(io.blert.proto.NpcAttack.TOB_VERZIK_P2_CABBAGE),
    TOB_VERZIK_P2_ZAP(io.blert.proto.NpcAttack.TOB_VERZIK_P2_ZAP),
    TOB_VERZIK_P2_PURPLE(io.blert.proto.NpcAttack.TOB_VERZIK_P2_PURPLE),
    TOB_VERZIK_P2_MAGE(io.blert.proto.NpcAttack.TOB_VERZIK_P2_MAGE),
    TOB_VERZIK_P3_AUTO(io.blert.proto.NpcAttack.TOB_VERZIK_P3_AUTO),
    TOB_VERZIK_P3_WEBS(io.blert.proto.NpcAttack.TOB_VERZIK_P3_WEBS),
    TOB_VERZIK_P3_YELLOWS(io.blert.proto.NpcAttack.TOB_VERZIK_P3_YELLOWS),
    VERZIK_P3_BALL(io.blert.proto.NpcAttack.TOB_VERZIK_P3_BALL),

    COLOSSEUM_BERSERKER_AUTO(io.blert.proto.NpcAttack.COLOSSEUM_BERSERKER_AUTO),
    COLOSSEUM_SEER_AUTO(io.blert.proto.NpcAttack.COLOSSEUM_SEER_AUTO),
    COLOSSEUM_ARCHER_AUTO(io.blert.proto.NpcAttack.COLOSSEUM_ARCHER_AUTO),
    COLOSSEUM_SHAMAN_AUTO(io.blert.proto.NpcAttack.COLOSSEUM_SHAMAN_AUTO),
    COLOSSEUM_JAGUAR_AUTO(io.blert.proto.NpcAttack.COLOSSEUM_JAGUAR_AUTO),
    COLOSSEUM_JAVELIN_AUTO(io.blert.proto.NpcAttack.COLOSSEUM_JAVELIN_AUTO),
    COLOSSEUM_JAVELIN_TOSS(io.blert.proto.NpcAttack.COLOSSEUM_JAVELIN_TOSS),
    COLOSSEUM_MANTICORE_AUTO(io.blert.proto.NpcAttack.COLOSSEUM_MANTICORE_AUTO),
    COLOSSEUM_SHOCKWAVE_AUTO(io.blert.proto.NpcAttack.COLOSSEUM_SHOCKWAVE_AUTO),
    COLOSSEUM_MINOTAUR_AUTO(io.blert.proto.NpcAttack.COLOSSEUM_MINOTAUR_AUTO),
    COLOSSEUM_HEREDIT_THRUST(io.blert.proto.NpcAttack.COLOSSEUM_HEREDIT_THRUST),
    COLOSSEUM_HEREDIT_SLAM(io.blert.proto.NpcAttack.COLOSSEUM_HEREDIT_SLAM),
    COLOSSEUM_HEREDIT_BREAK(io.blert.proto.NpcAttack.COLOSSEUM_HEREDIT_BREAK),
    COLOSSEUM_HEREDIT_COMBO(io.blert.proto.NpcAttack.COLOSSEUM_HEREDIT_COMBO),

    ;

    private final io.blert.proto.NpcAttack protoValue;

    NpcAttack(io.blert.proto.NpcAttack protoValue) {
        this.protoValue = protoValue;
    }

    public io.blert.proto.NpcAttack toProto() {
        return protoValue;
    }
}
