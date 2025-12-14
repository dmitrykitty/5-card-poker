package com.dnikitin.poker.common.protocol.commands;

import com.dnikitin.poker.common.protocol.Command;

/**
 * Represents simple commands without additional parameters.
 * Examples: START, LEAVE, CALL, CHECK, FOLD, STATUS, QUIT
 */
public class SimpleCommand extends Command {

    public SimpleCommand(String gameId, String playerId, CommandType type) {
        super(gameId, playerId, type);
    }
}
