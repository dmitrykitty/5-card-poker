package com.dnikitin.poker.common.protocol.commands;

import com.dnikitin.poker.common.protocol.Command;
import lombok.Getter;

/**
 * BET AMOUNT=<n> or RAISE AMOUNT=<n>
 */
@Getter
public class BetCommand extends Command {
    private final int amount;

    public BetCommand(String gameId, String playerId, CommandType type, int amount) {
        super(gameId, playerId, type);
        this.amount = amount;
    }
}
