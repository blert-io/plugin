package io.blert.events.mokhaiotl;

import io.blert.core.Stage;
import io.blert.events.Event;
import io.blert.events.EventType;
import lombok.Getter;

@Getter
public class MokhaiotlLarvaLeakEvent extends Event {
    private final long roomId;
    private final int healAmount;

    public MokhaiotlLarvaLeakEvent(Stage stage, int tick, long roomId, int healAmount) {
        super(EventType.MOKHAIOTL_LARVA_LEAK, stage, tick, null);
        this.roomId = roomId;
        this.healAmount = healAmount;
    }

    @Override
    protected String eventDataString() {
        return "roomId=" + roomId + ", healAmount=" + healAmount;
    }
}
