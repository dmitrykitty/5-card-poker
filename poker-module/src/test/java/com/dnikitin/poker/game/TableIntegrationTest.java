package com.dnikitin.poker.game;

import com.dnikitin.poker.exceptions.moves.InvalidMoveException;
import com.dnikitin.poker.exceptions.moves.OutOfTurnException;
import com.dnikitin.poker.exceptions.moves.StateMismatchException;
import com.dnikitin.poker.exceptions.rules.IllegalPlayerAmountException;
import com.dnikitin.poker.game.setup.FiveCardDrawFactory;
import com.dnikitin.poker.game.setup.GameFactory;
import com.dnikitin.poker.game.state.GameState;
import com.dnikitin.poker.model.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class TableIntegrationTest {

    private Table table;
    private Player p1;
    private Player p2;
    private Player p3;

    @BeforeEach
    void setUp() {
        GameFactory factory = new FiveCardDrawFactory();
        table = new Table(factory);

        p1 = new Player("1", "Alice", 1000);
        p2 = new Player("2", "Bob", 1000);
        p3 = new Player("3", "Charlie", 1000);
    }

    @Test
    @DisplayName("FULL GAME: Should play a complete hand with showdown")
    void testFullGameFlow() {
        // 1. Setup & Start
        table.addPlayer(p1);
        table.addPlayer(p2);
        table.startGame();

        // Check Ante collected (10 each)
        assertThat(p1.getChips()).isEqualTo(990);
        assertThat(p2.getChips()).isEqualTo(990);
        assertThat(table.getPot()).isEqualTo(20);
        assertThat(table.getCurrentState()).isEqualTo(GameState.BETTING_1);

        // 2. Betting Round 1
        Player first = table.getCurrentPlayer();
        Player second = first == p1 ? p2 : p1;

        table.playerCheck(first);
        table.playerCheck(second);

        // Should transition to DRAWING
        assertThat(table.getCurrentState()).isEqualTo(GameState.DRAWING);

        // 3. Drawing Phase
        // First player active again (TurnOrder resets to left of dealer)
        Player drawer1 = table.getCurrentPlayer();
        Player drawer2 = drawer1 == p1 ? p2 : p1;

        table.playerExchangeCards(drawer1, List.of(0, 1)); // Exchange 2 cards
        table.playerExchangeCards(drawer2, List.of());     // Stand pat (0 cards)

        // Should transition to BETTING_2
        assertThat(table.getCurrentState()).isEqualTo(GameState.BETTING_2);

        // 4. Betting Round 2
        Player bettor1 = table.getCurrentPlayer();
        Player bettor2 = bettor1 == p1 ? p2 : p1;

        //table.playerBet(bettor1, 50); // Using Raise logic for opening bet if check not allowed or logic inside raise
        // W Twoim kodzie playerRaise obsługuje też Bet
        // table.playerRaise(bettor1, 50);
        // UWAGA: Metoda w Table nazywa się playerRaise, ale logika obsługuje betowanie.
        // Jeśli currentBet == 0, raise o 50 ustawia bet na 50.

        table.playerRaise(bettor1, 50);
        table.playerCall(bettor2);

        // 5. Showdown -> Finished
        assertThat(table.getCurrentState()).isEqualTo(GameState.FINISHED);

        // Winner logic validation
        // Pot should be 20 (ante) + 50 (bet) + 50 (call) = 120
        // One player should have > 940 (original - ante - bet + pot)
        int p1Chips = p1.getChips();
        int p2Chips = p2.getChips();

        assertAll(
                () -> assertThat(p1Chips + p2Chips).isEqualTo(2000), // Conservation of mass (chips)
                () -> assertThat(p1Chips).satisfiesAnyOf(
                        chips -> assertThat(chips).isEqualTo(1060),
                        chips -> assertThat(chips).isEqualTo(940)),
                () -> assertThat(p2Chips).satisfiesAnyOf(
                        chips -> assertThat(chips).isEqualTo(1060),
                        chips -> assertThat(chips).isEqualTo(940))
        );


    }

    @Test
    @DisplayName("FOLD VICTORY: Game should end immediately if all opponents fold")
    void testWinByFold() {
        table.addPlayer(p1);
        table.addPlayer(p2);
        table.addPlayer(p3);
        table.startGame();

        // Order depends on dealer, let's just get current
        Player active = table.getCurrentPlayer();
        table.playerRaise(active, 100);

        // Next player folds
        Player next = table.getCurrentPlayer();
        table.playerFold(next);

        // Last player folds
        Player last = table.getCurrentPlayer();
        table.playerFold(last);


        // The game should end immediately
        assertThat(table.getCurrentState()).isEqualTo(GameState.FINISHED);
        assertThat(active.getChips()).isGreaterThan(1000); // Won blinds/antes
    }

    @Test
    @DisplayName("EXCEPTION: Should throw OutOfTurnException when acting out of order")
    void testOutOfTurn() {
        table.addPlayer(p1);
        table.addPlayer(p2);
        table.startGame();

        Player current = table.getCurrentPlayer();
        Player notCurrent = current == p1 ? p2 : p1;

        assertThatThrownBy(() -> table.playerCheck(notCurrent))
                .isInstanceOf(OutOfTurnException.class)
                .hasMessage("Not your turn.");
    }

    @Test
    @DisplayName("EXCEPTION: Should throw StateMismatchException when drawing in betting phase")
    void testDrawInWrongPhase() {
        table.addPlayer(p1);
        table.addPlayer(p2);
        table.startGame();
        // State is BETTING_1

        Player current = table.getCurrentPlayer();

        assertThatThrownBy(() -> table.playerExchangeCards(current, List.of(0)))
                .isInstanceOf(StateMismatchException.class) // Or InvalidMoveException depending on hierarchy
                .hasMessageContaining("Cannot draw cards now. Current state:");
    }

    @Test
    @DisplayName("EXCEPTION: Should throw IllegalDrawException when drawing too many cards")
    void testDrawTooManyCards() {
        table.addPlayer(p1);
        table.addPlayer(p2);
        table.startGame();

        // Advance to DRAWING
        table.playerCheck(table.getCurrentPlayer());
        table.playerCheck(table.getCurrentPlayer());

        assertThat(table.getCurrentState()).isEqualTo(GameState.DRAWING);

        Player current = table.getCurrentPlayer();
        // Try to draw 4 cards (Limit is 3)
        List<Integer> invalidDraw = List.of(0, 1, 2, 3);

        assertThatThrownBy(() -> table.playerExchangeCards(current, invalidDraw))
                .isInstanceOf(InvalidMoveException.class)
                .hasMessageContaining("Cannot discard more than");
    }

    @Test
    @DisplayName("EXCEPTION: Should verify player count constraints")
    void testPlayerCountConstraints() {
        // Too few
        table.addPlayer(p1);
        assertThatThrownBy(() -> table.startGame())
                .isInstanceOf(IllegalPlayerAmountException.class);

        // Too many (Config max is 4)
        table.addPlayer(p2);
        table.addPlayer(p3);
        table.addPlayer(new Player("4", "D", 1000));

        Player p5 = new Player("5", "E", 1000);
        assertThatThrownBy(() -> table.addPlayer(p5))
                .isInstanceOf(IllegalPlayerAmountException.class)
                .hasMessageContaining("Table is full");
    }

    @Test
    @DisplayName("DISCONNECT: Should auto-fold disconnected player")
    void testDisconnectDuringTurn() {
        table.addPlayer(p1);
        table.addPlayer(p2);
        table.startGame();

        Player current = table.getCurrentPlayer();
        Player other = current == p1 ? p2 : p1;

        // Simulate disconnect
        table.playerDisconnect(current);

        // Player should be folded and set to SITTING_OUT
        assertThat(current.isFolded()).isTrue();

        // Since only 2 players, game should end immediately
        assertThat(table.getCurrentState()).isEqualTo(GameState.FINISHED);

        // Other player wins
        assertThat(other.getChips()).isGreaterThan(1000);
    }

    @Test
    @DisplayName("SIDE POT: Should handle All-In correctly")
    void testAllInSidePotIntegration() {
        Player rich1 = new Player("R1", "Rich1", 1000);
        Player rich2 = new Player("R2", "Rich2", 1000);
        Player poor = new Player("P1", "Poor", 50); // Can only afford Ante + small bet

        table.addPlayer(rich1);
        table.addPlayer(rich2);
        table.addPlayer(poor);

        table.startGame(); // Ante 10 taken. Poor has 40 left.

        // Betting Round 1
        // Active player logic handles dealer rotation, we just need to follow current
        while (table.getCurrentState() == GameState.BETTING_1) {
            Player p = table.getCurrentPlayer();
            if (p == poor) {
                // Poor goes All-In (40 chips)
                try {
                    table.playerRaise(p, 40);
                } catch (Exception e) {
                    // If raise not allowed (e.g. min raise), call/check
                    // In this test setup, we force the scenario
                    // If already bet, call.
                    if (table.getCurrentRoundHighestBet() > p.getCurrentBet())
                        table.playerCall(p);
                    else
                        table.playerCheck(p);
                }
            } else {
                // Rich players bet huge
                if (table.getCurrentRoundHighestBet() < 200) {
                    table.playerRaise(p, 200);
                } else {
                    table.playerCall(p);
                }
            }
        }

        // Verify PotManager created side pots
        assertThat(table.getPotManager().getPotCount()).isGreaterThanOrEqualTo(2);
        assertThat(poor.isAllIn()).isTrue();
    }
}