package com.dnikitin.poker.common.exceptions;

/**
 * Thrown by the Game Engine when the {@link com.dnikitin.poker.common.model.game.Deck} runs out of cards.
 * <p>
 * This is a critical game state error (Internal Server Error 500 equivalent) usually caused by:
 * <ul>
 * <li>Too many players for the deck size.</li>
 * <li>A game logic bug where cards are not reshuffled correctly.</li>
 * </ul>
 * </p>
 */
public class EmptyDeckException extends RuntimeException {
    public EmptyDeckException(String message) {
        super(message);
    }
}