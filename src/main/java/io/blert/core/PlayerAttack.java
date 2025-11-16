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

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ItemID;

import java.util.*;
import java.util.stream.Collectors;

public enum PlayerAttack {
    ABYSSAL_BLUDGEON(ItemID.ABYSSAL_BLUDGEON, 3298, 4, io.blert.proto.PlayerAttack.ABYSSAL_BLUDGEON),
    ACCURSED_SCEPTRE_AUTO(
            new int[]{ItemID.ACCURSED_SCEPTRE, ItemID.ACCURSED_SCEPTRE_A},
            1167,
            4,
            io.blert.proto.PlayerAttack.ACCURSED_SCEPTRE_AUTO
    ),
    ACCURSED_SCEPTRE_SPEC(
            new int[]{ItemID.ACCURSED_SCEPTRE, ItemID.ACCURSED_SCEPTRE_A},
            9961,
            4,
            io.blert.proto.PlayerAttack.ACCURSED_SCEPTRE_SPEC
    ),
    AGS_SPEC(
            new int[]{ItemID.ARMADYL_GODSWORD, ItemID.ARMADYL_GODSWORD_OR, ItemID.ARMADYL_GODSWORD_DEADMAN},
            new int[]{7644, 7645},
            6,
            io.blert.proto.PlayerAttack.AGS_SPEC
    ),
    ARCLIGHT_AUTO(ItemID.ARCLIGHT, new int[]{386, 390}, 4, io.blert.proto.PlayerAttack.ARCLIGHT_AUTO),
    ARCLIGHT_SPEC(ItemID.ARCLIGHT, 2890, 4, io.blert.proto.PlayerAttack.ARCLIGHT_SPEC),
    ATLATL_AUTO(ItemID.ECLIPSE_ATLATL, 11057, 3, io.blert.proto.PlayerAttack.ATLATL_AUTO),
    ATLATL_SPEC(ItemID.ECLIPSE_ATLATL, 11060, 3, io.blert.proto.PlayerAttack.ATLATL_SPEC),
    BGS_SPEC(
            new int[]{ItemID.BANDOS_GODSWORD, ItemID.BANDOS_GODSWORD_OR},
            new int[]{7642, 7643},
            6,
            io.blert.proto.PlayerAttack.BGS_SPEC
    ),
    BLOWPIPE(
            new int[]{ItemID.TOXIC_BLOWPIPE, ItemID.BLAZING_BLOWPIPE},
            new int[]{5061, 10656},
            2,
            io.blert.proto.PlayerAttack.BLOWPIPE,
            true
    ),
    BLOWPIPE_SPEC(
            new int[]{ItemID.TOXIC_BLOWPIPE, ItemID.BLAZING_BLOWPIPE},
            new int[]{},  // This is handled specially instead of being assigned automatically.
            2,
            io.blert.proto.PlayerAttack.BLOWPIPE_SPEC,
            true
    ),
    BOWFA(new int[]{ItemID.BOW_OF_FAERDHINEN, ItemID.BOW_OF_FAERDHINEN_C, 25884, 25886, 25888, 25890, 25892, 25894, 25896},
            426,
            4,
            io.blert.proto.PlayerAttack.BOWFA
    ),
    BURNING_CLAW_SCRATCH(
            ItemID.BURNING_CLAWS,
            new int[]{393, 1067},
            4,
            io.blert.proto.PlayerAttack.BURNING_CLAW_SCRATCH
    ),
    BURNING_CLAW_SPEC(ItemID.BURNING_CLAWS, 11140, 4, io.blert.proto.PlayerAttack.BURNING_CLAW_SPEC),
    CHALLY_SWIPE(
            ItemID.CRYSTAL_HALBERD,
            440,
            7,
            io.blert.proto.PlayerAttack.CHALLY_SWIPE
    ),
    CHALLY_SPEC(ItemID.CRYSTAL_HALBERD, 1203, 7, io.blert.proto.PlayerAttack.CHALLY_SPEC),
    CHIN_BLACK(ItemID.BLACK_CHINCHOMPA, 7618, 3, io.blert.proto.PlayerAttack.CHIN_BLACK),
    CHIN_GREY(ItemID.CHINCHOMPA, 7618, 3, io.blert.proto.PlayerAttack.CHIN_GREY),
    CHIN_RED(ItemID.RED_CHINCHOMPA_10034, 7618, 3, io.blert.proto.PlayerAttack.CHIN_RED),
    // CLAW_SCRATCH must come before CLAW_SPEC as it is the default assumption when the animation is unknown.
    CLAW_SCRATCH(
            new int[]{ItemID.DRAGON_CLAWS, ItemID.DRAGON_CLAWS_CR},
            new int[]{393, 1067},
            4,
            io.blert.proto.PlayerAttack.CLAW_SCRATCH
    ),
    CLAW_SPEC(
            new int[]{ItemID.DRAGON_CLAWS, ItemID.DRAGON_CLAWS_CR},
            7514,
            4,
            io.blert.proto.PlayerAttack.CLAW_SPEC
    ),
    DARK_DEMONBANE(
            new int[]{
                    ItemID.SLAYERS_STAFF,
                    ItemID.SLAYERS_STAFF_E,
                    ItemID.BLUE_MOON_SPEAR,
                    ItemID.BLUE_MOON_SPEAR_29849,
                    ItemID.MASTER_WAND,
                    ItemID.TOXIC_STAFF_OF_THE_DEAD,
                    ItemID.KODAI_WAND,
                    ItemID.PURGING_STAFF,
            },
            8977,
            5,
            io.blert.proto.PlayerAttack.DARK_DEMONBANE
    ),
    DARKLIGHT_AUTO(
            new int[]{ItemID.DARKLIGHT, ItemID.DARKLIGHT_8281},
            new int[]{386, 390},
            4,
            io.blert.proto.PlayerAttack.DARKLIGHT_AUTO
    ),
    DARKLIGHT_SPEC(
            new int[]{ItemID.DARKLIGHT, ItemID.DARKLIGHT_8281},
            2890,
            4,
            io.blert.proto.PlayerAttack.DARKLIGHT_SPEC
    ),
    DAWN_SPEC(ItemID.DAWNBRINGER, 1167, 4, new Projectile(1547, 51), io.blert.proto.PlayerAttack.DAWN_SPEC),
    DAWN_AUTO(ItemID.DAWNBRINGER, 1167, 4, new Projectile(1544, 51), io.blert.proto.PlayerAttack.DAWN_AUTO),
    DART(
            new int[]{
                    ItemID.RUNE_DART, ItemID.RUNE_DARTP, 5634, 5641,
                    ItemID.AMETHYST_DART, ItemID.AMETHYST_DARTP, 25855, 25857,
                    ItemID.DRAGON_DART, ItemID.DRAGON_DARTP, 11233, 11234,
            },
            7554,
            2,
            io.blert.proto.PlayerAttack.DART
    ),
    DDS_POKE(
            new int[]{ItemID.DRAGON_DAGGER, ItemID.DRAGON_DAGGERP, ItemID.DRAGON_DAGGERP_5680, ItemID.DRAGON_DAGGERP_5698},
            new int[]{376, 377},
            4,
            io.blert.proto.PlayerAttack.DDS_POKE
    ),
    DDS_SPEC(
            new int[]{ItemID.DRAGON_DAGGER, ItemID.DRAGON_DAGGERP, ItemID.DRAGON_DAGGERP_5680, ItemID.DRAGON_DAGGERP_5698},
            1062,
            4,
            io.blert.proto.PlayerAttack.DDS_SPEC
    ),
    DHAROKS_GREATAXE(
            new int[]{ItemID.DHAROKS_GREATAXE, ItemID.DHAROKS_GREATAXE_25, ItemID.DHAROKS_GREATAXE_50, ItemID.DHAROKS_GREATAXE_75, ItemID.DHAROKS_GREATAXE_100},
            new int[]{2066, 2067},
            7,
            io.blert.proto.PlayerAttack.DHAROKS_GREATAXE
    ),
    DINHS_SPEC(
            new int[]{ItemID.DINHS_BULWARK, ItemID.DINHS_BLAZING_BULWARK},
            7511,
            5,
            io.blert.proto.PlayerAttack.DINHS_SPEC
    ),
    DRAGON_HUNTER_LANCE(
            ItemID.DRAGON_HUNTER_LANCE,
            new int[]{8288, 8289, 8290},
            4,
            io.blert.proto.PlayerAttack.DRAGON_HUNTER_LANCE
    ),
    DRAGON_KNIFE_AUTO(
            new int[]{ItemID.DRAGON_KNIFE, ItemID.DRAGON_KNIFEP, ItemID.DRAGON_KNIFEP_22808, ItemID.DRAGON_KNIFEP_22810},
            new int[]{8194},
            2,
            io.blert.proto.PlayerAttack.DRAGON_KNIFE_AUTO
    ),
    DRAGON_KNIFE_SPEC(
            new int[]{ItemID.DRAGON_KNIFE, ItemID.DRAGON_KNIFEP, ItemID.DRAGON_KNIFEP_22808, ItemID.DRAGON_KNIFEP_22810},
            new int[]{8291},
            2,
            io.blert.proto.PlayerAttack.DRAGON_KNIFE_SPEC
    ),
    DRAGON_SCIMITAR(ItemID.DRAGON_SCIMITAR, new int[]{386, 390}, 4, io.blert.proto.PlayerAttack.DRAGON_SCIMITAR),
    DUAL_MACUAHUITL(ItemID.DUAL_MACUAHUITL, 10989, 4, io.blert.proto.PlayerAttack.DUAL_MACUAHUITL),
    EARTHBOUND_TECPATL(
            30957,
            new int[]{12342, 2068},
            4,
            io.blert.proto.PlayerAttack.EARTHBOUND_TECPATL
    ),
    ELDER_MAUL(
            new int[]{ItemID.ELDER_MAUL, ItemID.ELDER_MAUL_OR},
            7516,
            6,
            io.blert.proto.PlayerAttack.ELDER_MAUL
    ),
    ELDER_MAUL_SPEC(
            new int[]{ItemID.ELDER_MAUL, ItemID.ELDER_MAUL_OR},
            11124,
            6,
            io.blert.proto.PlayerAttack.ELDER_MAUL_SPEC
    ),
    EMBERLIGHT_AUTO(ItemID.EMBERLIGHT, new int[]{386, 390}, 4, io.blert.proto.PlayerAttack.EMBERLIGHT_AUTO),
    EMBERLIGHT_SPEC(ItemID.EMBERLIGHT, 11138, 4, io.blert.proto.PlayerAttack.EMBERLIGHT_SPEC),
    EYE_OF_AYAK_AUTO(
            new int[]{31113},
            new int[]{12397},
            3,
            io.blert.proto.PlayerAttack.EYE_OF_AYAK_AUTO,
            true
    ),
    EYE_OF_AYAK_SPEC(
            new int[]{31113},
            new int[]{12394},
            5,
            io.blert.proto.PlayerAttack.EYE_OF_AYAK_SPEC,
            true
    ),
    FANG(
            new int[]{ItemID.OSMUMTENS_FANG, ItemID.OSMUMTENS_FANG_OR},
            9471,
            5,
            io.blert.proto.PlayerAttack.FANG_STAB
    ),
    FANG_SPEC(
            new int[]{ItemID.OSMUMTENS_FANG, ItemID.OSMUMTENS_FANG_OR},
            11222,
            5,
            io.blert.proto.PlayerAttack.FANG_SPEC
    ),
    GLACIAL_TEMOTLI(
            ItemID.GLACIAL_TEMOTLI,
            2068,
            4,
            io.blert.proto.PlayerAttack.GLACIAL_TEMOTLI
    ),
    GOBLIN_PAINT_CANNON(
            ItemID.GOBLIN_PAINT_CANNON,
            2323,
            3,
            io.blert.proto.PlayerAttack.GOBLIN_PAINT_CANNON
    ),
    GODSWORD_SMACK(
            new int[]{
                    ItemID.ANCIENT_GODSWORD,
                    ItemID.ANCIENT_GODSWORD_27184,
                    ItemID.ARMADYL_GODSWORD,
                    ItemID.ARMADYL_GODSWORD_OR,
                    ItemID.ARMADYL_GODSWORD_DEADMAN,
                    ItemID.BANDOS_GODSWORD,
                    ItemID.BANDOS_GODSWORD_OR,
                    ItemID.SARADOMIN_GODSWORD,
                    ItemID.SARADOMIN_GODSWORD_OR,
                    ItemID.ZAMORAK_GODSWORD,
                    ItemID.ZAMORAK_GODSWORD_OR,
            },
            new int[]{7044, 7045, 7054, 7055},
            6,
            io.blert.proto.PlayerAttack.GODSWORD_SMACK
    ),
    GUTHANS_WARSPEAR(
            new int[]{ItemID.GUTHANS_WARSPEAR, ItemID.GUTHANS_WARSPEAR_25, ItemID.GUTHANS_WARSPEAR_50, ItemID.GUTHANS_WARSPEAR_75, ItemID.GUTHANS_WARSPEAR_100},
            new int[]{2080, 2081, 2082},
            5,
            io.blert.proto.PlayerAttack.GUTHANS_WARSPEAR
    ),
    HAMMER_BOP(
            new int[]{ItemID.DRAGON_WARHAMMER, ItemID.DRAGON_WARHAMMER_CR},
            401,
            6,
            io.blert.proto.PlayerAttack.HAMMER_BOP
    ),
    HAMMER_SPEC(
            new int[]{ItemID.DRAGON_WARHAMMER, ItemID.DRAGON_WARHAMMER_CR},
            1378,
            6,
            io.blert.proto.PlayerAttack.HAMMER_SPEC
    ),
    HAM_JOINT(ItemID.HAM_JOINT, 401, 3, io.blert.proto.PlayerAttack.HAM_JOINT),
    ICE_RUSH(
            new int[]{
                    ItemID.KODAI_WAND,
                    ItemID.NIGHTMARE_STAFF,
                    ItemID.VOLATILE_NIGHTMARE_STAFF,
                    ItemID.CORRUPTED_VOLATILE_NIGHTMARE_STAFF,
                    ItemID.VOLATILE_NIGHTMARE_STAFF_DEADMAN,
                    ItemID.HARMONISED_NIGHTMARE_STAFF,
                    ItemID.ELDRITCH_NIGHTMARE_STAFF,
                    ItemID.EYE_OF_AYAK,
                    ItemID.ANCIENT_SCEPTRE,
                    ItemID.ANCIENT_SCEPTRE_L,
                    ItemID.SMOKE_ANCIENT_SCEPTRE,
                    ItemID.SMOKE_ANCIENT_SCEPTRE_28264,
                    ItemID.SMOKE_ANCIENT_SCEPTRE_L,
                    ItemID.SHADOW_ANCIENT_SCEPTRE,
                    ItemID.SHADOW_ANCIENT_SCEPTRE_28266,
                    ItemID.SHADOW_ANCIENT_SCEPTRE_L,
                    ItemID.BLOOD_ANCIENT_SCEPTRE,
                    ItemID.BLOOD_ANCIENT_SCEPTRE_28260,
                    ItemID.BLOOD_ANCIENT_SCEPTRE_L,
                    ItemID.ICE_ANCIENT_SCEPTRE,
                    ItemID.ICE_ANCIENT_SCEPTRE_28262,
                    ItemID.ICE_ANCIENT_SCEPTRE_L,
                    ItemID.SANGUINESTI_STAFF,
                    ItemID.HOLY_SANGUINESTI_STAFF,
                    ItemID.STAFF_OF_THE_DEAD,
                    ItemID.STAFF_OF_LIGHT,
                    ItemID.TOXIC_STAFF_OF_THE_DEAD,
                    ItemID.TOXIC_STAFF_UNCHARGED,
                    ItemID.TUMEKENS_SHADOW,
            },
            new int[]{1978, 10091},
            5,
            new Projectile(360, 51),
            io.blert.proto.PlayerAttack.ICE_RUSH,
            false
    ),
    INQUISITORS_MACE(ItemID.INQUISITORS_MACE, new int[]{400, 4503}, 4, io.blert.proto.PlayerAttack.INQUISITORS_MACE),
    KARILS_CROSSBOW(
            new int[]{ItemID.KARILS_CROSSBOW, ItemID.KARILS_CROSSBOW_25, ItemID.KARILS_CROSSBOW_50, ItemID.KARILS_CROSSBOW_75, ItemID.KARILS_CROSSBOW_100},
            2075,
            4,
            io.blert.proto.PlayerAttack.KARILS_CROSSBOW
    ),
    KICK(-1, 423, 4, io.blert.proto.PlayerAttack.KICK),
    KODAI_BARRAGE(ItemID.KODAI_WAND, 10092, 5,
            io.blert.proto.PlayerAttack.KODAI_BARRAGE),
    KODAI_BASH(ItemID.KODAI_WAND, 393, 5, io.blert.proto.PlayerAttack.KODAI_BASH),
    NM_STAFF_BARRAGE(
            new int[]{
                    ItemID.NIGHTMARE_STAFF,
                    ItemID.CORRUPTED_VOLATILE_NIGHTMARE_STAFF,
                    ItemID.VOLATILE_NIGHTMARE_STAFF,
                    ItemID.VOLATILE_NIGHTMARE_STAFF_DEADMAN,
                    ItemID.HARMONISED_NIGHTMARE_STAFF,
                    ItemID.ELDRITCH_NIGHTMARE_STAFF,
            },
            10092,
            5,
            io.blert.proto.PlayerAttack.NM_STAFF_BARRAGE
    ),
    NM_STAFF_BASH(
            new int[]{
                    ItemID.NIGHTMARE_STAFF,
                    ItemID.CORRUPTED_VOLATILE_NIGHTMARE_STAFF,
                    ItemID.VOLATILE_NIGHTMARE_STAFF,
                    ItemID.VOLATILE_NIGHTMARE_STAFF_DEADMAN,
                    ItemID.HARMONISED_NIGHTMARE_STAFF,
                    ItemID.ELDRITCH_NIGHTMARE_STAFF,
            },
            4505,
            5,
            io.blert.proto.PlayerAttack.NM_STAFF_BASH
    ),
    NOXIOUS_HALBERD(ItemID.NOXIOUS_HALBERD, new int[]{428, 440}, 5, io.blert.proto.PlayerAttack.NOXIOUS_HALBERD),
    PUNCH(-1, 422, 4, io.blert.proto.PlayerAttack.PUNCH),
    RAPIER(
            new int[]{ItemID.GHRAZI_RAPIER, ItemID.HOLY_GHRAZI_RAPIER},
            8145,
            4,
            io.blert.proto.PlayerAttack.RAPIER
    ),
    SAELDOR(
            new int[]{ItemID.BLADE_OF_SAELDOR, ItemID.BLADE_OF_SAELDOR_C, 25870, 25872, 25874, 25876, 25878, 25880, 25882},
            390,
            4,
            io.blert.proto.PlayerAttack.SAELDOR
    ),
    SANG(
            new int[]{ItemID.SANGUINESTI_STAFF, ItemID.HOLY_SANGUINESTI_STAFF},
            new int[]{1167, 11430},
            4,
            io.blert.proto.PlayerAttack.SANG
    ),
    SANG_BARRAGE(
            new int[]{ItemID.SANGUINESTI_STAFF, ItemID.HOLY_SANGUINESTI_STAFF},
            10092,
            5,
            io.blert.proto.PlayerAttack.SANG_BARRAGE
    ),
    SCEPTRE_BARRAGE(
            new int[]{
                    ItemID.ANCIENT_SCEPTRE,
                    ItemID.ANCIENT_SCEPTRE_L,
                    ItemID.SMOKE_ANCIENT_SCEPTRE,
                    ItemID.SMOKE_ANCIENT_SCEPTRE_28264,
                    ItemID.SMOKE_ANCIENT_SCEPTRE_L,
                    ItemID.SHADOW_ANCIENT_SCEPTRE,
                    ItemID.SHADOW_ANCIENT_SCEPTRE_28266,
                    ItemID.SHADOW_ANCIENT_SCEPTRE_L,
                    ItemID.BLOOD_ANCIENT_SCEPTRE,
                    ItemID.BLOOD_ANCIENT_SCEPTRE_28260,
                    ItemID.BLOOD_ANCIENT_SCEPTRE_L,
                    ItemID.ICE_ANCIENT_SCEPTRE,
                    ItemID.ICE_ANCIENT_SCEPTRE_28262,
                    ItemID.ICE_ANCIENT_SCEPTRE_L,
            },
            10092,
            5,
            io.blert.proto.PlayerAttack.SCEPTRE_BARRAGE
    ),
    SCORCHING_BOW_AUTO(ItemID.SCORCHING_BOW, 426, 4, io.blert.proto.PlayerAttack.SCORCHING_BOW_AUTO),
    SCORCHING_BOW_SPEC(ItemID.SCORCHING_BOW, 11133, 4, io.blert.proto.PlayerAttack.SCORCHING_BOW_SPEC),
    SCYTHE(
            new int[]{ItemID.SCYTHE_OF_VITUR, ItemID.HOLY_SCYTHE_OF_VITUR, ItemID.SANGUINE_SCYTHE_OF_VITUR},
            8056,
            5,
            io.blert.proto.PlayerAttack.SCYTHE
    ),
    SCYTHE_UNCHARGED(
            new int[]{ItemID.SCYTHE_OF_VITUR_UNCHARGED, ItemID.HOLY_SCYTHE_OF_VITUR_UNCHARGED, ItemID.SANGUINE_SCYTHE_OF_VITUR_UNCHARGED},
            8056,
            5,
            io.blert.proto.PlayerAttack.SCYTHE_UNCHARGED
    ),
    SGS_SPEC(
            new int[]{ItemID.SARADOMIN_GODSWORD, ItemID.SARADOMIN_GODSWORD_OR},
            new int[]{7640, 7641},
            6,
            io.blert.proto.PlayerAttack.SGS_SPEC
    ),
    SHADOW(
            ItemID.TUMEKENS_SHADOW,
            9493,
            5,
            io.blert.proto.PlayerAttack.SHADOW
    ),
    SHADOW_BARRAGE(  // please don't tob ever again
            ItemID.TUMEKENS_SHADOW,
            10092,
            5,
            io.blert.proto.PlayerAttack.SHADOW_BARRAGE
    ),
    SOTD_BARRAGE(
            ItemID.STAFF_OF_THE_DEAD,
            10092,
            5,
            io.blert.proto.PlayerAttack.SOTD_BARRAGE
    ),
    SOULREAPER_AXE(
            ItemID.SOULREAPER_AXE_28338,
            10172,
            5,
            io.blert.proto.PlayerAttack.SOULREAPER_AXE
    ),
    STAFF_OF_LIGHT_BARRAGE(
            ItemID.STAFF_OF_LIGHT,
            10092,
            5,
            io.blert.proto.PlayerAttack.STAFF_OF_LIGHT_BARRAGE
    ),
    STAFF_OF_LIGHT_SWIPE(
            ItemID.STAFF_OF_LIGHT,
            new int[]{419, 428, 440},
            4,
            io.blert.proto.PlayerAttack.STAFF_OF_LIGHT_SWIPE
    ),
    SULPHUR_BLADES(
            ItemID.SULPHUR_BLADES,
            2068,
            4,
            io.blert.proto.PlayerAttack.SULPHUR_BLADES
    ),
    SWIFT(
            ItemID.SWIFT_BLADE,
            new int[]{390, 8288},
            3,
            io.blert.proto.PlayerAttack.SWIFT_BLADE
    ),
    TENT_WHIP(
            new int[]{ItemID.ABYSSAL_TENTACLE, ItemID.ABYSSAL_TENTACLE_OR},
            1658,
            4,
            io.blert.proto.PlayerAttack.TENT_WHIP
    ),
    TONALZTICS_AUTO(
            new int[]{ItemID.TONALZTICS_OF_RALOS, ItemID.TONALZTICS_OF_RALOS_UNCHARGED},
            new int[]{10922, 10923},
            6,
            io.blert.proto.PlayerAttack.TONALZTICS_AUTO
    ),
    TONALZTICS_SPEC(
            new int[]{ItemID.TONALZTICS_OF_RALOS},
            10914,
            6,
            io.blert.proto.PlayerAttack.TONALZTICS_SPEC
    ),
    TONALZTICS_UNCHARGED(
            new int[]{ItemID.TONALZTICS_OF_RALOS_UNCHARGED},
            10916,
            6,
            io.blert.proto.PlayerAttack.TONALZTICS_UNCHARGED
    ),
    TORAGS_HAMMERS(
            new int[]{ItemID.TORAGS_HAMMERS, ItemID.TORAGS_HAMMERS_25, ItemID.TORAGS_HAMMERS_50, ItemID.TORAGS_HAMMERS_75, ItemID.TORAGS_HAMMERS_100},
            2068,
            5,
            io.blert.proto.PlayerAttack.TORAGS_HAMMERS
    ),
    // TOXIC_TRIDENT must come before TOXIC_TRIDENT_BARRAGE as it is the default assumption when the animation is unknown.
    TOXIC_TRIDENT(
            new int[]{ItemID.TRIDENT_OF_THE_SWAMP, ItemID.TRIDENT_OF_THE_SWAMP_E},
            11430,
            4,
            io.blert.proto.PlayerAttack.TOXIC_TRIDENT
    ),
    TOXIC_TRIDENT_BARRAGE(
            new int[]{ItemID.TRIDENT_OF_THE_SWAMP, ItemID.TRIDENT_OF_THE_SWAMP_E},
            10092,
            5,
            io.blert.proto.PlayerAttack.TOXIC_TRIDENT_BARRAGE
    ),
    // TOXIC_STAFF_BARRAGE must come before TOXIC_STAFF_SWIPE as it is the default assumption when the animation is unknown.
    // TODO(frolv): Track uncharged separately?
    TOXIC_STAFF_BARRAGE(
            new int[]{ItemID.TOXIC_STAFF_OF_THE_DEAD, ItemID.TOXIC_STAFF_UNCHARGED},
            10092,
            5,
            io.blert.proto.PlayerAttack.TOXIC_STAFF_BARRAGE
    ),
    TOXIC_STAFF_SWIPE(
            new int[]{ItemID.TOXIC_STAFF_OF_THE_DEAD, ItemID.TOXIC_STAFF_UNCHARGED},
            new int[]{428, 440},
            4,
            io.blert.proto.PlayerAttack.TOXIC_STAFF_SWIPE
    ),
    // TRIDENT must come before TRIDENT_BARRAGE as it is the default assumption when the animation is unknown.
    TRIDENT(
            new int[]{ItemID.TRIDENT_OF_THE_SEAS, ItemID.TRIDENT_OF_THE_SEAS_E},
            11430,
            4,
            io.blert.proto.PlayerAttack.TRIDENT
    ),
    TRIDENT_BARRAGE(
            new int[]{ItemID.TRIDENT_OF_THE_SEAS, ItemID.TRIDENT_OF_THE_SEAS_E},
            10092,
            5,
            io.blert.proto.PlayerAttack.TRIDENT_BARRAGE
    ),
    TWISTED_BOW(ItemID.TWISTED_BOW, 426, 5, io.blert.proto.PlayerAttack.TWISTED_BOW),
    VENATOR_BOW(new int[]{ItemID.VENATOR_BOW, ItemID.ECHO_VENATOR_BOW}, 9858, 4, io.blert.proto.PlayerAttack.VENATOR_BOW),
    VERACS_FLAIL(
            new int[]{ItemID.VERACS_FLAIL, ItemID.VERACS_FLAIL_25, ItemID.VERACS_FLAIL_50, ItemID.VERACS_FLAIL_75, ItemID.VERACS_FLAIL_100},
            2062,
            5,
            io.blert.proto.PlayerAttack.VERACS_FLAIL
    ),
    VOIDWAKER_AUTO(
            new int[]{ItemID.VOIDWAKER, ItemID.VOIDWAKER_DEADMAN},
            new int[]{386, 390},
            4,
            io.blert.proto.PlayerAttack.VOIDWAKER_AUTO
    ),
    VOIDWAKER_SPEC(
            new int[]{ItemID.VOIDWAKER, ItemID.VOIDWAKER_DEADMAN},
            new int[]{1378, 11275},
            4,
            io.blert.proto.PlayerAttack.VOIDWAKER_SPEC
    ),
    VOLATILE_NM_SPEC(
            new int[]{ItemID.VOLATILE_NIGHTMARE_STAFF, ItemID.CORRUPTED_VOLATILE_NIGHTMARE_STAFF, ItemID.VOLATILE_NIGHTMARE_STAFF_DEADMAN},
            8532,
            5,
            io.blert.proto.PlayerAttack.VOLATILE_NM_SPEC
    ),
    WEBWEAVER_AUTO(ItemID.WEBWEAVER_BOW, 426, 3, io.blert.proto.PlayerAttack.WEBWEAVER_AUTO),
    WEBWEAVER_SPEC(ItemID.WEBWEAVER_BOW, 9964, 3, io.blert.proto.PlayerAttack.WEBWEAVER_SPEC),
    XGS_SPEC(
            new int[]{ItemID.ANCIENT_GODSWORD, ItemID.ANCIENT_GODSWORD_27184},
            9171,
            6,
            io.blert.proto.PlayerAttack.XGS_SPEC
    ),
    ZCB_AUTO(ItemID.ZARYTE_CROSSBOW, 9168, 5, new Projectile(1468, 41), io.blert.proto.PlayerAttack.ZCB_AUTO),
    ZCB_SPEC(ItemID.ZARYTE_CROSSBOW, 9168, 5, new Projectile(1995, 41), io.blert.proto.PlayerAttack.ZCB_SPEC),
    ZGS_SPEC(
            new int[]{ItemID.ZAMORAK_GODSWORD, ItemID.ZAMORAK_GODSWORD_OR},
            new int[]{7638, 7639},
            6,
            io.blert.proto.PlayerAttack.ZGS_SPEC
    ),
    ZOMBIE_AXE(ItemID.ZOMBIE_AXE, new int[]{3852, 7004}, 5, io.blert.proto.PlayerAttack.ZOMBIE_AXE),

