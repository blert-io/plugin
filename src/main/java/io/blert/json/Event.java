/*
 * Copyright (c) 2025 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.blert.json;

import java.util.List;

/**
 * JSON representation of a game event sent to/from the server.
 */
public class Event {
    // General fields shared by all events
    public int type;
    public String challengeId;
    public Integer stage;
    public int tick;
    public int xCoord;
    public int yCoord;

    // Event-specific fields
    public Player player;
    public Attack playerAttack;
    public Npc npc;
    public NpcAttack npcAttack;
    public Spell playerSpell;

    // ToB event-specific fields
    public List<Coords> maidenBloodSplats;
    public BloatDown bloatDown;
    public List<Coords> bloatHands;
    public NyloWave nyloWave;
    public SoteMaze soteMaze;
    public Integer xarpusPhase;
    public XarpusExhumed xarpusExhumed;
    public XarpusSplat xarpusSplat;
    public Integer verzikPhase;
    public AttackStyle verzikAttackStyle;

    // Colosseum event-specific fields
    public Integer handicap;
    public List<Integer> handicapOptions;

    public static class Player {
        public static final int DATA_SOURCE_PRIMARY = 0;
        public static final int DATA_SOURCE_SECONDARY = 1;

        public String name;
        public int offCooldownTick;
        public Integer hitpoints;
        public Integer prayer;
        public Integer attack;
        public Integer strength;
        public Integer defence;
        public Integer ranged;
        public Integer magic;
        public List<Long> equipmentDeltas;
        public Long activePrayers;
        public int dataSource;

        public static class EquippedItem {
            public int slot;
            public int id;
            public int quantity;
        }
    }

    public static class Npc {
        public int id;
        public long roomId;
        public Integer hitpoints;

        public MaidenCrab maidenCrab;
        public Nylo nylo;
        public VerzikCrab verzikCrab;
    }

    public static class MaidenCrab {
        public int spawn;
        public int position;
        public boolean scuffed;
    }

    public static class Nylo {
        public int wave;
        public long parentRoomId;
        public boolean big;
        public int style;
        public int spawnType;
    }

    public static class VerzikCrab {
        public int phase;
        public int spawn;
    }

    public static class Attack {
        public int type;
        public Player.EquippedItem weapon;
        public Npc target;
        public int distanceToTarget;
    }

    public static class Spell {
        public int type;
        public String targetPlayer;
        public Npc targetNpc;
    }

    public static class NpcAttack {
        public int attack;
        public String target;
    }

    public static class BloatDown {
        public int downNumber;
        public int walkTime;
    }

    public static class NyloWave {
        public int wave;
        public int nylosAlive;
        public int roomCap;
    }

    public static class SoteMaze {
        public static final int MAZE_66 = 0;
        public static final int MAZE_33 = 1;

        public int maze;
        public List<Coords> overworldTiles;
        public List<Coords> overworldPivots;
        public List<Coords> underworldPivots;
    }

    public static class XarpusExhumed {
        public int spawnTick;
        public int healAmount;
        public List<Integer> healTicks;
    }

    public static class XarpusSplat {
        public static final int SOURCE_UNKNOWN = 0;
        public static final int SOURCE_XARPUS = 1;
        public static final int SOURCE_BOUNCE = 2;

        public int source;
        public Coords bounceFrom;
    }

    public static class AttackStyle {
        public static final int STYLE_MELEE = 0;
        public static final int STYLE_RANGE = 1;
        public static final int STYLE_MAGE = 2;

        public int style;
        public int npcAttackTick;
    }
}