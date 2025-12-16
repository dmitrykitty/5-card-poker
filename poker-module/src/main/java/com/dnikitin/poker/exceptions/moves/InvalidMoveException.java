package com.dnikitin.poker.exceptions.moves;

import com.dnikitin.poker.exceptions.PokerGameException;

public class InvalidMoveException extends PokerGameException {
    public InvalidMoveException(String message) {
        super("INVALID_MOVE", message);
    }

    protected InvalidMoveException(String code, String message) {
        super(code, message);
    }
}
