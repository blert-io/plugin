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

package io.blert.challenges.colosseum;

import io.blert.core.*;
import io.blert.events.ChallengeEndEvent;
import io.blert.events.ChallengeStartEvent;
import io.blert.util.DeferredTask;
import io.blert.util.Location;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public final class ColosseumChallenge extends RecordableChallenge {
    private static final int COLOSSEUM_REGION_ID = 7216;
    private static final int COLOSSEUM_LOBBY_REGION_ID = 7316;

    private static final int MINIMUS_NPC_ID = 12808;
    private static final int REWARD_CHEST_OBJECT_ID = 50741;

    private static final int HANDICAP_SELECTION_SCRIPT_ID = 4931;
    private static final int HANDICAP_SELECTION_VARBIT_ID = 9788;

    private int currentWave;
    private WaveDataTracker waveDataTracker;

    private final List<Handicap> waveHandicapOptions = new ArrayList<>(3);

    private @Nullable DeferredTask deferredTask = null;
    private boolean stateChangeCooldown;

    public ColosseumChallenge(Client client, EventBus eventBus, ClientThread clientThread) {
        super(Challenge.COLOSSEUM, client, eventBus, clientThread);
    }

    @Override
    public boolean containsLocation(WorldPoint worldPoint) {
        return worldPoint.getRegionID() == COLOSSEUM_REGION_ID || worldPoint.getRegionID() == COLOSSEUM_LOBBY_REGION_ID;
    }

    @Override
    protected void onInitialize() {
        addRaider(new Raider(client.getLocalPlayer(), true));
        currentWave = 0;
        stateChangeCooldown = false;
        waveHandicapOptions.clear();
    }

    @Override
    protected void onTerminate() {
        cleanup();
    }

    @Override
    protected void onTick() {
        if (!stateChangeCooldown) {
            checkColosseumState();
        }

        if (getState().isActive()) {
            if (waveDataTracker != null) {
                waveDataTracker.tick();
            }
        }

        if (deferredTask != null) {
            deferredTask.tick();
        }
    }

    @Subscribe
    private void onNpcSpawned(NpcSpawned event) {
        if (!stateChangeCooldown && event.getNpc().getId() == MINIMUS_NPC_ID) {
            prepareNextWave();
        }
    }

    @Subscribe
    private void onNpcDespawned(NpcDespawned event) {
        if (event.getNpc().getId() != MINIMUS_NPC_ID) {
            return;
        }

        if (getState() == ChallengeState.STARTING) {
            setState(ChallengeState.ACTIVE);
        } else if (getState() != ChallengeState.ACTIVE) {
            return;
        }

        stateChangeCooldown = true;
        deferredTask = new DeferredTask(() -> {
            stateChangeCooldown = false;

            if (!waveHandicapOptions.isEmpty()) {
                // The debuff selection varbit stores the index of the selected debuff option, starting from 1.
                int selectedDebuffIndex = client.getVarbitValue(HANDICAP_SELECTION_VARBIT_ID) - 1;
                Handicap selectedHandicap = waveHandicapOptions.get(selectedDebuffIndex);
                if (waveDataTracker != null) {
                    waveDataTracker.setHandicapOptions(waveHandicapOptions.toArray(new Handicap[3]));
                    waveDataTracker.setHandicap(selectedHandicap);
                }
            }
        }, 3);
    }

    @Subscribe
    private void onGameObjectSpawned(GameObjectSpawned event) {
        if (event.getGameObject().getId() == REWARD_CHEST_OBJECT_ID) {
            if (deferredTask != null) {
                deferredTask.cancel();
            }
            queueFinishColosseum(ChallengeState.COMPLETE);
        }
    }

    @Subscribe
    private void onScriptPreFired(ScriptPreFired event) {
        if (getState().isInactive() || event.getScriptId() != HANDICAP_SELECTION_SCRIPT_ID) {
            return;
        }

        waveHandicapOptions.clear();
        Object[] scriptArgs = event.getScriptEvent().getArguments();
        waveHandicapOptions.add(Handicap.withId((int) scriptArgs[2]));
        waveHandicapOptions.add(Handicap.withId((int) scriptArgs[3]));
        waveHandicapOptions.add(Handicap.withId((int) scriptArgs[4]));
    }

    private void checkColosseumState() {
        WorldPoint playerLocation = Location.getWorldLocation(client, client.getLocalPlayer().getWorldLocation());
        if (playerLocation == null) {
            return;
        }

        if (getState().isInactive()) {
            if (playerLocation.getRegionID() == COLOSSEUM_REGION_ID) {
                startColosseum();
            }
        } else if (playerLocation.getRegionID() != COLOSSEUM_REGION_ID) {
            clearWaveDataTracker();
            if (getState() == ChallengeState.COMPLETE) {
                setState(ChallengeState.INACTIVE);
            } else {
                queueFinishColosseum(ChallengeState.INACTIVE);
            }
        }
    }

    private void startColosseum() {
        log.debug("Starting Colosseum challenge");
        setState(ChallengeState.STARTING);

        List<String> usernames = getParty().stream().map(Raider::getUsername).collect(Collectors.toList());
        dispatchEvent(new ChallengeStartEvent(getChallenge(), ChallengeMode.NO_MODE, usernames, false));
    }

    private void queueFinishColosseum(ChallengeState state) {
        stateChangeCooldown = true;
        deferredTask = new DeferredTask(() -> finishColosseum(state), 3);
    }

    private void finishColosseum(ChallengeState state) {
        log.debug("Ending Colosseum challenge: {}", state);
        stateChangeCooldown = false;
        setState(state);

        cleanup();
        dispatchEvent(new ChallengeEndEvent(0));
    }

    private void prepareNextWave() {
        if (waveDataTracker != null) {
            clearWaveDataTracker();
        }

        currentWave++;
        if (currentWave < 13) {
            waveDataTracker = new WaveDataTracker(this, client, currentWave);
            getEventBus().register(waveDataTracker);
        }
    }

    private void clearWaveDataTracker() {
        if (waveDataTracker != null) {
            waveDataTracker.terminate();
            getEventBus().unregister(waveDataTracker);
            waveDataTracker = null;
        }
    }

    private void cleanup() {
        clearWaveDataTracker();
        waveHandicapOptions.clear();
        currentWave = 0;
        stateChangeCooldown = false;
    }
}
