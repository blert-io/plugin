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

package io.blert.raid.rooms.nylocas;

import io.blert.raid.Hitpoints;
import io.blert.raid.TobNpc;
import io.blert.raid.rooms.RoomNpc;
import lombok.Getter;
import net.runelite.api.NPC;
import org.jetbrains.annotations.NotNull;

@Getter
public class NyloBoss extends RoomNpc {
    private final boolean prince;

    protected NyloBoss(@NotNull NPC npc, @NotNull TobNpc tobNpc, long roomId, Hitpoints hitpoints) {
        super(npc, tobNpc, roomId, hitpoints);
        this.prince = TobNpc.isNylocasPrinkipas(tobNpc.getId());
    }
}
