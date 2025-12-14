package com.dnikitin.poker.common.protocol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a command sent from client to server.
 * Format: GAME_ID PLAYER_ID ACTION [PARAMS...]
 */
@Getter
@RequiredArgsConstructor
public abstract class Command {
    private final String gameId;
    private final String playerId;
    private final CommandType type;

    public enum CommandType {
        HELLO,
        CREATE,
        JOIN,
        LEAVE,
        START,
        BET,
        CALL,
        CHECK,
        FOLD,
        RAISE,
        DRAW,
        STATUS,
        SHOW,
        QUIT
    }
}
