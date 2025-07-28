package io.blert.events.mokhaiotl;

import io.blert.core.Stage;
import io.blert.events.Event;
import io.blert.events.EventType;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MokhaiotlShockwaveEvent extends Event {
    private final List<WorldPoint> shockwaveTiles;

    public MokhaiotlShockwaveEvent(Stage stage, int tick, List<WorldPoint> shockwaveTiles) {
        super(EventType.MOKHAIOTL_SHOCKWAVE, stage, tick, null);
        this.shockwaveTiles = shockwaveTiles;
    }

    @Override
    protected String eventDataString() {
        return "shockwaveTiles=" + shockwaveTiles.stream()
                .map(WorldPoint::toString)
                .collect(Collectors.joining(","));
    }
}
