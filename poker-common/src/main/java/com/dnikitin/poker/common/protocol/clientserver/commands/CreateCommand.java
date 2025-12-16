package com.dnikitin.poker.common.protocol.clientserver.commands;

import com.dnikitin.poker.common.protocol.clientserver.Command;
import lombok.Getter;

/**
 * CREATE ANTE=<n> BET=<n> LIMIT=FIXED
 */
@Getter
public class CreateCommand extends Command {
    private final int ante;
    private final int bet;
    private final String limit;

    public CreateCommand(String playerId, int ante, int bet, String limit) {
        super(null, playerId, CommandType.CREATE);
        this.ante = ante;
        this.bet = bet;
        this.limit = limit;
    }
}
