package com.blert.json;

import com.blert.events.PlayerUpdateEvent;
import com.blert.raid.EquipmentSlot;
import com.blert.raid.Hitpoints;
import com.blert.raid.Item;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Player {
    private String name;
    private @Nullable Hitpoints hitpoints = null;
    private Map<EquipmentSlot, Item> equipment = new HashMap<>();

    public Player(String name) {
        this.name = name;
    }

    public static Player fromBlertEvent(PlayerUpdateEvent event) {
        Player player = new Player(event.getUsername());
        event.getHitpoints().ifPresent(player::setHitpoints);
        player.equipment = event.getEquipment();
        return player;
    }
}
