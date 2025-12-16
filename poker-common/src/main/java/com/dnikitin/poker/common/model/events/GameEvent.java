package com.dnikitin.poker.common.model.events;

import com.dnikitin.poker.common.model.game.Card;

import java.util.List;

public sealed interface GameEvent {
    record PlayerJoined(String playerId, String name, int chips) implements GameEvent {
    }

    record GameStarted(String gameId) implements GameEvent {
    }

    record StateChanged(String newState) implements GameEvent {
    }

    record CardsDealt(String playerId, List<Card> cards) implements GameEvent {
    }

    record PlayerAction(String playerId, String actionType, int amount, String message) implements GameEvent {
    }

    record TurnChanged(String activePlayerId, String phase, int amountToCall, int minRaise) implements GameEvent {
    }

    record GameFinished(String winnerId, int potAmount, String handRank, List<Card> cards) implements GameEvent {
    }

    record RoundInfo(int potAmount, int highestBet) implements GameEvent {
    }
}
