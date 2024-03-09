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

package io.blert.raid;

import junit.framework.TestCase;

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
        assertEquals(0, Tick.fromTimeString(""));
        assertEquals(0, Tick.fromTimeString("0:00.00"));
        assertEquals(1, Tick.fromTimeString("0:00.60"));
        assertEquals(10, Tick.fromTimeString("0:06.00"));
        assertEquals(100, Tick.fromTimeString("1:00.00"));
        assertEquals(101, Tick.fromTimeString("1:00.60"));
        assertEquals(296, Tick.fromTimeString("2:57.60"));
        assertEquals(999, Tick.fromTimeString("9:59.40"));
        assertEquals(1000, Tick.fromTimeString("10:00.00"));

        // Non-precise time strings without centiseconds.
        assertEquals(0, Tick.fromTimeString("0:00"));
        assertEquals(2, Tick.fromTimeString("0:01"));
        assertEquals(4, Tick.fromTimeString("0:02"));
        assertEquals(10, Tick.fromTimeString("0:06"));
        assertEquals(99, Tick.fromTimeString("0:59"));
        assertEquals(100, Tick.fromTimeString("1:00"));
        assertEquals(295, Tick.fromTimeString("2:57"));
        assertEquals(297, Tick.fromTimeString("2:58"));
        assertEquals(999, Tick.fromTimeString("9:59"));
        assertEquals(1000, Tick.fromTimeString("10:00"));
    }
}