package com.dnikitin.poker.game;

import com.dnikitin.poker.gamelogic.HandEvaluator;
import com.dnikitin.poker.gamelogic.StandardFiveCardHandEvaluator;
import com.dnikitin.poker.model.Deck;

public class FiveCardDrawFactory implements GameFactory{
    private static final int MAX_PLAYERS = 4;
    private static final int MIN_PLAYERS = 2;
    private static final int STARTING_CHIPS = 1000;
    private static final int ANTE = 10;
    private static final int MAX_CARDS_TO_DRAW = 3;

    @Override
    public Deck createDeck() {
        return Deck.createDeck();
    }

    @Override
    public HandEvaluator createHandEvaluator() {
        return new StandardFiveCardHandEvaluator();
    }

    @Override
    public GameConfig createGameConfig() {
        return new GameConfig(
                MAX_PLAYERS,
                MIN_PLAYERS,
                STARTING_CHIPS,
                ANTE,
                MAX_CARDS_TO_DRAW
        );
    }
}
