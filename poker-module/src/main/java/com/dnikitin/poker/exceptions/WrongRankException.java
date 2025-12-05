package com.dnikitin.poker.exceptions;

public class WrongRankException extends RuntimeException {
    public WrongRankException(String message) {
        super(message);
    }
}
