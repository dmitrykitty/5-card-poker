package com.dnikitin.poker.exceptions.rules;

import com.dnikitin.poker.exceptions.PokerGameException;

public class IllegalPlayerAmountException extends PokerGameException {
    public IllegalPlayerAmountException(String message) {
        super("BAD_PLAYER_COUNT", message);
    }
}
