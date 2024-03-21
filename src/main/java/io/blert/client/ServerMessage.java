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

package io.blert.client;

import com.google.gson.Gson;
import io.blert.json.Event;
import io.blert.challenges.tob.Mode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Setter
public class ServerMessage {
    enum Type {
        HEARTBEAT_PING,
        HEARTBEAT_PONG,
        CONNECTION_RESPONSE,
        RAID_HISTORY_REQUEST,
        RAID_HISTORY_RESPONSE,
        RAID_START_RESPONSE,
        RAID_EVENTS,
    }

    @Getter
    @AllArgsConstructor
    static class User {
        private String id;
        private String name;
    }

    public enum RaidStatus {
        IN_PROGRESS,
        COMPLETED,
        MAIDEN_RESET,
        MAIDEN_WIPE,
        BLOAT_RESET,
        BLOAT_WIPE,
        NYLO_RESET,
        NYLO_WIPE,
        SOTE_RESET,
        SOTE_WIPE,
        XARPUS_RESET,
        XARPUS_WIPE,
        VERZIK_WIPE;

        @Override
        public String toString() {
            switch (this) {
                case IN_PROGRESS:
                    return "In Progress";
                case COMPLETED:
                    return "Completion";
                case MAIDEN_RESET:
                    return "Maiden Reset";
                case MAIDEN_WIPE:
                    return "Maiden Wipe";
                case BLOAT_RESET:
                    return "Bloat Reset";
                case BLOAT_WIPE:
                    return "Bloat Wipe";
                case NYLO_RESET:
                    return "Nylocas Reset";
                case NYLO_WIPE:
                    return "Nylocas Wipe";
                case SOTE_RESET:
                    return "Sotetseg Reset";
                case SOTE_WIPE:
                    return "Sotetseg Wipe";
                case XARPUS_RESET:
                    return "Xarpus Reset";
                case XARPUS_WIPE:
                    return "Xarpus Wipe";
                case VERZIK_WIPE:
                    return "Verzik Wipe";
                default:
                    return "Unknown";
            }
        }

        public boolean isCompletion() {
            return this == COMPLETED;
        }

        public boolean isWipe() {
            return this == MAIDEN_WIPE || this == BLOAT_WIPE || this == NYLO_WIPE
                    || this == SOTE_WIPE || this == XARPUS_WIPE || this == VERZIK_WIPE;
        }

        public boolean isReset() {
            return this == MAIDEN_RESET || this == BLOAT_RESET || this == NYLO_RESET
                    || this == SOTE_RESET || this == XARPUS_RESET;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class PastRaid {
        private String id;
        private RaidStatus status;
        private Mode mode;
        private List<String> party;
    }

    private Type type;
    private @Nullable User user;
    private @Nullable String raidId;

    /**
     * Serialized JSON string of raid events.
     */
    private @Nullable List<Event> events;

    private @Nullable List<PastRaid> history;

    ServerMessage(Type type) {
        this.type = type;
    }

    public String encode() {
        return new Gson().toJson(this);
    }
}
