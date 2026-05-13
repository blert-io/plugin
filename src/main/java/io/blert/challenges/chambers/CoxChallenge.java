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
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final Pattern FLOOR_COMPLETE_REGEX =
            Pattern.compile(".* level complete! Duration: .*");
    private static final Pattern MAP_LAYOUT_REGEX =
            Pattern.compile("Map Layout:.*");

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

    // Obstacle tracking for collision-flag room completion detection (dey0 methodology).
    // Indexed by CoxRoomUtil room type constant; obstacleP[i] == -1 means not registered.
    private final int[] obstacleP = new int[16];
    private final int[] obstacleX = new int[16];
    private final int[] obstacleY = new int[16];

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

    public CoxChallenge(Client client, ClientThread clientThread) {
        super(Challenge.COX, client, clientThread);
    }

    @Nullable
    @Override
    protected DataTracker getActiveTracker() {
        return roomDataTracker;
    }

    @Override
    protected void onInitialize() {
        reportedChallengeTime = -1;
        isChallengeMode = false;
        hitpointsScaled = false;
    }

    @Override
    protected void onTerminate() {
        if (roomDataTracker != null) {
            final RoomDataTracker tracker = roomDataTracker; // Capture non-null value
            tracker.terminate();
            removeEventHandler(tracker);
            roomDataTracker = null;
        }
        resetParty();
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
        Arrays.fill(obstacleP, -1);
        Arrays.fill(obstacleX, -1);
        Arrays.fill(obstacleY, -1);
        currentLocation = null;
        enteredLobby = false;
        setState(ChallengeState.INACTIVE);
    }

    @Override
    protected void onTick() {
        // Update current location
        if (client.getLocalPlayer() != null) {
            updateCurrentLocation(client.getLocalPlayer().getWorldLocation());
        }
        
        // Only use instance check for inRaid logic (like CoxTimersPlugin)
        boolean inInstance = client.getTopLevelWorldView() != null && client.getTopLevelWorldView().isInstance();

        if (inRaid && !inInstance) {
            log.info("Detected raid exit: inInstance={}", inInstance);
            if (getState() == ChallengeState.ACTIVE) {
                endRaid(ChallengeState.COMPLETE);
            }
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

        // Poll collision flags to detect room completion (dey0 methodology).
        if (getState() == ChallengeState.ACTIVE && roomDataTracker != null) {
            Stage currentStage = roomDataTracker.getStage();
            for (int i = 0; i < 16; i++) {
                if (obstacleP[i] == -1) continue;
                if (CoxRoomUtil.roomTypeToStage(i) != currentStage) continue;
                int p = obstacleP[i];
                int sceneX = obstacleX[i] - client.getTopLevelWorldView().getBaseX();
                int sceneY = obstacleY[i] - client.getTopLevelWorldView().getBaseY();
                if (p != client.getTopLevelWorldView().getPlane() || sceneX < 0 || sceneX >= 104 || sceneY < 0 || sceneY >= 104) {
                    obstacleP[i] = -1;
                    continue;
                }
                var collisionMaps = client.getTopLevelWorldView().getCollisionMaps();
                if (collisionMaps == null) {
                    continue;
                }
                int flags = collisionMaps[p].getFlags()[sceneX][sceneY];
                if ((flags & 0x100) == 0) {
                    int completionTick = getRelativeTick();
                    log.info("Room complete via collision flag: stage={}, tick={}", currentStage, completionTick);
                    obstacleP[i] = -1;
                    final RoomDataTracker tracker = roomDataTracker;
                    if (tracker == null) {
                        log.warn("Room completion detected but roomDataTracker is null");
                        return;
                    }
                    tracker.finishRoom(completionTick);
                    advanceToNextRoom(tracker, completionTick);
                    break;
                }
            }
        }
    }

    @Nullable
    @Override
    protected Stage getStage() {
        return roomDataTracker != null ? roomDataTracker.getStage() : null;
    }

    @Override
    public void onGameObjectSpawned(GameObjectSpawned event) {
        if (getState() == ChallengeState.ACTIVE && roomDataTracker != null) {
            final RoomDataTracker tracker = roomDataTracker;
            GameObject go = event.getGameObject();
            switch (go.getId()) {
                case 26209: // shamans / thieving / guardians
                case 29741: // mystics
                case 29749: // tightrope
                case 29753: case 29754: case 29755: case 29756: case 29757: // crabs
                case 29876: // ice demon
                case 30016: // vasa
                case 30017: // tekton / vanguards
                case 30018: // muttadiles
                case 30070: // vespula
                    Point pt = go.getSceneMinLocation();
                    int p = go.getPlane();
                    int sceneX = pt.getX();
                    int sceneY = pt.getY();
                    int template = client.getTopLevelWorldView().getInstanceTemplateChunks()[p][sceneX / 8][sceneY / 8];
                    int roomType = CoxRoomUtil.getRoomType(template);
                    if (roomType < 16) {
                        Stage expectedStage = CoxRoomUtil.roomTypeToStage(roomType);
                        if (expectedStage != null && expectedStage == tracker.getStage()) {
                            obstacleP[roomType] = p;
                            obstacleX[roomType] = sceneX + client.getTopLevelWorldView().getBaseX();
                            obstacleY[roomType] = sceneY + client.getTopLevelWorldView().getBaseY();
                            log.debug("Registered obstacle for room type {} (stage {}) at world ({},{})",
                                    roomType, expectedStage, obstacleX[roomType], obstacleY[roomType]);
                        }
                    }
                    break;
            }
        }
        super.onGameObjectSpawned(event);
    }

    @Override
    public void onChatMessage(ChatMessage message) {
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
            // finishLastRoom schedules StageUpdateEvent(COMPLETED) via invokeLater.
            // endRaid must be delayed with its own invokeLater so it runs AFTER that
            // StageUpdateEvent reaches the WebSocketEventHandler, otherwise CHALLENGE_END
            // is sent to the server before the final stage update.
            if (tracker == null) {
                log.warn("Raid completion detected but roomDataTracker is null");
                return;
            }
            tracker.finishLastRoom(currentTick);

            // Clean up the old tracker properly
            removeEventHandler(tracker);
            roomDataTracker = null;
            getClientThread().invokeLater(() -> {
                if (getState() == ChallengeState.ACTIVE) {
                    endRaid(ChallengeState.COMPLETE);
                }
            });
            return;
        }

        // Floor completion detection (like dey0's plugin)
        Matcher floorCompleteMatcher = FLOOR_COMPLETE_REGEX.matcher(stripped);
        if (floorCompleteMatcher.matches() && getState() == ChallengeState.ACTIVE) {
            final RoomDataTracker tracker = roomDataTracker;
            if (tracker == null) {
                log.warn("Floor completion detected but roomDataTracker is null");
                return;
            }
            
            int currentTick = getRelativeTick();
            tracker.finishRoom(currentTick);
            log.info("Finished floor at tick {} (detected via chat message)", currentTick);
            advanceToNextRoom(tracker, currentTick);
            return;
        }
    }


    private int getTick() {
        // Use the actual client tick count for accurate timing
        return client.getTickCount();
    }

    private int getRelativeTick() {
        return client.getTickCount() - raidStartTick;
    }

    private void advanceToNextRoom(RoomDataTracker finishedTracker, int currentTick) {
        removeEventHandler(finishedTracker);
        roomDataTracker = null;
        Stage nextStage = getNextStage(finishedTracker.getStage());
        if (nextStage != null) {
            getClientThread().invokeLater(() -> {
                roomDataTracker = createRoomDataTracker(nextStage);
                final RoomDataTracker newTracker = roomDataTracker;
                if (newTracker != null) {
                    newTracker.startRoom(currentTick);
                    log.info("Started tracking next room {} at tick {}", nextStage, currentTick);
                }
            });
        }
    }

    private void startRaid() {
        inRaid = true;
        setState(ChallengeState.ACTIVE);
        raidStartTick = client.getTickCount();
        startTick = 0; // relative to raid start
        Arrays.fill(obstacleP, -1);
        Arrays.fill(obstacleX, -1);
        Arrays.fill(obstacleY, -1);

        // Add the local player to the party to ensure scale is at least 1
        addRaider(new Raider(client.getLocalPlayer(), true));
        
        log.info("Starting COX raid with scale {} (party size: {})", getScale(), getParty().size());

        // Create the first room data tracker so challenge start includes the initial stage.
        Stage firstStage = getNextStage(null); // Gets the first room in COX_ROOM_ORDER
        if (firstStage != null) {
            roomDataTracker = createRoomDataTracker(firstStage);
        }

        dispatchEvent(new ChallengeStartEvent(getChallenge(), getChallengeMode(), getStage(), getPartyUsernames(), false));
        log.info("Chambers of Xeric raid started at tick {}", startTick);

        // Start the first room after challenge start so stage updates are queued properly.
        final RoomDataTracker tracker = roomDataTracker; // Capture non-null value
        if (tracker != null) {
            tracker.startRoom(0); // first room starts at tick 0
            log.info("Started tracking first room {} at tick {}", firstStage, 0);
        }
    }

    private void endRaid(ChallengeState completionState) {
        inRaid = false;
        setState(ChallengeState.ENDING);
        endTick = getRelativeTick() - 1; // Adjust end tick to be the last tick of the raid, not the tick after completion message
        int overallTime = endTick - startTick;
        log.info("Raid end detected at tick {} (end/relative tick: {}). Start Tick: {} ticks", getTick(), endTick, startTick);
        // Use parsed completion time if available, otherwise fall back to measured overall time.
        int challengeTime = reportedChallengeTime > 0 ? reportedChallengeTime : overallTime;
        log.info("Chambers of Xeric raid ended: challenge={}, overall={} ticks", challengeTime, overallTime);
        dispatchEvent(new ChallengeEndEvent(overallTime, overallTime));
        log.info("Chambers of Xeric raid ended: challenge={}, overall={} ticks", challengeTime, overallTime);
        onTerminate();
        // onTerminate() resets state to INACTIVE; override with the actual completion state.
        setState(completionState);
    }

    private List<String> getPartyUsernames() {
        List<String> names = new ArrayList<>();
        for (Raider r : getParty()) {
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
                
                addEventHandler(tracker);
                
                return tracker;
            case COX_CRABS:
                log.info("Creating CrabsDataTracker for stage {}", stage);
                RoomDataTracker crabsTracker = new CrabsDataTracker(this, stage, client);
                
                addEventHandler(crabsTracker);
                
                return crabsTracker;
            case COX_ICE_DEMON:
                log.info("Creating IceDemonDataTracker for stage {}", stage);
                RoomDataTracker iceDemonTracker = new IceDemonDataTracker(this, stage, client);
                
                addEventHandler(iceDemonTracker);
                
                return iceDemonTracker;
            case COX_SHAMANS:
                log.info("Creating ShamansDataTracker for stage {}", stage);
                RoomDataTracker shamansTracker = new ShamansDataTracker(this, stage, client);
                
                addEventHandler(shamansTracker);
                
                return shamansTracker;
            case COX_VANGUARDS:
                log.info("Creating VanguardsDataTracker for stage {}", stage);
                RoomDataTracker vanguardsTracker = new VanguardsDataTracker(this, stage, client);
                
                addEventHandler(vanguardsTracker);
                
                return vanguardsTracker;
            case COX_THIEVING:
                log.info("Creating ThievingDataTracker for stage {}", stage);
                RoomDataTracker thievingTracker = new ThievingDataTracker(this, stage, client);
                
                addEventHandler(thievingTracker);
                
                return thievingTracker;
            case COX_VESPULA:
                log.info("Creating VespulaDataTracker for stage {}", stage);
                RoomDataTracker vespulaTracker = new VespulaDataTracker(this, stage, client);
                
                addEventHandler(vespulaTracker);
                
                return vespulaTracker;
            case COX_TIGHTROPE:
                log.info("Creating TightropeDataTracker for stage {}", stage);
                RoomDataTracker tightropeTracker = new TightropeDataTracker(this, stage, client);
                
                addEventHandler(tightropeTracker);
                
                return tightropeTracker;
            case COX_GUARDIANS:
                log.info("Creating GuardiansDataTracker for stage {}", stage);
                RoomDataTracker guardiansTracker = new GuardiansDataTracker(this, stage, client);
                
                addEventHandler(guardiansTracker);
                
                return guardiansTracker;
            case COX_VASA:
                log.info("Creating VasaDataTracker for stage {}", stage);
                RoomDataTracker vasaTracker = new VasaDataTracker(this, stage, client);
                
                addEventHandler(vasaTracker);
                
                return vasaTracker;
            case COX_MYSTICS:
                log.info("Creating MysticsDataTracker for stage {}", stage);
                RoomDataTracker mysticsTracker = new MysticsDataTracker(this, stage, client);
                
                addEventHandler(mysticsTracker);
                
                return mysticsTracker;
            case COX_MUTTADILE:
                log.info("Creating MuttadilesDataTracker for stage {}", stage);
                RoomDataTracker muttadilesTracker = new MuttadilesDataTracker(this, stage, client);
                
                addEventHandler(muttadilesTracker);
                
                return muttadilesTracker;
            case COX_OLM:
                log.info("Creating OlmDataTracker for stage {}", stage);
                RoomDataTracker olmTracker = new OlmDataTracker(this, stage, client);
                
                addEventHandler(olmTracker);
                
                return olmTracker;
            default:
                log.info("Creating generic CoxRoomDataTracker for stage {}", stage);
                RoomDataTracker genericTracker = new CoxRoomDataTracker(this, stage, client);
                
                addEventHandler(genericTracker);
                
                return genericTracker;
        }
    }

    // Add methods for party management, room tracking, etc. as needed.

    private static final int COX_LOBBY_REGION_ID = 4919;
    // Fallback coordinates (center point from Mount Quidamortem bank)
    private static final int COX_LOBBY_X = 1232;
    private static final int COX_LOBBY_Y = 3572;
    private static final int COX_LOBBY_Z = 0;
    private static final int COX_LOBBY_RADIUS = 5; // Tiles from center point
    private boolean enteredLobby = false;
    
    @Nullable
    private CoxLocation currentLocation = null;

    /**
     * Gets the current location in the raid.
     * 
     * @return The current CoxLocation, or null if not in a valid COX location
     */
    @Nullable
    public CoxLocation getCurrentLocation() {
        return currentLocation;
    }

    /**
     * Updates the current location based on the player's world point.
     * Called automatically on tick.
     * 
     * @param worldPoint The player's current world point
     */
    private void updateCurrentLocation(net.runelite.api.coords.WorldPoint worldPoint) {
        CoxLocation newLocation = CoxLocation.fromWorldPoint(client, worldPoint);
        if (newLocation != currentLocation) {
            CoxLocation oldLocation = currentLocation;
            currentLocation = newLocation;
            
            if (currentLocation != null && oldLocation != null) {
                log.debug("Location changed: {} -> {}", oldLocation, currentLocation);
            }
        }
    }

    @Override
    public boolean containsLocation(net.runelite.api.coords.WorldPoint worldPoint) {
        if (!enteredLobby) {
            // Use CoxLocation for precise instance template chunk detection
            CoxLocation location = CoxLocation.fromWorldPoint(client, worldPoint);
            if (location != null && location != CoxLocation.UNKNOWN) {
                enteredLobby = true;
                currentLocation = location;
                log.debug("Entered COX raid at location: {}", location);
                return true;
            }
            
            // Fallback to region-based detection for non-instance areas (outside lobby)
            int regionId = worldPoint.getRegionID();
            if (regionId == COX_LOBBY_REGION_ID) {
                int dx = Math.abs(worldPoint.getX() - COX_LOBBY_X);
                int dy = Math.abs(worldPoint.getY() - COX_LOBBY_Y);
                int dz = Math.abs(worldPoint.getPlane() - COX_LOBBY_Z);
                
                if (dz == 0 && dx <= COX_LOBBY_RADIUS && dy <= COX_LOBBY_RADIUS) {
                    enteredLobby = true;
                    currentLocation = CoxLocation.LOBBY;
                    log.debug("Entered COX raid at lobby (fallback detection)");
                    return true;
                }
            }
            return false;
        }
        // After initial lobby entry, always return true
        return true;
    }
}