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

package io.blert.challenges.colosseum;

import io.blert.core.BasicTrackedNpc;
import io.blert.core.Hitpoints;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.NPC;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Setter
@Getter
public class Manticore extends BasicTrackedNpc {
    public final static int LOADING_ANIMATION = 10868;
    public final static int ATTACK_ANIMATION = 10869;

    private final static int MAGE_PROJECTILE = 2681;
    private final static int RANGE_PROJECTILE = 2683;

    enum Style {
        MAGE,
        RANGE,
    }

    private @Nullable Style style = null;

    public Manticore(@NotNull NPC npc, long roomId, Hitpoints hitpoints) {
        super(npc, roomId, hitpoints);
    }

    public void updateStyle() {
        if (style != null) {
            return;
        }
        if (getNpc().getAnimation() == LOADING_ANIMATION) {
            if (getNpc().hasSpotAnim(MAGE_PROJECTILE)) {
                style = Style.MAGE;
            } else if (getNpc().hasSpotAnim(RANGE_PROJECTILE)) {
                style = Style.RANGE;
            }
        }
    }
}
