/*
 * Copyright (c) 2025 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.blert.events;

import io.blert.core.Raider;
import io.blert.core.SpellDefinition;
import io.blert.core.Stage;
import io.blert.core.TrackedNpc;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;

@Getter
public class PlayerSpellEvent extends Event {
    private final SpellDefinition spell;
    private final String username;

    private final @Nullable String targetPlayer;
    private final int targetNpcId;
    private final long targetNpcRoomId;

    private PlayerSpellEvent(Stage stage, int tick, WorldPoint playerPoint, SpellDefinition spell,
                             Raider raider, @Nullable String targetPlayer, int targetNpcId, long targetNpcRoomId) {
        super(EventType.PLAYER_SPELL, stage, tick, playerPoint);
        this.spell = spell;
        this.username = raider.getUsername();
        this.targetPlayer = targetPlayer;
        this.targetNpcId = targetNpcId;
        this.targetNpcRoomId = targetNpcRoomId;
    }

    public static PlayerSpellEvent withNoTarget(Stage stage, int tick, WorldPoint playerPoint,
                                                SpellDefinition spell, Raider raider) {
        return new PlayerSpellEvent(stage, tick, playerPoint, spell, raider, null, -1, 0);
    }

    public static PlayerSpellEvent withPlayerTarget(Stage stage, int tick, WorldPoint playerPoint,
                                                    SpellDefinition spell, Raider raider, String targetPlayer) {
        return new PlayerSpellEvent(stage, tick, playerPoint, spell, raider, targetPlayer, -1, 0);
    }

    public static PlayerSpellEvent withNpcTarget(Stage stage, int tick, WorldPoint playerPoint,
                                                 SpellDefinition spell, Raider raider, TrackedNpc targetNpc) {
        return new PlayerSpellEvent(stage, tick, playerPoint, spell, raider, null,
                targetNpc.getNpcId(), targetNpc.getRoomId());
    }

    public boolean hasNpcTarget() {
        return targetNpcId != -1;
    }

    @Override
    protected String eventDataString() {
        StringBuilder sb = new StringBuilder("spell=").append(spell.getName());
        sb.append(", player=").append(username);
        if (targetPlayer != null) {
            sb.append(", targetPlayer=").append(targetPlayer);
        }
        if (targetNpcId != -1) {
            sb.append(", targetNpc=").append(targetNpcId);
        }
        return sb.toString();
    }
}
