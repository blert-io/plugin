package io.blert.challenges.mokhaiotl;

import io.blert.core.*;
import io.blert.events.mokhaiotl.*;
import io.blert.util.ChatText;
import io.blert.util.Tick;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;

@Slf4j
public class DelveDataTracker extends DataTracker {
    private static final Set<Integer> MOKHAIOTL_IDS = Set.of(14707, 14708, 14709);
    private static final Set<Integer> LARVA_IDS = Set.of(14710, 14711, 14712, 14713, 14788, 14789);

    private final Pattern delveEndRegex;
    private final int delve;

    private @Nullable NPC mokhaiotl = null;
    private final Queue<Hitsplat> healsThisTick = new LinkedList<>();
    private final Queue<TrackedNpc> larvaeLeakedThisTick = new LinkedList<>();

    public static Stage delveToStage(int delve) {
        if (delve > 8) {
            return Stage.MOKHAIOTL_DELVE_8PLUS;
        }
        return Stage.values()[Stage.MOKHAIOTL_DELVE_1.ordinal() + delve - 1];
    }

    public DelveDataTracker(
            RecordableChallenge challenge, Client client, ChallengeRecordingConfig recordingConfig, int delve) {
        super(challenge, client, recordingConfig, delveToStage(delve));
        this.delve = delve;
        if (delve > 8) {
            this.delveEndRegex =
                    Pattern.compile("Delve level: 8\\+ \\(" + delve + "\\) duration: (" + Tick.TIME_STRING_REGEX + ")");
        } else {
            this.delveEndRegex =
                    Pattern.compile("Delve level: " + delve + " duration: (" + Tick.TIME_STRING_REGEX + ")");
        }
    }

    protected void start() {
        super.start();
        log.info("Starting delve {}", delve);
    }

    @Override
    protected void onTick() {
        final int tick = getTick();

        while (!healsThisTick.isEmpty()) {
            Hitsplat hitsplat = healsThisTick.remove();
            TrackedNpc larva = larvaeLeakedThisTick.poll();
            if (larva == null) {
                log.warn("Mokhaiotl healed for {} without corresponding larva leak", hitsplat.getAmount());
                break;
            }
            dispatchEvent(new MokhaiotlLarvaLeakEvent(getStage(), tick, larva.getRoomId(), hitsplat.getAmount()));
        }

        healsThisTick.clear();
        larvaeLeakedThisTick.clear();
    }

    @Override
    protected void onMessage(ChatMessage event) {
        String stripped = ChatText.stripFormatting(event.getMessage());
        Matcher matcher = delveEndRegex.matcher(stripped);

        if (matcher.find()) {
            try {
                String inGameTime = matcher.group(1);
                finish(inGameTime);
            } catch (Exception e) {
                log.warn("Could not parse timestamp from delve end message: {}", stripped);
                finish(true);
            }
        }
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned event) {
        NPC npc = event.getNpc();
        if (MOKHAIOTL_IDS.contains(npc.getId())) {
            if (getState() == State.NOT_STARTED) {
                start();
            }

            mokhaiotl = npc;
        }

        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned event, @Nullable TrackedNpc trackedNpc) {
        if (trackedNpc == null) {
            return false;
        }

        if (LARVA_IDS.contains(event.getNpc().getId()) && mokhaiotl != null) {
            NPC larva = trackedNpc.getNpc();
            WorldPoint mokhaiotlSouthwest = getWorldLocation(mokhaiotl);
            int mokhaiotlSize = mokhaiotl.getComposition().getSize();
            WorldPoint mokhaiotlCenter = new WorldPoint(
                    mokhaiotlSouthwest.getX() + mokhaiotlSize / 2,
                    mokhaiotlSouthwest.getY() + mokhaiotlSize / 2,
                    mokhaiotlSouthwest.getPlane());

            if (getWorldLocation(larva).equals(mokhaiotlCenter)) {
                larvaeLeakedThisTick.add(trackedNpc);
            }
        }

        return true;
    }

    @Override
    protected void onHitsplat(HitsplatApplied hitsplatApplied) {
        if (hitsplatApplied.getActor() != mokhaiotl) {
            return;
        }

        Hitsplat hitsplat = hitsplatApplied.getHitsplat();
        if (hitsplat.getHitsplatType() == HitsplatID.HEAL) {
            healsThisTick.add(hitsplat);
        }
    }
}
