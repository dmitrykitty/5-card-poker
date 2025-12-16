package com.dnikitin.poker.game.engine;

import com.dnikitin.poker.model.Player;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Manages turn order and dealer button rotation.
 * Ensures players act in the correct sequence.
 */
@Slf4j
@Getter
public class TurnOrder {
    private int dealerIndex;
    private int currentPlayerIndex;
    private final List<Player> players;

    public TurnOrder(List<Player> players) {
        this.players = players;
        this.dealerIndex = -1;
        this.currentPlayerIndex = 0;
    }

    /**
     * Rotates the dealer button to the next player.
     * Should be called at the start of each new hand.
     */
    public void rotateDealer() {
        if (players.isEmpty()) {
            return;
        }

        if (dealerIndex == -1) {
            dealerIndex = 0; // First game
        } else {
            dealerIndex = (dealerIndex + 1) % players.size();
        }

        log.debug("Dealer rotated to position {}: {}",
            dealerIndex, players.get(dealerIndex).getName());
    }

    /**
     * Sets the current player to the left of the dealer.
     * Called at the start of a betting round.
     */
    public void startFromLeftOfDealer() {
        if (players.isEmpty()) {
            return;
        }
        currentPlayerIndex = (dealerIndex + 1) % players.size();
        skipFoldedPlayers();
        log.debug("Starting turn from left of dealer: {}",
            getCurrentPlayer().getName());
    }

    /**
     * Advances to the next player.
     *
     * @return true if successfully advanced, false if no active players remain
     */
    public boolean nextPlayer() {
        if (players.isEmpty()) {
            return false;
        }

        int attempts = 0;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            attempts++;
        } while (getCurrentPlayer().isFolded() && attempts < players.size());

        if (attempts >= players.size()) {
            log.warn("No active players found");
            return false;
        }

        log.debug("Turn advanced to: {}", getCurrentPlayer().getName());
        return true;
    }

    /**
     * Gets the current player who should act.
     *
     * @return The current player
     */
    public Player getCurrentPlayer() {
        if (players.isEmpty()) {
            return null;
        }
        return players.get(currentPlayerIndex);
    }

    /**
     * Gets the dealer player.
     *
     * @return The dealer player
     */
    public Player getDealer() {
        if (players.isEmpty() || dealerIndex < 0) {
            return null;
        }
        return players.get(dealerIndex);
    }

    /**
     * Checks if the current player is at a specific index.
     *
     * @param index The index to check
     * @return true if current player is at that index
     */
    public boolean isCurrentPlayerIndex(int index) {
        return currentPlayerIndex == index;
    }

    /**
     * Counts active (non-folded) players.
     *
     * @return Number of active players
     */
    public int countActivePlayers() {
        return (int) players.stream()
            .filter(p -> !p.isFolded())
            .count();
    }

    /**
     * Skips folded players to find the next active player.
     */
    private void skipFoldedPlayers() {
        if (players.isEmpty()) {
            return;
        }

        int attempts = 0;
        while (getCurrentPlayer().isFolded() && attempts < players.size()) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            attempts++;
        }
    }
}
