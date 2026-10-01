/*
 * Copyright (c) 2026 Alexei Frolov
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

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

public class AttackRegistryTest {
    @Test
    public void findChecksAttackerGraphics() {
        final int scythe = 22325;
        final int purgingStaff = 29594;
        final int scytheAnimation = 8056;
        final int darkDemonbaneAnimation = 8977;
        final int darkDemonbaneGraphic = 1869;
        final int greaterCorruptionGraphic = 1878;

        AttackRegistry registry = new AttackRegistry();
        registry.updateDefinitions(List.of(
                attack(38, scythe, scytheAnimation, new int[0]),
                attack(112, purgingStaff, darkDemonbaneAnimation, new int[] {darkDemonbaneGraphic})));

        assertEquals(
                38,
                registry.find(scythe, scytheAnimation, g -> false).orElseThrow().getProtoId());
        assertEquals(
                112,
                registry.find(
                                purgingStaff,
                                darkDemonbaneAnimation,
                                g -> g == darkDemonbaneGraphic || g == greaterCorruptionGraphic)
                        .orElseThrow()
                        .getProtoId());
        assertFalse(registry.find(purgingStaff, darkDemonbaneAnimation, g -> g == greaterCorruptionGraphic)
                .isPresent());
        assertFalse(registry.find(scythe, darkDemonbaneAnimation, g -> g == greaterCorruptionGraphic)
                .isPresent());
        assertTrue(registry.find(scythe, darkDemonbaneAnimation, g -> g == darkDemonbaneGraphic)
                .orElseThrow()
                .isUnknown());
    }

    private static AttackDefinition attack(int protoId, int weaponId, int animationId, int[] attackerGraphicIds) {
        return new AttackDefinition(
                protoId,
                "TEST_" + protoId,
                new int[] {weaponId},
                new int[] {animationId},
                attackerGraphicIds,
                5,
                null,
                false,
                0,
                Integer.MAX_VALUE,
                AttackDefinition.Category.MELEE);
    }
}