    // Attacks where the animation matches a known style of attack, but the weapon does not.
    UNKNOWN_BOW(426, io.blert.proto.PlayerAttack.UNKNOWN_BOW),
    UNKNOWN_BARRAGE(10092, 5, io.blert.proto.PlayerAttack.UNKNOWN_BARRAGE),
    UNKNOWN_POWERED_STAFF(1167, io.blert.proto.PlayerAttack.UNKNOWN_POWERED_STAFF),
    UNKNOWN(-1, io.blert.proto.PlayerAttack.UNKNOWN),
    ;

    /**
     * Every ID for this weapon (e.g. cosmetic overrides).
     * <p>
     * {@code weaponIds[0]} should *always* be the base weapon, as that is what will be reported by
     * {@link #getWeaponId()}.
     */
    private final int[] weaponIds;
    private final int[] animationIds;
    @Getter
    private final int cooldown;
    @Getter
    private final Projectile projectile;
    @Getter
    private final boolean unknown;
    private final io.blert.proto.PlayerAttack protoValue;
    @Getter
    private final boolean continuousAnimation;

    private static final ImmutableSet<Integer> VALID_ANIMATION_IDS;

    private static final Map<Integer, Set<Projectile>> ANIMATION_PROJECTILES;

    @RequiredArgsConstructor
    @Getter
    public static class Projectile {
        private final int id;
        private final int startCycleOffset;

