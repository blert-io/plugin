package io.blert.events.tob;

import io.blert.challenges.tob.rooms.Room;
import io.blert.events.EventType;
import io.blert.proto.Event;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;

@Getter
public class XarpusSplatEvent extends TobEvent {
    public enum Source {
        UNKNOWN(Event.XarpusSplat.Source.UNKNOWN),
        XARPUS(Event.XarpusSplat.Source.XARPUS),
        BOUNCE(Event.XarpusSplat.Source.BOUNCE);

        private final Event.XarpusSplat.Source protoSource;

        public Event.XarpusSplat.Source toProto() {
            return protoSource;
        }

        Source(Event.XarpusSplat.Source protoSource) {
            this.protoSource = protoSource;
        }
    }

    private final Source source;

    /**
     * If this is a bounce splat, the splat it bounced from.
     */
    private final WorldPoint bounceFrom;

    public XarpusSplatEvent(int tick, WorldPoint point, Source source, @Nullable WorldPoint bounceFrom) {
        super(EventType.XARPUS_SPLAT, Room.XARPUS, tick, point);

        if (source == Source.BOUNCE && bounceFrom == null) {
            throw new IllegalArgumentException("Bounce splat must have a bounceFrom point");
        }

        this.source = source;
        this.bounceFrom = bounceFrom;
    }

    @Override
    public String eventDataString() {
        return "source=" + source + (source == Source.BOUNCE ? ", bounceFrom=" + bounceFrom : "");
    }
}
