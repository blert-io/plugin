package io.blert.challenges.chambers;

import io.blert.core.Stage;
import io.blert.core.TrackedNpc;
import net.runelite.api.Client;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.NpcDespawned;
import javax.annotation.Nullable;
import java.util.Optional;
import io.blert.core.RecordableChallenge;

public class CoxRoomDataTracker extends RoomDataTracker {
    public CoxRoomDataTracker(RecordableChallenge challenge, Stage stage, Client client) {
        super(challenge, stage, client);
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned event) {
        // Chambers rooms do not require special NPC spawn handling for timing.
        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned event, @Nullable TrackedNpc trackedNpc) {
        // Chambers rooms do not require special NPC despawn handling for timing.
        return false;
    }
}
