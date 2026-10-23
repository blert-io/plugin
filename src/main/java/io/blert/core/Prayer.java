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
import net.runelite.api.gameval.VarbitID;

public enum Prayer {
    THICK_SKIN(0, 0, VarbitID.PRAYER_THICKSKIN),
    BURST_OF_STRENGTH(0, 1, VarbitID.PRAYER_BURSTOFSTRENGTH),
    CLARITY_OF_THOUGHT(0, 2, VarbitID.PRAYER_CLARITYOFTHOUGHT),
    SHARP_EYE(0, 3, VarbitID.PRAYER_SHARPEYE),
    MYSTIC_WILL(0, 4, VarbitID.PRAYER_MYSTICWILL),
    ROCK_SKIN(0, 5, VarbitID.PRAYER_ROCKSKIN),
    SUPERHUMAN_STRENGTH(0, 6, VarbitID.PRAYER_SUPERHUMANSTRENGTH),
    IMPROVED_REFLEXES(0, 7, VarbitID.PRAYER_IMPROVEDREFLEXES),
    RAPID_RESTORE(0, 8, VarbitID.PRAYER_RAPIDRESTORE),
    RAPID_HEAL(0, 9, VarbitID.PRAYER_RAPIDHEAL),
    PROTECT_ITEM(0, 10, VarbitID.PRAYER_PROTECTITEM),
    HAWK_EYE(0, 11, VarbitID.PRAYER_HAWKEYE),
    MYSTIC_LORE(0, 12, VarbitID.PRAYER_MYSTICLORE),
    STEEL_SKIN(0, 13, VarbitID.PRAYER_STEELSKIN),
    ULTIMATE_STRENGTH(0, 14, VarbitID.PRAYER_ULTIMATESTRENGTH),
    INCREDIBLE_REFLEXES(0, 15, VarbitID.PRAYER_INCREDIBLEREFLEXES),
    PROTECT_FROM_MAGIC(0, 16, VarbitID.PRAYER_PROTECTFROMMAGIC),
    PROTECT_FROM_MISSILES(0, 17, VarbitID.PRAYER_PROTECTFROMMISSILES),
    PROTECT_FROM_MELEE(0, 18, VarbitID.PRAYER_PROTECTFROMMELEE),
    EAGLE_EYE(0, 19, VarbitID.PRAYER_EAGLEEYE),
    MYSTIC_MIGHT(0, 20, VarbitID.PRAYER_MYSTICMIGHT),
    RETRIBUTION(0, 21, VarbitID.PRAYER_RETRIBUTION),
    REDEMPTION(0, 22, VarbitID.PRAYER_REDEMPTION),
    SMITE(0, 23, VarbitID.PRAYER_SMITE),
    PRESERVE(0, 24, VarbitID.PRAYER_PRESERVE),
    CHIVALRY(0, 25, VarbitID.PRAYER_CHIVALRY),
    PIETY(0, 26, VarbitID.PRAYER_PIETY),
    RIGOUR(0, 27, VarbitID.PRAYER_RIGOUR),
    AUGURY(0, 28, VarbitID.PRAYER_AUGURY),
    DEADEYE(0, 29, VarbitID.PRAYER_DEADEYE),
    MYSTIC_VIGOUR(0, 30, VarbitID.PRAYER_MYSTICVIGOUR),
    ZEAL(0, 31, VarbitID.PRAYER_ZEAL),
    ;

    public static final int PRAYER_BOOK_NORMAL = 0;
    public static final int MAX_PRAYERS_PER_BOOK = 50;

    @Getter(AccessLevel.MODULE)
    private final int prayerBook;

    @Getter(AccessLevel.MODULE)
    private final int id;

    @Getter
    private final int varbit;

    Prayer(int prayerBook, int id, int varbit) {
        this.prayerBook = prayerBook;
        this.id = id;
        this.varbit = varbit;
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
