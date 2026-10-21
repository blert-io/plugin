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

package io.blert.challenges.tfa.bosses.dreadnoth;

import io.blert.challenges.tfa.ArchiveChallenge;
import io.blert.challenges.tfa.bosses.BossDataTracker;
import io.blert.core.ChallengeRecordingConfig;
import io.blert.core.Stage;
import io.blert.core.TrackedNpc;
import java.util.Optional;
import javax.annotation.Nullable;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;

public class DreadnothDataTracker extends BossDataTracker {
    private static final WorldArea ARENA = new WorldArea(4184, 5656, 15, 15, 0);

    public DreadnothDataTracker(
            ArchiveChallenge archiveChallenge, Client client, ChallengeRecordingConfig recordingConfig) {
        super(archiveChallenge, client, recordingConfig, Stage.TFA_DREADNOTH, ARENA);
    }

    @Override
    protected void onTick() {
        super.onTick();
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned event) {
        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned event, @Nullable TrackedNpc trackedNpc) {
        return false;
    }
}
