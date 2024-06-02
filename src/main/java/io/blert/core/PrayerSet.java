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

import lombok.Getter;
import lombok.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Represents a set of prayers that can be activated.
 */
@Getter
public class PrayerSet implements Set<Prayer> {
    /**
     * Prayers are stored within a 53-bit integer (Javascript limit), with the highest 3 bits representing the prayer
     * book and the remaining 50 bits representing the prayers that are activated.
     */
    private long value;

    private static final long PRAYER_MASK = 0x1FFFFFFFFFFFFFL;

    public PrayerSet(int prayerBook) {
        this.value = ((long) prayerBook & 0x7) << Prayer.MAX_PRAYERS_PER_BOOK;
    }

    @Override
    public int size() {
        return Long.bitCount(value);
    }

    @Override
    public boolean isEmpty() {
        return (value & PRAYER_MASK) == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Prayer) {
            Prayer prayer = (Prayer) o;
            return (value & (1L << prayer.getId())) != 0;
        }
        return false;
    }

    @NonNull
    @Override
    public Iterator<Prayer> iterator() {
        return new PrayerIterator(value);
    }

    @NonNull
    @Override
    public Prayer @NonNull [] toArray() {
        Prayer[] prayers = new Prayer[size()];
        int i = 0;
        for (Prayer prayer : this) {
            prayers[i++] = prayer;
        }
        return prayers;
    }

    @NonNull
    @Override
    public <T> T @NonNull [] toArray(@NonNull T @NonNull [] ts) {
        if (ts.length < size()) {
            throw new ArrayIndexOutOfBoundsException("Array is too small to store all prayers");
        }
        int i = 0;
        for (Prayer prayer : this) {
            ts[i++] = (T) prayer;
        }
        return ts;
    }

    @Override
    public boolean add(Prayer prayer) {
        if (prayer.getPrayerBook() == getPrayerBook()) {
            value |= 1L << prayer.getId();
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof Prayer) {
            Prayer prayer = (Prayer) o;
            if (prayer.getPrayerBook() == getPrayerBook()) {
                value &= ~(1L << prayer.getId());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
        return collection.stream().anyMatch(this::contains);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends Prayer> collection) {
        boolean changed = false;
        for (Prayer prayer : collection) {
            changed |= add(prayer);
        }
        return changed;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> collection) {
        return false;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        return false;
    }

    @Override
    public void clear() {
        value &= ~PRAYER_MASK;
    }

    public static class PrayerIterator implements Iterator<Prayer> {
        private long remaining;

        private PrayerIterator(long value) {
            remaining = value & PRAYER_MASK;
        }

        @Override
        public boolean hasNext() {
            return remaining != 0;
        }

        @Override
        public Prayer next() {
            long id = Long.numberOfTrailingZeros(remaining);
            remaining &= ~(1L << id);
            return Prayer.withId((int) id);
        }
    }

    public int getPrayerBook() {
        return (int) (value >> Prayer.MAX_PRAYERS_PER_BOOK) & 0x7;
    }
}
