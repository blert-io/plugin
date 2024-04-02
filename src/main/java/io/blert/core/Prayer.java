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

package io.blert.core;

import lombok.AccessLevel;
import lombok.Getter;

public enum Prayer {
    THICK_SKIN(0, 0, net.runelite.api.Prayer.THICK_SKIN),
    BURST_OF_STRENGTH(0, 1, net.runelite.api.Prayer.BURST_OF_STRENGTH),
    CLARITY_OF_THOUGHT(0, 2, net.runelite.api.Prayer.CLARITY_OF_THOUGHT),
    SHARP_EYE(0, 3, net.runelite.api.Prayer.SHARP_EYE),
    MYSTIC_WILL(0, 4, net.runelite.api.Prayer.MYSTIC_WILL),
    ROCK_SKIN(0, 5, net.runelite.api.Prayer.ROCK_SKIN),
    SUPERHUMAN_STRENGTH(0, 6, net.runelite.api.Prayer.SUPERHUMAN_STRENGTH),
    IMPROVED_REFLEXES(0, 7, net.runelite.api.Prayer.IMPROVED_REFLEXES),
    RAPID_RESTORE(0, 8, net.runelite.api.Prayer.RAPID_RESTORE),
    RAPID_HEAL(0, 9, net.runelite.api.Prayer.RAPID_HEAL),
    PROTECT_ITEM(0, 10, net.runelite.api.Prayer.PROTECT_ITEM),
    HAWK_EYE(0, 11, net.runelite.api.Prayer.HAWK_EYE),
    MYSTIC_LORE(0, 12, net.runelite.api.Prayer.MYSTIC_LORE),
    STEEL_SKIN(0, 13, net.runelite.api.Prayer.STEEL_SKIN),
    ULTIMATE_STRENGTH(0, 14, net.runelite.api.Prayer.ULTIMATE_STRENGTH),
    INCREDIBLE_REFLEXES(0, 15, net.runelite.api.Prayer.INCREDIBLE_REFLEXES),
    PROTECT_FROM_MAGIC(0, 16, net.runelite.api.Prayer.PROTECT_FROM_MAGIC),
    PROTECT_FROM_MISSILES(0, 17, net.runelite.api.Prayer.PROTECT_FROM_MISSILES),
    PROTECT_FROM_MELEE(0, 18, net.runelite.api.Prayer.PROTECT_FROM_MELEE),
    EAGLE_EYE(0, 19, net.runelite.api.Prayer.EAGLE_EYE),
    MYSTIC_MIGHT(0, 20, net.runelite.api.Prayer.MYSTIC_MIGHT),
    RETRIBUTION(0, 21, net.runelite.api.Prayer.RETRIBUTION),
    REDEMPTION(0, 22, net.runelite.api.Prayer.REDEMPTION),
    SMITE(0, 23, net.runelite.api.Prayer.SMITE),
    PRESERVE(0, 24, net.runelite.api.Prayer.PRESERVE),
    CHIVALRY(0, 25, net.runelite.api.Prayer.CHIVALRY),
    PIETY(0, 26, net.runelite.api.Prayer.PIETY),
    RIGOUR(0, 27, net.runelite.api.Prayer.RIGOUR),
    AUGURY(0, 28, net.runelite.api.Prayer.AUGURY),
    ;

    public static final int PRAYER_BOOK_NORMAL = 0;
    public static final int MAX_PRAYERS_PER_BOOK = 50;

    @Getter(AccessLevel.MODULE)
    private final int prayerBook;
    @Getter(AccessLevel.MODULE)
    private final int id;
    @Getter
    private final net.runelite.api.Prayer runelitePrayer;

    Prayer(int prayerBook, int id, net.runelite.api.Prayer runelitePrayer) {
        this.prayerBook = prayerBook;
        this.id = id;
        this.runelitePrayer = runelitePrayer;
    }

    public static Prayer withId(int id) {
        for (Prayer prayer : values()) {
            if (prayer.id == id) {
                return prayer;
            }
        }
        return null;
    }
}
