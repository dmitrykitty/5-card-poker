package com.dnikitin.poker.game.engine;

import com.dnikitin.poker.common.model.game.Card;
import com.dnikitin.poker.game.Player;
import com.dnikitin.poker.model.Deck;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages deck operations: shuffling, dealing, and card exchange.
 * Extracts dealing logic from Table for better separation of concerns.
 */
@Slf4j
@Getter
public class Dealer {
    private Deck deck;
    private long deckSeed;
    private final SecureRandom random;

    public Dealer() {
        this.random = new SecureRandom();
        resetDeck();
    }

    /**
     * Creates a new shuffled deck for a new hand.
     * Records the seed for audit purposes.
     */
    public void resetDeck() {
        this.deckSeed = random.nextLong();
        this.deck = Deck.createDeck();
        deck.shuffle();
        log.info("New deck created and shuffled. Seed: {} (for audit)", deckSeed);
    }

    /**
     * Deals a specified number of cards to a player.
     *
     * @param count Number of cards to deal
     * @return List of dealt cards
     */
    public List<Card> dealCards(int count) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (!deck.isEmpty()) {
                cards.add(deck.deal());
            } else {
                log.error("Deck is empty! Cannot deal more cards.");
                break;
            }
        }
        return cards;
    }

    /**
     * Deals initial hands to all players.
     *
     * @param players List of players
     * @param cardsPerPlayer Number of cards each player should receive
     */
    public void dealInitialHands(List<Player> players, int cardsPerPlayer) {
        log.info("Dealing {} cards to {} players", cardsPerPlayer, players.size());

        for (Player player : players) {
            List<Card> hand = dealCards(cardsPerPlayer);
            player.receiveCards(hand);
            log.debug("Dealt {} cards to player {}", hand.size(), player.getName());
        }
    }

    /**
     * Exchanges cards for a player during the draw phase.
     *
     * @param player Player exchanging cards
     * @param indexesToDiscard Indexes of cards to discard
     * @return New cards dealt to the player
     */
    public List<Card> exchangeCards(Player player, List<Integer> indexesToDiscard) {
        // Remove old cards
        player.discardCards(indexesToDiscard);

        // Deal new cards
        List<Card> newCards = dealCards(indexesToDiscard.size());
        player.receiveCards(newCards);

        log.info("Player {} exchanged {} cards", player.getName(), indexesToDiscard.size());
        return newCards;
    }

    /**
     * Gets the remaining number of cards in the deck.
     *
     * @return Number of cards left
     */
    public int getRemainingCards() {
        return deck.size();
    }

    /**
     * Checks if the deck has enough cards for an operation.
     *
     * @param requiredCards Number of cards needed
     * @return true if deck has enough cards
     */
    public boolean hasEnoughCards(int requiredCards) {
        return deck.size() >= requiredCards;
    }

    /**
     * Gets the deck seed for audit logging.
     * This allows game reconstruction and verification.
     *
     * @return The seed used to initialize the deck
     */
    public String getDeckSeedForAudit() {
        return String.format("DECK_SEED:%016X", deckSeed);
    }
}
