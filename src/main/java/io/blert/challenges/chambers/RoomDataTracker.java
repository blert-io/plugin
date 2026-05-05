package io.blert.challenges.chambers;

import io.blert.core.DataTracker;
import io.blert.core.RecordableChallenge;
// import io.blert.core.Hitpoints;
import io.blert.core.Stage;
import io.blert.events.Event;
import io.blert.events.EventHandler;
import io.blert.events.PlayerDeathEvent;
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
    
    // HP tracking fields
    private boolean shouldUpdateHitpoints = false;
    private int healTick = -1;

    public RoomDataTracker(RecordableChallenge challenge, Stage stage, Client client) {
        super(challenge, client, stage);
        this.stage = stage;
        this.roomEndRegex = Pattern.compile("(Combat room|Puzzle) `.*` complete! Duration: .*");
        // this.roomEndRegex = Pattern.compile("Congratulations - your raid is complete!.*");
    }

    /**
     * Begins tracking data for the room.
     */
    public void startRoom(int currentTick) {
        if (started) return;
        started = true;
        startTick = currentTick;
        
        // Start with no offset so getTick() returns room-relative ticks (0-based for duration matching)
        start();
        
        log.info("Started tracking room {} at tick {}", stage, startTick);
    }

    /**
     * Finishes tracking data for the room.
     */
    public void finishRoom(int currentTick) {
        if (!started) return;
        endTick = currentTick;
        int duration = endTick - startTick;
        finish(true, duration, true);
        started = false;
        log.info("Finished room {} at tick {} with duration {}", stage, endTick, duration);
        // Don't dispatch an event - just finish tracking
        // The COX challenge will handle room transitions internally
    }

    public void finishLastRoom(int currentTick) {
        if (!started) return;
        endTick = currentTick;
        int duration = endTick - startTick;
        finish(true, duration, true);
        started = false;
        log.info("Finished Last room {} at tick {} with duration {}", stage, endTick, duration);
        // Don't dispatch an event - just finish tracking
        // The COX challenge will handle final room completion internally
    }

    @Override
    protected void onTick() {
        // Room tick logic if needed
    }
    
    /**
     * Sets flag to update hitpoints from varbit on next tick.
     * Called by subclass varbit handlers.
     */
    protected void setShouldUpdateHitpoints(boolean shouldUpdate) {
        this.shouldUpdateHitpoints = shouldUpdate;
    }
    
    /**
     * Gets whether hitpoints should be updated from varbit.
     */
    protected boolean getShouldUpdateHitpoints() {
        return shouldUpdateHitpoints;
    }
    
    /**
     * Sets the heal tick for HP update timing.
     */
    protected void setHealTick(int tick) {
        this.healTick = tick;
    }
    
    /**
     * Gets the heal tick for HP update timing.
     */
    protected int getHealTick() {
        return healTick;
    }
    protected int getStartTick() {
        return startTick;
    }

    @Override
    protected void onMessage(ChatMessage chatMessage) {
        String stripped = Text.removeTags(chatMessage.getMessage());
        Matcher matcher = roomEndRegex.matcher(stripped);
        if (matcher.find()) {
            finishRoom(getStartTick() + getTick());
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
