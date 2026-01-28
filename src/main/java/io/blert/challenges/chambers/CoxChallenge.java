package io.blert.challenges.chambers;

import io.blert.challenges.chambers.rooms.tekton.TektonDataTracker;
import io.blert.challenges.chambers.rooms.crabs.CrabsDataTracker;
import io.blert.challenges.chambers.rooms.icedemon.IceDemonDataTracker;
import io.blert.challenges.chambers.rooms.guardians.GuardiansDataTracker;
import io.blert.challenges.chambers.rooms.mystics.MysticsDataTracker;
import io.blert.challenges.chambers.rooms.shamans.ShamansDataTracker;
import io.blert.challenges.chambers.rooms.vanguards.VanguardsDataTracker;
import io.blert.challenges.chambers.rooms.thieving.ThievingDataTracker;
import io.blert.challenges.chambers.rooms.muttadiles.MuttadilesDataTracker;
import io.blert.challenges.chambers.rooms.vespula.VespulaDataTracker;
import io.blert.challenges.chambers.rooms.tightrope.TightropeDataTracker;
import io.blert.challenges.chambers.rooms.vasa.VasaDataTracker;
import io.blert.challenges.chambers.rooms.olm.OlmDataTracker;
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

    // Tracks if the player is currently in a raid instance (like CoxTimersPlugin)
    private boolean inRaid = false;
    private static final Pattern RAID_ENTRY_REGEX =
            Pattern.compile("The raid has begun!.*");
    private static final Pattern RAID_COMPLETION_REGEX =
            Pattern.compile("Congratulations - your raid is complete!.*");
    // private static final Pattern RAID_COMPLETION_REGEX =
    //     Pattern.compile("(Combat room|Puzzle) `.*` complete! Duration: .*");
    private static final Pattern ROOM_COMPLETE_REGEX =
            Pattern.compile("(Combat room|Puzzle) `.*` complete! Duration: .*");
    // private static final Pattern ROOM_COMPLETE_REGEX =
    //     Pattern.compile("Congratulations - your raid is complete!.*");
    private static final Pattern FLOOR_COMPLETE_REGEX =
            Pattern.compile(".* level complete! Duration: .*");
    private static final Pattern MAP_LAYOUT_REGEX =
            Pattern.compile("Map Layout:.*");

    private List<Raider> party = new ArrayList<>();
    @Nullable
    private RoomDataTracker roomDataTracker = null;
    private int reportedChallengeTime = -1;
    private int startTick = -1;
    private int endTick = -1;
    private int raidStartTick = -1;
    private boolean isChallengeMode = false; // Track if this is a Challenge Mode raid
    private boolean hitpointsScaled = false; // Track if we've already scaled the hitpoints
    private int maxCombatLevel = -1; // Cached max combat level for the party
    private int avgMiningLevel = -1; // Cached average mining level for the party

    private static final List<Stage> COX_ROOM_ORDER = List.of(
        // Stage.COX_THIEVING,
        // Stage.COX_GUARDIANS,
        // Stage.COX_ICE_DEMON,
        // Stage.COX_VASA,
        // Stage.COX_VESPULA,
        // Stage.COX_MUTTADILE,
        // Stage.COX_VANGUARDS,
        // Stage.COX_MYSTICS,

        Stage.COX_TEKTON,
        Stage.COX_CRABS,
        Stage.COX_ICE_DEMON,
        Stage.COX_SHAMANS,
        Stage.COX_FLOOR_1,
        Stage.COX_VANGUARDS,
        Stage.COX_THIEVING,
        Stage.COX_VESPULA,
        Stage.COX_TIGHTROPE,
        Stage.COX_FLOOR_2,
        Stage.COX_GUARDIANS,
        Stage.COX_VASA,
        Stage.COX_MYSTICS,
        Stage.COX_MUTTADILE,
        Stage.COX_FLOOR_3,
        Stage.COX_OLM
        // Stage.COX_OLM,
        // Stage.COX_OLM,
        // Stage.COX_OLM,
        // Stage.COX_OLM,
        // Stage.COX_OLM
    );

    public CoxChallenge(Client client, EventBus eventBus, ClientThread clientThread) {
        super(Challenge.COX, client, eventBus, clientThread);
    }

    @Override
    protected void onInitialize() {
        reportedChallengeTime = -1;
        party.clear();
        isChallengeMode = false;
        hitpointsScaled = false;
    }

    @Override
    protected void onTerminate() {
        if (roomDataTracker != null) {
            final RoomDataTracker tracker = roomDataTracker; // Capture non-null value
            tracker.terminate();
            removeEventHandler(tracker);
            getEventBus().unregister(tracker);
            roomDataTracker = null;
        }
        party.clear();
        reportedChallengeTime = -1;
        startTick = -1;
        endTick = -1;
        raidStartTick = -1;
        inRaid = false;
        isChallengeMode = false;
        hitpointsScaled = false;
        maxCombatLevel = -1;
        avgMiningLevel = -1;
        
        // Reset all NPC scaled hitpoints
        for (CoxNpc npc : CoxNpc.values()) {
            npc.resetScaledHitpoints();
        }
        setState(ChallengeState.INACTIVE);
    }

    @Override
    protected void onTick() {
        // Only use instance check for inRaid logic (like CoxTimersPlugin)
        boolean inInstance = client.getTopLevelWorldView() != null && client.getTopLevelWorldView().isInstance();

        if (inRaid && !inInstance) {
            log.info("Detected raid exit: inInstance={}", inInstance);
            endRaid();
            inRaid = false;
        } else if (!inRaid && inInstance) {
            log.info("Detected raid entry: inInstance={}", inInstance);
            inRaid = true;
            // Optionally: could auto-start raid here if desired
        }

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
        if (entryMatcher.matches() && getState() == ChallengeState.INACTIVE) {
            log.info("Detected raid start from chat message.");
            startRaid();
            return;
        }
        // Map Layout detection for Challenge Mode
        Matcher mapLayoutMatcher = MAP_LAYOUT_REGEX.matcher(stripped);
        if (mapLayoutMatcher.find()) {
            boolean wasChallengeMode = isChallengeMode;
            isChallengeMode = stripped.toLowerCase().contains("challenge mode");
            if (isChallengeMode != wasChallengeMode) {
                log.info("Detected raid mode: {} (from message: '{}')", isChallengeMode ? "Challenge Mode" : "Normal Mode", stripped);
                // Scale hitpoints when challenge mode is detected (after team composition is known)
                if (getState() == ChallengeState.ACTIVE) {
                    scaleNpcHitpoints();
                }
            }
        }

        // Raid end
        Matcher completionMatcher = RAID_COMPLETION_REGEX.matcher(stripped);
        if (completionMatcher.matches() && getState() == ChallengeState.ACTIVE) {
            int currentTick = getRelativeTick();
            int currentTick2 = getTick();
            final RoomDataTracker tracker = roomDataTracker; // Capture non-null value
            log.info("Last Room, currentTick(relative): {}, currentTick: {}, startTick: {}, endTick: {}", currentTick, currentTick2, startTick, endTick);
            tracker.finishLastRoom(currentTick);

            // Clean up the old tracker properly
            getEventBus().unregister(tracker);
            removeEventHandler(tracker);
            roomDataTracker = null;
            endRaid();
            return;
        }

        // Floor complete (dispatch floor event)
        Matcher floorMatcher = FLOOR_COMPLETE_REGEX.matcher(stripped);
        if (floorMatcher.find() && getState() == ChallengeState.ACTIVE) {
            int currentTick = getRelativeTick();
            final RoomDataTracker tracker = roomDataTracker; // Capture non-null value
            tracker.finishRoom(currentTick);
            log.info("Finished floor at tick {}", currentTick);
            getEventBus().unregister(tracker);
            removeEventHandler(tracker);
            roomDataTracker = null;
            // Implicitly start tracking the next room
            Stage nextStage = getNextStage(tracker.getStage());
            if (nextStage != null) {
                roomDataTracker = createRoomDataTracker(nextStage);
                final RoomDataTracker newTracker = roomDataTracker; // Capture new non-null value
                if (newTracker != null) {
                    newTracker.startRoom(currentTick);
                    log.info("Started tracking next room {} at tick {}", nextStage, currentTick);
                }
            }
        }

        // Room complete (dispatch room event)
        Matcher roomMatcher = ROOM_COMPLETE_REGEX.matcher(stripped);
        if (roomMatcher.find() && roomDataTracker != null) {
            int currentTick = getRelativeTick();
            final RoomDataTracker tracker = roomDataTracker; // Capture non-null value
            tracker.finishRoom(currentTick);

            // Clean up the old tracker properly
            getEventBus().unregister(tracker);
            removeEventHandler(tracker);
            roomDataTracker = null;

            // Implicitly start tracking the next room
            Stage nextStage = getNextStage(tracker.getStage());
            if (nextStage != null) {
                roomDataTracker = createRoomDataTracker(nextStage);
                final RoomDataTracker newTracker = roomDataTracker; // Capture new non-null value
                if (newTracker != null) {
                    newTracker.startRoom(currentTick);
                    log.info("Started tracking next room {} at tick {}", nextStage, currentTick);
                }
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
        inRaid = true;
        setState(ChallengeState.ACTIVE);
        raidStartTick = client.getTickCount();
        startTick = 0; // relative to raid start
        
        // Add the local player to the party to ensure scale is at least 1
        addRaider(new Raider(client.getLocalPlayer(), true));
        
        log.info("Starting COX raid with scale {} (party size: {})", getScale(), getParty().size());

        // Start the first room data tracker
        Stage firstStage = getNextStage(null); // Gets the first room in COX_ROOM_ORDER
        if (firstStage != null) {
            roomDataTracker = createRoomDataTracker(firstStage);
            final RoomDataTracker tracker = roomDataTracker; // Capture non-null value
            if (tracker != null) {
                tracker.startRoom(0); // first room starts at tick 0
                log.info("Started tracking first room {} at tick {}", firstStage, 0);
            }
        }

        dispatchEvent(new ChallengeStartEvent(getChallenge(), getChallengeMode(), getStage(), getPartyUsernames(), false));
        log.info("Chambers of Xeric raid started at tick {}", startTick);
    }

    private void endRaid() {
        inRaid = false;
        setState(ChallengeState.ENDING);
        endTick = getRelativeTick();
        dispatchEvent(new ChallengeEndEvent(reportedChallengeTime, endTick - startTick));
        log.info("Chambers of Xeric raid ended at tick {}", endTick - startTick);
        onTerminate();
        // State is reset to INACTIVE in onTerminate()
    }

    private List<String> getPartyUsernames() {
        List<String> names = new ArrayList<>();
        for (Raider r : party) {
            names.add(r.getUsername());
        }
        return names;
    }

    /**
     * Returns whether this raid is Challenge Mode.
     * This is determined by checking if the "Map Layout:" chat message contains "Challenge Mode".
     * @return true if this is a Challenge Mode raid, false otherwise
     */
    public boolean isChallengeMode() {
        return isChallengeMode;
    }

    /**
     * Scales all COX NPC base hitpoints once based on team size and challenge mode.
     * After calling this, getBaseHitpoints() will return the scaled values.
     */
    public void scaleNpcHitpoints() {
        if (hitpointsScaled) {
            return; // Already scaled
        }
        
        int partySize = getScale(); // PS: Party size
        boolean challengeMode = isChallengeMode(); // CM: Challenge mode
        
        // Get party stats - cache them for back-calculation later
        maxCombatLevel = client.getLocalPlayer().getCombatLevel(); // CMB: Maximum player combat level in the party
        int maxHpLevel = client.getRealSkillLevel(net.runelite.api.Skill.HITPOINTS);      // HP: Maximum player HP level in the party  
        avgMiningLevel = client.getRealSkillLevel(net.runelite.api.Skill.MINING);     // MIN: Average mining level of the party
        
        log.info("Scaling NPC hitpoints for PS:{} CMB:{} HP:{} MIN:{} CM:{}", 
                    partySize, maxCombatLevel, maxHpLevel, avgMiningLevel, challengeMode);
        
        // Scale all NPCs that will be used in this raid
        for (CoxNpc npc : CoxNpc.values()) {
            if (npc.getOriginalBaseHitpoints() > 0) {
                int scaledHp = calculateScaledHitpoints(npc, partySize, maxCombatLevel, maxHpLevel, avgMiningLevel, challengeMode);
                npc.setScaledHitpoints(scaledHp);
                log.debug("NPC {} scaled from {} to {} HP", npc, npc.getOriginalBaseHitpoints(), scaledHp);
            }
        }
        
        hitpointsScaled = true;
        log.info("NPC hitpoint scaling complete");
    }

    /**
     * Calculates scaled hitpoints for a COX NPC using the proper formulas:
     * - All but Guardians and Olm: hp=base_hp*CMB/126*(PS/2+0.5)*(CM?1.5:1)
     * - Guardians: hp=(151+MIN)*CMB/126*(PS/2+0.5)*(CM?1.5:1)
     * - Olm: hp=300*(PS-PS/8*3+1) (ignored for now as requested)
     */
    private int calculateScaledHitpoints(CoxNpc npc, int partySize, int maxCombatLevel, int maxHpLevel, int avgMiningLevel, boolean challengeMode) {
        int baseHp = npc.getOriginalBaseHitpoints();
        // Skip NPCs without defined HP
        if (baseHp <= 0) {
            return baseHp;
        }
        // Skip Olm for now as requested
        if (npc == CoxNpc.OLM_HEAD || npc == CoxNpc.OLM_MAGE_HAND || npc == CoxNpc.OLM_MELEE_HAND) {
            return baseHp;
        }
        double scaledHp;
        if (npc == CoxNpc.GUARDIAN_1 || npc == CoxNpc.GUARDIAN_2) {
            // Guardians: hp=(151+MIN)*CMB/126*(PS/2+0.5)*(CM?1.5:1)
            scaledHp = (151.0 + avgMiningLevel) * maxCombatLevel / 126.0 * (partySize / 2.0 + 0.5) * (challengeMode ? 1.5 : 1.0);
        } else {
            // All but Guardians and Olm: hp=base_hp*CMB/126*(PS/2+0.5)*(CM?1.5:1)
            scaledHp = baseHp * maxCombatLevel / 126.0 * (partySize / 2.0 + 0.5) * (challengeMode ? 1.5 : 1.0);
        }
        return (int) Math.round(scaledHp);
    }

    /**
     * Gets the scaled hitpoints for an NPC. After scaleNpcHitpoints() is called,
     * this is equivalent to calling coxNpc.getBaseHitpoints().
     * 
     * @param coxNpc The NPC to get scaled hitpoints for
     * @return The scaled hitpoints
     */
    public int getScaledHitpoints(CoxNpc coxNpc) {
        if (!hitpointsScaled) {
            log.warn("Hitpoints not yet scaled! Call scaleNpcHitpoints() first.");
            return coxNpc.getOriginalBaseHitpoints();
        }
        return coxNpc.getBaseHitpoints();
    }

    /**
     * Back-calculates the actual combat level based on observed NPC HP.
     * When an NPC spawns with HP that doesn't match our expected scaling,
     * we can reverse the formula to find what combat level would produce that HP.
     * 
     * Formula: CMB = (actual_hp * 126) / (base_hp * (PS/2+0.5) * (CM?1.5:1))
     * For Guardians: CMB = (actual_hp * 126) / ((151+MIN) * (PS/2+0.5) * (CM?1.5:1))
     * 
     * @param npc The NPC type
     * @param actualHp The observed HP from varbit
     * @return The calculated combat level (rounded to nearest integer)
     */
    public int backCalculateCombatLevel(CoxNpc npc, int actualHp) {
        int partySize = getScale();
        boolean challengeMode = isChallengeMode();
        
        double partySizeMultiplier = (partySize / 2.0 + 0.5);
        double challengeModeMultiplier = challengeMode ? 1.5 : 1.0;
        
        double calculatedCombatLevel;
        if (npc == CoxNpc.GUARDIAN_1 || npc == CoxNpc.GUARDIAN_2) {
            // Guardians use (151+MIN) as base
            double effectiveBase = 151.0 + avgMiningLevel;
            calculatedCombatLevel = (actualHp * 126.0) / (effectiveBase * partySizeMultiplier * challengeModeMultiplier);
        } else {
            // Regular NPCs use their base HP
            int baseHp = npc.getOriginalBaseHitpoints();
            calculatedCombatLevel = (actualHp * 126.0) / (baseHp * partySizeMultiplier * challengeModeMultiplier);
        }
        
        return (int) Math.round(calculatedCombatLevel);
    }

    /**
     * Updates the combat level and rescales all NPC hitpoints.
     * Called when we detect actual HP values that indicate a different combat level than expected.
     * 
     * @param newCombatLevel The new combat level to use for scaling
     */
    public void updateCombatLevelAndRescale(int newCombatLevel) {
        if (newCombatLevel == maxCombatLevel) {
            return; // No change needed
        }
        
        log.info("Updating combat level from {} to {} and rescaling NPCs", maxCombatLevel, newCombatLevel);
        maxCombatLevel = newCombatLevel;
        
        // Rescale all NPCs with the new combat level
        int partySize = getScale();
        boolean challengeMode = isChallengeMode();
        int maxHpLevel = client.getRealSkillLevel(net.runelite.api.Skill.HITPOINTS);
        
        for (CoxNpc npc : CoxNpc.values()) {
            if (npc.getOriginalBaseHitpoints() > 0) {
                int scaledHp = calculateScaledHitpoints(npc, partySize, maxCombatLevel, maxHpLevel, avgMiningLevel, challengeMode);
                npc.setScaledHitpoints(scaledHp);
                log.debug("NPC {} rescaled to {} HP (CMB={})", npc, scaledHp, maxCombatLevel);
            }
        }
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
                RoomDataTracker tracker = new TektonDataTracker(this, stage, client);
                
                // Register with event bus to receive NPC spawn/despawn events
                getEventBus().register(tracker);
                addEventHandler(tracker);
                
                return tracker;
            case COX_CRABS:
                log.info("Creating CrabsDataTracker for stage {}", stage);
                RoomDataTracker crabsTracker = new CrabsDataTracker(this, stage, client);
                
                // Register with event bus to receive NPC spawn/despawn events
                getEventBus().register(crabsTracker);
                addEventHandler(crabsTracker);
                
                return crabsTracker;
            case COX_ICE_DEMON:
                log.info("Creating IceDemonDataTracker for stage {}", stage);
                RoomDataTracker iceDemonTracker = new IceDemonDataTracker(this, stage, client);
                
                // Register with event bus to receive NPC spawn/despawn events
                getEventBus().register(iceDemonTracker);
                addEventHandler(iceDemonTracker);
                
                return iceDemonTracker;
            case COX_SHAMANS:
                log.info("Creating ShamansDataTracker for stage {}", stage);
                RoomDataTracker shamansTracker = new ShamansDataTracker(this, stage, client);
                
                // Register with event bus to receive NPC spawn/despawn events
                getEventBus().register(shamansTracker);
                addEventHandler(shamansTracker);
                
                return shamansTracker;
            case COX_VANGUARDS:
                log.info("Creating VanguardsDataTracker for stage {}", stage);
                RoomDataTracker vanguardsTracker = new VanguardsDataTracker(this, stage, client);
                
                // Register with event bus to receive NPC spawn/despawn events
                getEventBus().register(vanguardsTracker);
                addEventHandler(vanguardsTracker);
                
                return vanguardsTracker;
            case COX_THIEVING:
                log.info("Creating ThievingDataTracker for stage {}", stage);
                RoomDataTracker thievingTracker = new ThievingDataTracker(this, stage, client);
                
                // Register with event bus to receive NPC spawn/despawn events
                getEventBus().register(thievingTracker);
                addEventHandler(thievingTracker);
                
                return thievingTracker;
            case COX_VESPULA:
                log.info("Creating VespulaDataTracker for stage {}", stage);
                RoomDataTracker vespulaTracker = new VespulaDataTracker(this, stage, client);
                
                // Register with event bus to receive NPC spawn/despawn events
                getEventBus().register(vespulaTracker);
                addEventHandler(vespulaTracker);
                
                return vespulaTracker;
            case COX_TIGHTROPE:
                log.info("Creating TightropeDataTracker for stage {}", stage);
                RoomDataTracker tightropeTracker = new TightropeDataTracker(this, stage, client);
                
                // Register with event bus to receive NPC spawn/despawn events
                getEventBus().register(tightropeTracker);
                addEventHandler(tightropeTracker);
                
                return tightropeTracker;
            case COX_GUARDIANS:
                log.info("Creating GuardiansDataTracker for stage {}", stage);
                RoomDataTracker guardiansTracker = new GuardiansDataTracker(this, stage, client);
                
                // Register with event bus to receive NPC spawn/despawn events
                getEventBus().register(guardiansTracker);
                addEventHandler(guardiansTracker);
                
                return guardiansTracker;
            case COX_VASA:
                log.info("Creating VasaDataTracker for stage {}", stage);
                RoomDataTracker vasaTracker = new VasaDataTracker(this, stage, client);
                
                // Register with event bus to receive NPC spawn/despawn events
                getEventBus().register(vasaTracker);
                addEventHandler(vasaTracker);
                
                return vasaTracker;
            case COX_MYSTICS:
                log.info("Creating MysticsDataTracker for stage {}", stage);
                RoomDataTracker mysticsTracker = new MysticsDataTracker(this, stage, client);
                
                // Register with event bus to receive NPC spawn/despawn events
                getEventBus().register(mysticsTracker);
                addEventHandler(mysticsTracker);
                
                return mysticsTracker;
            case COX_MUTTADILE:
                log.info("Creating MuttadilesDataTracker for stage {}", stage);
                RoomDataTracker muttadilesTracker = new MuttadilesDataTracker(this, stage, client);
                
                // Register with event bus to receive NPC spawn/despawn events
                getEventBus().register(muttadilesTracker);
                addEventHandler(muttadilesTracker);
                
                return muttadilesTracker;
            case COX_OLM:
                log.info("Creating OlmDataTracker for stage {}", stage);
                RoomDataTracker olmTracker = new OlmDataTracker(this, stage, client);
                
                // Register with event bus to receive NPC spawn/despawn events
                getEventBus().register(olmTracker);
                addEventHandler(olmTracker);
                
                return olmTracker;
            default:
                log.info("Creating generic CoxRoomDataTracker for stage {}", stage);
                RoomDataTracker genericTracker = new CoxRoomDataTracker(this, stage, client);
                
                // Register with event bus
                getEventBus().register(genericTracker);
                addEventHandler(genericTracker);
                
                return genericTracker;
        }
    }

    // Add methods for party management, room tracking, etc. as needed.

    private static final int COX_LOBBY_REGION_ID = 4919;
    // COX lobby area coordinates (center point from Mount Quidamortem bank)
    private static final int COX_LOBBY_X = 1232;
    private static final int COX_LOBBY_Y = 3572;
    private static final int COX_LOBBY_Z = 0;
    private static final int COX_LOBBY_RADIUS = 5; // Tiles from center point
    private boolean enteredLobby = false;

    @Override
    public boolean containsLocation(net.runelite.api.coords.WorldPoint worldPoint) {
        if (!enteredLobby) {
            // Check if player is in the COX lobby area
            int regionId = worldPoint.getRegionID();
            if (regionId == COX_LOBBY_REGION_ID) {
                int dx = Math.abs(worldPoint.getX() - COX_LOBBY_X);
                int dy = Math.abs(worldPoint.getY() - COX_LOBBY_Y);
                int dz = Math.abs(worldPoint.getPlane() - COX_LOBBY_Z);
                
                if (dz == 0 && dx <= COX_LOBBY_RADIUS && dy <= COX_LOBBY_RADIUS) {
                    enteredLobby = true;
                    return true;
                }
            }
            return false;
        }
        // After initial lobby entry, always return true
        return true;
    }
}