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

package io.blert.events.tob;

import io.blert.challenges.tob.rooms.Room;
import io.blert.challenges.tob.rooms.sotetseg.Maze;
import io.blert.events.EventType;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Optional;

@Getter
public class SoteMazeEvent extends TobEvent {
    private final Maze maze;
    private final String chosenPlayer;

    public static SoteMazeEvent mazeProc(int tick, Maze maze) {
        return new SoteMazeEvent(EventType.SOTE_MAZE_PROC, tick, maze, null);
    }

    public static SoteMazeEvent mazeEnd(int tick, Maze maze,
                                        @Nullable String chosen) {
        return new SoteMazeEvent(EventType.SOTE_MAZE_END, tick, maze, chosen);
    }

    private SoteMazeEvent(EventType type, int tick, Maze maze, String chosen) {
        super(type, Room.SOTETSEG, tick, null);
        this.maze = maze;
        this.chosenPlayer = chosen;
    }

    public Optional<String> getChosenPlayer() {
        return Optional.ofNullable(chosenPlayer);
    }

    @Override
    protected String eventDataString() {
        return null;
    }
}
