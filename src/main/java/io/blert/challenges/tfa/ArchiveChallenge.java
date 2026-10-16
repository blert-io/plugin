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

package io.blert.challenges.tfa;

import io.blert.challenges.tfa.bosses.BossDataTracker;
import io.blert.challenges.tfa.bosses.bork.BorkDataTracker;
import io.blert.challenges.tfa.bosses.dagonhai.DagonhaiDataTracker;
import io.blert.challenges.tfa.bosses.dreadnoth.DreadnothDataTracker;
import io.blert.challenges.tfa.bosses.ezekiel.EzekielDataTracker;
import io.blert.challenges.tfa.bosses.finalboss.FinalBossDataTracker;
import io.blert.challenges.tfa.bosses.guardian.GuardianDataTracker;
import io.blert.challenges.tfa.bosses.necromancer.NecromancerDataTracker;
import io.blert.challenges.tfa.bosses.xarth.XarthDataTracker;
import io.blert.core.Challenge;
import io.blert.core.DataTracker;
import io.blert.core.RecordableChallenge;
import io.blert.core.Stage;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;

@Slf4j
public class ArchiveChallenge extends RecordableChallenge {
    private Location location = Location.ELSEWHERE;

    @Nullable
    private BossDataTracker bossDataTracker = null;

    public ArchiveChallenge(Client client, ClientThread clientThread) {
        super(Challenge.TFA, client, clientThread);
    }

    @Nullable
    @Override
    protected DataTracker getActiveTracker() {
        return bossDataTracker;
    }

    @Override
    public boolean containsLocation(WorldPoint worldPoint) {
        return Location.fromWorldPoint(worldPoint) != Location.ELSEWHERE;
    }

    @Override
    protected void onInitialize() {}

    @Override
    protected void onTerminate() {
        clearBossDataTracker();
    }

    @Override
    protected void onTick() {
        updateLocation();

        if (!inChallenge()) {
            return;
        }

        if (bossDataTracker != null) {
            bossDataTracker.tick();
        }
    }

    @Nullable
    @Override
    protected Stage getStage() {
        return bossDataTracker != null ? bossDataTracker.getStage() : null;
    }

    /**
     * Checks the player's location, initializing a boss tracker if they are in a boss room.
     */
    private void updateLocation() {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return;
        }

        Location loc = Location.fromWorldPoint(WorldPoint.fromLocalInstance(client, player.getLocalLocation()));
        if (location == loc) {
            return;
        }

        log.debug("Location changed to {}", loc);
        location = loc;

        Stage stage = location.getStage();
        if (stage != null && (bossDataTracker == null || bossDataTracker.getStage() != stage)) {
            initializeBossDataTracker(stage);
        }
    }

    private void clearBossDataTracker() {
        if (bossDataTracker != null) {
            bossDataTracker.terminate();
            bossDataTracker = null;
        }
    }

    private void initializeBossDataTracker(Stage stage) {
        clearBossDataTracker();

        var recordingConfig = currentRecordingConfig();
        switch (stage) {
            case TFA_GUARDIAN:
                bossDataTracker = new GuardianDataTracker(this, client, recordingConfig);
                break;
            case TFA_EZEKIEL:
                bossDataTracker = new EzekielDataTracker(this, client, recordingConfig);
                break;
            case TFA_BORK:
                bossDataTracker = new BorkDataTracker(this, client, recordingConfig);
                break;
            case TFA_NECROMANCER:
                bossDataTracker = new NecromancerDataTracker(this, client, recordingConfig);
                break;
            case TFA_DREADNOTH:
                bossDataTracker = new DreadnothDataTracker(this, client, recordingConfig);
                break;
            case TFA_XARTH:
                bossDataTracker = new XarthDataTracker(this, client, recordingConfig);
                break;
            case TFA_DAGONHAI:
                bossDataTracker = new DagonhaiDataTracker(this, client, recordingConfig);
                break;
            case TFA_FINAL:
                bossDataTracker = new FinalBossDataTracker(this, client, recordingConfig);
                break;
            default:
                return;
        }

        log.info("Initialized boss data tracker for {}", stage);
    }
}
