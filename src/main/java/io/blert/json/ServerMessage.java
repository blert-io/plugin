/*
 * Copyright (c) 2025 Alexei Frolov
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

package io.blert.json;

import java.util.List;

public class ServerMessage {
    public static final int TYPE_PING = 0;
    public static final int TYPE_PONG = 1;
    public static final int TYPE_ERROR = 2;
    public static final int TYPE_CONNECTION_RESPONSE = 3;
    public static final int TYPE_HISTORY_REQUEST = 4;
    public static final int TYPE_HISTORY_RESPONSE = 5;
    public static final int TYPE_EVENT_STREAM = 6;
    public static final int TYPE_SERVER_STATUS = 8;
    public static final int TYPE_GAME_STATE = 10;
    public static final int TYPE_PLAYER_STATE = 11;
    public static final int TYPE_CHALLENGE_STATE_CONFIRMATION = 12;
    public static final int TYPE_CHALLENGE_START_REQUEST = 13;
    public static final int TYPE_CHALLENGE_START_RESPONSE = 14;
    public static final int TYPE_CHALLENGE_END_REQUEST = 15;
    public static final int TYPE_CHALLENGE_END_RESPONSE = 16;
    public static final int TYPE_CHALLENGE_UPDATE = 17;
    public static final int TYPE_GAME_STATE_REQUEST = 18;
    public static final int TYPE_ATTACK_DEFINITIONS = 19;
    public static final int TYPE_SPELL_DEFINITIONS = 20;

    public int type;
    public User user;
    public ErrorData error;
    public String activeChallengeId;
    public List<PastChallenge> recentRecordings;
    public List<Event> challengeEvents;
    public ServerStatus serverStatus;
    public GameState gameState;
    public List<PlayerState> playerState;
    public ChallengeStateConfirmation challengeStateConfirmation;
    public ChallengeStartRequest challengeStartRequest;
    public ChallengeEndRequest challengeEndRequest;
    public ChallengeUpdate challengeUpdate;
    public List<AttackDefinition> attackDefinitions;
    public List<SpellDefinition> spellDefinitions;
    public Integer requestId;
}