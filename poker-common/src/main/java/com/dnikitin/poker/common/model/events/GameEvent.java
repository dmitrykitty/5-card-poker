package com.dnikitin.poker.common.model.events;

import com.dnikitin.poker.common.model.game.Card;

import java.util.List;

public sealed interface GameEvent {
    // Zdarzenie: Gracz dołączył do stołu
    record PlayerJoined(String playerId, String name, int chips) implements GameEvent {
    }

    // Zdarzenie: Nowa gra/rozdanie się rozpoczęło
    record GameStarted(String gameId) implements GameEvent {
    }

    // Zdarzenie: Nowa runda (np. ANTE, BETTING, DRAW)
    record StateChanged(String newState) implements GameEvent {
    }

    // Zdarzenie: Karty zostały rozdane (serwer potem zamaskuje karty dla innych graczy)
    record CardsDealt(String playerId, List<Card> cards) implements GameEvent {
    }

    // Zdarzenie: Ruch gracza (informacja publiczna)
    record PlayerAction(String playerId, String actionType, int amount, String message) implements GameEvent {
    }

    // Zdarzenie: Zmiana tury (czyja kolej)
    record TurnChanged(String activePlayerId) implements GameEvent {
    }

    // Zdarzenie: Koniec gry/rozdania - wynik
    record GameFinished(String winnerId, int potAmount, String handRank) implements GameEvent {
    }

    // Zdarzenie: Błąd (np. nielegalny ruch)
    record ErrorOccurred(String playerId, String errorMessage) implements GameEvent {
    }

    record RoundInfo(int potAmount, int highestBet) implements GameEvent {
    }
}
