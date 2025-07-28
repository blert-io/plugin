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

public enum Challenge {
    TOB("Theatre of Blood", io.blert.proto.Challenge.TOB),
    COX("Chambers of Xeric", io.blert.proto.Challenge.COX),
    TOA("Tombs of Amascut", io.blert.proto.Challenge.TOA),
    COLOSSEUM("Colosseum", io.blert.proto.Challenge.COLOSSEUM),
    INFERNO("Inferno", io.blert.proto.Challenge.INFERNO),
    MOKHAIOTL("Mokhaiotl", io.blert.proto.Challenge.MOKHAIOTL),
    ;

    @Getter
    private final String name;
    private final io.blert.proto.Challenge protoValue;

    public io.blert.proto.Challenge toProto() {
        return protoValue;
    }

    Challenge(String name, io.blert.proto.Challenge protoValue) {
        this.name = name;
        this.protoValue = protoValue;
    }
}
