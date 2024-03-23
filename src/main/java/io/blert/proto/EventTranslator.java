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
import io.blert.challenges.tob.rooms.maiden.MaidenCrab;
import io.blert.challenges.tob.rooms.nylocas.Nylo;
import io.blert.challenges.tob.rooms.sotetseg.Maze;
import io.blert.challenges.tob.rooms.verzik.VerzikCrab;
import io.blert.core.ChallengeMode;
import io.blert.core.Stage;
import io.blert.core.*;
import io.blert.events.*;
import io.blert.events.tob.*;
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.Nullable;

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
            case RAID_START: {
                ChallengeStartEvent challengeStartEvent = (ChallengeStartEvent) event;
                Event.ChallengeInfo.Builder builder = Event.ChallengeInfo.newBuilder()
                        .addAllParty(challengeStartEvent.getParty())
                        .setSpectator(challengeStartEvent.isSpectator());
                challengeStartEvent.getMode().map(ChallengeMode::toProto).ifPresent(builder::setMode);
                eventBuilder.setChallengeInfo(builder);
                break;
            }

            case RAID_UPDATE: {
                ChallengeUpdateEvent challengeUpdateEvent = (ChallengeUpdateEvent) event;
                Event.ChallengeInfo.Builder builder = Event.ChallengeInfo.newBuilder()
                        .setMode(challengeUpdateEvent.getMode().toProto());
                eventBuilder.setChallengeInfo(builder);
                break;
            }

            case RAID_END: {
                ChallengeEndEvent challengeEndEvent = (ChallengeEndEvent) event;
                Event.CompletedChallenge.Builder builder = Event.CompletedChallenge.newBuilder()
                        .setOverallTimeTicks(challengeEndEvent.getOverallTime());
                eventBuilder.setCompletedChallenge(builder);
                break;
            }

            case ROOM_STATUS: {
                StageUpdateEvent stageUpdateEvent = (StageUpdateEvent) event;
                Event.StageUpdate.Builder builder = Event.StageUpdate.newBuilder()
                        .setStatus(translateStageStatus(stageUpdateEvent.getStatus()))
                        .setAccurate(stageUpdateEvent.isAccurate());
                eventBuilder.setStageUpdate(builder);
                break;
            }

            case PLAYER_UPDATE: {
                PlayerUpdateEvent playerUpdateEvent = (PlayerUpdateEvent) event;
                Event.Player.Builder builder = Event.Player.newBuilder()
                        .setName(playerUpdateEvent.getUsername())
                        .setOffCooldownTick(playerUpdateEvent.getOffCooldownTick());

                playerUpdateEvent.getHitpoints().ifPresent(sl -> builder.setPrayer(translateSkillLevel(sl)));
                playerUpdateEvent.getPrayer().ifPresent(sl -> builder.setPrayer(translateSkillLevel(sl)));
                playerUpdateEvent.getAttack().ifPresent(sl -> builder.setAttack(translateSkillLevel(sl)));
                playerUpdateEvent.getStrength().ifPresent(sl -> builder.setStrength(translateSkillLevel(sl)));
                playerUpdateEvent.getDefence().ifPresent(sl -> builder.setDefence(translateSkillLevel(sl)));
                playerUpdateEvent.getRanged().ifPresent(sl -> builder.setRanged(translateSkillLevel(sl)));
                playerUpdateEvent.getMagic().ifPresent(sl -> builder.setMagic(translateSkillLevel(sl)));

                playerUpdateEvent.getEquipment().forEach(
                        (slot, item) -> builder.addEquipment(translateEquippedItem(slot, item)));

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
                        .setHitpoints(translateSkillLevel(npcEvent.getHitpoints()));
                addTranslatedNpcProperties(builder, npcEvent);

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

            case NYLO_WAVE_SPAWN:
            case NYLO_WAVE_STALL: {
                NyloWaveEvent nyloWaveEvent = (NyloWaveEvent) event;
                eventBuilder.setNyloWave(Event.NyloWave.newBuilder()
                        .setWave(nyloWaveEvent.getWave())
                        .setNylosAlive(nyloWaveEvent.getNyloCount())
                        .setRoomCap(nyloWaveEvent.getNyloCap()));
                break;
            }

            case SOTE_MAZE_PROC: {
                SoteMazeProcEvent mazeProcEvent = (SoteMazeProcEvent) event;
                var maze = mazeProcEvent.getMaze() == Maze.MAZE_66
                        ? Event.SoteMaze.Maze.MAZE_66
                        : Event.SoteMaze.Maze.MAZE_33;
                eventBuilder.setSoteMaze(Event.SoteMaze.newBuilder().setMaze(maze));
                break;
            }

            case XARPUS_PHASE: {
                XarpusPhaseEvent xarpusPhaseEvent = (XarpusPhaseEvent) event;
                eventBuilder.setXarpusPhaseValue(xarpusPhaseEvent.getPhase().ordinal());
                break;
            }

            case VERZIK_PHASE: {
                VerzikPhaseEvent verzikPhaseEvent = (VerzikPhaseEvent) event;
                eventBuilder.setVerzikPhaseValue(verzikPhaseEvent.getPhase().ordinal());
                break;
            }

            case VERZIK_ATTACK_STYLE: {
                VerzikAttackStyleEvent verzikAttackStyleEvent = (VerzikAttackStyleEvent) event;
                eventBuilder.setVerzikAttackStyle(Event.VerzikAttackStyle.newBuilder()
                        .setStyleValue(verzikAttackStyleEvent.getStyle().ordinal())
                        .setNpcAttackTick(verzikAttackStyleEvent.getAttackTick()));
                break;
            }
        }

        return eventBuilder.build();
    }

    private static Event.StageUpdate.Status translateStageStatus(StageUpdateEvent.Status status) {
        switch (status) {
            case ENTERED:
                return Event.StageUpdate.Status.ENTERED;
            case STARTED:
                return Event.StageUpdate.Status.STARTED;
            case COMPLETED:
                return Event.StageUpdate.Status.COMPLETED;
            case WIPED:
                return Event.StageUpdate.Status.WIPED;
            default:
                throw new NotImplementedException("Stage status translation not implemented for " + status);
        }
    }

    private static Event.SkillLevel.Builder translateSkillLevel(SkillLevel skillLevel) {
        return Event.SkillLevel.newBuilder().setBase(skillLevel.getBase()).setCurrent(skillLevel.getCurrent());
    }

    private static Event.Player.EquippedItem.Builder translateEquippedItem(EquipmentSlot slot, Item item) {
        return Event.Player.EquippedItem.newBuilder()
                .setSlot(slot.toProto())
                .setId(item.getId())
                .setQuantity(item.getQuantity());
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
