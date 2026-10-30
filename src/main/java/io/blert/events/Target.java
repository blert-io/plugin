/*
 * Copyright (c) 2026 Alexei Frolov
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

import io.blert.core.TrackedNpc;
import javax.annotation.Nullable;
import lombok.Getter;

@Getter
public class Target {
    private final @Nullable String player;
    private final @Nullable Long npcRoomId;

    public static Target player(String name) {
        return new Target(name, null);
    }

    public static Target npc(TrackedNpc trackedNpc) {
        return new Target(null, trackedNpc.getRoomId());
    }

    private Target(@Nullable String player, @Nullable Long npcRoomId) {
        this.player = player;
        this.npcRoomId = npcRoomId;
    }

    @Override
    public String toString() {
        return player != null ? "(player=" + player + ")" : "(npcRoomId=" + npcRoomId + ")";
    }
}
