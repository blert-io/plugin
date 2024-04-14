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

/**
 * An item delta represents a change to an item in a container, such as the player's inventory or equipment.
 */
@Getter
public class ItemDelta {
    // An item delta is a 53-bit integer value (fully representable within an IEEE 754 double for Javascript
    // compatibility), with the following layout:
    //
    //   Bits 0-30:  Absolute quantity of item added or removed.
    //   Bit 31:     Added bit (1 if the item was added, 0 if it was removed).
    //   Bits 32-47: In-game item ID.
    //   Bits 48-52: ID of the "slot" the item was added to or removed from. The meaning of slot is context-dependent.
    //
    private static final long QUANTITY_MASK = 0x7FFFFFFF;
    private static final long ADDED_BIT = 1L << 31;
    private static final long ID_SHIFT = 32;
    private static final long ID_MASK = 0xFFFF;
    private static final long SLOT_SHIFT = 48;
    private static final long SLOT_MASK = 0x1F;

    private long value;

    public ItemDelta(int id, int quantity, int slot, boolean added) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must be non-negative");
        }
        if (id < 0 || id > ID_MASK) {
            throw new IllegalArgumentException("ID must be between 0 and " + ID_MASK);
        }
        if (slot < 0 || slot > SLOT_MASK) {
            throw new IllegalArgumentException("Slot must be between 0 and " + SLOT_MASK);
        }

        value = quantity & QUANTITY_MASK;
        if (added) {
            value |= ADDED_BIT;
        }
        value |= (id & ID_MASK) << ID_SHIFT;
        value |= (slot & SLOT_MASK) << SLOT_SHIFT;
    }

    public int getQuantity() {
        return (int) (value & QUANTITY_MASK);
    }

    public boolean isAdded() {
        return (value & ADDED_BIT) != 0;
    }

    public int getId() {
        return (int) ((value >> ID_SHIFT) & ID_MASK);
    }

    public int getSlot() {
        return (int) ((value >> SLOT_SHIFT) & SLOT_MASK);
    }

    public String toString() {
        return "ItemDelta{id=" + getId() + ", quantity=" + getQuantity() + ", slot=" + getSlot() + ", added=" + isAdded() + "}";
    }
}
