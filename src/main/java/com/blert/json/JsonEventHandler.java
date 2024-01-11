package com.blert.json;

import com.blert.events.EventHandler;
import com.google.gson.Gson;

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

    @Override
    public void handleEvent(int clientTick, com.blert.events.Event event) {
        eventsByTick.putIfAbsent(clientTick, new ArrayList<>());
        eventsByTick.get(clientTick).add(Event.fromBlert(event));

        lastTick = clientTick;
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
