package com.dnikitin.poker.exceptions;

import lombok.Getter;

@Getter
public abstract class PokerGameException extends RuntimeException {
    private final String code;

    protected PokerGameException(String code, String message) {
        super(message);
        this.code = code;
    }
}
