package com.dnikitin.poker.exceptions;

public class NoFiveCardsException extends RuntimeException {
    public NoFiveCardsException(String message) {
        super(message);
    }
}
