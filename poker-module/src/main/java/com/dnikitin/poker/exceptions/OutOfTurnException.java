package com.dnikitin.poker.exceptions;

/**
 * Thrown when a player attempts to make a move when it is not their turn.
 */
public class OutOfTurnException extends InvalidMoveException {
    private static final String DEFAULT_MESSAGE = "Not your turn.";
    private static final String CODE = "OUT_OF_TURN";

    public OutOfTurnException() {
        super(DEFAULT_MESSAGE);
    }

    public OutOfTurnException(String message) {
        super(message);
    }

    public String getCode() {
        return CODE;
    }
}
