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

import io.blert.core.*;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.util.Optional;

@Getter
public class PlayerAttackEvent extends Event {
    private final PlayerAttack attack;
    private final @Nullable Item weapon;
    private final String username;
    private final int targetNpcId;
    private final long targetRoomId;
    private final int distanceToTarget;

    public PlayerAttackEvent(Stage stage, int tick, WorldPoint playerPoint, PlayerAttack attack,
                             @Nullable Item weapon, Raider raider, @Nullable TrackedNpc trackedNpc, int distanceToNpc) {
        super(EventType.PLAYER_ATTACK, stage, tick, playerPoint);
        this.attack = attack;
        this.weapon = weapon;
        this.username = raider.getUsername();
        this.distanceToTarget = distanceToNpc;

        if (trackedNpc != null) {
            this.targetNpcId = trackedNpc.getNpcId();
            this.targetRoomId = trackedNpc.getRoomId();
        } else {
            this.targetNpcId = 0;
            this.targetRoomId = 0;
        }
    }

    public Optional<Item> getWeapon() {
        return Optional.ofNullable(weapon);
    }

    @Override
    protected String eventDataString() {
        StringBuilder sb = new StringBuilder("type=" + attack);
        if (attack.isUnknown()) {
            sb.append("weapon=").append(attack.getWeaponId());
        }
        return sb.toString();
    }
}

