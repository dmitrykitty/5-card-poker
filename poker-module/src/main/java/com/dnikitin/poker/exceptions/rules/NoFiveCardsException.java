package com.dnikitin.poker.exceptions.rules;

import com.dnikitin.poker.exceptions.PokerGameException;

public class NoFiveCardsException extends PokerGameException {
    public NoFiveCardsException(String message) {
        super("INVALID_HAND_SIZE", message);
    }
}