        private PlayerAttack attack = null;

        public static Projectile BLOWPIPE_SPEC = new Projectile(1043, 32);
        public static Projectile BLAZING_BLOWPIPE_SPEC = new Projectile(2599, 32);
    }

    static {
        var animationBuilder = new ImmutableSet.Builder<Integer>();
        ANIMATION_PROJECTILES = new HashMap<>();

        Arrays.stream(PlayerAttack.values())
                .filter(attack -> !attack.isUnknown())
                .forEach(attack -> {
                    for (int id : attack.animationIds) {
                        animationBuilder.add(id);
                    }
                    if (attack.hasProjectile()) {
                        for (int id : attack.animationIds) {
                            ANIMATION_PROJECTILES.compute(id, (k, v) -> {
                                if (v == null) {
                                    return ImmutableSet.of(attack.projectile);
                                }
                                return ImmutableSet.<Projectile>builder().addAll(v).add(attack.projectile).build();
                            });
                        }
                        attack.projectile.attack = attack;
                    }
                });

        VALID_ANIMATION_IDS = animationBuilder.build();
    }

    /**
     * Finds a player attack based on a weapon and animation.
     *
     * @param weaponId    ID of the weapon used.
     * @param animationId ID of the animation played with the attack.
     * @return If the animation ID does not map to any known attack, returns {@code Optional.empty()}. Otherwise,
     * returns the attack if the weapon ID is consistent with the animation or {@code PlayerAttack.UNKNOWN} if not.
     */
    public static Optional<PlayerAttack> find(int weaponId, int animationId) {
        if (!VALID_ANIMATION_IDS.contains(animationId)) {
            return Optional.empty();
        }

        return Arrays.stream(PlayerAttack.values())
                .filter(attack -> attack.matches(weaponId, animationId))
                .findFirst()
                .or(() -> Arrays.stream(PlayerAttack.values())
                        .filter(attack -> attack.isUnknown() && attack.hasAnimation(animationId))
                        .findFirst()
                )
                .or(() -> Optional.of(PlayerAttack.UNKNOWN));
    }

