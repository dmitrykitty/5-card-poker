package com.dnikitin.poker.game.engine;

import com.dnikitin.poker.game.state.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoundTest {

    private Round round;

    @BeforeEach
    void setUp() {
        round = new Round(GameState.BETTING_1);
    }

    @Test
    @DisplayName("Round should be complete when all active players acted and bets match")
    void testRoundCompletion() {
        int activePlayers = 3;

        // Player 1 Checks
        round.recordAction();
        assertThat(round.isComplete(activePlayers)).isFalse();

        // Player 2 Checks
        round.recordAction();
        assertThat(round.isComplete(activePlayers)).isFalse();

        // Player 3 Checks
        round.recordAction();

        // 3 actions >= 3 players -> Complete
        assertThat(round.isComplete(activePlayers)).isTrue();
    }

    @Test
    @DisplayName("Raise should reset action count (reopening betting)")
    void testRaiseResetsCount() {
        int activePlayers = 3;

        round.recordAction(); // Check
        round.recordAction(); // Check

        // Player 3 raises
        round.recordRaise(50, 2);

        // Assert reset logic
        assertThat(round.getCurrentBet()).isEqualTo(50);
        // Actions reset to 1 (the raiser counts as 1 action)
        assertThat(round.getActionsInRound()).isEqualTo(1);

        // Should not be complete, because others must call
        assertThat(round.isComplete(activePlayers)).isFalse();

        round.recordAction(); // Call
        round.recordAction(); // Call
        assertThat(round.isComplete(activePlayers)).isTrue();
    }
}
