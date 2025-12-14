package com.dnikitin.poker.common.protocol.commands;

import com.dnikitin.poker.common.protocol.Command;
import lombok.Getter;

import java.util.List;

/**
 * DRAW CARDS=<i,j,k>
 * Indexes 0..4 to exchange (max 3)
 */
@Getter
public class DrawCommand extends Command {
    private final List<Integer> cardIndexes;

    public DrawCommand(String gameId, String playerId, List<Integer> cardIndexes) {
        super(gameId, playerId, CommandType.DRAW);
        this.cardIndexes = cardIndexes;
    }
}
