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

package io.blert.core;

import io.blert.events.ChallengeUpdateEvent;
import io.blert.events.Event;
import io.blert.events.EventHandler;
import io.blert.events.EventType;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.util.Text;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Slf4j
public abstract class RecordableChallenge {
    @Getter
    private final Challenge challenge;
    @Getter
    private @NonNull ChallengeMode challengeMode;

    protected final Client client;

    @Getter(AccessLevel.PROTECTED)
    private final EventBus eventBus;

    @Getter
    private final ClientThread clientThread;

    @Setter
    private EventHandler eventHandler;
    List<Event> pendingEvents = new ArrayList<>();

    @Getter(AccessLevel.PROTECTED)
    private ChallengeState state = ChallengeState.INACTIVE;

    private List<CompletableFuture<Status>> statusUpdateFutures = new ArrayList<>();

    @Getter
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Status {
        private final Challenge challenge;
        private final ChallengeMode mode;
        private final Stage stage;
        private final List<String> party;

        @Override
        public String toString() {
            return "Status(" +
                    "challenge=" + challenge +
                    ", mode=" + mode +
                    ", stage=" + stage +
                    ", party=" + party +
                    ')';
        }
    }

    /**
     * Players in the challenge party.
     */
    private final Map<String, Raider> party = new LinkedHashMap<>();

    protected RecordableChallenge(Challenge challenge, Client client, EventBus eventBus, ClientThread clientThread) {
        this.challenge = challenge;
        this.challengeMode = ChallengeMode.NO_MODE;
        this.client = client;
        this.eventBus = eventBus;
        this.clientThread = clientThread;
    }

    /**
     * Checks if the given world point is within the location of the challenge.
     *
     * @param worldPoint The world point to check.
     * @return True if the world point is within the location of the challenge, false otherwise.
     */
    public abstract boolean containsLocation(WorldPoint worldPoint);

    /**
     * Implementation-specific initialization handler.
     */
    protected abstract void onInitialize();

    /**
     * Implementation-specific termination handler.
     */
    protected abstract void onTerminate();

    /**
     * Implementation-specific game tick handler.
     */
    protected abstract void onTick();

    protected abstract @Nullable Stage getStage();

    public String getName() {
        return challenge.getName();
    }

    public int getScale() {
        return inChallenge() ? party.size() : 0;
    }

    public Collection<Raider> getParty() {
        return party.values();
    }

    public boolean playerIsInChallenge(@Nullable String username) {
        return username != null && party.containsKey(Text.standardize(username));
    }

    public @Nullable Raider getRaider(@Nullable String username) {
        return username != null ? party.get(Text.standardize(username)) : null;
    }

    /**
     * Updates the challenge mode. If the challenge is active and the mode has changed, an update event is dispatched.
     *
     * @param mode The new challenge mode.
     */
    public void updateMode(ChallengeMode mode) {
        if (challengeMode != mode) {
            log.debug("Raid mode set to " + mode);
            challengeMode = mode;

            if (state == ChallengeState.STARTING || state == ChallengeState.ACTIVE) {
                dispatchEvent(new ChallengeUpdateEvent(mode));
            }
        }
    }

    protected void setState(ChallengeState state) {
        if (this.state == ChallengeState.PREPARING && state != ChallengeState.PREPARING) {
            Status status = currentStatus();
            statusUpdateFutures.forEach(future -> future.complete(status));
            statusUpdateFutures.clear();
        }

        this.state = state;
    }

    protected void addRaider(Raider raider) {
        party.put(Text.standardize(raider.getUsername()), raider);
    }

    protected void resetParty() {
        party.clear();
    }

    public void initialize(EventHandler handler) {
        onInitialize();
        this.eventHandler = handler;
    }

    public void terminate() {
        onTerminate();

        this.eventHandler = null;
        state = ChallengeState.INACTIVE;
        party.clear();
    }

    public void tick() {
        onTick();
    }

    public boolean inChallenge() {
        return state.inChallenge();
    }

    public Future<Status> getStatus() {
        if (state == ChallengeState.INACTIVE) {
            return CompletableFuture.completedFuture(null);
        }

        if (state == ChallengeState.PREPARING) {
            CompletableFuture<Status> future = new CompletableFuture<>();
            statusUpdateFutures.add(future);
            return future;
        }

        return CompletableFuture.completedFuture(currentStatus());
    }

    public void dispatchEvent(Event event) {
        if (state == ChallengeState.INACTIVE || state == ChallengeState.PREPARING) {
            if (event.getType() != EventType.CHALLENGE_START && event.getType() != EventType.CHALLENGE_END) {
                pendingEvents.add(event);
                return;
            }
        }

        if (eventHandler != null) {
            eventHandler.handleEvent(client.getTickCount(), event);
        }
    }

    protected void dispatchPendingEvents() {
        pendingEvents.forEach(this::dispatchEvent);
        pendingEvents.clear();
    }

    protected void clearPendingEvents() {
        pendingEvents.clear();
    }

    private Status currentStatus() {
        return new Status(challenge, challengeMode, getStage(), new ArrayList<>(party.keySet()));
    }
}
