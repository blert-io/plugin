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

package io.blert.json;

import io.blert.events.EventHandler;
import com.google.gson.Gson;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * An event handler which serializes events to JSON.
 * <p>
 * Events posted to `HandleEvent` are stored, without any output being produced. Users must call a flush method to
 * consume events and generate their JSON output.
 */
public class JsonEventHandler implements EventHandler {
    private final Map<Integer, List<Event>> eventsByTick = new HashMap<>();
    private int lastTick = 0;

    @Setter
    private @Nullable String raidId;

    @Override
    public void handleEvent(int clientTick, io.blert.events.Event event) {
        eventsByTick.putIfAbsent(clientTick, new ArrayList<>());
        Event evt = Event.fromBlert(event);
        if (raidId != null) {
            evt.setRaidId(raidId);
        }
        eventsByTick.get(clientTick).add(evt);

        lastTick = clientTick;
    }

    public boolean hasEvents() {
        return !eventsByTick.isEmpty();
    }

    /**
     * Returns a JSON array of all stored events occurring up to and including the specified tick, and clears the events
     * from this handler.
     *
     * @param clientTick The tick up to which events should be consumed, inclusive.
     * @return Serialized JSON array of the consumed events.
     */
    public String flushEventsUpTo(int clientTick) {
        List<Event> events = eventsByTick
                .keySet()
                .stream()
                .filter(tick -> tick <= clientTick)
                .sorted()
                .map(eventsByTick::remove)
                .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
        return new Gson().toJson(events);
    }
}
