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

    INFERNO_WAVE_1(io.blert.proto.Stage.INFERNO_WAVE_1),
    INFERNO_WAVE_2(io.blert.proto.Stage.INFERNO_WAVE_2),
    INFERNO_WAVE_3(io.blert.proto.Stage.INFERNO_WAVE_3),
    INFERNO_WAVE_4(io.blert.proto.Stage.INFERNO_WAVE_4),
    INFERNO_WAVE_5(io.blert.proto.Stage.INFERNO_WAVE_5),
    INFERNO_WAVE_6(io.blert.proto.Stage.INFERNO_WAVE_6),
    INFERNO_WAVE_7(io.blert.proto.Stage.INFERNO_WAVE_7),
    INFERNO_WAVE_8(io.blert.proto.Stage.INFERNO_WAVE_8),
    INFERNO_WAVE_9(io.blert.proto.Stage.INFERNO_WAVE_9),
    INFERNO_WAVE_10(io.blert.proto.Stage.INFERNO_WAVE_10),
    INFERNO_WAVE_11(io.blert.proto.Stage.INFERNO_WAVE_11),
    INFERNO_WAVE_12(io.blert.proto.Stage.INFERNO_WAVE_12),
    INFERNO_WAVE_13(io.blert.proto.Stage.INFERNO_WAVE_13),
    INFERNO_WAVE_14(io.blert.proto.Stage.INFERNO_WAVE_14),
    INFERNO_WAVE_15(io.blert.proto.Stage.INFERNO_WAVE_15),
    INFERNO_WAVE_16(io.blert.proto.Stage.INFERNO_WAVE_16),
    INFERNO_WAVE_17(io.blert.proto.Stage.INFERNO_WAVE_17),
    INFERNO_WAVE_18(io.blert.proto.Stage.INFERNO_WAVE_18),
    INFERNO_WAVE_19(io.blert.proto.Stage.INFERNO_WAVE_19),
    INFERNO_WAVE_20(io.blert.proto.Stage.INFERNO_WAVE_20),
    INFERNO_WAVE_21(io.blert.proto.Stage.INFERNO_WAVE_21),
    INFERNO_WAVE_22(io.blert.proto.Stage.INFERNO_WAVE_22),
    INFERNO_WAVE_23(io.blert.proto.Stage.INFERNO_WAVE_23),
    INFERNO_WAVE_24(io.blert.proto.Stage.INFERNO_WAVE_24),
    INFERNO_WAVE_25(io.blert.proto.Stage.INFERNO_WAVE_25),
    INFERNO_WAVE_26(io.blert.proto.Stage.INFERNO_WAVE_26),
    INFERNO_WAVE_27(io.blert.proto.Stage.INFERNO_WAVE_27),
    INFERNO_WAVE_28(io.blert.proto.Stage.INFERNO_WAVE_28),
    INFERNO_WAVE_29(io.blert.proto.Stage.INFERNO_WAVE_29),
    INFERNO_WAVE_30(io.blert.proto.Stage.INFERNO_WAVE_30),
    INFERNO_WAVE_31(io.blert.proto.Stage.INFERNO_WAVE_31),
    INFERNO_WAVE_32(io.blert.proto.Stage.INFERNO_WAVE_32),
    INFERNO_WAVE_33(io.blert.proto.Stage.INFERNO_WAVE_33),
    INFERNO_WAVE_34(io.blert.proto.Stage.INFERNO_WAVE_34),
    INFERNO_WAVE_35(io.blert.proto.Stage.INFERNO_WAVE_35),
    INFERNO_WAVE_36(io.blert.proto.Stage.INFERNO_WAVE_36),
    INFERNO_WAVE_37(io.blert.proto.Stage.INFERNO_WAVE_37),
    INFERNO_WAVE_38(io.blert.proto.Stage.INFERNO_WAVE_38),
    INFERNO_WAVE_39(io.blert.proto.Stage.INFERNO_WAVE_39),
    INFERNO_WAVE_40(io.blert.proto.Stage.INFERNO_WAVE_40),
    INFERNO_WAVE_41(io.blert.proto.Stage.INFERNO_WAVE_41),
    INFERNO_WAVE_42(io.blert.proto.Stage.INFERNO_WAVE_42),
    INFERNO_WAVE_43(io.blert.proto.Stage.INFERNO_WAVE_43),
    INFERNO_WAVE_44(io.blert.proto.Stage.INFERNO_WAVE_44),
    INFERNO_WAVE_45(io.blert.proto.Stage.INFERNO_WAVE_45),
    INFERNO_WAVE_46(io.blert.proto.Stage.INFERNO_WAVE_46),
    INFERNO_WAVE_47(io.blert.proto.Stage.INFERNO_WAVE_47),
    INFERNO_WAVE_48(io.blert.proto.Stage.INFERNO_WAVE_48),
    INFERNO_WAVE_49(io.blert.proto.Stage.INFERNO_WAVE_49),
    INFERNO_WAVE_50(io.blert.proto.Stage.INFERNO_WAVE_50),
    INFERNO_WAVE_51(io.blert.proto.Stage.INFERNO_WAVE_51),
    INFERNO_WAVE_52(io.blert.proto.Stage.INFERNO_WAVE_52),
    INFERNO_WAVE_53(io.blert.proto.Stage.INFERNO_WAVE_53),
    INFERNO_WAVE_54(io.blert.proto.Stage.INFERNO_WAVE_54),
    INFERNO_WAVE_55(io.blert.proto.Stage.INFERNO_WAVE_55),
    INFERNO_WAVE_56(io.blert.proto.Stage.INFERNO_WAVE_56),
    INFERNO_WAVE_57(io.blert.proto.Stage.INFERNO_WAVE_57),
    INFERNO_WAVE_58(io.blert.proto.Stage.INFERNO_WAVE_58),
    INFERNO_WAVE_59(io.blert.proto.Stage.INFERNO_WAVE_59),
    INFERNO_WAVE_60(io.blert.proto.Stage.INFERNO_WAVE_60),
    INFERNO_WAVE_61(io.blert.proto.Stage.INFERNO_WAVE_61),
    INFERNO_WAVE_62(io.blert.proto.Stage.INFERNO_WAVE_62),
    INFERNO_WAVE_63(io.blert.proto.Stage.INFERNO_WAVE_63),
    INFERNO_WAVE_64(io.blert.proto.Stage.INFERNO_WAVE_64),
    INFERNO_WAVE_65(io.blert.proto.Stage.INFERNO_WAVE_65),
    INFERNO_WAVE_66(io.blert.proto.Stage.INFERNO_WAVE_66),
    INFERNO_WAVE_67(io.blert.proto.Stage.INFERNO_WAVE_67),
    INFERNO_WAVE_68(io.blert.proto.Stage.INFERNO_WAVE_68),
    INFERNO_WAVE_69(io.blert.proto.Stage.INFERNO_WAVE_69),
    ;

    private final io.blert.proto.Stage protoValue;

    Stage(io.blert.proto.Stage protoValue) {
        this.protoValue = protoValue;
    }

    public io.blert.proto.Stage toProto() {
        return protoValue;
    }
}
