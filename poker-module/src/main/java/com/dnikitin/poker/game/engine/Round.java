package com.dnikitin.poker.game.engine;

import com.dnikitin.poker.game.state.GameState;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a single betting round state.
 * Tracks the current bet level, actions taken, and who raised last.
 */
@Slf4j
@Getter
@Setter
public class Round {
    private int currentBet;
    private int actionsInRound;
    private int lastAggressorIndex;
    private GameState phase;

    public Round(GameState phase) {
        this.phase = phase;
        this.currentBet = 0;
        this.actionsInRound = 0;
        this.lastAggressorIndex = -1;
    }

    /**
     * Records a player action (check, call, fold).
     */
    public void recordAction() {
        actionsInRound++;
        log.debug("Action recorded in {}. Total actions: {}", phase, actionsInRound);
    }

    /**
     * Records a bet or raise action.
     *
     * @param newBetAmount  The new bet amount
     * @param playerIndex   Index of the player who raised
     */
    public void recordRaise(int newBetAmount, int playerIndex) {
        currentBet = newBetAmount;
        lastAggressorIndex = playerIndex;
        actionsInRound = 1; // Reset counter as everyone must respond
        log.debug("Raise recorded in {}. New bet: {}, aggressor: {}",
            phase, newBetAmount, playerIndex);
    }

    /**
     * Checks if the betting round is complete.
     * A round is complete when all active players have acted and matched the current bet.
     *
     * @param activePlayers Number of active (non-folded) players
     * @return true if round is complete
     */
    public boolean isComplete(int activePlayers) {
        if (activePlayers < 2) {
            return true; // Only one player left
        }
        // All active players must have acted
        return actionsInRound >= activePlayers;
    }

    /**
     * Resets the round for a new betting phase.
     */
    public void reset(GameState newPhase) {
        phase = newPhase;
        currentBet = 0;
        actionsInRound = 0;
        lastAggressorIndex = -1;
        log.debug("Round reset for phase: {}", phase);
    }

    @Override
    public String toString() {
        return String.format("Round{phase=%s, bet=%d, actions=%d}",
            phase, currentBet, actionsInRound);
    }
}
