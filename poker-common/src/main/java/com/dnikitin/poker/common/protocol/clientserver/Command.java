package com.dnikitin.poker.common.protocol.clientserver;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Abstract base class for all commands sent from the Client to the Server.
 * <p>
 * Represents the <b>Command Pattern</b> payload in the network protocol.
 * Every command carries the minimal context required for authorization and routing:
 * <ul>
 * <li><b>Game ID:</b> Which game session is targeted.</li>
 * <li><b>Player ID:</b> Who is performing the action (authentication context).</li>
 * <li><b>Type:</b> What specific operation is requested.</li>
 * </ul>
 * </p>
 */
@Getter
@RequiredArgsConstructor
public abstract class Command {
    private final String gameId;
    private final String playerId;
    private final CommandType type;

    /**
     * Enumerates all supported protocol operations.
     */
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