    /**
     * Finds a player attack based on an animation ID only, ignoring weapon ID.
     *
     * @param animationId ID of the animation played with the attack.
     * @return If the animation ID does not map to any known attack, returns
     * {@code Optional.empty()}. Otherwise, returns the first attack that
     * matches the animation ID.
     */
    public static Optional<PlayerAttack> firstWithAnimation(int animationId) {
        if (!VALID_ANIMATION_IDS.contains(animationId)) {
            return Optional.empty();
        }

        return Arrays.stream(PlayerAttack.values())
                .filter(attack -> attack.hasAnimation(animationId))
                .findFirst()
                .or(() -> Optional.of(PlayerAttack.UNKNOWN));
    }

    /**
     * Finds a player attack using only a weapon ID, ignoring animation. If multiple attacks share a weapon,
     * assumptions are made about which to prefer. For example, a {@code CLAW_SCRATCH} will be returned over a
     * {@code CLAW_SPEC}.
     *
     * @param weaponId ID of the weapon used.
     * @return A player attack matching the weapon, or {@code PlayerAttack.UNKNOWN} if none exist.
     */
    public static Optional<PlayerAttack> findByWeaponOnly(int weaponId) {
        return Arrays.stream(PlayerAttack.values())
                .filter(attack -> attack.hasWeapon(weaponId))
                .findFirst()
                .or(() -> Optional.of(PlayerAttack.UNKNOWN));
    }

