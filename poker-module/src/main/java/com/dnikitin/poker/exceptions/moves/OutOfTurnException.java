package com.dnikitin.poker.exceptions.moves;

/**
 * Thrown when a player attempts to make a move when it is not their turn.
 */
public class OutOfTurnException extends InvalidMoveException {
    public OutOfTurnException() {
        super("OUT_OF_TURN", "Not your turn.");
    }

    public OutOfTurnException(String message) {
        super("OUT_OF_TURN", message);
    }
}
