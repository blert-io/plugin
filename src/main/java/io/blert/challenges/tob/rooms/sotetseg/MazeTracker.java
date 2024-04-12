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

package io.blert.challenges.tob.rooms.sotetseg;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class MazeTracker {
    private final List<WorldPoint> overworldPivots = new ArrayList<>();
    private final List<WorldPoint> underworldPivots = new ArrayList<>();
    private final Set<WorldPoint> raggedPoints = new HashSet<>();

    public MazeTracker() {
    }

    public void addPotentialOverworldPoint(WorldPoint point) {
        if (!overworldPivots.contains(point)) {
            overworldPivots.add(point);
        }
    }

    public void removeOverworldPoint(WorldPoint point) {
        raggedPoints.add(point);
    }

    public void finishMaze() {
        // If the last recorded point is on the penultimate row, assume the final pivot has the same x coordinate.
        if (!overworldPivots.isEmpty()) {
            final WorldPoint lastPoint = overworldPivots.get(overworldPivots.size() - 1);
            final int penultimateRow = Maze.OVERWORLD_MAZE_START.getY() + Maze.HEIGHT - 2;
            if (lastPoint.getY() == penultimateRow) {
                overworldPivots.add(new WorldPoint(lastPoint.getX(), penultimateRow + 1, lastPoint.getPlane()));
            }
        }

        overworldPivots.removeAll(raggedPoints);
        overworldPivots.removeIf(point -> (point.getY() - Maze.OVERWORLD_MAZE_START.getY()) % 2 == 1);
    }

    public void addUnderworldPoint(WorldPoint point) {
        int y = point.getY() - Maze.UNDERWORLD_MAZE_START.getY();
        if (y % 2 == 1) {
            // Only even rows are pivots.
            return;
        }

        if (!underworldPivots.contains(point)) {
            underworldPivots.add(point);
        }
    }

    public boolean hasOverworldMaze() {
        if (overworldPivots.size() != 8) {
            return false;
        }

        for (int i = 0; i < 8; i++) {
            final int expectedY = Maze.OVERWORLD_MAZE_START.getY() + i * 2;
            if (overworldPivots.get(i).getY() != expectedY) {
                return false;
            }
        }

        return true;
    }

    public boolean hasOverworldPivots() {
        return !overworldPivots.isEmpty();
    }

    public boolean hasUnderworldPivots() {
        return !underworldPivots.isEmpty();
    }

    public int[] getPivots() {
        return overworldPivots.stream().mapToInt(WorldPoint::getX).toArray();
    }

    public void reset() {
        overworldPivots.clear();
        underworldPivots.clear();
        raggedPoints.clear();
    }
}
