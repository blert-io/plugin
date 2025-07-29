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

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.game.ItemVariationMapping;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A player in a PvM challenge.
 */
@Slf4j
public class Raider {
    private static final Set<Integer> DIZANAS_QUIVER_IDS = ImmutableSet.<Integer>builder()
            .addAll(ItemVariationMapping.getVariations(ItemVariationMapping.map(ItemID.DIZANAS_QUIVER)))
            .addAll(ItemVariationMapping.getVariations(ItemVariationMapping.map(ItemID.BLESSED_DIZANAS_QUIVER)))
            .addAll(ItemVariationMapping.getVariations(ItemVariationMapping.map(ItemID.DIZANAS_MAX_CAPE)))
            .build();

    // Whether the player is using a continuous animation weapon such as a toxic blowpipe.
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
    private final boolean localPlayer;

    @Getter
    @Setter
    private boolean active;

    @Getter
    @Setter
    private boolean dead;

    private BlowpipeState blowpiping;

    private Item[] equipment = new Item[EquipmentSlot.values().length];
    @Getter
    private final List<ItemDelta> equipmentChangesThisTick = new ArrayList<>();

    @Getter
    private int animationId;
    @Getter
    private int animationTick;

    @Getter
    private @Nullable PlayerAttack lastAttack;
    @Getter
    private int offCooldownTick;

    @Getter
    private @Nullable Prayer overheadPrayer;

    public Raider(@NonNull String username, boolean localPlayer) {
        this.username = username;
        this.localPlayer = localPlayer;
        this.active = true;
        this.overheadPrayer = null;
    }

    public Raider(@NonNull Player player, boolean localPlayer) {
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
        equipment = new Item[EquipmentSlot.values().length];
        equipmentChangesThisTick.clear();
        animationId = -1;
        animationTick = 0;
        lastAttack = null;
        offCooldownTick = 0;
        overheadPrayer = null;
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
        return Optional.ofNullable(equipment[slot.ordinal()]);
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

        updateOverheadPrayer();

        equipmentChangesThisTick.clear();

        int oldWeaponId = equipment[EquipmentSlot.WEAPON.ordinal()] != null
                ? equipment[EquipmentSlot.WEAPON.ordinal()].getId()
                : -1;

        if (localPlayer) {
            updateEquipmentFromLocalPlayer(client);
        } else {
            updateEquipmentFromVisibleItems(client);
        }

        Item newWeapon = equipment[EquipmentSlot.WEAPON.ordinal()];

        if (isBlowpiping() && isOffCooldownOn(tick)) {
            // Some weapon animations are continuous and can't be relied on to
            // determine when a player stops attacking. When a blowpiping player
            // comes off cooldown, infer whether they are continuing to attack or
            // have stopped.
            boolean hasTarget = getTarget().isPresent();
            int newWeaponId = newWeapon != null ? newWeapon.getId() : -1;

            if (newWeaponId == oldWeaponId && hasTarget) {
                blowpiping = BlowpipeState.PIPING;
            } else {
                blowpiping = BlowpipeState.STOPPED_PIPING;
            }
        }
    }

    public void recordAttack(int tick, @NonNull PlayerAttack attack) {
        lastAttack = attack;
        offCooldownTick = tick + attack.getCooldown();

        if (attack.isContinuousAnimation()) {
            blowpiping = BlowpipeState.PIPING;
        } else {
            blowpiping = BlowpipeState.NOT_PIPING;
        }
    }

    public void setAnimation(int tick, int animationId) {
        animationTick = tick;
        this.animationId = animationId;

        if (PlayerAttack.firstWithAnimation(animationId).map(PlayerAttack::isContinuousAnimation).orElse(false)) {
            blowpiping = BlowpipeState.PIPING;
        } else {
            blowpiping = BlowpipeState.NOT_PIPING;
        }
    }

