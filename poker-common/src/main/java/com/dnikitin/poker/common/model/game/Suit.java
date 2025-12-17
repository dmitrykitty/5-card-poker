package com.dnikitin.poker.common.model.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Represents the four standard playing card suits.
 * <p>
 * In standard Poker rules, suits are generally equal in strength,
 * but are used to determine Flush hands.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum Suit {
    HEARTS("♥"),   // Red
    DIAMONDS("♦"), // Red
    CLUBS("♣"),    // Black
    SPADES("♠");   // Black

    /**
     * The Unicode symbol representing the suit.
     */
    private final String symbol;

    @Override
    public String toString() {
        return symbol;
    }
}