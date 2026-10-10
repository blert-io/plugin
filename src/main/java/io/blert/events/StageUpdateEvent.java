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

import io.blert.core.Stage;
import java.util.Optional;
import lombok.Getter;

@Getter
public class StageUpdateEvent extends Event {
    public enum Status {
        ENTERED,
        STARTED,
        COMPLETED,
        WIPED,
    }

    private final Status status;
    private final boolean accurate;
    private final Optional<Integer> inGameTicks;
    private final boolean gameTicksPrecise;
    private final Optional<Integer> recordingConfigVersion;

    public static StageUpdateEvent entered(Stage stage) {
        return new StageUpdateEvent(stage, 0, Status.ENTERED, false, Optional.empty(), false, Optional.empty());
    }

    public static StageUpdateEvent started(Stage stage, int recordingConfigVersion) {
        return new StageUpdateEvent(
                stage, 0, Status.STARTED, false, Optional.empty(), false, Optional.of(recordingConfigVersion));
    }

    public static StageUpdateEvent completed(
            Stage stage, int tick, boolean accurate, Optional<Integer> inGameTicks, boolean gameTicksPrecise) {
        return new StageUpdateEvent(
                stage, tick, Status.COMPLETED, accurate, inGameTicks, gameTicksPrecise, Optional.empty());
    }

    public static StageUpdateEvent wiped(Stage stage, int tick) {
        return new StageUpdateEvent(stage, tick, Status.WIPED, false, Optional.empty(), false, Optional.empty());
    }

    private StageUpdateEvent(
            Stage stage,
            int tick,
            Status status,
            boolean accurate,
            Optional<Integer> inGameTicks,
            boolean gameTicksPrecise,
            Optional<Integer> recordingConfigVersion) {
        super(EventType.STAGE_UPDATE, stage, tick, null);
        this.status = status;
        this.accurate = accurate;
        this.inGameTicks = inGameTicks;
        this.gameTicksPrecise = gameTicksPrecise;
        this.recordingConfigVersion = recordingConfigVersion;
    }

    @Override
    protected String eventDataString() {
        return "status=" + status;
    }
}
