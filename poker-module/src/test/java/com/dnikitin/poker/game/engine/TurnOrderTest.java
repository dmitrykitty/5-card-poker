package com.dnikitin.poker.game.engine;

import com.dnikitin.poker.game.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TurnOrderTest {

    private TurnOrder turnOrder;
    private Player p1, p2, p3;

    @BeforeEach
    void setUp() {
        p1 = new Player("1", "A", 1000);
        p2 = new Player("2", "B", 1000);
        p3 = new Player("3", "C", 1000);
        turnOrder = new TurnOrder(List.of(p1, p2, p3));
    }

    @Test
    @DisplayName("Should rotate dealer correctly")
    void testRotateDealer() {
        // Initial state: dealerIndex = -1
        assertThat(turnOrder.getDealer()).isNull();

        turnOrder.rotateDealer();
        assertThat(turnOrder.getDealer()).isEqualTo(p1);

        turnOrder.rotateDealer();
        assertThat(turnOrder.getDealer()).isEqualTo(p2);

        turnOrder.rotateDealer();
        assertThat(turnOrder.getDealer()).isEqualTo(p3);

        turnOrder.rotateDealer();
        assertThat(turnOrder.getDealer()).isEqualTo(p1); // Wrap around
    }

    @Test
    @DisplayName("Should start turn from left of dealer")
    void testStartFromLeftOfDealer() {
        turnOrder.rotateDealer(); // Dealer is p1 (index 0)

        turnOrder.startFromLeftOfDealer();

        // Left of p1 (0) is p2 (1)
        assertThat(turnOrder.getCurrentPlayer()).isEqualTo(p2);
    }

    @Test
    @DisplayName("Should skip folded players when advancing turn")
    void testSkipFoldedPlayers() {
        // Order: p1 -> p2 -> p3
        turnOrder.rotateDealer(); // D=p1
        turnOrder.startFromLeftOfDealer(); // Current=p2

        // p3 folds
        p3.fold();

        // when
        boolean morePlayers = turnOrder.nextPlayer();

        // then
        assertThat(morePlayers).isTrue();
        // Should skip p3 and go back to p1
        assertThat(turnOrder.getCurrentPlayer()).isEqualTo(p1);
        assertThat(turnOrder.countActivePlayers()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return false if no active players left")
    void testNoPlayersLeft() {
        p1.fold();
        p2.fold();
        p3.fold();

        boolean result = turnOrder.nextPlayer();
        assertThat(result).isFalse();
    }
}
