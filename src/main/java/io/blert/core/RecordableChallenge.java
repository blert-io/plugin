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

import io.blert.events.Event;
import io.blert.events.EventHandler;
import io.blert.events.EventType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public abstract class RecordableChallenge {
    @Getter
    private final String name;

    protected final Client client;

    @Getter(AccessLevel.PROTECTED)
    private final EventBus eventBus;

    @Getter
    private final ClientThread clientThread;

    @Setter
    private EventHandler eventHandler;
    List<Event> pendingEvents = new ArrayList<>();

    @Setter(AccessLevel.PROTECTED)
    @Getter(AccessLevel.PROTECTED)
    private ChallengeState state = ChallengeState.INACTIVE;

    protected RecordableChallenge(String name, Client client, EventBus eventBus, ClientThread clientThread) {
        this.name = name;
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
     * Implementation-specific game tick handler.
     */
    protected abstract void onTick();

    public void initialize(EventHandler handler) {
        this.eventHandler = handler;
        eventBus.register(this);
    }

    public void terminate() {
        eventBus.unregister(this);
        this.eventHandler = null;
        state = ChallengeState.INACTIVE;
    }

    public void tick() {
        onTick();
    }

    public boolean inChallenge() {
        return state.isActive();
    }

    public void dispatchEvent(Event event) {
        if (state == ChallengeState.INACTIVE || state == ChallengeState.STARTING) {
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
}
