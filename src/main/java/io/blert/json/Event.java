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

package io.blert.json;

import com.google.gson.Gson;
import io.blert.challenges.tob.Mode;
import io.blert.challenges.tob.rooms.Room;
import io.blert.challenges.tob.rooms.verzik.VerzikPhase;
import io.blert.challenges.tob.rooms.xarpus.XarpusPhase;
import io.blert.core.Stage;
import io.blert.events.*;
import io.blert.events.tob.*;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class Event {
    private EventType type;

    private @Nullable Room room = null;
    private int tick = 0;
    private int xCoord = 0;
    private int yCoord = 0;

    @Setter
    private String raidId = null;

    // All possible sub-objects whose data can be stored in an event. These are optional; a null value will not be
    // serialized.
    private @Nullable RaidInfo raidInfo = null;
    private @Nullable CompletedRaid completedRaid = null;
    private @Nullable RoomStatus roomStatus = null;
    private @Nullable Player player = null;
    private @Nullable Npc npc = null;
    private @Nullable NpcAttack npcAttack = null;
    private @Nullable PlayerAttack attack = null;
    private @Nullable List<Coords> maidenBloodSplats = null;
    private @Nullable BloatStatus bloatStatus = null;
    private @Nullable NyloWave nyloWave = null;
    private @Nullable SoteMaze soteMaze = null;
    private @Nullable XarpusPhase xarpusPhase = null;
    private @Nullable VerzikPhase verzikPhase = null;
    private @Nullable VerzikAttack verzikAttack = null;

    private static Room stageToTobRoom(@Nullable Stage stage) {
        if (stage == null) {
            return null;
        }

        switch (stage) {
            case TOB_MAIDEN:
                return Room.MAIDEN;
            case TOB_BLOAT:
                return Room.BLOAT;
            case TOB_NYLOCAS:
                return Room.NYLOCAS;
            case TOB_SOTETSEG:
                return Room.SOTETSEG;
            case TOB_XARPUS:
                return Room.XARPUS;
            case TOB_VERZIK:
                return Room.VERZIK;
            default:
                return null;
        }
    }

    public static Event fromBlert(io.blert.events.Event blertEvent) {
        Event event = new Event();
        event.type = blertEvent.getType();
        event.room = blertEvent.getStage().map(Event::stageToTobRoom).orElse(null);
        event.tick = blertEvent.getTick();
        event.xCoord = blertEvent.getXCoord();
        event.yCoord = blertEvent.getYCoord();

        switch (event.getType()) {
            case RAID_START: {
                ChallengeStartEvent challengeStartEvent = (ChallengeStartEvent) blertEvent;
                Mode mode = challengeStartEvent.getMode().map(m -> {
                    switch (m) {
                        case TOB_ENTRY:
                            return Mode.ENTRY;
                        case TOB_REGULAR:
                            return Mode.REGULAR;
                        case TOB_HARD:
                            return Mode.HARD;
                        default:
                            return null;
                    }
                }).orElse(null);
                event.raidInfo = new RaidInfo(
                        challengeStartEvent.getParty(), mode, challengeStartEvent.isSpectator());
                break;
            }

            case RAID_UPDATE: {
                ChallengeUpdateEvent challengeUpdateEvent = (ChallengeUpdateEvent) blertEvent;
                Mode mode = null;
                switch (challengeUpdateEvent.getMode()) {
                    case TOB_ENTRY:
                        mode = Mode.ENTRY;
                        break;
                    case TOB_REGULAR:
                        mode = Mode.REGULAR;
                        break;
                    case TOB_HARD:
                        mode = Mode.HARD;
                        break;
                }
                event.raidInfo = new RaidInfo(new ArrayList<>(), mode, false);
                break;
            }

            case RAID_END:
                ChallengeEndEvent challengeEndEvent = (ChallengeEndEvent) blertEvent;
                event.completedRaid = new CompletedRaid(challengeEndEvent.getOverallTime());
                break;

            case ROOM_STATUS:
                StageUpdateEvent stageUpdateEvent = (StageUpdateEvent) blertEvent;
                event.roomStatus = new RoomStatus(stageUpdateEvent.getStatus(), stageUpdateEvent.isAccurate());
                break;

            case PLAYER_UPDATE:
                event.player = Player.fromBlertEvent((PlayerUpdateEvent) blertEvent);
                break;

            case PLAYER_ATTACK:
                PlayerAttackEvent playerAttackEvent = (PlayerAttackEvent) blertEvent;
                event.attack = PlayerAttack.fromPlayerAttackEvent(playerAttackEvent);
                event.player = new Player(playerAttackEvent.getUsername());
                break;

            case PLAYER_DEATH:
                event.player = new Player(((PlayerDeathEvent) blertEvent).getUsername());
                break;

            case NPC_SPAWN:
            case NPC_UPDATE:
            case NPC_DEATH:
                event.npc = Npc.fromNpcEvent((NpcEvent) blertEvent);
                break;

            case NPC_ATTACK:
                NpcAttackEvent npcAttackEvent = (NpcAttackEvent) blertEvent;
                event.npc = new Npc(npcAttackEvent.getNpcId(), npcAttackEvent.getRoomId());
                event.npcAttack = new NpcAttack(npcAttackEvent.getAttack(), npcAttackEvent.getTarget());
                break;

            case MAIDEN_CRAB_LEAK:
                MaidenCrabLeakEvent leakEvent = (MaidenCrabLeakEvent) blertEvent;
                event.npc = new Npc(leakEvent.getNpcId(), leakEvent.getRoomId(),
                        leakEvent.getHitpoints(), leakEvent.getProperties());
                break;

            case MAIDEN_BLOOD_SPLATS:
                MaidenBloodSplatsEvent bloodSplatsEvent = (MaidenBloodSplatsEvent) blertEvent;
                event.maidenBloodSplats = bloodSplatsEvent.getBloodSplats().stream()
                        .map(Coords::fromWorldPoint)
                        .collect(Collectors.toList());
                break;

            case BLOAT_DOWN:
                BloatDownEvent bloatDownEvent = (BloatDownEvent) blertEvent;
                event.bloatStatus = new BloatStatus(bloatDownEvent.getUptime());
                break;

            case NYLO_WAVE_SPAWN:
            case NYLO_WAVE_STALL:
                NyloWaveEvent waveStall = (NyloWaveEvent) blertEvent;
                event.nyloWave = new NyloWave(waveStall.getWave(), waveStall.getNyloCount(), waveStall.getNyloCap());
                break;

            case SOTE_MAZE_PROC:
                SoteMazeProcEvent mazeProc = (SoteMazeProcEvent) blertEvent;
                event.soteMaze = new SoteMaze(mazeProc.getMaze());
                break;

            case XARPUS_PHASE:
                XarpusPhaseEvent xarpusPhaseEvent = (XarpusPhaseEvent) blertEvent;
                event.xarpusPhase = xarpusPhaseEvent.getPhase();
                break;

            case VERZIK_PHASE:
                VerzikPhaseEvent verzikPhase = (VerzikPhaseEvent) blertEvent;
                event.verzikPhase = verzikPhase.getPhase();
                break;

            case VERZIK_REDS_SPAWN:
                VerzikRedsSpawnEvent verzikReds = (VerzikRedsSpawnEvent) blertEvent;
                event.verzikPhase = verzikReds.getPhase();
                break;

            case VERZIK_ATTACK_STYLE:
                VerzikAttackStyleEvent verzikAttackStyle = (VerzikAttackStyleEvent) blertEvent;
                event.verzikAttack = new VerzikAttack(verzikAttackStyle.getStyle(), verzikAttackStyle.getAttackTick());
                break;
        }

        return event;
    }

    public String encode() {
        return new Gson().toJson(this);
    }
}
