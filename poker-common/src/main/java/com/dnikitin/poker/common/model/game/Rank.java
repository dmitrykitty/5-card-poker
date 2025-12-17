package com.dnikitin.poker.common.model.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Represents the rank (face value) of a playing card.
 * <p>
 * Encapsulates the game rule that Ace is the highest card (power 14)
 * and 2 is the lowest (power 2).
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum Rank {
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K"),
    ACE(14, "A");

    /**
     * Numerical power for determining high card and pair strength.
     */
    private final int power;
    private final String label;

    @Override
    public String toString() {
        return label;
    }
}