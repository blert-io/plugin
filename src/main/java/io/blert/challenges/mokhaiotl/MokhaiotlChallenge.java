/*
 * Copyright (c) 2025 Alexei Frolov
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

package io.blert.challenges.mokhaiotl;

import io.blert.core.*;
import io.blert.events.ChallengeEndEvent;
import io.blert.events.ChallengeStartEvent;
import io.blert.util.Location;
import io.blert.util.Tick;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class MokhaiotlChallenge extends RecordableChallenge {
    private static final int MOKHAIOTL_BURROW_OBJECT_ID = 57285;
    private static final Pattern MOKHAIOTL_END_REGEX =
            Pattern.compile("Delve level 1 - 8 duration: (" + Tick.TIME_STRING_REGEX + ")");

    private DelveDataTracker delveDataTracker;
    int delve;
    int recordedChallengeTicks;
    int reportedChallengeTicks;

    public MokhaiotlChallenge(Client client, ClientThread clientThread) {
        super(Challenge.MOKHAIOTL, client, clientThread);
        delveDataTracker = null;
        delve = 0;
        recordedChallengeTicks = 0;
        reportedChallengeTicks = -1;
    }

    @Nullable
    @Override
    protected DataTracker getActiveTracker() {
        return delveDataTracker;
    }

    @Override
    public boolean containsLocation(WorldPoint worldPoint) {
        return MokhaiotlLocation.fromWorldPoint(worldPoint) != MokhaiotlLocation.ELSEWHERE;
    }

    @Override
    protected void onInitialize() {
        reset();
        addRaider(new Raider(client.getLocalPlayer(), true));
    }

    @Override
    protected void onTerminate() {
        if (getState().inChallenge()) {
            log.warn("Terminating Mokhaiotl challenge while still active");
            finishMokhaiotl();
        }
        cleanup();
    }

    @Override
    protected void onTick() {
        updateChallengeState();
        if (getState().isInactive()) {
            return;
        }

        if (delveDataTracker != null) {
            delveDataTracker.tick();

            if (delveDataTracker.completed()) {
                prepareNextDelve();
            }
        }
    }

    @Nullable
    @Override
    protected Stage getStage() {
        return delveDataTracker != null ? delveDataTracker.getStage() : null;
    }

    @Override
    public void onChatMessage(ChatMessage event) {
        if (!getState().isInactive()) {
            Matcher matcher =
                    MokhaiotlChallenge.MOKHAIOTL_END_REGEX.matcher(Text.removeTags(event.getMessage()));
            if (matcher.find()) {
                try {
                    reportedChallengeTicks = Tick.fromTimeString(matcher.group(1)).map(Pair::getLeft).orElse(-1);
                } catch (Exception e) {
                    reportedChallengeTicks = -1;
                }
            }
        }
        super.onChatMessage(event);
    }

    void updateChallengeState() {
        WorldPoint playerLocation = Location.getWorldLocation(client, client.getLocalPlayer().getWorldLocation());
        if (playerLocation == null) {
            return;
        }

        MokhaiotlLocation location = MokhaiotlLocation.fromWorldPoint(playerLocation);

        if (getState().isInactive()) {
            if (location.inMokhaiotl()) {
                startMokhaiotl();
            }
        } else if (!location.inMokhaiotl()) {
            clearDelveDataTracker();
            finishMokhaiotl();
        }
    }

    private void startMokhaiotl() {
        log.info("Starting Mokhaiotl challenge");
        setState(ChallengeState.ACTIVE);
        reportedChallengeTicks = -1;

        // TODO(frolv): What happens if someone turns on their plugin mid-challenge?
        Stage stage = Stage.MOKHAIOTL_DELVE_1;

        List<String> usernames = getParty().stream().map(Raider::getUsername).collect(Collectors.toList());
        dispatchEvent(new ChallengeStartEvent(getChallenge(), ChallengeMode.NO_MODE, stage, usernames, false));

        // Immediately start delve 1.
        prepareNextDelve();
    }

    private void finishMokhaiotl() {
        log.info("Finishing Mokhaiotl challenge with state: {}", getState());
        cleanup();

        dispatchEvent(new ChallengeEndEvent(reportedChallengeTicks, -1));
        setState(ChallengeState.INACTIVE);
    }

    void prepareNextDelve() {
        clearDelveDataTracker();

        delve++;
        if (delve > 8) {
            setState(ChallengeState.COMPLETE);
        }

        delveDataTracker = new DelveDataTracker(this, client, delve);
    }

    private void clearDelveDataTracker() {
        if (delveDataTracker != null) {
            delveDataTracker.terminate();
            delveDataTracker = null;
        }
    }

    private void cleanup() {
        clearDelveDataTracker();
        reset();
    }

    private void reset() {
        delve = 0;
        recordedChallengeTicks = 0;
        reportedChallengeTicks = -1;
    }
}