    /**
     * Certain player attack animations are hidden if used immediately after
     * a continuous animation. This checks to see if a player is wielding any
     * affected weapon, and returns that weapon's attack if so.
     *
     * @param weaponId ID of the weapon used.
     * @return A player attack matching the weapon, or {@code Optional.empty()} if none exist.
     */
    public static Optional<PlayerAttack> findSuppressedAttack(int weaponId) {
        // TODO(frolv): Barrages are also hidden after a blowpipe, but it requires checking for applied graphics.
        PlayerAttack[] attacks = new PlayerAttack[]{CHIN_BLACK, CHIN_GREY, CHIN_RED};
        return Arrays.stream(attacks)
                .filter(attack -> attack.hasWeapon(weaponId))
                .findFirst();
    }

    /**
     * Returns whether the given animation ID can suppress the animation of another attack.
     *
     * @param animationId ID of the animation to check.
     * @return {@code true} if the animation ID is one that suppresses other
     * animations, {@code false} otherwise.
     */
    public static boolean isSuppressingAnimation(int animationId) {
        return Arrays.stream(PlayerAttack.values())
                .anyMatch(a -> a.isContinuousAnimation() && a.hasAnimation(animationId));
    }

    /**
     * The base ID of the weapon, ignoring cosmetics or other overrides.
     *
     * @return ID of the weapon used in the attack.
     */
    public int getWeaponId() {
        return weaponIds[0];
    }

