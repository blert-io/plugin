/*
 * Copyright (c) 2023-2024 Alexei Frolov
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

package io.blert.challenges.chambers.rooms.crabs;

// import io.blert.challenges.chambers.CoxNpc;
import io.blert.challenges.chambers.RoomDataTracker;
// import io.blert.core.BasicTrackedNpc;
// import io.blert.core.Hitpoints;
// import io.blert.core.NpcAttack;
import io.blert.core.RecordableChallenge;
import io.blert.core.Stage;
import io.blert.core.TrackedNpc;
// import io.blert.events.NpcAttackEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;

import javax.annotation.Nullable;
// import java.util.HashMap;
// import java.util.Map;
import java.util.Optional;

/**
 * Tracks Lizardman Mystic room events, spawns, HP changes, and attacks with full lifecycle management.
 * Uses NPC health ratio/scale for HP tracking instead of varbits.
 * 
 * TODO: Add Mystic-specific attack animations when identified.
 */
@Slf4j
public class CrabsDataTracker extends RoomDataTracker
{

    public CrabsDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[CrabsDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        // final int tick = getTick();

    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned spawned)
    {
        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    // TODO: Make sure this is also dependent on hp being 0 otherwise it can die off player screen and cause issues
    {
        return true;
    }

    @Override
    protected void onAnimation(AnimationChanged event)
    {
        
    }

    @Override
    protected void onHitsplat(HitsplatApplied event)
    {
        
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        // No longer using varbits for Mystic HP - using health ratio/scale instead
    }
}
