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

package io.blert.json;

import io.blert.events.PlayerAttackEvent;
import io.blert.raid.Item;
import io.blert.raid.PlayerAttack;
import lombok.Getter;

import javax.annotation.Nullable;

@Getter
public class Attack {
    private final PlayerAttack type;
    private @Nullable Item weapon;
    private @Nullable Npc target;

    public static Attack fromPlayerAttackEvent(PlayerAttackEvent event) {
        Attack attack = new Attack(event.getAttack());
        attack.weapon = event.getWeapon();
        if (event.getTargetNpcId() != -1) {
            attack.target = new Npc(event.getTargetNpcId(), event.getTargetRoomId());
        }
        return attack;
    }

    private Attack(PlayerAttack attack) {
        this.type = attack;
    }
}
