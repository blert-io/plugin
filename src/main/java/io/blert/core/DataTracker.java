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
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

public abstract class DataTracker {
    protected final Client client;
    protected final ClientThread clientThread;

    private int startClientTick;

    @Getter
    protected final Stage stage;

    public DataTracker(Client client, ClientThread clientThread, Stage stage) {
        this.client = client;
        this.clientThread = clientThread;
        this.stage = stage;
    }

    /**
     * Starts the tracker.
     */
    public void start() {
        this.startClientTick = client.getTickCount();
    }

    /**
     * Returns number of ticks the tracker has been active.
     *
     * @return The current tick.
     */
    public int getTick() {
        return client.getTickCount() - this.startClientTick;
    }
}
