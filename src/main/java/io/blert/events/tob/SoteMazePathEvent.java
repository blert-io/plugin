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

import io.blert.challenges.tob.rooms.sotetseg.Maze;
import io.blert.core.Stage;
import io.blert.events.Event;
import io.blert.events.EventType;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Getter
public class SoteMazePathEvent extends Event {
    public enum TileType {
        OVERWORLD_TILES,
        UNDERWORLD_PIVOTS,
        OVERWORLD_PIVOTS,
    }

    private final Maze maze;
    private final TileType tileType;
    private final List<WorldPoint> mazeTiles;

    public static SoteMazePathEvent overworldTiles(int tick, Maze maze, List<WorldPoint> mazeTiles) {
        return new SoteMazePathEvent(tick, maze, TileType.OVERWORLD_TILES, mazeTiles);
    }

    public static SoteMazePathEvent underworldPivots(int tick, Maze maze, List<WorldPoint> mazeTiles) {
        return new SoteMazePathEvent(tick, maze, TileType.UNDERWORLD_PIVOTS, mazeTiles);
    }

    public static SoteMazePathEvent overworldPivots(int tick, Maze maze, List<WorldPoint> mazeTiles) {
        return new SoteMazePathEvent(tick, maze, TileType.OVERWORLD_PIVOTS, mazeTiles);
    }

    public Stream<WorldPoint> mazeRelativePoints() {
        return mazeTiles.stream().map(pt -> toMazeRelativePoint(tileType, pt));
    }

    private SoteMazePathEvent(int tick, Maze maze, TileType tileType, List<WorldPoint> mazeTiles) {
        super(EventType.SOTE_MAZE_PATH, Stage.TOB_SOTETSEG, tick, null);
        this.maze = maze;
        this.tileType = tileType;
        this.mazeTiles = mazeTiles;
    }

    @Override
    protected String eventDataString() {
        return "maze=" + maze + ", mazeTiles=" + Arrays.toString(mazeRelativePoints().toArray());
    }

    private static WorldPoint toMazeRelativePoint(TileType tileType, WorldPoint point) {
        WorldPoint start = tileType == TileType.UNDERWORLD_PIVOTS ? Maze.UNDERWORLD_MAZE_START : Maze.OVERWORLD_MAZE_START;
        return new WorldPoint(point.getX() - start.getX(), point.getY() - start.getY(), point.getPlane());
    }
}
