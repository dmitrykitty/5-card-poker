package com.dnikitin.poker.game;

import com.dnikitin.poker.model.Deck;
import com.dnikitin.poker.gamelogic.HandEvaluator;

public interface GameFactory {
    Deck createDeck();
    HandEvaluator createHandEvaluator();
    GameConfig createGameConfig();
}
