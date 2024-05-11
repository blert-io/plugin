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

public enum ChallengeState {
    /**
     * Not in a challenge.
     */
    INACTIVE,
    /**
     * A challenge start has been triggered, but is delayed pending some challenge-specific initialization.
     * Not used in all challenges.
     */
    PREPARING,
    /**
     * A challenge has been initialized but the player has not yet entered.
     */
    STARTING,
    /**
     * The player is in the challenge.
     */
    ACTIVE,
    /**
     * The challenge has ended and is in the process of cleaning up.
     */
    ENDING,
    /**
     * The challenge has ended and all cleanup has been completed, but the player has not yet left.
     */
    COMPLETE;

    public boolean isInactive() {
        return this == INACTIVE;
    }

    public boolean hasStarted() {
        return this == ACTIVE || this == ENDING || this == COMPLETE;
    }

    public boolean inChallenge() {
        return !isInactive();
    }
}
