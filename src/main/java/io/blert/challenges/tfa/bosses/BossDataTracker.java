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

package io.blert.challenges.tfa.bosses;

import io.blert.challenges.tfa.ArchiveChallenge;
import io.blert.core.*;
import io.blert.events.ChallengeUpdateEvent;
import io.blert.util.ChatText;
import io.blert.util.Tick;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.util.Text;

@Slf4j
public abstract class BossDataTracker extends DataTracker {
    private static final int TFA_ORB1_VARBIT = 16387;

    private static final Pattern BOSS_START_REGEX = Pattern.compile("Challenge started: (.+)\\.");
    private static final Pattern BOSS_END_REGEX = Pattern.compile("Challenge complete: (.+?)\\. Duration: (?<duration>"
            + Tick.TIME_STRING_REGEX + ")(\\. Total: (?<total>" + Tick.TIME_STRING_REGEX + "))?");

    // Boss names as they appear in the start and completion messages.
    private static final Map<Stage, String> BOSS_NAMES = Map.of(
            Stage.TFA_GUARDIAN, "Archive Guardian",
            Stage.TFA_EZEKIEL, "Ezekiel",
            Stage.TFA_BORK, "Bork",
            Stage.TFA_MOIA, "Moia",
            Stage.TFA_DREADNOTH, "Dreadnoth the Jellied",
            Stage.TFA_XARTH, "Xarth",
            Stage.TFA_DAGONHAI, "The Dagon'hai",
            Stage.TFA_OCELLUS, "Ocellus");

    protected final ArchiveChallenge archiveChallenge;

    // Bosses who have a specific fight arena in their room, to detect starts.
    @Nullable
    private final WorldArea arena;

    private boolean completionReceived = false;

    protected BossDataTracker(
            ArchiveChallenge archiveChallenge, Client client, ChallengeRecordingConfig recordingConfig, Stage stage) {
        this(archiveChallenge, client, recordingConfig, stage, null);
    }

    protected BossDataTracker(
            ArchiveChallenge archiveChallenge,
            Client client,
            ChallengeRecordingConfig recordingConfig,
            Stage stage,
            @Nullable WorldArea arena) {
        super(archiveChallenge, client, recordingConfig, stage);
        this.archiveChallenge = archiveChallenge;
        this.arena = arena;
    }

    /**
     * Ends the boss fight unsuccessfully at the end of the current tick.
     */
    public void wipe() {
        finish(false);
    }

    /**
     * Starts the boss fight if the room shows it to be under way without its
     * start message having been received.
     */
    public void checkStart() {
        if (getState() == State.NOT_STARTED && !completionReceived && bossIsActive()) {
            log.debug("Stage {} started via activity check", getStage());
            startBoss();
        }
    }

    /**
     * Updates the base and current hitpoints of all NPCs in the room to match the given raid scale.
     *
     * @param scale The raid scale.
     */
    public void correctNpcHitpointsForScale(int scale) {
        getTrackedNpcs().forEach(trackedNpc -> {
            if (trackedNpc instanceof GenericTrackedNpc) {
                ((GenericTrackedNpc) trackedNpc).correctHitpointsForScale(scale);
            }
        });
    }

    /**
     * Returns whether the current state of the room shows the boss fight to be under way.
     */
    protected boolean bossIsActive() {
        if (arena == null) {
            return false;
        }
        return client.getTopLevelWorldView().players().stream()
                .filter(player -> archiveChallenge.playerIsInChallenge(player.getName()))
                .anyMatch(player -> arena.contains(getWorldLocation(player)));
    }

    @Override
    protected void onTick() {
        checkForDeaths();
    }

    @Override
    protected final void onMessage(ChatMessage chatMessage) {
        String stripped = ChatText.stripFormatting(chatMessage.getMessage());
        Matcher matcher = BOSS_START_REGEX.matcher(stripped);
        if (matcher.matches()) {
            if (isBossStartMessage(matcher.group(1))) {
                startBoss();
            }
            return;
        }

        matcher = BOSS_END_REGEX.matcher(stripped);
        if (matcher.find() && isBossStartMessage(matcher.group(1))) {
            completionReceived = true;
            finish(matcher.group("duration"), matcher.group("total"));
        }
    }

    private boolean isBossStartMessage(String name) {
        String expected = BOSS_NAMES.get(getStage());
        if (expected == null || expected.equals(name)) {
            return true;
        }

        log.warn("Stage {} ignoring message for boss \"{}\"", getStage(), name);
        return false;
    }

    private void startBoss() {
        if (getState() != State.NOT_STARTED) {
            return;
        }

        start();

        // Players may leave between rooms; send the current party when the room starts.
        if (archiveChallenge.getState() != ChallengeState.PREPARING) {
            List<String> party = new ArrayList<>();
            archiveChallenge.forEachOrb((orb, username) -> party.add(username));
            dispatchEvent(new ChallengeUpdateEvent(archiveChallenge.getChallengeMode(), party));
        }
    }

    private void checkForDeaths() {
        if (archiveChallenge.getState() == ChallengeState.PREPARING) {
            // The party isn't read from the orbs until the raid starts.
            return;
        }

        final int tick = getTick();

        // Check party orb health for dead players to catch any deaths that occurred before the client joined as a
        // spectator or while the client was disconnected.
        archiveChallenge.forEachOrb((orb, username) -> {
            Raider raider = archiveChallenge.getRaider(Text.standardize(username));
            if (raider == null) {
                log.warn("Player {} is in orb list but not in party", username);
                return;
            }

            // Orb health. 1-27 = health, 30 = dead, 31 = not in the room.
            int orbHealth = client.getVarbitValue(TFA_ORB1_VARBIT + orb);
            if (orbHealth == 30 && !raider.isDead()) {
                raider.setDead(tick);
            }
        });
    }
}