    private void updateEquipmentFromLocalPlayer(Client client) {
        var equippedItems = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equippedItems == null) {
            updateEquipmentFromVisibleItems(client);
            return;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            net.runelite.api.Item item;

            if (slot == EquipmentSlot.QUIVER) {
                if (!hasQuiver(client)) {
                    continue;
                }
                final int quiverAmmoId = client.getVarpValue(VarPlayer.DIZANAS_QUIVER_ITEM_ID);
                if (quiverAmmoId <= 0) {
                    continue;
                }
                item = new net.runelite.api.Item(quiverAmmoId, client.getVarpValue(VarPlayer.DIZANAS_QUIVER_ITEM_COUNT));
            } else {
                item = equippedItems.getItem(slot.getInventorySlotIndex());
            }

            Item previous = this.equipment[slot.ordinal()];

            if (item != null) {
                if (previous == null || previous.getId() != item.getId()) {
                    equipmentChangesThisTick.add(new ItemDelta(item.getId(), item.getQuantity(), slot.ordinal(), true));
                } else {
                    int delta = item.getQuantity() - previous.getQuantity();
                    if (delta != 0) {
                        equipmentChangesThisTick.add(
                                new ItemDelta(item.getId(), Math.abs(delta), slot.ordinal(), delta > 0));
                    }
                }
                equipment[slot.ordinal()] = new Item(item.getId(), item.getQuantity());
            } else if (previous != null) {
                equipmentChangesThisTick.add(new ItemDelta(previous.getId(), previous.getQuantity(), slot.ordinal(), false));
                equipment[slot.ordinal()] = null;
            }
        }
    }

    private void updateEquipmentFromVisibleItems(Client client) {
        Arrays.stream(EquipmentSlot.values()).filter(slot -> slot.getKitType() != null).forEach(slot -> {
            Item previous = equipment[slot.ordinal()];

            int id = Objects.requireNonNull(player).getPlayerComposition().getEquipmentId(slot.getKitType());
            if (id != -1) {
                var comp = client.getItemDefinition(id);

                if (previous == null || previous.getId() != comp.getId()) {
                    equipmentChangesThisTick.add(new ItemDelta(comp.getId(), 1, slot.ordinal(), true));
                    equipment[slot.ordinal()] = new Item(comp.getId(), 1);
                }
            } else if (previous != null) {
                equipmentChangesThisTick.add(new ItemDelta(previous.getId(), previous.getQuantity(), slot.ordinal(), false));
                equipment[slot.ordinal()] = null;
            }
        });
    }

    private void updateOverheadPrayer() {
        if (player == null) {
            overheadPrayer = null;
            return;
        }

        HeadIcon overheadIcon = player.getOverheadIcon();
        if (overheadIcon == null) {
            overheadPrayer = null;
            return;
        }

        switch (overheadIcon) {
            case MAGIC:
                overheadPrayer = Prayer.PROTECT_FROM_MAGIC;
                break;
            case MELEE:
                overheadPrayer = Prayer.PROTECT_FROM_MELEE;
                break;
            case RANGED:
                overheadPrayer = Prayer.PROTECT_FROM_MISSILES;
                break;
            case REDEMPTION:
                overheadPrayer = Prayer.REDEMPTION;
                break;
            case RETRIBUTION:
                overheadPrayer = Prayer.RETRIBUTION;
                break;
            case SMITE:
                overheadPrayer = Prayer.SMITE;
                break;
            default:
                overheadPrayer = null;
                break;
        }
    }

    private boolean hasQuiver(Client client) {
        var equippedItems = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equippedItems != null) {
            var cape = equippedItems.getItem(EquipmentInventorySlot.CAPE.getSlotIdx());
            if (cape != null && DIZANAS_QUIVER_IDS.contains(cape.getId())) {
                return true;
            }
        }

        var inventoryItems = client.getItemContainer(InventoryID.INVENTORY);
        if (inventoryItems != null) {
            for (var item : inventoryItems.getItems()) {
                if (DIZANAS_QUIVER_IDS.contains(item.getId())) {
                    return true;
                }
            }
        }

        return false;
    }
}
