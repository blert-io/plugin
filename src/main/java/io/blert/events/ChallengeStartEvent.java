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

package io.blert.events;

import io.blert.core.ChallengeMode;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class ChallengeStartEvent extends Event {
    @Getter
    private final List<String> party;
    private final @Nullable ChallengeMode mode;
    @Getter
    private final boolean isSpectator;

    public ChallengeStartEvent(List<String> party, @Nullable ChallengeMode mode, boolean isSpectator) {
        super(EventType.RAID_START);
        this.party = party;
        this.mode = mode;
        this.isSpectator = isSpectator;
    }

    public Optional<ChallengeMode> getMode() {
        return Optional.ofNullable(mode);
    }

    @Override
    protected String eventDataString() {
        return "party=" + party.toString() + ", mode=" + mode;
    }
}