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
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class ProjectilesEvent extends Event {
    @AllArgsConstructor
    @Getter
    public static class Projectile {
        private final int id;
        private final @Nullable Target source;
        private final @Nullable WorldPoint sourceCoords;
        private final @Nullable Target target;
        private final @Nullable WorldPoint targetCoords;
        private final int landingTick;

        @Override
        public String toString() {
            return "(id=" + id + ", source=" + source + ", sourceCoords=" + sourceCoords + ", target=" + target
                    + ", targetCoords=" + targetCoords + ", landingTick=" + landingTick + ")";
        }
    }

    private final List<Projectile> projectiles;

    public ProjectilesEvent(Stage stage, int tick, List<Projectile> projectiles) {
        super(EventType.PROJECTILES, stage, tick, null);
        this.projectiles = projectiles;
    }

    @Override
    protected String eventDataString() {
        return "projectiles=" + projectiles;
    }
}
