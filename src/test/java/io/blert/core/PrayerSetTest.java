/*
 * Copyright (c) 2026 Alexei Frolov
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class PrayerSetTest {
    @Test
    public void newSetIsEmpty() {
        PrayerSet set = new PrayerSet(Prayer.PRAYER_BOOK_NORMAL);
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertFalse(set.contains(Prayer.THICK_SKIN));
    }

    @Test
    public void addMakesPrayerPresent() {
        PrayerSet set = new PrayerSet(Prayer.PRAYER_BOOK_NORMAL);
        assertTrue(set.add(Prayer.PIETY));
        assertTrue(set.contains(Prayer.PIETY));
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertFalse(set.contains(Prayer.RIGOUR));
    }

    @Test
    public void addIsIdempotent() {
        PrayerSet set = new PrayerSet(Prayer.PRAYER_BOOK_NORMAL);
        set.add(Prayer.AUGURY);
        set.add(Prayer.AUGURY);
        assertEquals(1, set.size());
        assertTrue(set.contains(Prayer.AUGURY));
    }

    @Test
    public void prayersAreStoredIndependently() {
        PrayerSet set = new PrayerSet(Prayer.PRAYER_BOOK_NORMAL);
        set.add(Prayer.PROTECT_FROM_MAGIC);
        set.add(Prayer.PROTECT_FROM_MELEE);
        assertEquals(2, set.size());

        assertTrue(set.remove(Prayer.PROTECT_FROM_MAGIC));
        assertFalse(set.contains(Prayer.PROTECT_FROM_MAGIC));
        assertTrue(set.contains(Prayer.PROTECT_FROM_MELEE));
        assertEquals(1, set.size());
    }

    @Test
    public void clearRemovesEverything() {
        PrayerSet set = new PrayerSet(Prayer.PRAYER_BOOK_NORMAL);
        set.add(Prayer.THICK_SKIN);
        set.add(Prayer.AUGURY);
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void iteratesInAscendingIdOrder() {
        PrayerSet set = new PrayerSet(Prayer.PRAYER_BOOK_NORMAL);
        set.add(Prayer.AUGURY); // id 28
        set.add(Prayer.THICK_SKIN); // id 0
        set.add(Prayer.PROTECT_FROM_MELEE); // id 18

        List<Prayer> iterated = new ArrayList<>();
        for (Prayer prayer : set) {
            iterated.add(prayer);
        }

        assertEquals(Arrays.asList(Prayer.THICK_SKIN, Prayer.PROTECT_FROM_MELEE, Prayer.AUGURY), iterated);
    }

    @Test
    public void toArrayMatchesContents() {
        PrayerSet set = new PrayerSet(Prayer.PRAYER_BOOK_NORMAL);
        set.add(Prayer.PIETY);
        set.add(Prayer.RIGOUR);

        Prayer[] prayers = set.toArray();
        assertEquals(2, prayers.length);
        assertEquals(Prayer.PIETY, prayers[0]);
        assertEquals(Prayer.RIGOUR, prayers[1]);
    }

    @Test
    public void prayerBookRoundTrips() {
        assertEquals(Prayer.PRAYER_BOOK_NORMAL, new PrayerSet(Prayer.PRAYER_BOOK_NORMAL).getPrayerBook());
        assertEquals(3, new PrayerSet(3).getPrayerBook());
    }

    @Test
    public void containsAllRequiresEveryElement() {
        PrayerSet set = new PrayerSet(Prayer.PRAYER_BOOK_NORMAL);
        set.add(Prayer.PIETY);
        set.add(Prayer.RIGOUR);

        assertTrue(set.containsAll(Collections.emptyList()));
        assertTrue(set.containsAll(Collections.singletonList(Prayer.PIETY)));
        assertTrue(set.containsAll(Arrays.asList(Prayer.PIETY, Prayer.RIGOUR)));

        assertFalse(set.containsAll(Arrays.asList(Prayer.PIETY, Prayer.AUGURY)));
        assertFalse(set.containsAll(Collections.singletonList(Prayer.AUGURY)));
    }

    @Test
    public void rejectsPrayersFromAnotherBook() {
        PrayerSet otherBook = new PrayerSet(Prayer.PRAYER_BOOK_NORMAL + 1);
        assertFalse(otherBook.add(Prayer.PROTECT_FROM_MELEE));
        assertFalse(otherBook.contains(Prayer.PROTECT_FROM_MELEE));
    }
}