    public boolean hasProjectile() {
        return projectile != null;
    }

    public boolean matches(int weaponId, int animationId) {
        return !isUnknown() && hasWeapon(weaponId) && hasAnimation(animationId);
    }

    public boolean hasWeapon(int weaponId) {
        for (int wid : weaponIds) {
            if (wid == weaponId) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnimation(int animationId) {
        for (int aid : animationIds) {
            if (aid == animationId) {
                return true;
            }
        }
        return false;
    }

    public io.blert.proto.PlayerAttack toProto() {
        return protoValue;
    }

    public List<Projectile> projectilesForAnimation() {
        return Arrays.stream(animationIds)
                .mapToObj(ANIMATION_PROJECTILES::get)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .collect(Collectors.toList());
    }

    PlayerAttack(int[] weaponIds, int[] animationIds, int cooldown,
                 Projectile projectile,
                 io.blert.proto.PlayerAttack protoValue,
                 boolean continuousAnimation) {
        this.weaponIds = weaponIds;
        this.animationIds = animationIds;
        this.cooldown = cooldown;
        this.projectile = projectile;
        this.unknown = false;
        this.protoValue = protoValue;
        this.continuousAnimation = continuousAnimation;
    }

    PlayerAttack(int[] weaponIds, int[] animationIds, int cooldown,
                 io.blert.proto.PlayerAttack protoValue, boolean continuousAnimation) {
        this(weaponIds, animationIds, cooldown, null, protoValue, continuousAnimation);
    }

    PlayerAttack(int[] weaponIds, int[] animationIds, int cooldown, io.blert.proto.PlayerAttack protoValue) {
        this(weaponIds, animationIds, cooldown, null, protoValue, false);
    }

    PlayerAttack(int[] weaponIds, int animationId, int cooldown, io.blert.proto.PlayerAttack protoValue) {
        this(weaponIds, new int[]{animationId}, cooldown, protoValue);
    }

    PlayerAttack(int weaponId, int[] animationIds, int cooldown, io.blert.proto.PlayerAttack protoValue) {
        this(new int[]{weaponId}, animationIds, cooldown, protoValue);
    }

    PlayerAttack(int weaponId, int animationId, int cooldown, io.blert.proto.PlayerAttack protoValue) {
        this(new int[]{weaponId}, new int[]{animationId}, cooldown, protoValue);
    }

    PlayerAttack(int weaponId, int animationId, int cooldown,
                 Projectile projectile, io.blert.proto.PlayerAttack protoValue) {
        this(new int[]{weaponId}, new int[]{animationId}, cooldown,
                projectile, protoValue, false);
    }

    PlayerAttack(int animationId, int cooldown, io.blert.proto.PlayerAttack protoValue) {
        this.weaponIds = new int[]{-1};
        this.animationIds = new int[]{animationId};
        this.cooldown = cooldown;
        this.projectile = null;
        this.unknown = true;
        this.protoValue = protoValue;
        this.continuousAnimation = false;
    }

    PlayerAttack(int animationId, io.blert.proto.PlayerAttack protoValue) {
        this.weaponIds = new int[]{-1};
        this.animationIds = new int[]{animationId};
        this.cooldown = 0;
        this.projectile = null;
        this.unknown = true;
        this.protoValue = protoValue;
        this.continuousAnimation = false;
    }
}
