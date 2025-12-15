package com.dnikitin.poker.common.protocol.clientserver.commands;

import com.dnikitin.poker.common.protocol.clientserver.Command;
import lombok.Getter;

/**
 * JOIN GAME=<gameId> NAME=<nick>
 */
@Getter
public class JoinCommand extends Command {
    private final String name;

    public JoinCommand(String gameId, String name) {
        super(gameId, null, CommandType.JOIN);
        this.name = name;
    }
}
