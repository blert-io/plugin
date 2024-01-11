package com.blert.events;

public interface EventHandler {
    /**
     * Processes an event occurring in a raid.
     * @param clientTick Local monotonically increasing tick at which this event was sent. May be used to batch events.
     * @param event The event that occurred.
     */
    void handleEvent(int clientTick, Event event);
}
