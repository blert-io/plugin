
package io.blert.events.chambers;
import io.blert.core.Stage;
import io.blert.events.Event;
import io.blert.events.EventType;

public class CoxEvent extends Event {
    private final int startTick;
    private final int endTick;
    private final String duration;

    public CoxEvent(EventType type, Stage stage, int startTick, int endTick, String duration) {
        super(type, stage, endTick, null);
        this.startTick = startTick;
        this.endTick = endTick;
        this.duration = duration;
    }

    @Override
    protected String eventDataString() {
        return String.format("Room: %s, StartTick: %d, EndTick: %d, Duration: %s", getStage(), startTick, endTick, duration);
    }

    public int getStartTick() { return startTick; }
    public int getEndTick() { return endTick; }
    public String getDuration() { return duration; }
}