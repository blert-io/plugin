/*
 * Copyright (c) 2025 Alexei Frolov
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

package io.blert.events.tob;

import io.blert.challenges.tob.rooms.Room;
import io.blert.events.EventType;
import lombok.Getter;
import lombok.NonNull;
import net.runelite.api.Player;

import javax.annotation.Nullable;
import java.util.Optional;

@Getter
public class VerzikBounceEvent extends TobEvent {
    private final int attackTick;
    private final int playersInRange;
    private final int playersNotInRange;
    @Nullable
    private final String bouncedPlayer;

    /**
     * Creates an event where {@code bouncedPlayer} was bounced by Verzik.
     *
     * @param tick              Tick on which the event occurred
     * @param attackTick        Tick on which Verzik bounced the player
     * @param playersInRange    Number of players in range of Verzik
     * @param playersNotInRange Number of players not in range of Verzik
     * @param bouncedPlayer     Player who was bounced by Verzik
     */
    public VerzikBounceEvent(int tick,
                             int attackTick,
                             int playersInRange,
                             int playersNotInRange,
                             @NonNull Player bouncedPlayer) {
        super(EventType.VERZIK_BOUNCE, Room.VERZIK, tick, null);
        this.attackTick = attackTick;
        this.playersInRange = playersInRange;
        this.playersNotInRange = playersNotInRange;
        this.bouncedPlayer = bouncedPlayer.getName();
    }

    /**
     * Creates a bounce event in which no player was bounced.
     *
     * @param tick              Tick on which the event occurred
     * @param playersInRange    Number of players in range of Verzik
     * @param playersNotInRange Number of players not in range of Verzik
     */
    public VerzikBounceEvent(int tick, int playersInRange, int playersNotInRange) {
        super(EventType.VERZIK_BOUNCE, Room.VERZIK, tick, null);
        this.attackTick = -1;
        this.playersInRange = playersInRange;
        this.playersNotInRange = playersNotInRange;
        this.bouncedPlayer = null;
    }

    public Optional<String> getBouncedPlayer() {
        return Optional.ofNullable(bouncedPlayer);
    }

    @Override
    protected String eventDataString() {
        return String.format("attackTick=%d, playersInRange=%d, playersNotInRange=%d, bouncedPlayer=%s",
                attackTick, playersInRange, playersNotInRange,
                bouncedPlayer != null ? bouncedPlayer : "(none)");
    }
}
