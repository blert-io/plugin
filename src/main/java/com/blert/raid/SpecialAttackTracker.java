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

package com.blert.raid;

import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Hitsplat;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;

import java.util.function.Consumer;

@Slf4j

public class SpecialAttackTracker {
    NPC specTarget = null;
    Item specWeapon = null;
    Hitsplat specHitsplat = null;
    int expectedHitsplatTick;
    int specialPercent = -1;

    @Getter
    @AllArgsConstructor
    public static class SpecialAttack {
        NPC target;
        Item weapon;
        int damage;
    }

    final Consumer<SpecialAttack> specCallback;

    static final ImmutableSet<Integer> TRACKED_SPECIAL_WEAPONS = ImmutableSet.of(
            ItemID.DRAGON_WARHAMMER,
            ItemID.DRAGON_WARHAMMER_CR,
            ItemID.BANDOS_GODSWORD,
            ItemID.BANDOS_GODSWORD_OR);

    public SpecialAttackTracker(Consumer<SpecialAttack> specCallback) {
        this.specCallback = specCallback;
    }

    /**
     * Sets the special attack percentage to the specified value.
     *
     * @param percent The new special attack percentage.
     * @return The old percentage before the update.
     */
    public int updateSpecialPercent(int percent) {
        int old = specialPercent;
        specialPercent = percent;
        return old;
    }

    public void recordSpecialUsed(NPC target, Item weapon, int specTick) {
        if (!TRACKED_SPECIAL_WEAPONS.contains(weapon.getId())) {
            return;
        }

        specTarget = TobNpc.withId(target.getId()).isPresent() ? target : null;
        if (specTarget != null) {
            expectedHitsplatTick = specTick + hitDelay();
            specWeapon = weapon;
        }
    }

    public void recordHitsplat(NPC target, Hitsplat hitsplat, int hitsplatTick) {
        if (specTarget == null || target != specTarget) {
            return;
        }

        if (TobNpc.withId(target.getId()).isEmpty()) {
            return;
        }

        if (hitsplatTick == expectedHitsplatTick) {
            specHitsplat = hitsplat;
        }
    }

    public void processPendingSpecial() {
        if (specTarget == null || specHitsplat == null) {
            return;
        }

        specCallback.accept(new SpecialAttack(specTarget, specWeapon, specHitsplat.getAmount()));

        specTarget = null;
        specWeapon = null;
        specHitsplat = null;
        expectedHitsplatTick = 0;
    }

    private int hitDelay() {
        // TODO(frolv): This should be set per weapon, but only melee weapons are tracked right now.
        return 1;
    }
}
