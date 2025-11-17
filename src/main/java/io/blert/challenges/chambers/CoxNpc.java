package io.blert.challenges.chambers;

import lombok.Getter;

import java.util.Optional;

@Getter
public enum CoxNpc {
    TEKTON(new int[]{7540, 7541, 7542, 7545}),
    TEKTON_ENRAGED(new int[]{7543, 7544}),
    ICE_DEMON(7585),
    LIZARDMAN_SHAMAN(7573),
    VANGUARD_MELEE(7527),
    VANGUARD_RANGED(7528),
    VANGUARD_MAGIC(7529),
    ABYSSAL_PORTAL(7533),
    VASA_NISTIRIO(7566),
    GLOWING_CRYSTAL(7568),
    SKELETAL_MYSTIC(7604),
    MUTTADILE_LARGE(7561),
    MUTTADILE_SMALL(7562),
    OLM_MAGE_HAND(7553),
    OLM_MELEE_HAND(7555),
    OLM_HEAD(7554);

    private final int[] ids;
    private final int hitpoints;

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

    public int getBaseHitpoints(int scale) {
        if (scale < 1 || scale > 8) {
            return -1;
        }

        // For COX, hitpoints scale with party size
        // These are approximate values - need to be verified with actual game data
        switch (this) {
            case TEKTON:
                return 450;
            case TEKTON_ENRAGED:
                // Base Tekton HP is ~550 for solo, scales up to ~800+ for max team
                return 450;
            default:
                return hitpoints > 0 ? hitpoints * scale : -1;
        }
    }

    CoxNpc(int[] ids) {
        this(ids, -1);
    }

    CoxNpc(int[] ids, int hitpoints) {
        this.ids = ids;
        this.hitpoints = hitpoints;
    }

    CoxNpc(int id) {
        this(new int[]{id});
    }
}
