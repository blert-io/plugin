package io.blert.events.mokhaiotl;

import io.blert.challenges.mokhaiotl.Orb;
import io.blert.core.Stage;
import io.blert.events.Event;
import io.blert.events.EventType;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class MokhaiotlOrbEvent extends Event {
    private final Orb.Source source;
    private final WorldPoint sourcePoint;
    private final Orb.Style style;
    private final int startTick;
    private final int endTick;

    public MokhaiotlOrbEvent(Stage stage, int tick, Orb orb, WorldPoint sourcePoint) {
        super(EventType.MOKHAIOTL_ORB, stage, tick, null);
        this.source = orb.getSource();
        this.sourcePoint = sourcePoint;
        this.style = orb.getStyle();
        this.startTick = orb.getSpawnTick();
        this.endTick = tick;
    }

    @Override
    protected String eventDataString() {
        return String.format("source=%s, sourcePoint=%s, style=%s, startTick=%d, endTick=%d",
                source, sourcePoint, style, startTick, endTick);
    }
}
