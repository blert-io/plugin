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

package io.blert.raid.rooms;

import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;

public class RoomNpcCollection implements Collection<RoomNpc> {
    private final HashMap<Long, RoomNpc> byRoomId = new HashMap<>();
    private final HashMap<Integer, RoomNpc> byNpc = new HashMap<>();

    public RoomNpcCollection() {
    }

    public Optional<RoomNpc> getByRoomId(long roomId) {
        return Optional.ofNullable(byRoomId.get(roomId));
    }

    public Optional<RoomNpc> getByNpc(NPC npc) {
        return Optional.ofNullable(byNpc.get(npc.hashCode()));
    }

    @Override
    public int size() {
        return byRoomId.size();
    }

    @Override
    public boolean isEmpty() {
        return byRoomId.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof RoomNpc)) {
            return false;
        }

        RoomNpc roomNpc = (RoomNpc) o;
        return byRoomId.containsKey(roomNpc.getRoomId());
    }

    @NotNull
    @Override
    public Iterator<RoomNpc> iterator() {
        return byRoomId.values().iterator();
    }

    @NotNull
    @Override
    public RoomNpc @NotNull [] toArray() {
        return byRoomId.values().toArray(new RoomNpc[0]);
    }

    @NotNull
    @Override
    public <T> T @NotNull [] toArray(@NotNull T @NotNull [] ts) {
        return byRoomId.values().toArray(ts);
    }

    @Override
    public boolean add(RoomNpc roomNpc) {
        byRoomId.put(roomNpc.getRoomId(), roomNpc);
        byNpc.put(roomNpc.getNpc().hashCode(), roomNpc);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof RoomNpc)) {
            return false;
        }

        RoomNpc roomNpc = (RoomNpc) o;
        byNpc.remove(roomNpc.getNpc().hashCode());
        return byRoomId.remove(roomNpc.getRoomId()) != null;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        return collection.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends RoomNpc> collection) {
        int sizeBefore = byRoomId.size();
        collection.forEach(this::add);
        return byRoomId.size() != sizeBefore;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
        int sizeBefore = byRoomId.size();
        collection.forEach(this::remove);
        return byRoomId.size() != sizeBefore;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
        int sizeBefore = byRoomId.size();
        byRoomId.values().retainAll(collection);
        byNpc.values().retainAll(collection);
        return byRoomId.size() != sizeBefore;
    }

    @Override
    public void clear() {
        byRoomId.clear();
        byNpc.clear();
    }

    /**
     * Creates a deterministic unique identifier for each NPC spawned in a room.
     *
     * @param spawnTick     Tick on which the NPC spawned.
     * @param npcId         ID of the spawned NPC.
     * @param spawnLocation Location at which the NPC spawned.
     * @return A unique ID for the NPC within the room.
     */
    @Contract(pure = true)
    public static long npcRoomId(int spawnTick, int npcId, WorldPoint spawnLocation) {
        // This ID should be limited to at most 53 bits to ensure that its integer value falls within the representable
        // range of an IEEE 754 64-bit double, as this is what Javascript uses as integers.
        long x = spawnLocation.getRegionX() & 0x3ff;           // 10 bits
        long y = ((long) spawnLocation.getRegionY() & 0x3ff);  // 10 bits
        long tick = ((long) spawnTick & 0x7ff);                // 11 bits
        long id = ((long) npcId & 0x1fffff);                   // 21 bits
        return x | (y << 10) | (tick << 20) | (id << 31);
    }
}
