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

import io.blert.challenges.colosseum.Handicap;
import io.blert.challenges.tob.rooms.maiden.MaidenCrab;
import io.blert.challenges.tob.rooms.nylocas.Nylo;
import io.blert.challenges.tob.rooms.sotetseg.Maze;
import io.blert.challenges.tob.rooms.verzik.VerzikCrab;
import io.blert.core.EquipmentSlot;
import io.blert.core.ItemDelta;
import io.blert.core.Stage;
import io.blert.core.TrackedNpc;
import io.blert.events.*;
import io.blert.events.colosseum.HandicapChoiceEvent;
import io.blert.events.tob.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class JsonEventTranslator {
    public static Event toJson(io.blert.events.Event event, @Nullable String challengeId) {
        Event json = new Event();
        json.type = event.getType().getId();
        json.tick = event.getTick();
        json.xCoord = event.getXCoord();
        json.yCoord = event.getYCoord();

        event.getStage().map(Stage::getId).ifPresent(s -> json.stage = s);

        if (challengeId != null) {
            json.challengeId = challengeId;
        }

        switch (event.getType()) {
            case CHALLENGE_START:
            case CHALLENGE_END:
            case CHALLENGE_UPDATE:
            case STAGE_UPDATE:
                throw new AssertionError(String.format("Event type %s has no JSON representation", event.getType()));

            case PLAYER_UPDATE: {
                PlayerUpdateEvent playerUpdateEvent = (PlayerUpdateEvent) event;

                int dataSource = playerUpdateEvent.getSource() == PlayerUpdateEvent.Source.PRIMARY
                        ? Event.Player.DATA_SOURCE_PRIMARY
                        : Event.Player.DATA_SOURCE_SECONDARY;

                Event.Player player = new Event.Player();
                player.dataSource = dataSource;
                player.name = playerUpdateEvent.getUsername();
                player.offCooldownTick = playerUpdateEvent.getOffCooldownTick();

                playerUpdateEvent.getHitpoints().ifPresent(sl -> player.hitpoints = sl.getValue());
                playerUpdateEvent.getPrayer().ifPresent(sl -> player.prayer = sl.getValue());
                playerUpdateEvent.getAttack().ifPresent(sl -> player.attack = sl.getValue());
                playerUpdateEvent.getStrength().ifPresent(sl -> player.strength = sl.getValue());
                playerUpdateEvent.getDefence().ifPresent(sl -> player.defence = sl.getValue());
                playerUpdateEvent.getRanged().ifPresent(sl -> player.ranged = sl.getValue());
                playerUpdateEvent.getMagic().ifPresent(sl -> player.magic = sl.getValue());

                playerUpdateEvent.getActivePrayers().ifPresent(prayers -> player.activePrayers = prayers.getValue());

                if (!playerUpdateEvent.getEquipmentChangesThisTick().isEmpty()) {
                    player.equipmentDeltas = playerUpdateEvent.getEquipmentChangesThisTick().stream()
                            .map(ItemDelta::getValue)
                            .collect(Collectors.toList());
                }

                json.player = player;
                break;
            }

            case PLAYER_ATTACK: {
                PlayerAttackEvent playerAttackEvent = (PlayerAttackEvent) event;
                Event.Attack attack = new Event.Attack();
                attack.type = playerAttackEvent.getAttack().getProtoId();
                attack.distanceToTarget = playerAttackEvent.getDistanceToTarget();

                playerAttackEvent.getWeapon().ifPresent(item -> {
                    attack.weapon = new Event.Player.EquippedItem();
                    attack.weapon.slot = EquipmentSlot.WEAPON.getId();
                    attack.weapon.id = item.getId();
                    attack.weapon.quantity = item.getQuantity();
                });

                if (playerAttackEvent.getTargetNpcId() != -1) {
                    attack.target = new Event.Npc();
                    attack.target.id = playerAttackEvent.getTargetNpcId();
                    attack.target.roomId = playerAttackEvent.getTargetRoomId();
                }

                json.playerAttack = attack;
                json.player = new Event.Player();
                json.player.name = playerAttackEvent.getUsername();
                break;
            }

            case PLAYER_SPELL: {
                PlayerSpellEvent playerSpellEvent = (PlayerSpellEvent) event;
                Event.Spell spell = new Event.Spell();
                spell.type = playerSpellEvent.getSpell().getId();

                if (playerSpellEvent.getTargetPlayer() != null) {
                    spell.targetPlayer = playerSpellEvent.getTargetPlayer();
                } else if (playerSpellEvent.hasNpcTarget()) {
                    spell.targetNpc = new Event.Npc();
                    spell.targetNpc.id = playerSpellEvent.getTargetNpcId();
                    spell.targetNpc.roomId = playerSpellEvent.getTargetNpcRoomId();
                }

                json.playerSpell = spell;
                json.player = new Event.Player();
                json.player.name = playerSpellEvent.getUsername();
                break;
            }

            case PLAYER_DEATH: {
                PlayerDeathEvent playerDeathEvent = (PlayerDeathEvent) event;
                json.player = new Event.Player();
                json.player.name = playerDeathEvent.getUsername();
                break;
            }

            case NPC_SPAWN:
            case NPC_UPDATE:
            case NPC_DEATH:
            case MAIDEN_CRAB_LEAK: {
                NpcEvent npcEvent = (NpcEvent) event;
                Event.Npc npc = new Event.Npc();
                npc.id = npcEvent.getNpcId();
                npc.roomId = npcEvent.getRoomId();
                npc.hitpoints = npcEvent.getHitpoints().getValue();

                if (npcEvent.propertiesChanged()) {
                    addTranslatedNpcProperties(npc, npcEvent);
                }

                json.npc = npc;
                break;
            }

            case NPC_ATTACK: {
                NpcAttackEvent npcAttackEvent = (NpcAttackEvent) event;
                json.npc = new Event.Npc();
                json.npc.id = npcAttackEvent.getNpcId();
                json.npc.roomId = npcAttackEvent.getRoomId();

                Event.NpcAttack npcAttack = new Event.NpcAttack();
                npcAttack.attack = npcAttackEvent.getAttack().getId();
                if (npcAttackEvent.getTarget() != null) {
                    npcAttack.target = npcAttackEvent.getTarget();
                }
                json.npcAttack = npcAttack;
                break;
            }

            case MAIDEN_BLOOD_SPLATS: {
                MaidenBloodSplatsEvent bloodSplatsEvent = (MaidenBloodSplatsEvent) event;
                json.maidenBloodSplats = toCoordsList(bloodSplatsEvent.getBloodSplats());
                break;
            }

            case BLOAT_DOWN: {
                BloatDownEvent bloatDownEvent = (BloatDownEvent) event;
                json.bloatDown = new Event.BloatDown();
                json.bloatDown.downNumber = bloatDownEvent.getDownNumber();
                json.bloatDown.walkTime = bloatDownEvent.getUptime();
                break;
            }

            case NYLO_WAVE_SPAWN:
            case NYLO_WAVE_STALL: {
                NyloWaveEvent nyloWaveEvent = (NyloWaveEvent) event;
                json.nyloWave = new Event.NyloWave();
                json.nyloWave.wave = nyloWaveEvent.getWave();
                json.nyloWave.nylosAlive = nyloWaveEvent.getNyloCount();
                json.nyloWave.roomCap = nyloWaveEvent.getNyloCap();
                break;
            }

            case SOTE_MAZE_PROC:
            case SOTE_MAZE_END: {
                SoteMazeEvent mazeEvent = (SoteMazeEvent) event;
                int maze = mazeEvent.getMaze() == Maze.MAZE_66
                        ? Event.SoteMaze.MAZE_66
                        : Event.SoteMaze.MAZE_33;
                json.soteMaze = new Event.SoteMaze();
                json.soteMaze.maze = maze;
                break;
            }

            case SOTE_MAZE_PATH: {
                SoteMazePathEvent mazePathEvent = (SoteMazePathEvent) event;
                int maze = mazePathEvent.getMaze() == Maze.MAZE_66
                        ? Event.SoteMaze.MAZE_66
                        : Event.SoteMaze.MAZE_33;
                var coords = mazePathEvent.mazeRelativePoints()
                        .map(JsonEventTranslator::worldPointToCoords)
                        .collect(Collectors.toList());

                json.soteMaze = new Event.SoteMaze();
                json.soteMaze.maze = maze;

                switch (mazePathEvent.getTileType()) {
                    case OVERWORLD_TILES:
                        json.soteMaze.overworldTiles = coords;
                        break;
                    case UNDERWORLD_PIVOTS:
                        json.soteMaze.underworldPivots = coords;
                        break;
                    case OVERWORLD_PIVOTS:
                        json.soteMaze.overworldPivots = coords;
                        break;
                }
                break;
            }

            case XARPUS_PHASE: {
                XarpusPhaseEvent xarpusPhaseEvent = (XarpusPhaseEvent) event;
                json.xarpusPhase = xarpusPhaseEvent.getPhase().ordinal();
                break;
            }

            case XARPUS_EXHUMED: {
                XarpusExhumedEvent xarpusExhumedEvent = (XarpusExhumedEvent) event;
                json.xarpusExhumed = new Event.XarpusExhumed();
                json.xarpusExhumed.spawnTick = xarpusExhumedEvent.getSpawnTick();
                json.xarpusExhumed.healAmount = xarpusExhumedEvent.getHealAmount();
                json.xarpusExhumed.healTicks = new ArrayList<>(xarpusExhumedEvent.getHealTicks());
                break;
            }


            case XARPUS_SPLAT: {
                XarpusSplatEvent xarpusSplatEvent = (XarpusSplatEvent) event;
                json.xarpusSplat = new Event.XarpusSplat();
                json.xarpusSplat.source = xarpusSplatEvent.getSource().getId();
                if (xarpusSplatEvent.getBounceFrom() != null) {
                    json.xarpusSplat.bounceFrom =
                            worldPointToCoords(xarpusSplatEvent.getBounceFrom());
                }
                break;
            }

            case VERZIK_PHASE: {
                VerzikPhaseEvent verzikPhaseEvent = (VerzikPhaseEvent) event;
                json.verzikPhase = verzikPhaseEvent.getPhase().ordinal();
                break;
            }

            case VERZIK_ATTACK_STYLE: {
                VerzikAttackStyleEvent verzikAttackStyleEvent = (VerzikAttackStyleEvent) event;
                json.verzikAttackStyle = new Event.AttackStyle();
                json.verzikAttackStyle.style = verzikAttackStyleEvent.getStyle().ordinal();
                json.verzikAttackStyle.npcAttackTick = verzikAttackStyleEvent.getAttackTick();
                break;
            }

            case COLOSSEUM_HANDICAP_CHOICE: {
                HandicapChoiceEvent handicapChoiceEvent = (HandicapChoiceEvent) event;
                json.handicap = handicapChoiceEvent.getHandicap().getId();
                json.handicapOptions =
                        Arrays.stream(handicapChoiceEvent.getHandicapOptions())
                                .map(Handicap::getId)
                                .collect(Collectors.toList());
                break;
            }

            default:
                break;
        }

        return json;
    }

    private static List<Coords> toCoordsList(List<WorldPoint> points) {
        return points.stream()
                .map(JsonEventTranslator::worldPointToCoords)
                .collect(Collectors.toList());
    }

    private static Coords worldPointToCoords(WorldPoint point) {
        return new Coords(point.getX(), point.getY());
    }

    /**
     * Adds tracked NPC-specific properties to the given {@link Event.Npc} based
     * on the {@link NpcEvent}.
     *
     * @param npc   Npc to which the properties should be added.
     * @param event Event describing the NPC.
     */
    private static void addTranslatedNpcProperties(Event.Npc npc, NpcEvent event) {
        TrackedNpc.Properties properties = event.getProperties();

        if (properties instanceof MaidenCrab.Properties) {
            var crab = (MaidenCrab.Properties) properties;
            npc.maidenCrab = new Event.MaidenCrab();
            npc.maidenCrab.spawn = crab.getSpawn().ordinal();
            npc.maidenCrab.position = crab.getPosition().ordinal();
            npc.maidenCrab.scuffed = crab.isScuffed();
            return;
        }

        if (properties instanceof Nylo.Properties) {
            var nylo = (Nylo.Properties) properties;
            npc.nylo = new Event.Nylo();
            npc.nylo.wave = nylo.getWave();
            npc.nylo.parentRoomId = nylo.getParentRoomId();
            npc.nylo.big = nylo.isBig();
            npc.nylo.style = nylo.getStyle().ordinal();
            npc.nylo.spawnType = nylo.getSpawnType().ordinal();
            return;
        }

        if (properties instanceof VerzikCrab.Properties) {
            var crab = (VerzikCrab.Properties) properties;
            npc.verzikCrab = new Event.VerzikCrab();
            npc.verzikCrab.phase = crab.getPhase().ordinal();
            npc.verzikCrab.spawn = crab.getSpawn().ordinal();
        }
    }
}