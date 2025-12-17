package com.dnikitin.poker.common.model.game;

import lombok.NonNull;

/**
 * Represents a standard playing card in a 52-card deck.
 * <p>
 * Implemented as a Java <b>Record</b> to provide:
 * <ul>
 * <li>Immutability (cannot change rank/suit after creation).</li>
 * <li>Automatic <code>equals()</code> and <code>hashCode()</code> implementation.</li>
 * <li>Concise syntax.</li>
 * </ul>
 * </p>
 * <p>
 * Implements {@link Comparable} to support automatic sorting (e.g., in a hand).
 * </p>
 *
 * @param rank The rank of the card (e.g., ACE, KING, TEN).
 * @param suit The suit of the card (e.g., HEARTS, SPADES).
 */
public record Card(Rank rank, Suit suit) implements Comparable<Card> {

    /**
     * Compares this card with another card for ordering.
     * <p>
     * Ordering logic:
     * 1. Rank (Power) - Higher rank comes first (descending order usually handled by comparator).
     * 2. Suit - Tie-breaker if ranks are equal.
     * </p>
     *
     * @param other The card to compare to.
     * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(@NonNull Card other) {
        int compare = Integer.compare(rank.getPower(), other.rank.getPower());
        return compare != 0 ? compare : suit.compareTo(other.suit);
    }

    /**
     * Compares cards only based on their Rank power, ignoring suits.
     * Useful for game logic where suits don't affect hand strength (except for Flush).
     */
    public int compareByPowerOnly(@NonNull Card other){
        return Integer.compare(rank.getPower(), other.rank.getPower());
    }

    /**
     * Returns the string representation (e.g., "Ah" for Ace of Hearts).
     */
    @Override
    @NonNull
    public String toString() {
        return rank.toString() + suit.toString();
    }
}