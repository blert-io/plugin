/*
 * Copyright (c) 2025 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.blert.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.blert.proto.PlayerAttack;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Slf4j
public class AttackDefinition {
    @Getter
    public static class Projectile {
        private final int id;
        private final int startCycleOffset;
        /**
         * Optional weapon ID this projectile is specific to.
         * If <= 0, the projectile applies to all weapons for its attack.
         */
        private final int weaponId;

        public Projectile(int id, int startCycleOffset) {
            this(id, startCycleOffset, -1);
        }

        public Projectile(int id, int startCycleOffset, int weaponId) {
            this.id = id;
            this.startCycleOffset = startCycleOffset;
            this.weaponId = weaponId;
        }

        public boolean isWeaponSpecific() {
            return weaponId > 0;
        }

        public static Projectile fromProto(io.blert.proto.AttackDefinition.Projectile proto) {
            int weaponId = proto.hasWeaponId() ? proto.getWeaponId() : -1;
            return new Projectile(proto.getId(), proto.getStartCycleOffset(), weaponId);
        }
    }

    public enum Category {
        MELEE,
        RANGED,
        MAGIC;

        public static Category fromProto(io.blert.proto.AttackDefinition.Category proto) {
            switch (proto) {
                case RANGED:
                    return RANGED;
                case MAGIC:
                    return MAGIC;
                case MELEE:
                default:
                    return MELEE;
            }
        }
    }

    /**
     * Unique identifier for this attack: the raw proto PlayerAttack enum value.
     */
    private final int protoId;

    /**
     * Human-readable name for logging/debugging.
     */
    private final String name;

    /**
     * Item IDs that can perform this attack.
     */
    private final int[] weaponIds;

    /**
     * Animation IDs that trigger this attack.
     */
    private final int[] animationIds;

    /**
     * Attack cooldown in game ticks.
     */
    private final int cooldown;

    /**
     * Projectile info. May contain multiple entries for weapon-specific projectiles.
     */
    private final List<Projectile> projectiles;

    /**
     * Whether this attack uses a continuous animation (e.g. blowpipe).
     */
    private final boolean continuousAnimation;

    /**
     * Category of attack.
     */
    private final Category category;

    /**
     * Whether this attack is an "unknown" type.
     */
    private final boolean unknown;

    public AttackDefinition(int protoId, String name, int[] weaponIds, int[] animationIds,
                            int cooldown, List<Projectile> projectiles,
                            boolean continuousAnimation, Category category) {
        this.protoId = protoId;
        this.name = name;
        this.weaponIds = weaponIds;
        this.animationIds = animationIds;
        this.cooldown = cooldown;
        this.projectiles = projectiles != null ? projectiles : Collections.emptyList();
        this.continuousAnimation = continuousAnimation;
        this.category = category;
        this.unknown = protoId == 0 || (name != null && name.startsWith("UNKNOWN"));
    }

    /**
     * Returns whether this attack matches the given proto enum value.
     *
     * @param proto Enum value.
     * @return True if the attack matches, false otherwise.
     */
    public boolean is(PlayerAttack proto) {
        return this.protoId == proto.getNumber();
    }

    /**
     * Returns whether this attack can be performed with the given weapon.
     */
    public boolean hasWeapon(int weaponId) {
        for (int wid : weaponIds) {
            if (wid == weaponId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether this attack has any associated projectiles.
     */
    public boolean hasProjectile() {
        return !projectiles.isEmpty();
    }

    /**
     * Returns the projectile for the given weapon ID.
     * First checks for a weapon-specific projectile, then falls back to any
     * generic projectile.
     *
     * @param weaponId The weapon ID to look up.
     * @return The projectile for the weapon, or null if none.
     */
    @Nullable
    public Projectile getProjectileForWeapon(int weaponId) {
        if (projectiles.isEmpty()) {
            return null;
        }

        Projectile generic = null;

        for (Projectile p : projectiles) {
            if (p.getWeaponId() == weaponId) {
                return p;
            }
            if (!p.isWeaponSpecific() && generic == null) {
                generic = p;
            }
        }

        return generic;
    }

    /**
     * Creates an AttackDefinition from a proto AttackDefinition message.
     */
    public static AttackDefinition fromProto(io.blert.proto.AttackDefinition proto) {
        List<Projectile> projectiles;

        if (proto.getWeaponProjectilesCount() > 0) {
            projectiles = proto.getWeaponProjectilesList().stream()
                    .map(Projectile::fromProto)
                    .collect(Collectors.toList());
        } else if (proto.hasProjectile()) {
            projectiles = Collections.singletonList(Projectile.fromProto(proto.getProjectile()));
        } else {
            projectiles = Collections.emptyList();
        }

        return new AttackDefinition(
                proto.getIdValue(),
                proto.getName(),
                proto.getWeaponIdsList().stream().mapToInt(Integer::intValue).toArray(),
                proto.getAnimationIdsList().stream().mapToInt(Integer::intValue).toArray(),
                proto.getCooldown(),
                projectiles,
                proto.getContinuousAnimation(),
                Category.fromProto(proto.getCategory())
        );
    }

    /**
     * Loads attack definitions from a JSON input stream.
     */
    public static List<AttackDefinition> loadFromJson(InputStream inputStream) throws IOException {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<JsonAttackDefinition>>() {
        }.getType();
        try (Reader r = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            List<JsonAttackDefinition> jsonDefs = gson.fromJson(r, listType);
            return jsonDefs.stream().map(JsonAttackDefinition::toAttackDefinition).collect(Collectors.toList());
        }
    }

    private static class JsonAttackDefinition {
        int protoId;
        String name;
        int[] weaponIds;
        int[] animationIds;
        int cooldown;
        JsonProjectile projectile;
        JsonProjectile[] weaponProjectiles;
        boolean continuousAnimation;
        String category;

        private static class JsonProjectile {
            int id;
            int startCycleOffset;
            int weaponId;
        }

        AttackDefinition toAttackDefinition() {
            List<Projectile> projectileList;

            if (weaponProjectiles != null && weaponProjectiles.length > 0) {
                projectileList = new ArrayList<>();
                for (JsonProjectile jp : weaponProjectiles) {
                    projectileList.add(new Projectile(jp.id, jp.startCycleOffset, jp.weaponId));
                }
            } else if (projectile != null) {
                projectileList = Collections.singletonList(
                        new Projectile(projectile.id, projectile.startCycleOffset));
            } else {
                projectileList = Collections.emptyList();
            }

            Category cat = Category.MELEE;
            if (category != null) {
                try {
                    cat = Category.valueOf(category);
                } catch (IllegalArgumentException ignored) {
                }
            }

            return new AttackDefinition(
                    protoId,
                    name,
                    weaponIds != null ? weaponIds : new int[0],
                    animationIds != null ? animationIds : new int[0],
                    cooldown,
                    projectileList,
                    continuousAnimation,
                    cat
            );
        }
    }
}