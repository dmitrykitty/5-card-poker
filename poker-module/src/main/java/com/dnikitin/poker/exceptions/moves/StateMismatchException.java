package com.dnikitin.poker.exceptions.moves;


/**
 * Thrown when an action is attempted in an invalid game state.
 * Example: trying to draw cards during betting phase.
 */
public class StateMismatchException extends InvalidMoveException {

    public StateMismatchException(String message) {
        super("STATE_MISMATCH", message);
    }
}
