package com.blert.events;

import com.blert.raid.EquipmentSlot;
import com.blert.raid.Hitpoints;
import com.blert.raid.Item;
import com.blert.raid.rooms.Room;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public class PlayerUpdateEvent extends Event {
    /**
     * Type of source for the player update data, indicating the level of confidence about its completeness and
     * accuracy.
     */
    public enum Source {
        /**
         * Data originates directly from the client the player is logged into. Fully accurate.
         */
        PRIMARY,

        /**
         * Data originates from a separate client. Some information may be missing or inaccurate.
         */
        SECONDARY,
    }

    private final Source source;

    @Getter
    private final String username;

    private @Nullable Hitpoints hitpoints = null;

    @Getter
    private final Map<EquipmentSlot, Item> equipment = new HashMap<>();

    /**
     * Returns a PlayerUpdateEvent populated with information about the logged-in player, read directly from the local
     * client.
     *
     * @param room   Room in which the event occurred.
     * @param tick   Room tick at which the event occurred.
     * @param client Local client instance.
     * @return Event containing information about the local player.
     */
    public static PlayerUpdateEvent fromLocalPlayer(Room room, int tick, Client client) {
        Player player = client.getLocalPlayer();
        WorldPoint point = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
        PlayerUpdateEvent evt = new PlayerUpdateEvent(room, tick, point, Source.PRIMARY, player.getName());
        evt.hitpoints = new Hitpoints(
                client.getBoostedSkillLevel(Skill.HITPOINTS),
                client.getRealSkillLevel(Skill.HITPOINTS));


        var equippedItems = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equippedItems != null) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                var item = equippedItems.getItem(slot.getInventorySlotIndex());
                if (item != null) {
                    var comp = client.getItemDefinition(item.getId());
                    evt.equipment.put(slot, new Item(item.getId(), comp.getName(), item.getQuantity()));
                }
            }
        } else {
            getVisibleEquipment(evt.equipment, client, player);
        }

        return evt;
    }

    /**
     * Returns a PlayerUpdateEvent populated with partial information about another player which is observable from
     * the location client.
     *
     * @param room   Room in which the event occurred.
     * @param tick   Room tick at which the event occurred.
     * @param client Local client instance.
     * @param player The player in question.
     * @return Event containing information about the queried player.
     */
    public static PlayerUpdateEvent fromPlayer(Room room, int tick, Client client, Player player) {
        WorldPoint point = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
        PlayerUpdateEvent evt = new PlayerUpdateEvent(room, tick, point, Source.SECONDARY, player.getName());
        getVisibleEquipment(evt.equipment, client, player);
        return evt;
    }

    private PlayerUpdateEvent(Room room, int tick, WorldPoint point, Source source, String username) {
        super(EventType.PLAYER_UPDATE, room, tick, point);
        this.source = source;
        this.username = username;
    }

    /**
     * Returns the player's hitpoints, if available.
     */
    public Optional<Hitpoints> getHitpoints() {
        return Optional.ofNullable(hitpoints);
    }

    private static void getVisibleEquipment(Map<EquipmentSlot, Item> equipment, Client client, Player player) {
        Arrays.stream(EquipmentSlot.values()).filter(slot -> slot.getKitType() != null).forEach(slot -> {
            int id = player.getPlayerComposition().getEquipmentId(slot.getKitType());
            if (id != -1) {
                var comp = client.getItemDefinition(id);
                equipment.put(slot, new Item(comp.getId(), comp.getName(), 1));
            }
        });
    }

    @Override
    protected String eventDataString() {
        StringBuilder string = new StringBuilder("player=(");
        string.append("name=").append(getUsername());

        getHitpoints().ifPresent(hp -> string.append(", hp=").append(hp));

        string.append(')');
        return string.toString();
    }
}
