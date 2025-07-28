package io.blert.events.mokhaiotl;

import io.blert.core.Stage;
import io.blert.events.Event;
import io.blert.events.EventType;
import lombok.Getter;

@Getter
public class MokhaiotlAttackStyleEvent extends Event {
    public enum Style {
        MELEE,
        RANGED,
        MAGE,
    }

    private final Style style;
    private final int attackTick;

    public MokhaiotlAttackStyleEvent(Stage stage, int tick, Style style, int attackTick) {
        super(EventType.MOKHAIOTL_ATTACK_STYLE, stage, tick, null);
        this.style = style;
        this.attackTick = attackTick;
    }

    @Override
    protected String eventDataString() {
        return "style=" + style.name() + ", attackTick=" + attackTick;
    }
}
