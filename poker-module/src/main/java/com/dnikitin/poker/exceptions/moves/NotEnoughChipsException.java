package com.dnikitin.poker.exceptions.moves;

public class NotEnoughChipsException extends InvalidMoveException {
    public NotEnoughChipsException(String message) {
        super("NOT_ENOUGH_CHIPS", message);
    }
}
