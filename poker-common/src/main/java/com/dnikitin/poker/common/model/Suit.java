package com.dnikitin.poker.common.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString(includeFieldNames = false)
public enum Suit {
    HEARTS("♥"),   // Red
    DIAMONDS("♦"), // Red
    CLUBS("♣"),    // Black
    SPADES("♠");   // Black


    private final String symbol;
}
