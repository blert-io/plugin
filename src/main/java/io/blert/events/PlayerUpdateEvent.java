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

import io.blert.core.*;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
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
    private @Nullable SkillLevel prayer = null;
    private @Nullable SkillLevel attack = null;
    private @Nullable SkillLevel strength = null;
    private @Nullable SkillLevel defence = null;
    private @Nullable SkillLevel ranged = null;
    private @Nullable SkillLevel magic = null;

    @Getter
    private final Map<EquipmentSlot, Item> equipment = new HashMap<>();
    @Getter
    private int offCooldownTick = 0;

    /**
     * Returns a PlayerUpdateEvent populated with information about a player in the raid.
     *
     * @param stage  Stage during which the event occurred.
     * @param tick   Room tick at which the event occurred.
     * @param client Local client instance.
     * @param raider The player in question.
     * @return Event containing information about the queried player.
     */
    public static PlayerUpdateEvent fromRaider(Stage stage, int tick, WorldPoint point, Client client, Raider raider) {
        Source source = raider.isLocalPlayer() ? Source.PRIMARY : Source.SECONDARY;

        PlayerUpdateEvent evt = new PlayerUpdateEvent(stage, tick, point, source, raider.getUsername());
        evt.equipment.putAll(raider.getEquipment());
        evt.offCooldownTick = raider.getOffCooldownTick();

        if (raider.isLocalPlayer()) {
            evt.hitpoints = new Hitpoints(
                    client.getBoostedSkillLevel(net.runelite.api.Skill.HITPOINTS),
                    client.getRealSkillLevel(net.runelite.api.Skill.HITPOINTS));
            evt.prayer = new SkillLevel(
                    Skill.PRAYER,
                    client.getBoostedSkillLevel(net.runelite.api.Skill.PRAYER),
                    client.getRealSkillLevel(net.runelite.api.Skill.PRAYER));
            evt.attack = new SkillLevel(
                    Skill.ATTACK,
                    client.getBoostedSkillLevel(net.runelite.api.Skill.ATTACK),
                    client.getRealSkillLevel(net.runelite.api.Skill.ATTACK));
            evt.strength = new SkillLevel(
                    Skill.STRENGTH,
                    client.getBoostedSkillLevel(net.runelite.api.Skill.STRENGTH),
                    client.getRealSkillLevel(net.runelite.api.Skill.STRENGTH));
            evt.defence = new SkillLevel(
                    Skill.DEFENCE,
                    client.getBoostedSkillLevel(net.runelite.api.Skill.DEFENCE),
                    client.getRealSkillLevel(net.runelite.api.Skill.DEFENCE));
            evt.ranged = new SkillLevel(
                    Skill.RANGED,
                    client.getBoostedSkillLevel(net.runelite.api.Skill.RANGED),
                    client.getRealSkillLevel(net.runelite.api.Skill.RANGED));
            evt.magic = new SkillLevel(
                    Skill.MAGIC,
                    client.getBoostedSkillLevel(net.runelite.api.Skill.MAGIC),
                    client.getRealSkillLevel(net.runelite.api.Skill.MAGIC));
        }

        return evt;
    }


    private PlayerUpdateEvent(Stage stage, int tick, WorldPoint point, Source source, String username) {
        super(EventType.PLAYER_UPDATE, stage, tick, point);
        this.source = source;
        this.username = username;
    }

    /**
     * Returns the player's hitpoints, if available.
     */
    public Optional<Hitpoints> getHitpoints() {
        return Optional.ofNullable(hitpoints);
    }

    public Optional<SkillLevel> getPrayer() {
        return Optional.ofNullable(prayer);
    }

    public Optional<SkillLevel> getAttack() {
        return Optional.ofNullable(attack);
    }

    public Optional<SkillLevel> getStrength() {
        return Optional.ofNullable(strength);
    }

    public Optional<SkillLevel> getDefence() {
        return Optional.ofNullable(defence);
    }

    public Optional<SkillLevel> getRanged() {
        return Optional.ofNullable(ranged);
    }

    public Optional<SkillLevel> getMagic() {
        return Optional.ofNullable(magic);
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
