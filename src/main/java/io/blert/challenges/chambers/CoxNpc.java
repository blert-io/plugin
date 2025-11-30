package io.blert.challenges.chambers;

import lombok.Getter;

import java.util.Optional;

@Getter
public enum CoxNpc {
    TEKTON(new int[]{7540, 7541, 7542, 7545}, 300),
    TEKTON_ENRAGED(new int[]{7543, 7544}, 300),
    // ICE_DEMON_FROZEN(7584, 140),
    // ICE_DEMON_THAWED(7585, 140), // Ice Demon can spawn with different IDs
    ICE_DEMON(new int[]{7584, 7585}, 280), // Use single ID for Ice Demon; track state separately if needed
    LIZARDMAN_SHAMAN_1(7573, 190),
    LIZARDMAN_SHAMAN_2(7574, 190),
    VANGUARD_INITIAL(7525, 180),  // Initial spawn ID before transformation
    VANGUARD_MELEE(7527, 180),
    VANGUARD_RANGED(7528, 180),
    VANGUARD_MAGIC(7529, 180),
    CORRUPTED_SCAVENGER(7602, 100),
    ABYSSAL_PORTAL(7533, 250),
    GUARDIAN_1(new int[]{7569, 7571}, 250), // Guardian 1: live (7569) and dead (7571)
    GUARDIAN_2(new int[]{7570, 7572}, 250), // Guardian 2: live (7570) and dead (7572)
    VASA_NISTIRIO(new int[]{7565, 7566, 7567}, 300),
    GLOWING_CRYSTAL(7568, 120),
    SKELETAL_MYSTIC_1(7604, 160),
    SKELETAL_MYSTIC_2(7605, 160),
    SKELETAL_MYSTIC_3(7606, 160),
    MUTTADILE_LARGE(new int[]{7561, 7563}, 250),
    MUTTADILE_SMALL(7562, 250),
    OLM_MAGE_HAND(new int[]{7550, 7553}, 600),
    OLM_MELEE_HAND(new int[]{7552, 7555}, 600),
    OLM_HEAD(new int[]{7551, 7554}, 800);

    private final int[] ids;
    private final int baseHitpoints;
    private final int hitpoints; // Kept for backward compatibility
    private int scaledHitpoints; // Stores the scaled HP after scaleNpcHitpoints() is called

    public static Optional<CoxNpc> withId(int id) {
        for (CoxNpc npc : values()) {
            if (npc.hasId(id)) {
                return Optional.of(npc);
            }
        }
        return Optional.empty();
    }

    public boolean hasId(int id) {
        for (int npcId : ids) {
            if (npcId == id) return true;
        }
        return false;
    }

    public static boolean isTekton(int id) {
        return TEKTON.hasId(id) || TEKTON_ENRAGED.hasId(id);
    }

    public int getBaseHitpoints() {
        // Return scaled HP if it has been set, otherwise return base HP
        if (scaledHitpoints > 0) {
            return scaledHitpoints;
        }
        
        // Handle NPCs without defined HP
        if (baseHitpoints <= 0) {
            return -1;
        }
        
        return baseHitpoints;
    }

    /**
     * Sets the scaled hitpoints for this NPC. Called by CoxChallenge.scaleNpcHitpoints()
     * @param scaledHp The scaled hitpoints value
     */
    public void setScaledHitpoints(int scaledHp) {
        this.scaledHitpoints = scaledHp;
    }

    /**
     * Resets scaled hitpoints back to 0. Called when raid ends.
     */
    public void resetScaledHitpoints() {
        this.scaledHitpoints = 0;
    }

    /**
     * Gets the original base hitpoints (unscaled)
     * @return The original base hitpoints
     */
    public int getOriginalBaseHitpoints() {
        return baseHitpoints;
    }

    CoxNpc(int[] ids) {
        this(ids, -1);
    }

    CoxNpc(int[] ids, int baseHitpoints) {
        this.ids = ids;
        this.baseHitpoints = baseHitpoints;
        this.hitpoints = baseHitpoints; // For backward compatibility
        this.scaledHitpoints = 0; // Will be set by scaleNpcHitpoints()
    }

    CoxNpc(int id) {
        this(new int[]{id}, -1);
    }

    CoxNpc(int id, int baseHitpoints) {
        this(new int[]{id}, baseHitpoints);
    }
}
