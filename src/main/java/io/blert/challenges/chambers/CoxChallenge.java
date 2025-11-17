package io.blert.challenges.chambers;

import io.blert.challenges.chambers.rooms.tekton.TektonDataTracker;
import io.blert.core.*;
import io.blert.events.ChallengeStartEvent;
import io.blert.events.ChallengeEndEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CoxChallenge extends RecordableChallenge {
    private static final Pattern RAID_ENTRY_REGEX =
            Pattern.compile("The raid has begun!.*");
    private static final Pattern RAID_COMPLETION_REGEX =
            Pattern.compile("Congratulations - your raid is complete!.*");
    private static final Pattern ROOM_COMPLETE_REGEX =
            Pattern.compile("(Combat room|Puzzle) `.*` complete! Duration: .*");

    private ChallengeState state = ChallengeState.INACTIVE;
    private List<Raider> party = new ArrayList<>();
    @Nullable
    private RoomDataTracker roomDataTracker = null;
    private int reportedChallengeTime = -1;
    private int startTick = -1;
    private int endTick = -1;
    private int raidStartTick = -1;

    private static final List<Stage> COX_ROOM_ORDER = List.of(
        Stage.COX_TEKTON,
        Stage.COX_CRABS,
        Stage.COX_ICE_DEMON,
        Stage.COX_SHAMANS,
        Stage.COX_VANGUARDS,
        Stage.COX_THIEVING,
        Stage.COX_VESPULA,
        Stage.COX_TIGHTROPE,
        Stage.COX_GUARDIANS,
        Stage.COX_VASA,
        Stage.COX_MYSTICS,
        Stage.COX_MUTTADILE,
        Stage.COX_OLM
    );

    public CoxChallenge(Client client, EventBus eventBus, ClientThread clientThread) {
        super(Challenge.COX, client, eventBus, clientThread);
    }

    @Override
    protected void onInitialize() {
        reportedChallengeTime = -1;
        party.clear();
        state = ChallengeState.INACTIVE;
    }

    @Override
    protected void onTerminate() {
        roomDataTracker = null;
        party.clear();
        reportedChallengeTime = -1;
        state = ChallengeState.INACTIVE;
    }

    @Override
    protected void onTick() {
        // Room tracking logic, e.g. check for entry, tick roomDataTracker, etc.
        if (roomDataTracker != null) {
            roomDataTracker.tick();
        }
    }

    @Nullable
    @Override
    protected Stage getStage() {
        return roomDataTracker != null ? roomDataTracker.getStage() : null;
    }

    @Subscribe(priority = 5)
    private void onChatMessage(ChatMessage message) {
        String stripped = Text.removeTags(message.getMessage());
        // log.info("Chat message received: {}", stripped);

        // Raid start
        Matcher entryMatcher = RAID_ENTRY_REGEX.matcher(stripped);
        if (entryMatcher.matches() && state == ChallengeState.INACTIVE) {
            log.info("Detected raid start from chat message.");
            startRaid();
            return;
        }

        // Raid end
        Matcher completionMatcher = RAID_COMPLETION_REGEX.matcher(stripped);
        if (completionMatcher.matches() && state == ChallengeState.ACTIVE) {
            endRaid();
            return;
        }

        // Room complete (dispatch room event)
        Matcher roomMatcher = ROOM_COMPLETE_REGEX.matcher(stripped);
        if (roomMatcher.find() && roomDataTracker != null) {
            int currentTick = getRelativeTick();
            roomDataTracker.finishRoom(currentTick);

            // Implicitly start tracking the next room
            Stage nextStage = getNextStage(roomDataTracker.getStage());
            if (nextStage != null) {
                roomDataTracker = createRoomDataTracker(nextStage);
                roomDataTracker.startRoom(currentTick);
                log.info("Started tracking next room {} at tick {}", nextStage, currentTick);
            }
        }
    }


    private int getTick() {
        // Use the actual client tick count for accurate timing
        return client.getTickCount();
    }

    private int getRelativeTick() {
        return client.getTickCount() - raidStartTick;
    }

    private void startRaid() {
        state = ChallengeState.ACTIVE;
        raidStartTick = client.getTickCount();
        startTick = 0; // relative to raid start
        // Initialize party, roomDataTracker, etc.
        // Example: party = getPartyFromWidgetOrOrbs();

        // Start the first room data tracker
        Stage firstStage = getNextStage(null); // Gets the first room in COX_ROOM_ORDER
        if (firstStage != null) {
            roomDataTracker = createRoomDataTracker(firstStage);
            roomDataTracker.startRoom(0); // first room starts at tick 0
            log.info("Started tracking first room {} at tick {}", firstStage, 0);
        }

        dispatchEvent(new ChallengeStartEvent(getChallenge(), getChallengeMode(), getStage(), getPartyUsernames(), false));
        log.info("Chambers of Xeric raid started at tick {}", startTick);
    }

    private void endRaid() {
        state = ChallengeState.ENDING;
        endTick = getTick();
        dispatchEvent(new ChallengeEndEvent(reportedChallengeTime, endTick - startTick));
        log.info("Chambers of Xeric raid ended at tick {}", endTick);
        onTerminate();
    }

    private List<String> getPartyUsernames() {
        List<String> names = new ArrayList<>();
        for (Raider r : party) {
            names.add(r.getUsername());
        }
        return names;
    }

    private Stage getNextStage(Stage currentStage) {
        if (currentStage == null) {
            return COX_ROOM_ORDER.get(0); // First room
        }
        int idx = COX_ROOM_ORDER.indexOf(currentStage);
        if (idx >= 0 && idx < COX_ROOM_ORDER.size() - 1) {
            return COX_ROOM_ORDER.get(idx + 1);
        }
        return null;
    }

    private RoomDataTracker createRoomDataTracker(Stage stage) {
        switch (stage) {
            case COX_TEKTON:
                log.info("Creating TektonDataTracker for stage {}", stage);
                return new TektonDataTracker(this, stage, client);
            default:
                log.info("Creating generic CoxRoomDataTracker for stage {}", stage);
                return new CoxRoomDataTracker(this, stage, client);
        }
    }

    // Add methods for party management, room tracking, etc. as needed.

    private boolean enteredLobby = false;

    @Override
    public boolean containsLocation(net.runelite.api.coords.WorldPoint worldPoint) {
        if (!enteredLobby) {
            // Replace with your actual lobby area check
            // if (CoxLocation.LOBBY_AREA.contains(worldPoint)) {
            //     enteredLobby = true;
            //     return true;
            // }
            return true;
        }
        // After initial lobby entry, always return true
        return true;
    }
}