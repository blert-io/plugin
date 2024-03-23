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
    // TODO(frolv): Prefix with challenge.
    MAIDEN_AUTO(io.blert.proto.NpcAttack.TOB_MAIDEN_AUTO),
    MAIDEN_BLOOD_THROW(io.blert.proto.NpcAttack.TOB_MAIDEN_BLOOD_THROW),
    BLOAT_STOMP(io.blert.proto.NpcAttack.TOB_BLOAT_STOMP),
    NYLO_BOSS_MELEE(io.blert.proto.NpcAttack.TOB_NYLO_BOSS_MELEE),
    NYLO_BOSS_RANGE(io.blert.proto.NpcAttack.TOB_NYLO_BOSS_RANGE),
    NYLO_BOSS_MAGE(io.blert.proto.NpcAttack.TOB_NYLO_BOSS_MAGE),
    SOTE_MELEE(io.blert.proto.NpcAttack.TOB_SOTE_MELEE),
    SOTE_BALL(io.blert.proto.NpcAttack.TOB_SOTE_BALL),
    SOTE_DEATH_BALL(io.blert.proto.NpcAttack.TOB_SOTE_DEATH_BALL),
    XARPUS_SPIT(io.blert.proto.NpcAttack.TOB_XARPUS_SPIT),
    XARPUS_TURN(io.blert.proto.NpcAttack.TOB_XARPUS_TURN),
    VERZIK_P1_AUTO(io.blert.proto.NpcAttack.TOB_VERZIK_P1_AUTO),
    VERZIK_P2_BOUNCE(io.blert.proto.NpcAttack.TOB_VERZIK_P2_BOUNCE),
    VERZIK_P2_CABBAGE(io.blert.proto.NpcAttack.TOB_VERZIK_P2_CABBAGE),
    VERZIK_P2_ZAP(io.blert.proto.NpcAttack.TOB_VERZIK_P2_ZAP),
    VERZIK_P2_PURPLE(io.blert.proto.NpcAttack.TOB_VERZIK_P2_PURPLE),
    VERZIK_P2_MAGE(io.blert.proto.NpcAttack.TOB_VERZIK_P2_MAGE),
    VERZIK_P3_AUTO(io.blert.proto.NpcAttack.TOB_VERZIK_P3_AUTO),
    VERZIK_P3_WEBS(io.blert.proto.NpcAttack.TOB_VERZIK_P3_WEBS),
    VERZIK_P3_YELLOWS(io.blert.proto.NpcAttack.TOB_VERZIK_P3_YELLOWS),
    VERZIK_P3_BALL(io.blert.proto.NpcAttack.TOB_VERZIK_P3_BALL),

    ;

    private final io.blert.proto.NpcAttack protoValue;

    NpcAttack(io.blert.proto.NpcAttack protoValue) {
        this.protoValue = protoValue;
    }

    public io.blert.proto.NpcAttack toProto() {
        return protoValue;
    }
}
