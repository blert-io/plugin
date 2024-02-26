/*
 * Copyright (c) 2024 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.blert.events;

import io.blert.raid.EquipmentSlot;
import io.blert.raid.Hitpoints;
import io.blert.raid.Item;
import io.blert.raid.Raider;
import io.blert.raid.rooms.Room;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.util.*;

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
    @Getter
    private int offCooldownTick = 0;

    /**
     * Returns a PlayerUpdateEvent populated with information about a player in the raid.
     *
     * @param room   Room in which the event occurred.
     * @param tick   Room tick at which the event occurred.
     * @param client Local client instance.
     * @param raider The player in question.
     * @return Event containing information about the queried player.
     */
    public static PlayerUpdateEvent fromRaider(Room room, int tick, Client client, Raider raider) {
        Player player = Objects.requireNonNull(raider.getPlayer());
        WorldPoint point = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
        Source source = raider.isLocalPlayer() ? Source.PRIMARY : Source.SECONDARY;

        PlayerUpdateEvent evt = new PlayerUpdateEvent(room, tick, point, source, raider.getUsername());
        evt.equipment.putAll(raider.getEquipment());
        evt.offCooldownTick = raider.getOffCooldownTick();

        if (raider.isLocalPlayer()) {
            evt.hitpoints = new Hitpoints(
                    client.getBoostedSkillLevel(Skill.HITPOINTS),
                    client.getRealSkillLevel(Skill.HITPOINTS));
        }

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
