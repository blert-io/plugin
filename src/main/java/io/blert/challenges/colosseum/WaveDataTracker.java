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
import io.blert.events.NpcAttackEvent;
import io.blert.events.colosseum.HandicapChoiceEvent;
import io.blert.util.Tick;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.util.Text;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class WaveDataTracker extends DataTracker {
    private final static String bossStartMessage = "Sol Heredit jumps down from his seat...";
    private final String waveStartMessage;
    private final Pattern waveEndRegex;

    private final int ticksOnEntry;
    @Setter
    private Handicap handicap;
    @Setter
    private Handicap[] handicapOptions;

    public static Stage waveToStage(int wave) {
        return Stage.values()[Stage.COLOSSEUM_WAVE_1.ordinal() + wave - 1];
    }

    public WaveDataTracker(RecordableChallenge challenge, Client client, int wave, int ticksOnEntry) {
        super(challenge, client, waveToStage(wave));

        this.waveStartMessage = "Wave: " + wave;
        this.waveEndRegex = Pattern.compile("Wave " + wave + " completed! Wave duration: ([0-9:.]+)");
        this.ticksOnEntry = ticksOnEntry;
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned event) {
        return ColosseumNpc.withId(event.getNpc().getId()).map(colosseumNpc -> {
            NPC npc = event.getNpc();
            if (colosseumNpc.isManticore()) {
                return new Manticore(npc, generateRoomId(npc), new Hitpoints(colosseumNpc.getHitpoints()));
            }
            return new BasicTrackedNpc(npc, generateRoomId(npc), new Hitpoints(colosseumNpc.getHitpoints()));
        });
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned event, @Nullable TrackedNpc trackedNpc) {
        return ColosseumNpc.withId(event.getNpc().getId()).isPresent();
    }

    @Override
    protected void onAnimation(AnimationChanged event) {
        if (!(event.getActor() instanceof NPC)) {
            return;
        }

        NPC npc = (NPC) event.getActor();
        Optional<ColosseumNpc> maybeNpc = ColosseumNpc.withId(npc.getId());
        Optional<TrackedNpc> maybeTrackedNpc = getTrackedNpcs().getByNpc(npc);
        if (maybeNpc.isEmpty() || maybeTrackedNpc.isEmpty() || npc.getAnimation() == -1) {
            return;
        }

        ColosseumNpc colosseumNpc = maybeNpc.get();
        TrackedNpc trackedNpc = maybeTrackedNpc.get();

        if (colosseumNpc.isManticore()) {
            Manticore manticore = (Manticore) trackedNpc;
            if (npc.getAnimation() == Manticore.ATTACK_ANIMATION) {
                manticore.startAttack();
            }
            return;
        }

        colosseumNpc.getAttack(npc.getAnimation()).ifPresent(attack -> {
            WorldPoint location = getWorldLocation(npc);
            dispatchEvent(new NpcAttackEvent(getStage(), getTick(), location, attack, trackedNpc));
        });
    }

    @Override
    protected void onTick() {
        if (notStarted()) {
            return;
        }

        getTrackedNpcs().stream()
                .filter(trackedNpc -> trackedNpc instanceof Manticore)
                .forEach(trackedNpc -> {
                    Manticore manticore = (Manticore) trackedNpc;
                    // Check for an attack using the previous tick's style before updating.
                    NpcAttack attack = manticore.continueAttack();
                    if (attack != null) {
                        WorldPoint location = getWorldLocation(manticore.getNpc());
                        dispatchEvent(new NpcAttackEvent(getStage(), getTick(), location, attack, manticore));
                    }
                    manticore.updateStyle();
                });
    }

    @Override
    protected void onMessage(ChatMessage event) {
        String stripped = Text.removeTags(event.getMessage());

        if (stripped.equals(waveStartMessage)) {
            startWave(0);
            return;
        }
        if (stripped.equals(bossStartMessage)) {
            startWave(-1);
            return;
        }

        if (getStage() == Stage.COLOSSEUM_WAVE_12) {
            Matcher matcher = ColosseumChallenge.COLOSSEUM_END_REGEX.matcher(stripped);
            if (matcher.find()) {
                try {
                    var ticks = Tick.fromTimeString(matcher.group(1));
                    if (ticks.isPresent()) {
                        int challengeTicks = ticks.get().getLeft();
                        int bossTicks = challengeTicks - ticksOnEntry;
                        finish(true, bossTicks, ticks.get().getRight());
                    } else {
                        finish(true);
                    }
                } catch (Exception e) {
                    log.warn("Could not parse timestamp from colosseum end message: {}", stripped);
                    finish(true);
                }
            }
        } else {
            Matcher matcher = waveEndRegex.matcher(stripped);
            if (matcher.find()) {
                try {
                    String inGameTime = matcher.group(1);
                    finish(inGameTime);
                } catch (Exception e) {
                    log.warn("Could not parse timestamp from wave end message: {}", stripped);
                    finish(true);
                }
            }
        }
    }

    private void startWave(int tickOffset) {
        if (getState() == State.NOT_STARTED) {
            super.start(tickOffset);
            dispatchEvent(new HandicapChoiceEvent(
                    getStage(), handicap, Arrays.copyOf(handicapOptions, handicapOptions.length)));
        }
    }
}
