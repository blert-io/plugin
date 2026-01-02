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

import java.util.Optional;

public enum ChallengeMode {
    NO_MODE(0),

    TOB_ENTRY(10),
    TOB_REGULAR(11),
    TOB_HARD(12),

    COX_REGULAR(20),
    COX_CHALLENGE(21),

    TOA_ENTRY(30),
    TOA_NORMAL(31),
    TOA_EXPERT(32),

    ;

    @Getter
    private final int id;

    ChallengeMode(int id) {
        this.id = id;
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
