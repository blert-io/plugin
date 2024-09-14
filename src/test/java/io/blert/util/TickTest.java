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

package io.blert.util;

import junit.framework.TestCase;
import org.apache.commons.lang3.tuple.Pair;

public class TickTest extends TestCase {
    public void testAsTimeString() {
        assertEquals("0:00.00", Tick.asTimeString(0));
        assertEquals("0:00.60", Tick.asTimeString(1));
        assertEquals("0:06.00", Tick.asTimeString(10));
        assertEquals("1:00.00", Tick.asTimeString(100));
        assertEquals("1:00.60", Tick.asTimeString(101));
        assertEquals("2:57.60", Tick.asTimeString(296));
        assertEquals("9:59.40", Tick.asTimeString(999));
        assertEquals("10:00.00", Tick.asTimeString(1000));
    }

    public void testFromTimeString() {
        assertTrue(Tick.fromTimeString("").isEmpty());

        assertEquals(Pair.of(0, true), Tick.fromTimeString("0:00.00").orElseThrow());
        assertEquals(Pair.of(1, true), Tick.fromTimeString("0:00.60").orElseThrow());
        assertEquals(Pair.of(10, true), Tick.fromTimeString("0:06.00").orElseThrow());
        assertEquals(Pair.of(100, true), Tick.fromTimeString("1:00.00").orElseThrow());
        assertEquals(Pair.of(101, true), Tick.fromTimeString("1:00.60").orElseThrow());
        assertEquals(Pair.of(296, true), Tick.fromTimeString("2:57.60").orElseThrow());
        assertEquals(Pair.of(999, true), Tick.fromTimeString("9:59.40").orElseThrow());
        assertEquals(Pair.of(1000, true), Tick.fromTimeString("10:00.00").orElseThrow());

        // Non-precise time strings without centiseconds.
        assertEquals(Pair.of(0, false), Tick.fromTimeString("0:00").orElseThrow());
        assertEquals(Pair.of(2, false), Tick.fromTimeString("0:01").orElseThrow());
        assertEquals(Pair.of(4, false), Tick.fromTimeString("0:02").orElseThrow());
        assertEquals(Pair.of(10, false), Tick.fromTimeString("0:06").orElseThrow());
        assertEquals(Pair.of(99, false), Tick.fromTimeString("0:59").orElseThrow());
        assertEquals(Pair.of(100, false), Tick.fromTimeString("1:00").orElseThrow());
        assertEquals(Pair.of(295, false), Tick.fromTimeString("2:57").orElseThrow());
        assertEquals(Pair.of(297, false), Tick.fromTimeString("2:58").orElseThrow());
        assertEquals(Pair.of(999, false), Tick.fromTimeString("9:59").orElseThrow());
        assertEquals(Pair.of(1000, false), Tick.fromTimeString("10:00").orElseThrow());
    }
}