package io.blert.events.chambers;

import io.blert.core.Stage;
import io.blert.events.Event;
import io.blert.events.EventType;

public class CoxRoomCompleteEvent extends Event {
    private final int startTick;
    private final int endTick;

    public CoxRoomCompleteEvent(Stage stage, int startTick, int endTick) {
        // Use COX_ROOM_COMPLETE which doesn't have special casting in WebSocketEventHandler
        // super(EventType.COX_ROOM_COMPLETE, stage, endTick, null);
        super(EventType.STAGE_UPDATE, stage, endTick, null); // Use STAGE_UPDATE or define a new EventType if needed
        this.startTick = startTick;
        this.endTick = endTick;
    }

    @Override
    protected String eventDataString() {
        return String.format("Room: %s, Start: %d, End: %d, Duration: %d", getStage(), startTick, endTick,
                (endTick - startTick));
    }

    public int getStartTick() { return startTick; }
    public int getEndTick() { return endTick; }
}