package com.dnikitin.poker.exceptions;

/**
 * Thrown when an action is attempted in an invalid game state.
 * Example: trying to draw cards during betting phase.
 */
public class StateMismatchException extends InvalidMoveException {
    private static final String CODE = "STATE_MISMATCH";

    public StateMismatchException(String message) {
        super(message);
    }

    public String getCode() {
        return CODE;
    }
}
