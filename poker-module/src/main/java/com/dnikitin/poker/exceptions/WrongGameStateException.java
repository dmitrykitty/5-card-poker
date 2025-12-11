package com.dnikitin.poker.exceptions;

public class WrongGameStateException extends RuntimeException {
    public WrongGameStateException(String message) {
        super(message);
    }
}
