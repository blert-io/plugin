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

import io.blert.events.NpcEvent;
import io.blert.challenges.tob.Mode;
import io.blert.challenges.tob.TobNpc;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.NPC;
import org.jetbrains.annotations.NotNull;

/**
 * A {@code TrackedNpc} is an NPC within a PVM challenge room whose data is trackable and reported via
 * {@link NpcEvent}s. The {@code TrackedNpc} wraps an existing Runelite {@link NPC} with additional Blert-specific
 * data fields. Each {@code TrackedNpc} also has an ID which uniquely identifies it among all NPCs within its
 * environment.
 */
public abstract class TrackedNpc {
    /**
     * Properties are an immutable copy of NPC-specific data fields for a {@link TrackedNpc}.
     */
    public abstract static class Properties {
    }

    @Getter
    private final long roomId;

    @Getter
    @Setter
    private @NotNull NPC npc;

    private final @NotNull Mode mode;

    @Getter
    @Setter
    private Hitpoints hitpoints;

    @Getter
    @Setter
    private int spawnTick;

    protected TrackedNpc(@NotNull NPC npc, @NotNull TobNpc tobNpc, long roomId, Hitpoints hitpoints) {
        this.npc = npc;
        this.mode = tobNpc.getMode();
        this.roomId = roomId;
        this.hitpoints = hitpoints;
        this.spawnTick = -1;
    }

    /**
     * Returns a snapshot of NPC-specific properties for the NPC at a point in time.
     *
     * @return Copy of the current state of the NPC.
     */
    public abstract @NotNull Properties getProperties();

    public int getNpcId() {
        return npc.getId();
    }

    public String getName() {
        return npc.getName();
    }

    public Mode getRaidMode() {
        return mode;
    }
}
