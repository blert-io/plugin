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

import java.util.concurrent.atomic.AtomicBoolean;

public class DeferredTaskTest extends TestCase {

    public void testTick() {
        AtomicBoolean complete = new AtomicBoolean(false);
        DeferredTask task = new DeferredTask(() -> complete.set(true), 5);
        for (int i = 0; i < 5; i++) {
            assertFalse(complete.get());
            task.tick();
        }
        assertTrue(complete.get());
    }

    public void testCancel() {
        AtomicBoolean complete = new AtomicBoolean(false);
        DeferredTask task = new DeferredTask(() -> complete.set(true), 5);
        task.tick();  // 4
        assertFalse(complete.get());
        task.tick();  // 3
        assertFalse(complete.get());
        task.tick();  // 2
        assertFalse(complete.get());
        task.tick();  // 1
        assertFalse(complete.get());
        task.cancel();
        task.tick();  // 0
        assertFalse(complete.get());
    }

    public void testReset() {
        AtomicBoolean complete = new AtomicBoolean(false);
        DeferredTask task = new DeferredTask(() -> complete.set(true), 3);

        task.tick();  // 2
        assertFalse(complete.get());
        task.tick();  // 1
        assertFalse(complete.get());

        task.reset(3);
        task.tick();  // 2
        assertFalse(complete.get());
        task.tick();  // 1
        assertFalse(complete.get());
        task.tick();  // 0
        assertTrue(complete.get());

        complete.set(false);
        task.reset(1);
        task.tick();  // 0
        assertTrue(complete.get());
    }
}