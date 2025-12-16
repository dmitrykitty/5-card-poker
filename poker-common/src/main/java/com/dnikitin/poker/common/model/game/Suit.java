package com.dnikitin.poker.common.model.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
public enum Suit {
    HEARTS("♥"),   // Red
    DIAMONDS("♦"), // Red
    CLUBS("♣"),    // Black
    SPADES("♠");   // Black


    private final String symbol;

    @Override
    public String toString() {
        return symbol;
    }
}
