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

import java.util.Optional;

public enum ChallengeMode {
    NO_MODE(io.blert.proto.ChallengeMode.NO_MODE),

    TOB_ENTRY(io.blert.proto.ChallengeMode.TOB_ENTRY),
    TOB_REGULAR(io.blert.proto.ChallengeMode.TOB_REGULAR),
    TOB_HARD(io.blert.proto.ChallengeMode.TOB_HARD),

    COX_REGULAR(io.blert.proto.ChallengeMode.COX_REGULAR),
    COX_CHALLENGE(io.blert.proto.ChallengeMode.COX_CHALLENGE),

    TOA_ENTRY(io.blert.proto.ChallengeMode.TOA_ENTRY),
    TOA_NORMAL(io.blert.proto.ChallengeMode.TOA_NORMAL),
    TOA_EXPERT(io.blert.proto.ChallengeMode.TOA_EXPERT),

    ;

    private final io.blert.proto.ChallengeMode protoValue;

    ChallengeMode(io.blert.proto.ChallengeMode protoValue) {
        this.protoValue = protoValue;
    }

    public io.blert.proto.ChallengeMode toProto() {
        return protoValue;
    }

    public static Optional<ChallengeMode> parseTob(String string) {
        switch (string.toLowerCase()) {
            case "entry":
            case "story":
                return Optional.of(TOB_ENTRY);
            case "normal":
            case "regular":
                return Optional.of(TOB_REGULAR);
            case "hard":
                return Optional.of(TOB_HARD);
            default:
                return Optional.empty();
        }
    }
}
