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

package io.blert.json;

import java.util.List;

/**
 * JSON representation of an attack definition received from the server.
 */
public class AttackDefinition {
    public int protoId;
    public String name;
    public List<Integer> weaponIds;
    public List<Integer> animationIds;
    public int cooldown;
    public Projectile projectile;
    public List<Projectile> weaponProjectiles;
    public boolean continuousAnimation;
    public String category;

    public static class Projectile {
        public int id;
        public int startCycleOffset;
        public Integer weaponId;
    }

    public io.blert.core.AttackDefinition toCore() {
        java.util.List<io.blert.core.AttackDefinition.Projectile> projectileList;

        if (weaponProjectiles != null && !weaponProjectiles.isEmpty()) {
            projectileList = new java.util.ArrayList<>();
            for (Projectile jp : weaponProjectiles) {
                int wid = jp.weaponId != null ? jp.weaponId : -1;
                projectileList.add(new io.blert.core.AttackDefinition.Projectile(jp.id, jp.startCycleOffset, wid));
            }
        } else if (projectile != null) {
            projectileList = java.util.Collections.singletonList(
                    new io.blert.core.AttackDefinition.Projectile(projectile.id, projectile.startCycleOffset));
        } else {
            projectileList = java.util.Collections.emptyList();
        }

        io.blert.core.AttackDefinition.Category cat = io.blert.core.AttackDefinition.Category.MELEE;
        if (category != null) {
            try {
                cat = io.blert.core.AttackDefinition.Category.valueOf(category);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return new io.blert.core.AttackDefinition(
                protoId,
                name,
                weaponIds != null ? weaponIds.stream().mapToInt(Integer::intValue).toArray() : new int[0],
                animationIds != null ? animationIds.stream().mapToInt(Integer::intValue).toArray() : new int[0],
                cooldown,
                projectileList,
                continuousAnimation,
                cat
        );
    }
}