package io.blert.challenges.chambers;

import io.blert.core.DataTracker;
import io.blert.core.RecordableChallenge;
// import io.blert.core.Hitpoints;
import io.blert.core.Stage;
import io.blert.events.Event;
import io.blert.events.EventHandler;
import io.blert.events.PlayerDeathEvent;
import io.blert.events.chambers.CoxRoomCompleteEvent;
import io.blert.util.Tick;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
// import net.runelite.api.NPC;
import net.runelite.api.Player;
// import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.util.Text;

// import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks Chambers of Xeric room times and dispatches events to the Blert event handler.
 * Timer logic adapted from CoxTimersPlugin.java. Structure follows DelveDataTracker, WaveDataTracker, and RoomDataTracker.
 */
@Slf4j
public abstract class RoomDataTracker extends DataTracker implements EventHandler {
    private final Pattern roomEndRegex;
    private final Stage stage;
    private boolean started = false;
    private int startTick = 0;
    private int endTick = 0;

    public RoomDataTracker(RecordableChallenge challenge, Stage stage, Client client) {
        super(challenge, client, stage);
        this.stage = stage;
        this.roomEndRegex = Pattern.compile("(Combat room|Puzzle) `.*` complete! Duration: .*");
    }

    /**
     * Begins tracking data for the room.
     */
    public void startRoom(int currentTick) {
        if (started) return;
        started = true;
        startTick = currentTick;
        log.info("Started tracking room {} at tick {}", stage, startTick);
    }

    /**
     * Finishes tracking data for the room.
     */
    public void finishRoom(int currentTick) {
        if (!started) return;
        endTick = currentTick;
        started = false;
        log.info("Finished room {} at tick {} with duration {}", stage, endTick, endTick - startTick);
        // Dispatch event to Blert event handler
        dispatchEvent(new CoxRoomCompleteEvent(stage, startTick, endTick));
    }

    @Override
    protected void onTick() {
        // Room tick logic if needed
    }

    @Override
    protected void onMessage(ChatMessage chatMessage) {
        String stripped = Text.removeTags(chatMessage.getMessage());
        Matcher matcher = roomEndRegex.matcher(stripped);
        if (matcher.find()) {
            finishRoom(getTick());
        }
    }

    @Override
    protected void onGameState(GameStateChanged event) {
        if (event.getGameState() == net.runelite.api.GameState.LOGIN_SCREEN && started) {
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer == null) return;
            // Consider logout as a death event
            dispatchEvent(new PlayerDeathEvent(stage, getTick(), getWorldLocation(localPlayer), localPlayer.getName()));
        }
    }

    @Override
    public void handleEvent(int clientTick, Event event) {
        // Custom event handling logic if needed
    }

    protected String formattedRoomTime() {
        return Tick.asTimeString(getTick());
    }
}
