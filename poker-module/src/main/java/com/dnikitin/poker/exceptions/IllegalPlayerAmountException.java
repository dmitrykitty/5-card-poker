package com.dnikitin.poker.exceptions;

public class IllegalPlayerAmountException extends RuntimeException {
    public IllegalPlayerAmountException(String message) {
        super(message);
    }
}
