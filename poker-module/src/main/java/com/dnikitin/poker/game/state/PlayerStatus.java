package com.dnikitin.poker.game.state;

/**
 * Represents the current status of a player in a poker hand.
 */
public enum PlayerStatus {
    /**
     * Player is actively participating in the hand.
     */
    ACTIVE,

    /**
     * Player has folded their hand.
     */
    FOLDED,

    /**
     * Player is all-in (has bet all their chips).
     */
    ALL_IN,

    /**
     * Player is sitting out or disconnected.
     */
    SITTING_OUT
}
