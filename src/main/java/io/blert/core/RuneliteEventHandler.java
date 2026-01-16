/*
 * Copyright (c) 2026 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.blert.core;

import net.runelite.api.events.*;

/**
 * Interface for components that receive RuneLite events routed from the central plugin.
 */
public interface RuneliteEventHandler {
    default void onGameStateChanged(GameStateChanged event) {}
    default void onNpcSpawned(NpcSpawned event) {}
    default void onNpcDespawned(NpcDespawned event) {}
    default void onNpcChanged(NpcChanged event) {}
    default void onAnimationChanged(AnimationChanged event) {}
    default void onProjectileMoved(ProjectileMoved event) {}
    default void onChatMessage(ChatMessage event) {}
    default void onHitsplatApplied(HitsplatApplied event) {}
    default void onGameObjectSpawned(GameObjectSpawned event) {}
    default void onGameObjectDespawned(GameObjectDespawned event) {}
    default void onGroundObjectSpawned(GroundObjectSpawned event) {}
    default void onGroundObjectDespawned(GroundObjectDespawned event) {}
    default void onGraphicChanged(GraphicChanged event) {}
    default void onGraphicsObjectCreated(GraphicsObjectCreated event) {}
    default void onActorDeath(ActorDeath event) {}
    default void onVarbitChanged(VarbitChanged event) {}
    default void onScriptPreFired(ScriptPreFired event) {}
}
