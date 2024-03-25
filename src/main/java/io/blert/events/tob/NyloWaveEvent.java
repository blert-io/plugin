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

package io.blert.events.tob;

import io.blert.challenges.tob.rooms.Room;
import io.blert.events.EventType;
import lombok.Getter;

@Getter
public class NyloWaveEvent extends TobEvent {
    private final int wave;
    private final int nyloCount;
    private final int nyloCap;

    public static NyloWaveEvent spawn(int tick, int wave, int nyloCount, int nyloCap) {
        return new NyloWaveEvent(EventType.NYLO_WAVE_SPAWN, tick, wave, nyloCount, nyloCap);
    }

    public static NyloWaveEvent stall(int tick, int wave, int nyloCount, int nyloCap) {
        return new NyloWaveEvent(EventType.NYLO_WAVE_STALL, tick, wave, nyloCount, nyloCap);
    }

    private NyloWaveEvent(EventType type, int tick, int wave, int nyloCount, int nyloCap) {
        super(type, Room.NYLOCAS, tick, null);
        this.wave = wave;
        this.nyloCount = nyloCount;
        this.nyloCap = nyloCap;
    }

    @Override
    protected String eventDataString() {
        return "wave=" + wave + ", nylos_alive=" + nyloCount;
    }
}
