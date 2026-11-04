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

package io.blert.events;

import io.blert.core.Stage;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class SpotAnimsEvent extends Event {
    @Getter
    public static class SpotAnim {
        private final int id;
        private final @Nullable Target actor;
        private final @Nullable WorldPoint tile;
        private final int drawTick;
        private final int endTick;

        public static SpotAnim onActor(int id, Target actor, int drawTick, int endTick) {
            return new SpotAnim(id, actor, null, drawTick, endTick);
        }

        public static SpotAnim onTile(int id, WorldPoint tile, int drawTick, int endTick) {
            return new SpotAnim(id, null, tile, drawTick, endTick);
        }

        private SpotAnim(int id, @Nullable Target actor, @Nullable WorldPoint tile, int drawTick, int endTick) {
            this.id = id;
            this.actor = actor;
            this.tile = tile;
            this.drawTick = drawTick;
            this.endTick = endTick;
        }

        @Override
        public String toString() {
            return "(id=" + id + ", actor=" + actor + ", tile=" + tile + ", drawTick=" + drawTick + ", endTick="
                    + endTick + ")";
        }
    }

    private final List<SpotAnim> spotAnims;

    public SpotAnimsEvent(Stage stage, int tick, List<SpotAnim> spotAnims) {
        super(EventType.SPOT_ANIMS, stage, tick, null);
        this.spotAnims = spotAnims;
    }

    @Override
    protected String eventDataString() {
        return "spotAnims=" + spotAnims;
    }
}
