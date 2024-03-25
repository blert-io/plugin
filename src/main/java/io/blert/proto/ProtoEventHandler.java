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

import io.blert.events.EventHandler;

import javax.annotation.Nullable;
import java.util.*;

/**
 * An event handler which converts events to their corresponding Protobuf messages.
 * <p>
 * Events posted to `HandleEvent` are stored, without any output being produced. Users must call a flush method to
 * consume the events.
 */
public class ProtoEventHandler implements EventHandler {
    private final Map<Integer, List<Event>> eventsByTick = new HashMap<>();
    private int earliestTickStored = 0;

    private @Nullable String challengeId = null;

    @Override
    public synchronized void handleEvent(int clientTick, io.blert.events.Event event) {
        eventsByTick.putIfAbsent(clientTick, new ArrayList<>());
        eventsByTick.get(clientTick).add(EventTranslator.toProto(event, challengeId));
    }

    /**
     * Sets a challenge ID to be applied to received events.
     * <p>
     * If the challenge ID is null, events will not have a challenge ID set. Otherwise, if any events are already
     * stored without a challenge ID, they will be updated to have the new challenge ID.
     *
     * @param challengeId The challenge ID to set.
     */
    public synchronized void setChallengeId(@Nullable String challengeId) {
        this.challengeId = challengeId;

        if (challengeId != null) {
            eventsByTick.values().forEach(events -> {
                ListIterator<Event> iterator = events.listIterator();
                while (iterator.hasNext()) {
                    Event event = iterator.next();
                    if (event.getChallengeId().isEmpty()) {
                        iterator.set(event.toBuilder().setChallengeId(challengeId).build());
                    }
                }
            });
        }
    }

    public boolean hasEvents() {
        return !eventsByTick.isEmpty();
    }

    public List<Event> flushEventsUpTo(int tick) {
        List<Event> events = new ArrayList<>();
        for (int i = earliestTickStored; i <= tick; i++) {
            if (eventsByTick.containsKey(i)) {
                events.addAll(eventsByTick.get(i));
                eventsByTick.remove(i);
            }
        }
        earliestTickStored = tick;
        return events;
    }
}
