package io.blert.challenges.chambers;

import io.blert.core.Stage;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;

/**
 * Represents different locations within the Chambers of Xeric raid.
 * Uses instance template chunk detection for precise location identification.
 */
public enum CoxLocation {
    LOBBY,
    FLOOR_END,
    SCAVENGERS,
    FARMING,
    TEKTON,
    CRABS,
    ICE_DEMON,
    SHAMANS,
    VANGUARDS,
    THIEVING,
    VESPULA,
    TIGHTROPE,
    GUARDIANS,
    VASA,
    MYSTICS,
    MUTTADILES,
    OLM,
    UNKNOWN;

    /**
     * Determines the COX location from a world point using instance template chunk detection.
     * 
     * @param client The game client
     * @param worldPoint The world point to check
     * @return The COX location, or null if not in a COX instance
     */
    @Nullable
    public static CoxLocation fromWorldPoint(Client client, WorldPoint worldPoint) {
        if (client == null || worldPoint == null) {
            return null;
        }

        var topLevelWorldView = client.getTopLevelWorldView();
        if (topLevelWorldView == null || !topLevelWorldView.isInstance()) {
            return null;
        }

        var templateChunks = topLevelWorldView.getInstanceTemplateChunks();
        if (templateChunks == null) {
            return null;
        }

        int plane = worldPoint.getPlane();
        int sceneX = worldPoint.getX() - topLevelWorldView.getBaseX();
        int sceneY = worldPoint.getY() - topLevelWorldView.getBaseY();

        // Check bounds before accessing array
        if (plane < 0 || plane >= templateChunks.length ||
            sceneX < 0 || sceneX >= 104 || sceneY < 0 || sceneY >= 104) {
            return null;
        }

        int chunkX = sceneX / 8;
        int chunkY = sceneY / 8;

        if (chunkX >= templateChunks[plane].length || 
            chunkY >= templateChunks[plane][chunkX].length) {
            return null;
        }

        int template = templateChunks[plane][chunkX][chunkY];
        int roomType = CoxRoomUtil.getRoomType(template);

        return fromRoomType(roomType);
    }

    /**
     * Maps a CoxRoomUtil room type constant to a CoxLocation.
     * 
     * @param roomType The room type constant from CoxRoomUtil
     * @return The corresponding CoxLocation
     */
    public static CoxLocation fromRoomType(int roomType) {
        switch (roomType) {
            case CoxRoomUtil.FL_START:
                return LOBBY;
            case CoxRoomUtil.FL_END:
                return FLOOR_END;
            case CoxRoomUtil.SCAVENGERS:
                return SCAVENGERS;
            case CoxRoomUtil.FARMING:
                return FARMING;
            case CoxRoomUtil.TEKTON:
                return TEKTON;
            case CoxRoomUtil.CRABS:
                return CRABS;
            case CoxRoomUtil.ICE_DEMON:
                return ICE_DEMON;
            case CoxRoomUtil.SHAMANS:
                return SHAMANS;
            case CoxRoomUtil.VANGUARDS:
                return VANGUARDS;
            case CoxRoomUtil.THIEVING:
                return THIEVING;
            case CoxRoomUtil.VESPULA:
                return VESPULA;
            case CoxRoomUtil.TIGHTROPE:
                return TIGHTROPE;
            case CoxRoomUtil.GUARDIANS:
                return GUARDIANS;
            case CoxRoomUtil.VASA:
                return VASA;
            case CoxRoomUtil.MYSTICS:
                return MYSTICS;
            case CoxRoomUtil.MUTTADILES:
                return MUTTADILES;
            case CoxRoomUtil.OLM:
                return OLM;
            default:
                return UNKNOWN;
        }
    }

    /**
     * Checks if this location is a combat room.
     * 
     * @return true if this is a combat room (not lobby, floor end, scavengers, or farming)
     */
    public boolean isCombatRoom() {
        switch (this) {
            case TEKTON:
            case CRABS:
            case ICE_DEMON:
            case SHAMANS:
            case VANGUARDS:
            case THIEVING:
            case VESPULA:
            case TIGHTROPE:
            case GUARDIANS:
            case VASA:
            case MYSTICS:
            case MUTTADILES:
            case OLM:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if this location is a puzzle room.
     * 
     * @return true if this is a puzzle room (thieving or tightrope)
     */
    public boolean isPuzzleRoom() {
        return this == THIEVING || this == TIGHTROPE;
    }

    /**
     * Converts this location to the corresponding blert Stage.
     * 
     * @return The corresponding Stage, or null if no mapping exists
     */
    @Nullable
    public Stage toStage() {
        switch (this) {
            case TEKTON:     return Stage.COX_TEKTON;
            case CRABS:      return Stage.COX_CRABS;
            case ICE_DEMON:  return Stage.COX_ICE_DEMON;
            case SHAMANS:    return Stage.COX_SHAMANS;
            case VANGUARDS:  return Stage.COX_VANGUARDS;
            case THIEVING:   return Stage.COX_THIEVING;
            case VESPULA:    return Stage.COX_VESPULA;
            case TIGHTROPE:  return Stage.COX_TIGHTROPE;
            case GUARDIANS:  return Stage.COX_GUARDIANS;
            case VASA:       return Stage.COX_VASA;
            case MYSTICS:    return Stage.COX_MYSTICS;
            case MUTTADILES: return Stage.COX_MUTTADILE;
            case OLM:        return Stage.COX_OLM;
            default:         return null;
        }
    }

    /**
     * Converts a blert Stage to the corresponding CoxLocation.
     * 
     * @param stage The Stage to convert
     * @return The corresponding CoxLocation, or null if no mapping exists
     */
    @Nullable
    public static CoxLocation fromStage(Stage stage) {
        if (stage == null) {
            return null;
        }
        switch (stage) {
            case COX_TEKTON:     return TEKTON;
            case COX_CRABS:      return CRABS;
            case COX_ICE_DEMON:  return ICE_DEMON;
            case COX_SHAMANS:    return SHAMANS;
            case COX_VANGUARDS:  return VANGUARDS;
            case COX_THIEVING:   return THIEVING;
            case COX_VESPULA:    return VESPULA;
            case COX_TIGHTROPE:  return TIGHTROPE;
            case COX_GUARDIANS:  return GUARDIANS;
            case COX_VASA:       return VASA;
            case COX_MYSTICS:    return MYSTICS;
            case COX_MUTTADILE:  return MUTTADILES;
            case COX_OLM:        return OLM;
            case COX_FLOOR_1:
            case COX_FLOOR_2:
            case COX_FLOOR_3:    return FLOOR_END;
            default:             return null;
        }
    }
}