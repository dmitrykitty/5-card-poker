package com.dnikitin.poker.exceptions;

/**
 * Thrown when a player attempts an invalid card draw operation.
 * Examples: drawing more than 3 cards, invalid card indexes, duplicate indexes.
 */
public class IllegalDrawException extends InvalidMoveException {
    private static final String CODE = "ILLEGAL_DRAW";

    public IllegalDrawException(String message) {
        super(message);
    }

    public String getCode() {
        return CODE;
    }
}
