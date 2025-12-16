package com.dnikitin.poker.exceptions.moves;

public class WrongGameStateException extends InvalidMoveException {
    public WrongGameStateException(String message) {
        super("WRONG_STATE", message);
    }
}
