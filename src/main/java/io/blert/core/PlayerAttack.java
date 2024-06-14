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
    AGS_SPEC(
            new int[]{ItemID.ARMADYL_GODSWORD, ItemID.ARMADYL_GODSWORD_OR},
            new int[]{7644, 7645},
            6,
            io.blert.proto.PlayerAttack.AGS_SPEC
    ),
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
            io.blert.proto.PlayerAttack.BLOWPIPE
    ),
    BLOWPIPE_SPEC(
            new int[]{ItemID.TOXIC_BLOWPIPE, ItemID.BLAZING_BLOWPIPE},
            new int[]{},  // This is handled specially instead of being assigned automatically.
            2,
            io.blert.proto.PlayerAttack.BLOWPIPE_SPEC
    ),
    BOWFA(new int[]{ItemID.BOW_OF_FAERDHINEN, ItemID.BOW_OF_FAERDHINEN_C, 25884, 25886, 25888, 25890, 25892, 25894, 25896},
            426,
            4,
            io.blert.proto.PlayerAttack.BOWFA
    ),
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
            393,
            4,
            io.blert.proto.PlayerAttack.CLAW_SCRATCH
    ),
    CLAW_SPEC(
            new int[]{ItemID.DRAGON_CLAWS, ItemID.DRAGON_CLAWS_CR},
            7514,
            4,
            io.blert.proto.PlayerAttack.CLAW_SPEC
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
    DINHS_SPEC(
            new int[]{ItemID.DINHS_BULWARK, ItemID.DINHS_BLAZING_BULWARK},
            7511,
            5,
            io.blert.proto.PlayerAttack.DINHS_SPEC
    ),
    DUAL_MACUAHUITL(ItemID.DUAL_MACUAHUITL, 10989, 4, io.blert.proto.PlayerAttack.DUAL_MACUAHUITL),
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
    FANG(
            new int[]{ItemID.OSMUMTENS_FANG, ItemID.OSMUMTENS_FANG_OR},
            9471,
            5,
            io.blert.proto.PlayerAttack.FANG_STAB
    ),
    GODSWORD_SMACK(
            new int[]{
                    ItemID.ANCIENT_GODSWORD,
                    ItemID.ANCIENT_GODSWORD_27184,
                    ItemID.ARMADYL_GODSWORD,
                    ItemID.ARMADYL_GODSWORD_OR,
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
    INQUISITORS_MACE(ItemID.INQUISITORS_MACE, new int[]{400, 4503}, 4, io.blert.proto.PlayerAttack.INQUISITORS_MACE),
    KICK(-1, 423, 4, io.blert.proto.PlayerAttack.KICK),
    KODAI_BARRAGE(ItemID.KODAI_WAND, 1979, 5, io.blert.proto.PlayerAttack.KODAI_BARRAGE),
    KODAI_BASH(ItemID.KODAI_WAND, 393, 5, io.blert.proto.PlayerAttack.KODAI_BASH),
    NM_STAFF_BARRAGE(
            new int[]{ItemID.NIGHTMARE_STAFF, ItemID.VOLATILE_NIGHTMARE_STAFF, ItemID.HARMONISED_NIGHTMARE_STAFF, ItemID.ELDRITCH_NIGHTMARE_STAFF},
            1979,
            5,
            io.blert.proto.PlayerAttack.NM_STAFF_BARRAGE
    ),
    NM_STAFF_BASH(
            new int[]{ItemID.NIGHTMARE_STAFF, ItemID.VOLATILE_NIGHTMARE_STAFF, ItemID.HARMONISED_NIGHTMARE_STAFF, ItemID.ELDRITCH_NIGHTMARE_STAFF},
            4505,
            5,
            io.blert.proto.PlayerAttack.NM_STAFF_BASH
    ),
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
            1167,
            4,
            io.blert.proto.PlayerAttack.SANG
    ),
    SANG_BARRAGE(
            new int[]{ItemID.SANGUINESTI_STAFF, ItemID.HOLY_SANGUINESTI_STAFF},
            1979,
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
            1979,
            5,
            io.blert.proto.PlayerAttack.SCEPTRE_BARRAGE
    ),
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
            1979,
            5,
            io.blert.proto.PlayerAttack.SHADOW_BARRAGE
    ),
    SOTD_BARRAGE(
            ItemID.STAFF_OF_THE_DEAD,
            1979,
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
            1979,
            5,
            io.blert.proto.PlayerAttack.STAFF_OF_LIGHT_BARRAGE
    ),
    STAFF_OF_LIGHT_SWIPE(
            ItemID.STAFF_OF_LIGHT,
            new int[]{419, 428, 440},
            4,
            io.blert.proto.PlayerAttack.STAFF_OF_LIGHT_SWIPE
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
    // TOXIC_TRIDENT must come before TOXIC_TRIDENT_BARRAGE as it is the default assumption when the animation is unknown.
    TOXIC_TRIDENT(
            new int[]{ItemID.TRIDENT_OF_THE_SWAMP, ItemID.TRIDENT_OF_THE_SWAMP_E},
            1167,
            4,
            io.blert.proto.PlayerAttack.TOXIC_TRIDENT
    ),
    TOXIC_TRIDENT_BARRAGE(
            new int[]{ItemID.TRIDENT_OF_THE_SWAMP, ItemID.TRIDENT_OF_THE_SWAMP_E},
            1979,
            5,
            io.blert.proto.PlayerAttack.TOXIC_TRIDENT_BARRAGE
    ),
    // TOXIC_STAFF_BARRAGE must come before TOXIC_STAFF_SWIPE as it is the default assumption when the animation is unknown.
    // TODO(frolv): Track uncharged separately?
    TOXIC_STAFF_BARRAGE(
            new int[]{ItemID.TOXIC_STAFF_OF_THE_DEAD, ItemID.TOXIC_STAFF_UNCHARGED},
            1979,
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
            1167,
            4,
            io.blert.proto.PlayerAttack.TRIDENT
    ),
    TRIDENT_BARRAGE(
            new int[]{ItemID.TRIDENT_OF_THE_SEAS, ItemID.TRIDENT_OF_THE_SEAS_E},
            1979,
            5,
            io.blert.proto.PlayerAttack.TRIDENT_BARRAGE
    ),
    TWISTED_BOW(ItemID.TWISTED_BOW, 426, 5, io.blert.proto.PlayerAttack.TWISTED_BOW),
    VENATOR_BOW(ItemID.VENATOR_BOW, 9858, 4, io.blert.proto.PlayerAttack.VENATOR_BOW),
    VOIDWAKER_AUTO(ItemID.VOIDWAKER, new int[]{386, 390}, 4, io.blert.proto.PlayerAttack.VOIDWAKER_AUTO),
    VOIDWAKER_SPEC(ItemID.VOIDWAKER, 1378, 4, io.blert.proto.PlayerAttack.VOIDWAKER_SPEC),
    VOLATILE_NM_SPEC(
            ItemID.VOLATILE_NIGHTMARE_STAFF,
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
    UNKNOWN_BARRAGE(1979, 5, io.blert.proto.PlayerAttack.UNKNOWN_BARRAGE),
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
                 Projectile projectile, io.blert.proto.PlayerAttack protoValue) {
        this.weaponIds = weaponIds;
        this.animationIds = animationIds;
        this.cooldown = cooldown;
        this.projectile = projectile;
        this.unknown = false;
        this.protoValue = protoValue;
    }

    PlayerAttack(int[] weaponIds, int[] animationIds, int cooldown, io.blert.proto.PlayerAttack protoValue) {
        this(weaponIds, animationIds, cooldown, null, protoValue);
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
        this(new int[]{weaponId}, new int[]{animationId}, cooldown, projectile, protoValue);
    }

    PlayerAttack(int animationId, int cooldown, io.blert.proto.PlayerAttack protoValue) {
        this.weaponIds = new int[]{-1};
        this.animationIds = new int[]{animationId};
        this.cooldown = cooldown;
        this.projectile = null;
        this.unknown = true;
        this.protoValue = protoValue;
    }

    PlayerAttack(int animationId, io.blert.proto.PlayerAttack protoValue) {
        this.weaponIds = new int[]{-1};
        this.animationIds = new int[]{animationId};
        this.cooldown = 0;
        this.projectile = null;
        this.unknown = true;
        this.protoValue = protoValue;
    }
}
