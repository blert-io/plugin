/*
 * Copyright (c) 2024 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.blert.proto;

import com.google.protobuf.Empty;
import io.blert.challenges.colosseum.Handicap;
import io.blert.challenges.tob.rooms.maiden.MaidenCrab;
import io.blert.challenges.tob.rooms.nylocas.Nylo;
import io.blert.challenges.tob.rooms.sotetseg.Maze;
import io.blert.challenges.tob.rooms.verzik.VerzikCrab;
import io.blert.core.*;
import io.blert.core.Stage;
import io.blert.events.*;
import io.blert.events.colosseum.HandicapChoiceEvent;
import io.blert.events.mokhaiotl.*;
import io.blert.events.tob.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.util.stream.Collectors;

@Slf4j
public class EventTranslator {
    public static Event toProto(io.blert.events.Event event, @Nullable String challengeId) {
        var eventBuilder = Event.newBuilder()
                .setType(event.getType().toProto())
                .setTick(event.getTick())
                .setXCoord(event.getXCoord())
                .setYCoord(event.getYCoord());

        event.getStage().map(Stage::toProto).ifPresent(eventBuilder::setStage);

        if (challengeId != null) {
            eventBuilder.setChallengeId(challengeId);
        }

        switch (event.getType()) {
            case CHALLENGE_START:
            case CHALLENGE_END:
            case CHALLENGE_UPDATE:
            case STAGE_UPDATE:
                throw new AssertionError(String.format("Event type %s has no proto representation", event.getType()));

            case PLAYER_UPDATE: {
                PlayerUpdateEvent playerUpdateEvent = (PlayerUpdateEvent) event;

                Event.Player.DataSource dataSource = playerUpdateEvent.getSource() == PlayerUpdateEvent.Source.PRIMARY
                        ? Event.Player.DataSource.PRIMARY
                        : Event.Player.DataSource.SECONDARY;

                Event.Player.Builder builder = Event.Player.newBuilder()
                        .setDataSource(dataSource)
                        .setName(playerUpdateEvent.getUsername())
                        .setOffCooldownTick(playerUpdateEvent.getOffCooldownTick());

                playerUpdateEvent.getHitpoints().ifPresent(sl -> builder.setHitpoints(sl.getValue()));
                playerUpdateEvent.getPrayer().ifPresent(sl -> builder.setPrayer(sl.getValue()));
                playerUpdateEvent.getAttack().ifPresent(sl -> builder.setAttack(sl.getValue()));
                playerUpdateEvent.getStrength().ifPresent(sl -> builder.setStrength(sl.getValue()));
                playerUpdateEvent.getDefence().ifPresent(sl -> builder.setDefence(sl.getValue()));
                playerUpdateEvent.getRanged().ifPresent(sl -> builder.setRanged(sl.getValue()));
                playerUpdateEvent.getMagic().ifPresent(sl -> builder.setMagic(sl.getValue()));

                playerUpdateEvent.getActivePrayers().ifPresent(prayers -> builder.setActivePrayers(prayers.getValue()));

                playerUpdateEvent.getEquipmentChangesThisTick().stream()
                        .map(ItemDelta::getValue)
                        .forEach(builder::addEquipmentDeltas);

                eventBuilder.setPlayer(builder);
                break;
            }

            case PLAYER_ATTACK: {
                PlayerAttackEvent playerAttackEvent = (PlayerAttackEvent) event;

                Event.Attack.Builder builder = Event.Attack.newBuilder()
                        .setType(playerAttackEvent.getAttack().toProto())
                        .setDistanceToTarget(playerAttackEvent.getDistanceToTarget());
                playerAttackEvent.getWeapon().ifPresent(
                        item -> builder.setWeapon(translateEquippedItem(EquipmentSlot.WEAPON, item)));
                if (playerAttackEvent.getTargetNpcId() != -1) {
                    builder.setTarget(Event.Npc.newBuilder()
                            .setId(playerAttackEvent.getTargetNpcId())
                            .setRoomId(playerAttackEvent.getTargetRoomId()));
                }

                eventBuilder.setPlayerAttack(builder);
                eventBuilder.setPlayer(Event.Player.newBuilder().setName(playerAttackEvent.getUsername()));
                break;
            }

            case PLAYER_DEATH: {
                PlayerDeathEvent playerDeathEvent = (PlayerDeathEvent) event;
                eventBuilder.setPlayer(Event.Player.newBuilder().setName(playerDeathEvent.getUsername()));
                break;
            }

            case NPC_SPAWN:
            case NPC_UPDATE:
            case NPC_DEATH:
            case MAIDEN_CRAB_LEAK: {
                NpcEvent npcEvent = (NpcEvent) event;
                Event.Npc.Builder builder = Event.Npc.newBuilder()
                        .setId(npcEvent.getNpcId())
                        .setRoomId(npcEvent.getRoomId())
                        .setHitpoints(npcEvent.getHitpoints().getValue())
                        .setActivePrayers(npcEvent.getPrayers());
                if (npcEvent.propertiesChanged()) {
                    addTranslatedNpcProperties(builder, npcEvent);
                }

                eventBuilder.setNpc(builder);
                break;
            }

            case NPC_ATTACK: {
                NpcAttackEvent npcAttackEvent = (NpcAttackEvent) event;
                eventBuilder.setNpc(Event.Npc.newBuilder()
                        .setId(npcAttackEvent.getNpcId())
                        .setRoomId(npcAttackEvent.getRoomId()));
                Event.NpcAttacked.Builder builder = Event.NpcAttacked.newBuilder()
                        .setAttack(npcAttackEvent.getAttack().toProto());
                if (npcAttackEvent.getTarget() != null) {
                    builder.setTarget(npcAttackEvent.getTarget());
                }
                eventBuilder.setNpcAttack(builder);
                break;
            }

            case MAIDEN_BLOOD_SPLATS: {
                MaidenBloodSplatsEvent bloodSplatsEvent = (MaidenBloodSplatsEvent) event;
                bloodSplatsEvent.getBloodSplats().forEach(splat -> eventBuilder.addMaidenBloodSplats(
                        Coords.newBuilder().setX(splat.getX()).setY(splat.getY())));
                break;
            }

            case BLOAT_DOWN: {
                BloatDownEvent bloatDownEvent = (BloatDownEvent) event;
                eventBuilder.setBloatDown(Event.BloatDown.newBuilder()
                        .setDownNumber(bloatDownEvent.getDownNumber())
                        .setWalkTime(bloatDownEvent.getUptime()));
                break;
            }

            case BLOAT_HANDS_DROP:
            case BLOAT_HANDS_SPLAT: {
                BloatHandsEvent bloatHandsEvent = (BloatHandsEvent) event;
                bloatHandsEvent.getHands().forEach(hand -> eventBuilder.addBloatHands(
                        Coords.newBuilder().setX(hand.getX()).setY(hand.getY())));
                break;
            }

            case NYLO_WAVE_SPAWN:
            case NYLO_WAVE_STALL: {
                NyloWaveEvent nyloWaveEvent = (NyloWaveEvent) event;
                eventBuilder.setNyloWave(Event.NyloWave.newBuilder()
                        .setWave(nyloWaveEvent.getWave())
                        .setNylosAlive(nyloWaveEvent.getNyloCount())
                        .setRoomCap(nyloWaveEvent.getNyloCap()));
                break;
            }

            case SOTE_MAZE_PROC:
            case SOTE_MAZE_END: {
                SoteMazeEvent mazeEvent = (SoteMazeEvent) event;
                var maze = mazeEvent.getMaze() == Maze.MAZE_66
                        ? Event.SoteMaze.Maze.MAZE_66
                        : Event.SoteMaze.Maze.MAZE_33;
                Event.SoteMaze.Builder mazeBuilder =
                        Event.SoteMaze.newBuilder().setMaze(maze);
                mazeEvent.getChosenPlayer().ifPresent(mazeBuilder::setChosenPlayer);
                eventBuilder.setSoteMaze(mazeBuilder);
                break;
            }

            case SOTE_MAZE_PATH: {
                SoteMazePathEvent mazePathEvent = (SoteMazePathEvent) event;
                var maze = mazePathEvent.getMaze() == Maze.MAZE_66
                        ? Event.SoteMaze.Maze.MAZE_66
                        : Event.SoteMaze.Maze.MAZE_33;
                var coords = mazePathEvent.mazeRelativePoints()
                        .map(pt -> Coords.newBuilder().setX(pt.getX()).setY(pt.getY()).build())
                        .collect(Collectors.toList());
                Event.SoteMaze.Builder builder = Event.SoteMaze.newBuilder().setMaze(maze);
                switch (mazePathEvent.getTileType()) {
                    case OVERWORLD_TILES:
                        builder.addAllOverworldTiles(coords);
                        break;
                    case UNDERWORLD_PIVOTS:
                        builder.addAllUnderworldPivots(coords);
                        break;
                    case OVERWORLD_PIVOTS:
                        builder.addAllOverworldPivots(coords);
                        break;
                }
                eventBuilder.setSoteMaze(builder);
                break;
            }

            case XARPUS_PHASE: {
                XarpusPhaseEvent xarpusPhaseEvent = (XarpusPhaseEvent) event;
                eventBuilder.setXarpusPhaseValue(xarpusPhaseEvent.getPhase().ordinal());
                break;
            }

            case XARPUS_EXHUMED: {
                XarpusExhumedEvent xarpusExhumedEvent = (XarpusExhumedEvent) event;
                Event.XarpusExhumed.Builder builder = Event.XarpusExhumed.newBuilder()
                        .setSpawnTick(xarpusExhumedEvent.getSpawnTick())
                        .setHealAmount(xarpusExhumedEvent.getHealAmount())
                        .addAllHealTicks(xarpusExhumedEvent.getHealTicks());
                eventBuilder.setXarpusExhumed(builder);
                break;
            }

            case XARPUS_SPLAT: {
                XarpusSplatEvent xarpusSplatEvent = (XarpusSplatEvent) event;
                Event.XarpusSplat.Builder builder = Event.XarpusSplat.newBuilder()
                        .setSource(xarpusSplatEvent.getSource().toProto());
                if (xarpusSplatEvent.getBounceFrom() != null) {
                    builder.setBounceFrom(Coords.newBuilder()
                            .setX(xarpusSplatEvent.getBounceFrom().getX())
                            .setY(xarpusSplatEvent.getBounceFrom().getY()));
                }
                eventBuilder.setXarpusSplat(builder);
                break;
            }

            case VERZIK_PHASE: {
                VerzikPhaseEvent verzikPhaseEvent = (VerzikPhaseEvent) event;
                eventBuilder.setVerzikPhaseValue(verzikPhaseEvent.getPhase().ordinal());
                break;
            }

            case VERZIK_DAWN: {
                VerzikDawnEvent verzikDawnEvent = (VerzikDawnEvent) event;
                Event.VerzikDawn.Builder verzikDawnBuilder = Event.VerzikDawn.newBuilder()
                        .setAttackTick(verzikDawnEvent.getAttackTick())
                        .setDamage(verzikDawnEvent.getDamage())
                        .setPlayer(verzikDawnEvent.getPlayer());
                eventBuilder.setVerzikDawn(verzikDawnBuilder);
                break;
            }

            case VERZIK_BOUNCE: {
                VerzikBounceEvent verzikBounceEvent = (VerzikBounceEvent) event;
                Event.VerzikBounce.Builder verzikBounceBuilder = Event.VerzikBounce.newBuilder()
                        .setNpcAttackTick(verzikBounceEvent.getAttackTick())
                        .setPlayersInRange(verzikBounceEvent.getPlayersInRange())
                        .setPlayersNotInRange(verzikBounceEvent.getPlayersNotInRange());
                verzikBounceEvent.getBouncedPlayer().ifPresent(verzikBounceBuilder::setBouncedPlayer);
                eventBuilder.setVerzikBounce(verzikBounceBuilder);
                break;
            }

            case VERZIK_ATTACK_STYLE: {
                VerzikAttackStyleEvent verzikAttackStyleEvent = (VerzikAttackStyleEvent) event;
                eventBuilder.setVerzikAttackStyle(Event.AttackStyle.newBuilder()
                        .setStyleValue(verzikAttackStyleEvent.getStyle().ordinal())
                        .setNpcAttackTick(verzikAttackStyleEvent.getAttackTick()));
                break;
            }

            case VERZIK_YELLOWS: {
                VerzikYellowsEvent verzikYellowsEvent = (VerzikYellowsEvent) event;
                verzikYellowsEvent.getYellows().forEach(yellow -> eventBuilder.addVerzikYellows(
                        Coords.newBuilder().setX(yellow.getX()).setY(yellow.getY())));
                break;
            }

            case VERZIK_HEAL: {
                VerzikHealEvent verzikHealEvent = (VerzikHealEvent) event;
                eventBuilder.setVerzikHeal(Event.VerzikHeal.newBuilder()
                        .setPlayer(verzikHealEvent.getPlayer())
                        .setHealAmount(verzikHealEvent.getHealAmount()));
                break;
            }

            case COLOSSEUM_HANDICAP_CHOICE: {
                HandicapChoiceEvent handicapChoiceEvent = (HandicapChoiceEvent) event;
                eventBuilder.setHandicapValue(handicapChoiceEvent.getHandicap().getId());
                for (Handicap option : handicapChoiceEvent.getHandicapOptions()) {
                    eventBuilder.addHandicapOptionsValue(option.getId());
                }
                break;
            }

            case MOKHAIOTL_ATTACK_STYLE: {
                MokhaiotlAttackStyleEvent mokhaiotlAttackStyleEvent = (MokhaiotlAttackStyleEvent) event;
                eventBuilder.setMokhaiotlAttackStyle(Event.AttackStyle.newBuilder()
                        .setStyleValue(mokhaiotlAttackStyleEvent.getStyle().ordinal())
                        .setNpcAttackTick(mokhaiotlAttackStyleEvent.getAttackTick()));
                break;
            }

            case MOKHAIOTL_ORB: {
                MokhaiotlOrbEvent mokhaiotlOrbEvent = (MokhaiotlOrbEvent) event;
                Event.MokhaiotlOrb.Builder builder = Event.MokhaiotlOrb.newBuilder()
                        .setSource(mokhaiotlOrbEvent.getSource().toProto())
                        .setSourcePoint(Coords.newBuilder()
                                .setX(mokhaiotlOrbEvent.getSourcePoint().getX())
                                .setY(mokhaiotlOrbEvent.getSourcePoint().getY()))
                        .setStyleValue(mokhaiotlOrbEvent.getStyle().ordinal())
                        .setStartTick(mokhaiotlOrbEvent.getStartTick())
                        .setEndTick(mokhaiotlOrbEvent.getEndTick());
                eventBuilder.setMokhaiotlOrb(builder);
                break;
            }

            case MOKHAIOTL_OBJECTS: {
                MokhaiotlObjectsEvent mokhaiotlObjectsEvent = (MokhaiotlObjectsEvent) event;
                Event.MokhaiotlObjects.Builder builder = Event.MokhaiotlObjects.newBuilder();
                mokhaiotlObjectsEvent.getRocksSpawned().forEach(wp -> builder.addRocksSpawned(worldPointToCoords(wp)));
                mokhaiotlObjectsEvent.getSplatsSpawned().forEach(wp -> builder.addSplatsSpawned(worldPointToCoords(wp)));
                mokhaiotlObjectsEvent.getRocksDespawned().forEach(wp -> builder.addRocksDespawned(worldPointToCoords(wp)));
                mokhaiotlObjectsEvent.getSplatsDespawned().forEach(wp -> builder.addSplatsDespawned(worldPointToCoords(wp)));
                eventBuilder.setMokhaiotlObjects(builder);
                break;
            }

            case MOKHAIOTL_LARVA_LEAK: {
                MokhaiotlLarvaLeakEvent mokhaiotlLarvaLeakEvent = (MokhaiotlLarvaLeakEvent) event;
                Event.MokhaiotlLarvaLeak.Builder builder = Event.MokhaiotlLarvaLeak.newBuilder()
                        .setRoomId(mokhaiotlLarvaLeakEvent.getRoomId())
                        .setHealAmount(mokhaiotlLarvaLeakEvent.getHealAmount());
                eventBuilder.setMokhaiotlLarvaLeak(builder);
                break;
            }

            case MOKHAIOTL_SHOCKWAVE: {
                MokhaiotlShockwaveEvent mokhaiotlShockwaveEvent = (MokhaiotlShockwaveEvent) event;
                Event.MokhaiotlShockwave.Builder builder = Event.MokhaiotlShockwave.newBuilder();
                mokhaiotlShockwaveEvent.getShockwaveTiles().forEach(wp -> builder.addTiles(worldPointToCoords(wp)));
                eventBuilder.setMokhaiotlShockwave(builder);
                break;
            }
        }

        return eventBuilder.build();
    }

    private static Event.Player.EquippedItem.Builder translateEquippedItem(EquipmentSlot slot, Item item) {
        return Event.Player.EquippedItem.newBuilder()
                .setSlot(slot.toProto())
                .setId(item.getId())
                .setQuantity(item.getQuantity());
    }

    private static Coords worldPointToCoords(WorldPoint point) {
        return Coords.newBuilder()
                .setX(point.getX())
                .setY(point.getY())
                .build();
    }

    /**
     * Adds tracked NPC-specific properties to the given {@link Event.Npc.Builder} based on the {@link NpcEvent}.
     *
     * @param builder Builder to which the properties should be added.
     * @param event   Event describing the NPC.
     */
    private static void addTranslatedNpcProperties(Event.Npc.Builder builder, NpcEvent event) {
        TrackedNpc.Properties properties = event.getProperties();

        if (properties instanceof MaidenCrab.Properties) {
            var crab = (MaidenCrab.Properties) properties;
            builder.setMaidenCrab(Event.Npc.MaidenCrab.newBuilder()
                    .setSpawnValue(crab.getSpawn().ordinal())
                    .setPositionValue(crab.getPosition().ordinal())
                    .setScuffed(crab.isScuffed()));
            return;
        }

        if (properties instanceof Nylo.Properties) {
            var nylo = (Nylo.Properties) properties;
            builder.setNylo(Event.Npc.Nylo.newBuilder()
                    .setWave(nylo.getWave())
                    .setParentRoomId(nylo.getParentRoomId())
                    .setBig(nylo.isBig())
                    .setStyleValue(nylo.getStyle().ordinal())
                    .setSpawnTypeValue(nylo.getSpawnType().ordinal()));
            return;
        }

        if (properties instanceof VerzikCrab.Properties) {
            var crab = (VerzikCrab.Properties) properties;
            builder.setVerzikCrab(Event.Npc.VerzikCrab.newBuilder()
                    .setPhaseValue(crab.getPhase().ordinal())
                    .setSpawnValue(crab.getSpawn().ordinal()));
            return;
        }

        builder.setBasic(Empty.newBuilder());
    }
}
