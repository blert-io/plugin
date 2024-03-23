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
import net.runelite.api.ItemID;

import java.util.Arrays;
import java.util.Optional;

public enum PlayerAttack {
    BGS_SMACK(
            new int[]{ItemID.BANDOS_GODSWORD, ItemID.BANDOS_GODSWORD_OR},
            7045,
            6,
            io.blert.proto.PlayerAttack.BGS_SMACK
    ),
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
    DAWN_SPEC(ItemID.DAWNBRINGER, 1167, 4, io.blert.proto.PlayerAttack.DAWN_SPEC),
    DINHS_SPEC(
            new int[]{ItemID.DINHS_BULWARK, ItemID.DINHS_BLAZING_BULWARK},
            7511,
            5,
            io.blert.proto.PlayerAttack.DINHS_SPEC
    ),
    FANG(
            new int[]{ItemID.OSMUMTENS_FANG, ItemID.OSMUMTENS_FANG_OR},
            9471,
            5,
            io.blert.proto.PlayerAttack.FANG_STAB
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
    KODAI_BARRAGE(ItemID.KODAI_WAND, 1979, 5, io.blert.proto.PlayerAttack.KODAI_BARRAGE),
    KODAI_BASH(ItemID.KODAI_WAND, 393, 5, io.blert.proto.PlayerAttack.KODAI_BASH),
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
            new int[]{ItemID.ANCIENT_SCEPTRE, ItemID.SMOKE_ANCIENT_SCEPTRE, ItemID.SHADOW_ANCIENT_SCEPTRE, ItemID.BLOOD_ANCIENT_SCEPTRE, ItemID.ICE_ANCIENT_SCEPTRE, ItemID.ICE_ANCIENT_SCEPTRE_28262},
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
            ItemID.SCYTHE_OF_VITUR_UNCHARGED,
            8056,
            5,
            io.blert.proto.PlayerAttack.SCYTHE_UNCHARGED
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
            440,
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
            440,
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
    VOLATILE_NM_BARRAGE(
            ItemID.VOLATILE_NIGHTMARE_STAFF,
            1979,
            5,
            io.blert.proto.PlayerAttack.VOLATILE_NM_BARRAGE
    ),
    // TODO(frolv): Seems that autos and specs share an animation so we'd have to look at the projectile.
    ZCB(ItemID.ZARYTE_CROSSBOW, 9168, 5, io.blert.proto.PlayerAttack.ZCB_SPEC),

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
    private final boolean unknown;
    private final io.blert.proto.PlayerAttack protoValue;

    private static final ImmutableSet<Integer> VALID_ANIMATION_IDS;

    static {
        var builder = new ImmutableSet.Builder<Integer>();

        Arrays.stream(PlayerAttack.values())
                .filter(attack -> !attack.isUnknown())
                .forEach(attack -> {
                    for (int id : attack.animationIds) {
                        builder.add(id);
                    }
                });

        // Fill in some additional generic attack animations to try and catch unknown weapons.
        // TODO(frolv): Replace with real animation IDs.
        builder.add(-200);

        VALID_ANIMATION_IDS = builder.build();
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

    PlayerAttack(int[] weaponIds, int[] animationIds, int cooldown, io.blert.proto.PlayerAttack protoValue) {
        this.weaponIds = weaponIds;
        this.animationIds = animationIds;
        this.cooldown = cooldown;
        this.unknown = false;
        this.protoValue = protoValue;
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

    PlayerAttack(int animationId, int cooldown, io.blert.proto.PlayerAttack protoValue) {
        this.weaponIds = new int[]{-1};
        this.animationIds = new int[]{animationId};
        this.cooldown = cooldown;
        this.unknown = true;
        this.protoValue = protoValue;
    }

    PlayerAttack(int animationId, io.blert.proto.PlayerAttack protoValue) {
        this.weaponIds = new int[]{-1};
        this.animationIds = new int[]{animationId};
        this.cooldown = 0;
        this.unknown = true;
        this.protoValue = protoValue;
    }
}
