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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A player in a PvM challenge.
 */
@Slf4j
public class Raider {
    private enum BlowpipeState {
        NOT_PIPING,
        PIPING,
        STOPPED_PIPING,
    }

    @Getter
    private final String username;

    @Getter
    @Setter
    private @Nullable Player player;
    @Getter
    boolean localPlayer;

    @Setter
    boolean active;

    @Getter
    @Setter
    boolean dead;

    BlowpipeState blowpiping;

    @Getter
    Map<EquipmentSlot, io.blert.core.Item> equipment = new HashMap<>();

    @Getter
    int animationId;
    @Getter
    int animationTick;

    @Getter
    private @Nullable PlayerAttack lastAttack;
    @Getter
    int offCooldownTick;

    public Raider(@NotNull String username, boolean localPlayer) {
        this.username = username;
        this.localPlayer = localPlayer;
        this.active = true;
    }

    public Raider(@NotNull Player player, boolean localPlayer) {
        this(Objects.requireNonNull(player.getName()), localPlayer);
        this.player = player;
    }

    public boolean isAlive() {
        return !dead;
    }

    public boolean isOffCooldownOn(int tick) {
        return offCooldownTick <= tick;
    }

    /**
     * Resets the player's state to what it should be on entry to a new room in the raid.
     */
    public void resetForNewRoom() {
        dead = false;
        blowpiping = BlowpipeState.NOT_PIPING;
        equipment.clear();
        animationId = -1;
        animationTick = 0;
        lastAttack = null;
        offCooldownTick = 0;
    }

    public boolean isBlowpiping() {
        return blowpiping == BlowpipeState.PIPING;
    }

    /**
     * Returns true if the player was previously blowpiping but switched to a different weapon when they came off
     * cooldown.
     *
     * @return True if the player is no longer blowpiping.
     */
    public boolean stoppedBlowpiping() {
        return blowpiping == BlowpipeState.STOPPED_PIPING;
    }

    public Optional<io.blert.core.Item> getEquippedItem(EquipmentSlot slot) {
        return Optional.ofNullable(equipment.get(slot));
    }

    /**
     * Returns the NPC the player is targeting, if any.
     *
     * @return NPC with which the player is interacting, or empty if the player is either not interacting or
     * interacting with something other than an NPC.
     */
    public Optional<NPC> getTarget() {
        if (player == null) {
            return Optional.empty();
        }

        Actor actor = player.getInteracting();
        return actor instanceof NPC ? Optional.of((NPC) actor) : Optional.empty();
    }

    public void updateState(Client client, Player player, int tick) {
        this.player = player;

        if (blowpiping == BlowpipeState.STOPPED_PIPING) {
            blowpiping = BlowpipeState.NOT_PIPING;
        }

        equipment.clear();

        if (localPlayer) {
            updateEquipmentFromLocalPlayer(client);
        } else {
            updateEquipmentFromVisibleItems(client);
        }

        io.blert.core.Item newWeapon = equipment.get(EquipmentSlot.WEAPON);

        if (isBlowpiping() && isOffCooldownOn(tick)) {
            // The blowpipe animation is continuous, so it can't be relied on to determine when a player stops
            // attacking. When a blowpiping player comes off cooldown, infer whether they are continuing to attack or
            // have stopped.
            boolean hasTarget = getTarget().map(npc -> !npc.isDead()).orElse(false);

            if (newWeapon != null && PlayerAttack.BLOWPIPE.hasWeapon(newWeapon.getId()) && hasTarget) {
                blowpiping = BlowpipeState.PIPING;
            } else {
                blowpiping = BlowpipeState.STOPPED_PIPING;
            }
        }
    }

    public void recordAttack(int tick, @NotNull PlayerAttack attack) {
        lastAttack = attack;
        offCooldownTick = tick + attack.getCooldown();

        if (attack == PlayerAttack.BLOWPIPE) {
            blowpiping = BlowpipeState.PIPING;
        } else {
            blowpiping = BlowpipeState.NOT_PIPING;
        }
    }

    public void setAnimation(int tick, int animationId) {
        animationTick = tick;
        this.animationId = animationId;

        if (PlayerAttack.BLOWPIPE.hasAnimation(animationId)) {
            blowpiping = BlowpipeState.PIPING;
        } else {
            blowpiping = BlowpipeState.NOT_PIPING;
        }
    }

    private void updateEquipmentFromLocalPlayer(Client client) {
        var equippedItems = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equippedItems != null) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                var item = equippedItems.getItem(slot.getInventorySlotIndex());
                if (item != null) {
                    equipment.put(slot, new io.blert.core.Item(item.getId(), item.getQuantity()));
                }
            }
        } else {
            updateEquipmentFromVisibleItems(client);
        }
    }

    private void updateEquipmentFromVisibleItems(Client client) {
        Arrays.stream(EquipmentSlot.values()).filter(slot -> slot.getKitType() != null).forEach(slot -> {
            int id = Objects.requireNonNull(player).getPlayerComposition().getEquipmentId(slot.getKitType());
            if (id != -1) {
                var comp = client.getItemDefinition(id);
                equipment.put(slot, new Item(comp.getId(), 1));
            }
        });
    }
}
