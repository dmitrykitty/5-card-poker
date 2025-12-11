package com.dnikitin.poker.game;

import com.dnikitin.poker.common.model.Card;
import com.dnikitin.poker.exceptions.IllegalPlayerAmountException;
import com.dnikitin.poker.exceptions.NotEnoughChipsException;
import com.dnikitin.poker.exceptions.WrongGameStateException;
import com.dnikitin.poker.gamelogic.HandEvaluator;
import com.dnikitin.poker.model.Deck;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single Poker Table instance.
 * <p>
 * This class acts as the central Game Engine. It manages the lifecycle of the game
 * (State Machine), holds the deck and players, and enforces turn order.
 * It connects the "Physics" (Deck, Cards) with the "Rules" (Evaluator, Config).
 * </p>
 */
@Slf4j
@Getter
public class Table {

    private final String id;
    private final GameConfig config;
    private final HandEvaluator evaluator;

    private final List<Player> players = new ArrayList<>();
    private Deck deck;

    /**
     * Current state of the game loop (e.g. LOBBY, BETTING, FINISHED).
     */
    private GameState currentState;

    /**
     * Total amount of chips currently in the pot for this hand.
     */
    private int pot;

    /**
     * Index of the player who holds the "Dealer Button".
     * This player acts last in betting rounds. Rotates every hand.
     */
    private int dealerIndex = -1;

    /**
     * Index of the player expected to make a move right now.
     */
    private int currentPlayerIndex = 0;

    /**
     * Creates a new Table with specific rules provided by the factory.
     *
     * @param gameFactory Factory providing deck, evaluator, and config.
     */
    public Table(GameFactory gameFactory) {
        id = UUID.randomUUID().toString();
        config = gameFactory.createGameConfig();
        evaluator = gameFactory.createHandEvaluator();
        deck = gameFactory.createDeck();
        currentState = GameState.LOBBY;
    }

    /**
     * Adds a player to the table.
     * Allowed only if the game is in LOBBY state and table is not full.
     *
     * @param player The player to join.
     * @throws WrongGameStateException      if game has already started.
     * @throws IllegalPlayerAmountException if table is full.
     */
    public void addPlayer(Player player) {
        if (currentState != GameState.LOBBY) {
            throw new WrongGameStateException("Game already started");
        }
        if (players.size() >= config.maxPlayers()) {
            throw new IllegalPlayerAmountException("Table is full");
        }
        players.add(player);
        log.info("Player {} joined table {}. Total players: {}", player.getName(), id, players.size());
    }

    /**
     * Starts the game loop. Validates minimum player count.
     * Triggers the first hand.
     *
     * @throws IllegalPlayerAmountException if there are not enough players.
     */
    public void startGame() {
        if (players.size() < config.minPlayers()) {
            throw new IllegalPlayerAmountException("Not enough players to start. Min: " + config.minPlayers());
        }
        log.info("Starting game on table {}", id);
        startNewHand();
    }

    public Player getCurrentPlayer(){
        return players.isEmpty() ? null : players.get(currentPlayerIndex);
    }

    private void startNewHand() {
        pot = 0;
        deck = Deck.createDeck(); //each game - new Deck
        deck.shuffle();

        players.forEach(Player::clearHand);

        changeState(GameState.ANTE);
        collectAnte();

        changeState(GameState.DEALING);
        dealCards();

        changeState(GameState.BETTING_1);
        log.info("Hand started. Pot: {}. Deck size remaining: {}", pot, deck.size());
    }

    private void dealCards() {
        log.info("Dealing 5 cards to each active player.");
        for (Player player : players) {
            List<Card> hand = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                hand.add(deck.deal());
            }
            player.receiveCards(hand);
        }
    }

    private void collectAnte() {
        int ante = config.ante();
        log.info("Collecting ante: {}", ante);

        List<Player> bankrupts = new ArrayList<>();
        for (Player player : players) {
            try {
                player.bet(ante);
                pot += ante;
            } catch (NotEnoughChipsException e) {
                log.warn("Player {} cannot pay ante and has been removed. The reason: {}", player.getName(), e.getMessage());
                bankrupts.add(player);
            }
        }
        players.removeAll(bankrupts);
        if (players.size() < 2) {
            throw new IllegalPlayerAmountException("Not enough players after Ante collection");
        }
    }

    private void changeState(GameState newState) {
        log.info("State transition: {} -> {}", currentState, newState);
        currentState = newState;
    }
}
