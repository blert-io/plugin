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

import io.blert.core.ObjectType;
import io.blert.core.Stage;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class ObjectsEvent extends Event {
    /** Positions occupied by objects of a single type and ID. */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    public static class Update {
        private final ObjectType type;
        private final int id;
        /**
         * Whether {@code added} is the complete list of points present on the
         * tick. {@code removed} will be empty. Every update in a snapshot
         * event is a snapshot regardless of this flag.
         */
        private final boolean snapshot;

        private final List<WorldPoint> added;
        private final List<WorldPoint> removed;

        public static Update delta(ObjectType type, int id, List<WorldPoint> added, List<WorldPoint> removed) {
            return new Update(type, id, false, added, removed);
        }

        public static Update snapshot(ObjectType type, int id, List<WorldPoint> positions) {
            return new Update(type, id, true, positions, List.of());
        }
    }

    /**
     * If true, the updates are all snapshots and list every object present on
     * the tick. Objects which are not listed do not exist in the world.
     */
    private final boolean snapshot;

    private final List<Update> updates;

    public static ObjectsEvent deltas(Stage stage, int tick, List<Update> updates) {
        return new ObjectsEvent(stage, tick, false, updates);
    }

    public static ObjectsEvent snapshots(Stage stage, int tick, List<Update> updates) {
        return new ObjectsEvent(stage, tick, true, updates);
    }

    private ObjectsEvent(Stage stage, int tick, boolean snapshot, List<Update> updates) {
        super(EventType.OBJECTS, stage, tick, null);
        this.snapshot = snapshot;
        this.updates = updates;
    }

    @Override
    protected String eventDataString() {
        String data = updates.stream()
                .map(update -> String.format(
                        "%s:%d%s added=%s removed=%s",
                        update.getType(),
                        update.getId(),
                        update.isSnapshot() ? " snapshot" : "",
                        update.getAdded(),
                        update.getRemoved()))
                .collect(Collectors.joining(", "));
        if (!snapshot) {
            return data;
        }
        return data.isEmpty() ? "snapshot" : "snapshot " + data;
    }
}
