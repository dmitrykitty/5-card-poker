package com.dnikitin.poker.common.model.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Represents the hierarchy of Poker hands, from weakest to strongest.
 * <p>
 * Each rank has an associated numerical strength, allowing for easy comparison:
 * <code>rankA.getStrength() > rankB.getStrength()</code>.
 * </p>
 */
@Getter
@RequiredArgsConstructor
@ToString(of = {"label"}, includeFieldNames = false)
public enum HandRank {
    HIGH_CARD(1, "High Card"),
    ONE_PAIR(2, "One Pair"),
    TWO_PAIRS(3, "Two Pairs"),
    THREE_OF_A_KIND(4, "Three of a Kind"),
    STRAIGHT(5, "Straight"),                // 5 consecutive ranks
    FLUSH(6, "Flush"),                      // 5 cards of same suit
    FULL_HOUSE(7, "Full House"),            // 3 of a kind + Pair
    FOUR_OF_A_KIND(8, "Four of a Kind"),    // 4 cards of same rank
    STRAIGHT_FLUSH(9, "Straight Flush"),    // Straight + Flush
    ROYAL_FLUSH(10, "Royal Flush");         // A, K, Q, J, 10 same suit

    /**
     * Numerical strength for comparison logic.
     */
    private final int strength;

    /**
     * Human-readable name for UI display.
     */
    private final String label;
}