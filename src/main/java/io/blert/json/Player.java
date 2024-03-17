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

package io.blert.json;

import io.blert.events.PlayerUpdateEvent;
import io.blert.raid.EquipmentSlot;
import io.blert.raid.Hitpoints;
import io.blert.raid.Item;
import io.blert.raid.SkillLevel;
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
    private @Nullable SkillLevel prayer = null;
    private @Nullable SkillLevel attack = null;
    private @Nullable SkillLevel strength = null;
    private @Nullable SkillLevel defence = null;
    private @Nullable SkillLevel ranged = null;
    private @Nullable SkillLevel magic = null;
    private int offCooldownTick;

    public Player(String name) {
        this.name = name;
    }

    public static Player fromBlertEvent(PlayerUpdateEvent event) {
        Player player = new Player(event.getUsername());

        event.getHitpoints().ifPresent(player::setHitpoints);
        event.getPrayer().ifPresent(player::setPrayer);
        event.getAttack().ifPresent(player::setAttack);
        event.getStrength().ifPresent(player::setStrength);
        event.getDefence().ifPresent(player::setDefence);
        event.getRanged().ifPresent(player::setRanged);
        event.getMagic().ifPresent(player::setMagic);

        player.equipment = event.getEquipment();
        player.offCooldownTick = event.getOffCooldownTick();

        return player;
    }
}
