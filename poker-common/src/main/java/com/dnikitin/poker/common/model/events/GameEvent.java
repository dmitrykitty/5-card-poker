package com.dnikitin.poker.common.model.events;

import com.dnikitin.poker.common.model.game.Card;

import java.util.List;

/**
 * Represents the finite set of events that can occur within the Poker game logic.
 * <p>
 * <b>Architecture Note:</b>
 * This interface is <b>sealed</b>, which means all possible implementations are defined
 * in this file. This allows consumers (like the Protocol Encoder) to use
 * pattern matching in switch expressions with compiler-enforced exhaustiveness checks.
 * If a new event type is added, the compiler will force you to handle it in all switch cases.
 * </p>
 * <p>
 * These events act as the notification mechanism from the Core Domain (Game Engine)
 * to the Infrastructure Layer (Server/Network).
 * </p>
 */
public sealed interface GameEvent {

    /**
     * Triggered when a new player successfully sits at the table.
     *
     * @param playerId The unique ID of the player.
     * @param name     The display name.
     * @param chips    Initial chip count.
     */
    record PlayerJoined(String playerId, String name, int chips) implements GameEvent {
    }

    /**
     * Triggered when the game transitions from the Lobby phase to active gameplay.
     *
     * @param gameId The unique game session ID.
     */
    record GameStarted(String gameId) implements GameEvent {
    }

    /**
     * Triggered when the game phase changes (e.g., from BETTING to DRAW).
     *
     * @param newState The name of the new phase.
     */
    record StateChanged(String newState) implements GameEvent {
    }

    /**
     * Triggered when cards are distributed to a player.
     * <p>
     * <b>Security Note:</b> When sending this event over the network, the server MUST
     * mask the cards for all recipients except the owner of the hand to prevent cheating.
     * </p>
     *
     * @param playerId The recipient of the cards.
     * @param cards    The list of cards dealt.
     */
    record CardsDealt(String playerId, List<Card> cards) implements GameEvent {
    }

    /**
     * Triggered when a player performs a visible action (e.g., Bet, Fold).
     *
     * @param playerId   The actor.
     * @param actionType The type of action (e.g., "RAISE", "FOLD").
     * @param amount     The amount involved (e.g., bet size), or 0 if not applicable.
     * @param message    Additional description or chat message.
     */
    record PlayerAction(String playerId, String actionType, int amount, String message) implements GameEvent {
    }

    /**
     * Triggered when the active turn passes to another player.
     *
     * @param activePlayerId The ID of the player who must move next.
     * @param phase          The current game phase.
     * @param amountToCall   The amount needed to match the current highest bet.
     * @param minRaise       The minimum allowed raise amount.
     */
    record TurnChanged(String activePlayerId, String phase, int amountToCall, int minRaise) implements GameEvent {
    }

    /**
     * Triggered at the end of a hand when a winner is determined.
     *
     * @param winnerId The ID of the winning player.
     * @param potAmount The total amount won.
     * @param handRank  The description of the winning hand (e.g., "Flush", "Two Pairs").
     * @param cards     The winning cards (revealed to all).
     */
    record GameFinished(String winnerId, int potAmount, String handRank, List<Card> cards) implements GameEvent {
    }

    /**
     * Triggered to update public table information (Pot size, etc.).
     *
     * @param potAmount  Current total chips in the pot.
     * @param highestBet The current highest bet on the table.
     */
    record RoundInfo(int potAmount, int highestBet) implements GameEvent {
    }
}