package com.dnikitin.poker.game.state;
/**
 * Represents the lifecycle of a single Poker Hand (Round).
 * The server moves strictly from top to bottom (looping back from FINISHED to ANTE).
 */
public enum GameState {
    /**
     * Waiting for players to join and sit at the table.
     * Game logic is paused.
     */
    LOBBY,

    /**
     * Collecting mandatory chips (entry fee) from all active players before dealing.
     */
    ANTE,

    /**
     * Shuffling the deck and distributing 5 cards to each active player.
     */
    DEALING,

    /**
     * First round of betting. Players bet on their initial hand.
     * Options: Bet, Call, Raise, Check, Fold.
     */
    BETTING_1,

    /**
     * The Draw phase. Active players can discard and replace cards (typically up to 3).
     */
    DRAWING,

    /**
     * Second round of betting after players have received new cards.
     */
    BETTING_2,

    /**
     * Players reveal their hands. The system evaluates ranks and determines the winner(s).
     * The pot is awarded to the winner.
     */
    SHOWDOWN,

    /**
     * End of the round. Reseting deck, clearing hands, and preparing for the next Ante.
     * Checks if players still have chips to continue.
     */
    FINISHED
}

