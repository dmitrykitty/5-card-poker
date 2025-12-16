package com.dnikitin.poker.exceptions.rules;

import com.dnikitin.poker.exceptions.PokerGameException;

public class WrongRankException extends PokerGameException {
    public WrongRankException(String message) {
        super("INTERNAL_LOGIC_ERROR", message);
    }
}
