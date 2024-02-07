package com.blert.json;

import com.blert.events.*;
import com.blert.raid.rooms.Room;
import com.blert.raid.rooms.xarpus.XarpusPhase;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.ArrayList;

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
    private @Nullable RoomStatusEvent.Status roomStatus = null;
    private @Nullable Player player = null;
    private @Nullable Npc npc = null;
    private @Nullable Attack attack = null;
    private @Nullable MaidenEntity maidenEntity = null;
    private @Nullable BloatStatus bloatStatus = null;
    private @Nullable NyloWave nyloWave = null;
    private @Nullable SoteMaze soteMaze = null;
    private @Nullable XarpusPhase xarpusPhase = null;

    public static Event fromBlert(com.blert.events.Event blertEvent) {
        Event event = new Event();
        event.type = blertEvent.getType();
        event.room = blertEvent.getRoom().orElse(null);
        event.tick = blertEvent.getTick();
        event.xCoord = blertEvent.getXCoord();
        event.yCoord = blertEvent.getYCoord();

        switch (event.getType()) {
            case RAID_START:
                RaidStartEvent raidStartEvent = (RaidStartEvent) blertEvent;
                event.raidInfo = new RaidInfo(raidStartEvent.getParty(), raidStartEvent.getMode());
                break;
            case RAID_UPDATE:
                RaidUpdateEvent raidUpdateEvent = (RaidUpdateEvent) blertEvent;
                event.raidInfo = new RaidInfo(new ArrayList<>(), raidUpdateEvent.getMode());
                break;
            case ROOM_STATUS:
                event.roomStatus = ((RoomStatusEvent) blertEvent).getStatus();
                break;
            case PLAYER_UPDATE:
                event.player = Player.fromBlertEvent((PlayerUpdateEvent) blertEvent);
                break;
            case PLAYER_ATTACK:
                PlayerAttackEvent playerAttackEvent = (PlayerAttackEvent) blertEvent;
                event.attack = Attack.fromPlayerAttackEvent(playerAttackEvent);
                event.player = new Player(playerAttackEvent.getUsername());
                break;
            case PLAYER_DEATH:
                event.player = new Player(((PlayerDeathEvent) blertEvent).getUsername());
                break;
            case NPC_UPDATE:
                NpcUpdateEvent npcUpdateEvent = (NpcUpdateEvent) blertEvent;
                event.npc = new Npc(npcUpdateEvent.getNpcId(), npcUpdateEvent.getRoomId());
                npcUpdateEvent.getHitpoints().ifPresent(event.npc::setHitpoints);
                break;
            case MAIDEN_CRAB_SPAWN:
                MaidenCrabSpawnEvent spawnEvent = (MaidenCrabSpawnEvent) blertEvent;
                event.maidenEntity = MaidenEntity.fromCrab(spawnEvent.getSpawn(), spawnEvent.getCrab());
                break;
            case MAIDEN_CRAB_LEAK:
                MaidenCrabLeakEvent leakEvent = (MaidenCrabLeakEvent) blertEvent;
                event.maidenEntity = MaidenEntity.crabLeak(
                        leakEvent.getSpawn(), leakEvent.getPosition(), leakEvent.getHitpoints());
                break;
            case MAIDEN_BLOOD_SPLATS:
                MaidenBloodSplatsEvent bloodSplatsEvent = (MaidenBloodSplatsEvent) blertEvent;
                event.maidenEntity = MaidenEntity.bloodSplats(bloodSplatsEvent.getBloodSplats());
                break;
            case BLOAT_DOWN:
                BloatDownEvent bloatDownEvent = (BloatDownEvent) blertEvent;
                event.bloatStatus = new BloatStatus(bloatDownEvent.getUptime());
                break;
            case NYLO_WAVE_SPAWN:
                NyloWaveSpawnEvent waveSpawn = (NyloWaveSpawnEvent) blertEvent;
                event.nyloWave = new NyloWave(waveSpawn.getWave(), waveSpawn.getNyloCount(), waveSpawn.getNyloCap());
                break;
            case NYLO_WAVE_STALL:
                NyloWaveStallEvent waveStall = (NyloWaveStallEvent) blertEvent;
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
        }

        return event;
    }

    public String encode() {
        return new Gson().toJson(this);
    }
}
