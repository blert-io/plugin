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
import io.blert.challenges.tfa.bosses.guardian.GuardianDataTracker;
import io.blert.challenges.tfa.bosses.moia.MoiaDataTracker;
import io.blert.challenges.tfa.bosses.ocellus.OcellusDataTracker;
import io.blert.challenges.tfa.bosses.xarth.XarthDataTracker;
import io.blert.core.*;
import io.blert.events.ChallengeEndEvent;
import io.blert.events.ChallengeStartEvent;
import io.blert.events.StageUpdateEvent;
import io.blert.util.ChatText;
import io.blert.util.DeferredTask;
import io.blert.util.Tick;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public class ArchiveChallenge extends RecordableChallenge {
    private static final int TFA_PARTY_VARBIT = 16386;
    private static final int TFA_PARTY_LIST_COMPONENT_ID = 63897605;

    private static final int TFA_VARBIT_NO_PARTY = 0;
    private static final int TFA_VARBIT_IN_PARTY = 1;
    private static final int TFA_VARBIT_RAID_STARTED = 2;

    private static final String RAID_ENTRY_MESSAGE_1P = "You enter the Archive...";
    private static final Pattern RAID_ENTRY_REGEX_3P =
            Pattern.compile("(.+) has entered the Fractured Archive\\. Step inside to join (her|him|them)\\.\\.\\.");
    private static final String RAID_WIPE_MESSAGE = "You failed to survive the Fractured Archive.";
    private static final String RAID_ABANDON_MESSAGE = "You abandon the raid and leave the Fractured Archive.";
    private static final Pattern BOSS_COMPLETION_TOTAL_REGEX = Pattern.compile("Challenge complete: .+?\\. Duration: "
            + Tick.TIME_STRING_REGEX + "\\. Total: (?<total>" + Tick.TIME_STRING_REGEX + ")");
    private static final Pattern RAID_COMPLETION_REGEX =
            Pattern.compile("The Fractured Archive: Total completion time: (" + Tick.TIME_STRING_REGEX + ").*");

    private Location location = Location.ELSEWHERE;
    private boolean locationChangedThisTick = false;

    @Nullable
    private BossDataTracker bossDataTracker = null;

    private @Nullable DeferredTask deferredTask = null;

    // Ticks since Guardian start, from the latest boss completion message.
    private int reportedOverallTime = -1;

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
    protected void onInitialize() {
        // TODO(frolv): Temporary, for comparing TFA lifecycle against captures. Remove once verified.
        addEventHandler((clientTick, event) -> {
            switch (event.getType()) {
                case CHALLENGE_START:
                case CHALLENGE_UPDATE:
                case CHALLENGE_END:
                case PLAYER_DEATH:
                    log.warn("TFA-VERIFY tick={} state={} {}", clientTick, getState(), event);
                    break;
                case STAGE_UPDATE:
                    StageUpdateEvent update = (StageUpdateEvent) event;
                    log.warn(
                            "TFA-VERIFY tick={} state={} {} inGameTicks={} accurate={} overall={}",
                            clientTick,
                            getState(),
                            event,
                            update.getInGameTicks().orElse(-1),
                            update.isAccurate(),
                            reportedOverallTime);
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    protected void onTerminate() {
        clearBossDataTracker();
        resetParty();

        // The same instance records the next raid, so drop anything still
        // pending from this one.
        deferredTask = null;
        clearPendingEvents();
    }

    @Override
    protected void onTick() {
        updateLocation();

        // The in-raid party list is only written once the player enters, so unlike ToB, keep reading the lobby party
        // after the leader has entered.
        int partyState = client.getVarbitValue(TFA_PARTY_VARBIT);
        if (location == Location.LOBBY
                && getState().isInactive()
                && (partyState == TFA_VARBIT_IN_PARTY || partyState == TFA_VARBIT_RAID_STARTED)) {
            initializePartyFromLobby();
        }

        if (deferredTask != null) {
            deferredTask.tick();
        }

        if (getState().isInactive() && location.inRaid()) {
            queueRaidStart(true);
        } else if (getState() == ChallengeState.STARTING && location.inRaid()) {
            setState(ChallengeState.ACTIVE);
        } else if (!getState().isInactive() && !location.inRaid()) {
            // The player has left the raid.
            switch (getState()) {
                case COMPLETE:
                    setState(ChallengeState.INACTIVE);
                    break;
                case ACTIVE:
                    queueRaidEnd(ChallengeState.INACTIVE);
                    break;
                case STARTING:
                case PREPARING:
                    if (locationChangedThisTick) {
                        // If the player leaves the raid before it starts, cancel the start.
                        queueRaidEnd(ChallengeState.INACTIVE);
                    }
                    break;
                case ENDING:
                case INACTIVE:
                    // Do nothing.
                    break;
            }
        }

        locationChangedThisTick = false;
        if (!inChallenge()) {
            return;
        }

        if (bossDataTracker != null) {
            bossDataTracker.checkStart();
            bossDataTracker.tick();
        }
    }

    @Nullable
    @Override
    protected Stage getStage() {
        return bossDataTracker != null ? bossDataTracker.getStage() : null;
    }

    /**
     * Invokes a callback for each username displayed in the TFA orb list.
     *
     * @param callback Function to run on each user.
     */
    public void forEachOrb(BiConsumer<Integer, String> callback) {
        // ID of the client string containing the username of the first member in a TFA party. Subsequent party
        // members' usernames (if present) occupy the following seven IDs.
        final int TFA_P1_VARCSTR_ID = 1573;
        for (int player = 0; player < 8; player++) {
            String username = client.getVarcStrValue(TFA_P1_VARCSTR_ID + player);
            if (!Strings.isNullOrEmpty(username)) {
                callback.accept(player, sanitizePartyWidgetName(username));
            }
        }
    }

    private void queueRaidStart(boolean reinitializeParty) {
        log.info("Starting new TFA raid");
        setState(ChallengeState.PREPARING);
        reportedOverallTime = -1;

        if (reinitializeParty) {
            // The in-raid party list is written 7 ticks after the player's entry message;
            // delay for an additional tick before reading, plus the current tick.
            final int TICKS_TO_DELAY_ORB_CHECK = 9;
            deferredTask = new DeferredTask(
                    () -> {
                        initializePartyFromOrbs();
                        startRaid();
                    },
                    TICKS_TO_DELAY_ORB_CHECK);
        } else {
            deferredTask = new DeferredTask(this::startRaid, 1);
        }
    }

    private void startRaid() {
        deferredTask = null;

        updateLocation();
        setState(location.inRaid() ? ChallengeState.ACTIVE : ChallengeState.STARTING);

        boolean spectator = !playerIsInChallenge(client.getLocalPlayer().getName());

        List<String> names = getParty().stream().map(Raider::getUsername).collect(Collectors.toList());
        Stage stage = null;

        if (bossDataTracker != null) {
            // Raid scale information is not immediately available when the raid starts, so if any NPCs have already
            // spawned, their hitpoints must be corrected after the scale is known.
            bossDataTracker.correctNpcHitpointsForScale(getScale());
            stage = bossDataTracker.getStage();
        }

        dispatchEvent(new ChallengeStartEvent(getChallenge(), getChallengeMode(), stage, names, spectator));

        // Dispatch any events that were queued before the raid started.
        dispatchPendingEvents();
    }

    private void queueRaidEnd(ChallengeState state) {
        queueRaidEnd(state, -1);
    }

    private void queueRaidEnd(ChallengeState state, int overallTime) {
        if (getState() == ChallengeState.PREPARING) {
            // If a raid start is pending, cancel it.
            setState(ChallengeState.INACTIVE);
            clearBossDataTracker();
            clearPendingEvents();
            resetParty();
            deferredTask = null;
            log.info("TFA raid ended before it started");
            return;
        }

        setState(ChallengeState.ENDING);
        deferredTask = new DeferredTask(() -> endRaid(state, overallTime), 3);
    }

    private void endRaid(ChallengeState state, int overallTime) {
        log.info("TFA raid ended");

        // Spectators should send a soft end so they don't affect other recorders.
        boolean soft = !playerIsInChallenge(client.getLocalPlayer().getName());

        clearBossDataTracker();
        setState(state);
        resetParty();

        getClientThread().invokeAtTickEnd(() -> dispatchEvent(new ChallengeEndEvent(-1, overallTime, soft)));
    }

    /**
     * Checks the location of the logged-in player, initializing the appropriate {@link BossDataTracker} if a new boss
     * room has been entered.
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

        if (location == Location.ELSEWHERE) {
            // ELSEWHERE should only occur during a raid when logging out then
            // back in (since actually going ELSEWHERE would terminate the
            // ArchiveChallenge), or during a scene load, when the player's
            // position is meaningless. Don't reinitialize the data tracker.
            locationChangedThisTick = true;
            return;
        }

        Stage stage = location.getStage();
        if (stage != null && (bossDataTracker == null || bossDataTracker.getStage() != stage)) {
            initializeBossDataTracker(stage);
        }

        locationChangedThisTick = true;
    }

    @Override
    public void onNpcSpawned(NpcSpawned npcSpawned) {
        updateLocation();
        super.onNpcSpawned(npcSpawned);
    }

    @Override
    public void onVarbitChanged(VarbitChanged varbit) {
        if (varbit.getVarbitId() == TFA_PARTY_VARBIT) {
            if (getState().isInactive() && varbit.getValue() == TFA_VARBIT_NO_PARTY) {
                // Party was disbanded while in the lobby.
                resetParty();
            } else if (getState().isInactive() && varbit.getValue() == TFA_VARBIT_RAID_STARTED) {
                // A raid start due to a varbit change usually means that the player is joining late, rejoining, or
                // spectating. Party and mode information is not immediately available.
                // In TFA, it also changes for members still in the lobby when the leader enters, so use the lobby
                // party if one was read.
                log.debug("Raid started via varbit change");
                queueRaidStart(getParty().isEmpty());
            } else if (getState().inChallenge() && varbit.getValue() < TFA_VARBIT_RAID_STARTED) {
                switch (getState()) {
                    case COMPLETE:
                        setState(ChallengeState.INACTIVE);
                        break;
                    case STARTING:
                    case PREPARING:
                        // Raid ended while the player is still in the lobby.
                        log.debug("Raid ended via varbit change while in state {}", getState());
                        queueRaidEnd(ChallengeState.INACTIVE);
                        break;
                }
            }
        }

        super.onVarbitChanged(varbit);
    }

    @Override
    public void onChatMessage(ChatMessage message) {
        updateLocation();

        if (message.getType() == ChatMessageType.GAMEMESSAGE) {
            String stripped = ChatText.stripFormatting(message.getMessage());

            if (getState().isInactive()) {
                // Listen for a chat message indicating the start of a raid, and queue the start action immediately
                // instead of waiting to enter.
                if (stripped.equals(RAID_ENTRY_MESSAGE_1P)) {
                    log.debug("Raid started via 1p chat message");
                    queueRaidStart(getParty().isEmpty());
                    super.onChatMessage(message);
                    return;
                }

                Matcher matcher = RAID_ENTRY_REGEX_3P.matcher(stripped);
                if (matcher.matches()) {
                    log.debug("Raid started via 3p chat message (leader: {})", matcher.group(1));
                    queueRaidStart(getParty().isEmpty());
                }
                super.onChatMessage(message);
                return;
            }

            Matcher matcher = BOSS_COMPLETION_TOTAL_REGEX.matcher(stripped);
            if (matcher.find()) {
                reportedOverallTime = Tick.fromTimeString(matcher.group("total"))
                        .map(Pair::getLeft)
                        .orElse(-1);
            }

            if (getState() == ChallengeState.ACTIVE
                    && (stripped.equals(RAID_WIPE_MESSAGE) || stripped.equals(RAID_ABANDON_MESSAGE))) {
                if (bossDataTracker != null) {
                    bossDataTracker.wipe();
                }
                // The raid ends before the player is moved out of it, so it
                // remains complete until they leave.
                queueRaidEnd(ChallengeState.COMPLETE);
            }

            matcher = RAID_COMPLETION_REGEX.matcher(stripped);
            if (getState() == ChallengeState.ACTIVE && matcher.matches()) {
                int overallTime =
                        Tick.fromTimeString(matcher.group(1)).map(Pair::getLeft).orElse(-1);
                queueRaidEnd(ChallengeState.COMPLETE, overallTime);
            }
        }

        super.onChatMessage(message);
    }

    /**
     * Collects party members' names from the lobby party widget.
     */
    private void initializePartyFromLobby() {
        var partyWidget = client.getWidget(TFA_PARTY_LIST_COMPONENT_ID);
        if (partyWidget == null) {
            return;
        }

        resetParty();

        Arrays.stream(partyWidget.getText().split("<br>"))
                .map(ArchiveChallenge::sanitizePartyWidgetName)
                .filter(s -> !s.equals("-"))
                .forEach(s ->
                        addRaider(new Raider(s, s.equals(client.getLocalPlayer().getName()))));
    }

    /** Removes formatting from widget text. */
    private static String sanitizePartyWidgetName(String displayed) {
        return Text.sanitize(ChatText.stripFormatting(displayed));
    }

    private void initializePartyFromOrbs() {
        resetParty();
        String localPlayer = client.getLocalPlayer().getName();
        forEachOrb((orb, username) -> addRaider(new Raider(username, username.equals(localPlayer))));
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
            case TFA_MOIA:
                bossDataTracker = new MoiaDataTracker(this, client, recordingConfig);
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
            case TFA_OCELLUS:
                bossDataTracker = new OcellusDataTracker(this, client, recordingConfig);
                break;
            default:
                return;
        }

        log.info("Initialized boss data tracker for {}", stage);
        dispatchEvent(StageUpdateEvent.entered(stage));
        getParty().forEach(Raider::resetForNewRoom);
    }
}
