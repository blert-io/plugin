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

package io.blert.core;

import io.blert.core.ChallengeRecordingConfig.ObjectCapture;
import io.blert.events.ObjectsEvent;
import io.blert.util.Location;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

/** Tracks the positions occupied by a configured set of objects. */
class ObjectTracker {
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class ObjectKey {
        private final ObjectType type;
        private final int id;
    }

    private final Client client;
    private final ChallengeRecordingConfig config;

    private final Map<ObjectKey, Map<TileObject, WorldPoint>> objects = new HashMap<>();
    private final Map<ObjectKey, Set<WorldPoint>> createdGraphics = new HashMap<>();
    private Map<ObjectKey, Set<WorldPoint>> previousPositions = new HashMap<>();
    private boolean resyncPending = true;
    private boolean rescanPending = false;

    ObjectTracker(Client client, ChallengeRecordingConfig config) {
        this.client = client;
        this.config = config;
    }

    /** Finds game and ground objects already present in the world. */
    void scan(int plane) {
        Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();
        if (plane < 0 || plane >= tiles.length) {
            return;
        }
        for (Tile[] row : tiles[plane]) {
            for (Tile tile : row) {
                if (tile == null) {
                    continue;
                }
                if (tile.getGameObjects() != null) {
                    for (GameObject object : tile.getGameObjects()) {
                        if (object != null) {
                            add(object);
                        }
                    }
                }
                if (tile.getGroundObject() != null) {
                    add(tile.getGroundObject());
                }
            }
        }
    }

    void add(GameObject object) {
        add(ObjectType.GAME, object);
    }

    void add(GroundObject object) {
        add(ObjectType.GROUND, object);
    }

    /**
     * Records a graphics object created on the current tick.
     */
    void record(GraphicsObject object) {
        // Non-creation graphics are polled every tick in update.
        if (config.objectCapture(ObjectType.GRAPHICS, object.getId()) != ObjectCapture.ON_CREATION) {
            return;
        }

        WorldPoint point = getWorldLocation(object);
        if (point != null) {
            createdGraphics
                    .computeIfAbsent(new ObjectKey(ObjectType.GRAPHICS, object.getId()), k -> new HashSet<>())
                    .add(point);
        }
    }

    void remove(GameObject object) {
        remove(ObjectType.GAME, object);
    }

    void remove(GroundObject object) {
        remove(ObjectType.GROUND, object);
    }

    /**
     * Discards all tracked objects, queueing a scan on the next update.
     */
    void reset() {
        objects.clear();
        rescanPending = true;
        resyncPending = true;
    }

    /**
     * Updates the tracked graphics for a tick and returns an event describing
     * the changes, if any.
     */
    @Nullable
    ObjectsEvent update(Stage stage, int tick) {
        if (!config.recordsObjects()) {
            return null;
        }

        if (rescanPending) {
            scan(client.getTopLevelWorldView().getPlane());
            rescanPending = false;
        }

        Map<ObjectKey, Set<WorldPoint>> positions = new HashMap<>();
        objects.forEach((key, current) -> {
            if (!current.isEmpty()) {
                positions.put(key, new HashSet<>(current.values()));
            }
        });

        for (GraphicsObject object : client.getTopLevelWorldView().getGraphicsObjects()) {
            if (config.objectCapture(ObjectType.GRAPHICS, object.getId()) == ObjectCapture.PRESENCE) {
                WorldPoint point = getWorldLocation(object);
                if (point != null) {
                    positions
                            .computeIfAbsent(new ObjectKey(ObjectType.GRAPHICS, object.getId()), k -> new HashSet<>())
                            .add(point);
                }
            }
        }

        createdGraphics.forEach((key, points) ->
                positions.computeIfAbsent(key, k -> new HashSet<>()).addAll(points));
        createdGraphics.clear();

        Map<ObjectKey, Set<WorldPoint>> previous = previousPositions;
        previousPositions = positions;

        // Send an empty snapshot on every tick without objects to limit drift.
        if (resyncPending || positions.isEmpty()) {
            resyncPending = false;
            List<ObjectsEvent.Update> updates = new ArrayList<>();
            positions.forEach((key, points) ->
                    updates.add(ObjectsEvent.Update.snapshot(key.type, key.id, new ArrayList<>(points))));
            return ObjectsEvent.snapshots(stage, tick, updates);
        }

        Set<ObjectKey> keys = new HashSet<>(previous.keySet());
        keys.addAll(positions.keySet());

        List<ObjectsEvent.Update> updates = new ArrayList<>();
        for (ObjectKey key : keys) {
            Set<WorldPoint> before = previous.getOrDefault(key, Set.of());
            Set<WorldPoint> after = positions.getOrDefault(key, Set.of());
            List<WorldPoint> added = difference(after, before);
            List<WorldPoint> removed = difference(before, after);
            if (added.isEmpty() && removed.isEmpty()) {
                continue;
            }

            if (after.size() < added.size() + removed.size()) {
                updates.add(ObjectsEvent.Update.snapshot(key.type, key.id, new ArrayList<>(after)));
            } else {
                updates.add(ObjectsEvent.Update.delta(key.type, key.id, added, removed));
            }
        }

        return updates.isEmpty() ? null : ObjectsEvent.deltas(stage, tick, updates);
    }

    private void add(ObjectType type, TileObject object) {
        if (config.objectCapture(type, object.getId()) == ObjectCapture.NONE) {
            return;
        }

        WorldPoint point = Location.getWorldLocation(client, object.getWorldLocation());
        if (point != null) {
            objects.computeIfAbsent(new ObjectKey(type, object.getId()), k -> new HashMap<>())
                    .put(object, point);
        }
    }

    private void remove(ObjectType type, TileObject object) {
        Map<TileObject, WorldPoint> current = objects.get(new ObjectKey(type, object.getId()));
        if (current != null) {
            current.remove(object);
        }
    }

    @Nullable
    private WorldPoint getWorldLocation(GraphicsObject object) {
        return Location.getWorldLocation(client, WorldPoint.fromLocal(client, object.getLocation()));
    }

    private static List<WorldPoint> difference(Set<WorldPoint> from, Set<WorldPoint> exclude) {
        return from.stream().filter(point -> !exclude.contains(point)).collect(Collectors.toList());
    }
}
