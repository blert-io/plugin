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

public enum Stage {
    TOB_MAIDEN(io.blert.proto.Stage.TOB_MAIDEN),
    TOB_BLOAT(io.blert.proto.Stage.TOB_BLOAT),
    TOB_NYLOCAS(io.blert.proto.Stage.TOB_NYLOCAS),
    TOB_SOTETSEG(io.blert.proto.Stage.TOB_SOTETSEG),
    TOB_XARPUS(io.blert.proto.Stage.TOB_XARPUS),
    TOB_VERZIK(io.blert.proto.Stage.TOB_VERZIK),

    COX_TEKTON(io.blert.proto.Stage.COX_TEKTON),
    COX_CRABS(io.blert.proto.Stage.COX_CRABS),
    COX_ICE_DEMON(io.blert.proto.Stage.COX_ICE_DEMON),
    COX_SHAMANS(io.blert.proto.Stage.COX_SHAMANS),
    COX_VANGUARDS(io.blert.proto.Stage.COX_VANGUARDS),
    COX_THIEVING(io.blert.proto.Stage.COX_THIEVING),
    COX_VESPULA(io.blert.proto.Stage.COX_VESPULA),
    COX_TIGHTROPE(io.blert.proto.Stage.COX_TIGHTROPE),
    COX_GUARDIANS(io.blert.proto.Stage.COX_GUARDIANS),
    COX_VASA(io.blert.proto.Stage.COX_VASA),
    COX_MYSTICS(io.blert.proto.Stage.COX_MYSTICS),
    COX_MUTTADILE(io.blert.proto.Stage.COX_MUTTADILE),
    COX_OLM(io.blert.proto.Stage.COX_OLM),

    TOA_APMEKEN(io.blert.proto.Stage.TOA_APMEKEN),
    TOA_BABA(io.blert.proto.Stage.TOA_BABA),
    TOA_SCABARAS(io.blert.proto.Stage.TOA_SCABARAS),
    TOA_KEPHRI(io.blert.proto.Stage.TOA_KEPHRI),
    TOA_HET(io.blert.proto.Stage.TOA_HET),
    TOA_AKKHA(io.blert.proto.Stage.TOA_AKKHA),
    TOA_CRONDIS(io.blert.proto.Stage.TOA_CRONDIS),
    TOA_ZEBAK(io.blert.proto.Stage.TOA_ZEBAK),
    TOA_WARDENS(io.blert.proto.Stage.TOA_WARDENS),

    MOKHAIOTL_DELVE_1(io.blert.proto.Stage.MOKHAIOTL_DELVE_1),
    MOKHAIOTL_DELVE_2(io.blert.proto.Stage.MOKHAIOTL_DELVE_2),
    MOKHAIOTL_DELVE_3(io.blert.proto.Stage.MOKHAIOTL_DELVE_3),
    MOKHAIOTL_DELVE_4(io.blert.proto.Stage.MOKHAIOTL_DELVE_4),
    MOKHAIOTL_DELVE_5(io.blert.proto.Stage.MOKHAIOTL_DELVE_5),
    MOKHAIOTL_DELVE_6(io.blert.proto.Stage.MOKHAIOTL_DELVE_6),
    MOKHAIOTL_DELVE_7(io.blert.proto.Stage.MOKHAIOTL_DELVE_7),
    MOKHAIOTL_DELVE_8(io.blert.proto.Stage.MOKHAIOTL_DELVE_8),
    MOKHAIOTL_DELVE_8PLUS(io.blert.proto.Stage.MOKHAIOTL_DELVE_8PLUS),

    COLOSSEUM_WAVE_1(io.blert.proto.Stage.COLOSSEUM_WAVE_1),
    COLOSSEUM_WAVE_2(io.blert.proto.Stage.COLOSSEUM_WAVE_2),
    COLOSSEUM_WAVE_3(io.blert.proto.Stage.COLOSSEUM_WAVE_3),
    COLOSSEUM_WAVE_4(io.blert.proto.Stage.COLOSSEUM_WAVE_4),
    COLOSSEUM_WAVE_5(io.blert.proto.Stage.COLOSSEUM_WAVE_5),
    COLOSSEUM_WAVE_6(io.blert.proto.Stage.COLOSSEUM_WAVE_6),
    COLOSSEUM_WAVE_7(io.blert.proto.Stage.COLOSSEUM_WAVE_7),
    COLOSSEUM_WAVE_8(io.blert.proto.Stage.COLOSSEUM_WAVE_8),
    COLOSSEUM_WAVE_9(io.blert.proto.Stage.COLOSSEUM_WAVE_9),
    COLOSSEUM_WAVE_10(io.blert.proto.Stage.COLOSSEUM_WAVE_10),
    COLOSSEUM_WAVE_11(io.blert.proto.Stage.COLOSSEUM_WAVE_11),
    COLOSSEUM_WAVE_12(io.blert.proto.Stage.COLOSSEUM_WAVE_12),

    ;

    private final io.blert.proto.Stage protoValue;

    Stage(io.blert.proto.Stage protoValue) {
        this.protoValue = protoValue;
    }

    public io.blert.proto.Stage toProto() {
        return protoValue;
    }
}
