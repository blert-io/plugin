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

import lombok.Getter;

@Getter
public abstract class OverallTicks {
    private final int ticks;

    /**
     * An overall time reported by the game server.
     */
    @Getter
    public static final class Server extends OverallTicks {
        /**
         * Whether the time is precise, or rounded to the nearest second.
         */
        private final boolean precise;

        private Server(int ticks, boolean precise) {
            super(ticks);
            this.precise = precise;
        }
    }

    /**
     * An overall time measured by the plugin.
     */
    public static final class Local extends OverallTicks {
        private Local(int ticks) {
            super(ticks);
        }
    }

    public static OverallTicks server(int ticks, boolean precise) {
        return new Server(ticks, precise);
    }

    public static OverallTicks local(int ticks) {
        return new Local(ticks);
    }

    private OverallTicks(int ticks) {
        this.ticks = ticks;
    }
}
