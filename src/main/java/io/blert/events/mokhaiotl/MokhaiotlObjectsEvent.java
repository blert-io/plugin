package io.blert.events.mokhaiotl;

import io.blert.core.Stage;
import io.blert.events.Event;
import io.blert.events.EventType;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MokhaiotlObjectsEvent extends Event {
    private final List<WorldPoint> rocksSpawned;
    private final List<WorldPoint> rocksDespawned;
    private final List<WorldPoint> splatsSpawned;
    private final List<WorldPoint> splatsDespawned;

    public MokhaiotlObjectsEvent(
            Stage stage,
            int tick,
            List<WorldPoint> rocksSpawned,
            List<WorldPoint> rocksDespawned,
            List<WorldPoint> splatsSpawned,
            List<WorldPoint> splatsDespawned
    ) {
        super(EventType.MOKHAIOTL_OBJECTS, stage, tick, null);
        this.rocksSpawned = rocksSpawned;
        this.rocksDespawned = rocksDespawned;
        this.splatsSpawned = splatsSpawned;
        this.splatsDespawned = splatsDespawned;
    }

    @Override
    protected String eventDataString() {
        return String.format("rocks_spawned=%s, rocks_despawned=%s, splats_spawned=%s, splats_despawned=%s",
                rocksSpawned.stream().map(WorldPoint::toString).collect(Collectors.joining(", ")),
                rocksDespawned.stream().map(WorldPoint::toString).collect(Collectors.joining(", ")),
                splatsSpawned.stream().map(WorldPoint::toString).collect(Collectors.joining(", ")),
                splatsDespawned.stream().map(WorldPoint::toString).collect(Collectors.joining(", "))
        );
    }
}
