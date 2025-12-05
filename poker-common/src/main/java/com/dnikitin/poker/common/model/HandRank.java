package com.dnikitin.poker.common.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString(of = {"label"}, includeFieldNames = false)
public enum HandRank {
    HIGH_CARD(1, "High Card"),
    ONE_PAIR(2, "One Pair"),
    TWO_PAIRS(3, "Two Pairs"),
    THREE_OF_A_KIND(4, "Three of a Kind"),
    STRAIGHT(5, "Straight"),                // 2, 3, 4, 5, 6
    FLUSH(6, "Flush"),                      // the same color
    FULL_HOUSE(7, "Full House"),            // Threeset + Pair
    FOUR_OF_A_KIND(8, "Four of a Kind"),    // 4 the same
    STRAIGHT_FLUSH(9, "Straight Flush"),    // 2, 3, 4, 5, 6 and same color
    ROYAL_FLUSH(10, "Royal Flush");         // 10, J, Q, K, A and same color

    private final int strength;
    private final String label;
}
