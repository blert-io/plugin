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

import lombok.NonNull;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.NPC;

// TODO(frolv): Move this into TrackedNpc itself once existing uses are gone.
public class GenericTrackedNpc extends BasicTrackedNpc {
    private final ChallengeRecordingConfig.NpcDefinition definition;
    private int lastVarbitValue = -1;

    @Setter
    private int healTick = -1;

    GenericTrackedNpc(@NonNull NPC npc, ChallengeRecordingConfig.NpcDefinition definition, long roomId, int scale) {
        super(npc, definition.getMode(), roomId, new Hitpoints(definition.getBaseHitpoints(scale)));
        this.definition = definition;
    }

    public void correctHitpointsForScale(int scale) {
        int base = definition.getBaseHitpoints(scale);
        int healthRatio = getNpc().getHealthRatio();
        int healthScale = getNpc().getHealthScale();

        if (healthScale > 0 && healthRatio != -1) {
            setHitpoints(Hitpoints.fromRatio(healthRatio / (double) healthScale, base));
        } else {
            // Assume full HP if the bar isn't visible. Varbit NPCs will
            // later self-correct.
            setHitpoints(new Hitpoints(base));
        }
    }

    void updateHitpointsFromVarbit(Client client, int tick) {
        ChallengeRecordingConfig.HitpointsVarbit varbit = definition.getHitpointsVarbit();
        int base = getHitpoints().getBase();
        if (varbit == null || base < 0) {
            return;
        }

        int varbitValue = client.getVarbitValue(varbit.getId());

        if (lastVarbitValue == -1) {
            lastVarbitValue = varbitValue;
            return;
        }
        if (varbitValue == lastVarbitValue) {
            return;
        }

        // The hitpoints varbit is delayed by up to 3 ticks, so don't update immediately following a heal as it may
        // undo the hitpoints added by the heal.
        if (healTick != -1 && tick - healTick <= 3) {
            return;
        }

        lastVarbitValue = varbitValue;
        double ratio = varbitValue / (double) varbit.getScale();
        int updatedHitpoints = (int) (base * ratio);

        // As bosses can have several thousands of hitpoints, the calculated value is not entirely precise.
        // If the current value is within a few hitpoints of the calculated value, assume that it is correct.
        if (Math.abs(getHitpoints().getCurrent() - updatedHitpoints) > 5) {
            setHitpoints(getHitpoints().update(updatedHitpoints));
        }
    }
}
