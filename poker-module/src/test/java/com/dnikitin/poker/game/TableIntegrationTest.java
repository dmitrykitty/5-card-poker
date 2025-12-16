package com.dnikitin.poker.game;

import com.dnikitin.poker.common.model.game.Card;
import com.dnikitin.poker.common.model.game.Rank;
import com.dnikitin.poker.common.model.game.Suit;
import com.dnikitin.poker.exceptions.moves.InvalidMoveException;
import com.dnikitin.poker.exceptions.moves.OutOfTurnException;
import com.dnikitin.poker.exceptions.moves.StateMismatchException;
import com.dnikitin.poker.exceptions.rules.IllegalPlayerAmountException;
import com.dnikitin.poker.game.setup.FiveCardDrawFactory;
import com.dnikitin.poker.game.setup.GameConfig;
import com.dnikitin.poker.game.setup.GameFactory;
import com.dnikitin.poker.game.state.GameState;
import com.dnikitin.poker.gamelogic.StandardFiveCardHandEvaluator;
import com.dnikitin.poker.model.Deck;
import com.dnikitin.poker.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    @DisplayName("LOBBY: Should remove player completely on disconnect in Lobby")
    void testDisconnectInLobby() {
        // given
        table.addPlayer(p1);
        table.addPlayer(p2);

        assertThat(table.getCurrentState()).isEqualTo(GameState.LOBBY);
        assertThat(table.getPlayers()).hasSize(2);

        // when
        table.playerDisconnect(p1);

        // then
        assertThat(table.getPlayers()).hasSize(1);
        assertThat(table.getPlayers()).containsExactly(p2);
        assertThat(table.getPlayers()).doesNotContain(p1);
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

        table.startGame(); // poor has 40 left.

        // Betting Round 1
        // Active player logic handles dealer rotation, we just need to follow current
        while (table.getCurrentState() == GameState.BETTING_1) {
            Player player = table.getCurrentPlayer();
            if (player == poor) {
                // Poor goes All-In (40 chips)
                int chips = player.getChips();
                int toCall = table.getCurrentRoundHighestBet() - player.getCurrentBet();

                if (toCall >= chips) {
                    assertThatThrownBy(() -> table.playerCheck(player))
                            .isInstanceOf(InvalidMoveException.class)
                            .hasMessageContaining("Cannot CHECK, you must CALL");

                    table.playerCall(player);
                } else {
                    table.playerRaise(player, chips);
                }
            } else {
                // Rich players bet huge
                if (table.getCurrentRoundHighestBet() < 200) {
                    table.playerRaise(player, 200);
                } else {
                    table.playerCall(player);
                }
            }
        }

        // Verify PotManager created side pots
        assertThat(table.getPotManager().getPotCount()).isGreaterThanOrEqualTo(2);
        assertThat(poor.isAllIn()).isTrue();
    }

    @Test
    @DisplayName("SPLIT POT: Should split pot 3-ways (Mockito Version)")
    void testThreeWaySplitWithMocks() {
        // 1. CREATE MOCKS
        GameFactory mockFactory = mock(GameFactory.class);
        Deck mockDeck = mock(Deck.class);

        // 2. CARD SETUP (Royal Flush for everyone)
        Card[] p2Cards = {
                new Card(Rank.ACE, Suit.HEARTS), new Card(Rank.KING, Suit.HEARTS),
                new Card(Rank.QUEEN, Suit.HEARTS), new Card(Rank.JACK, Suit.HEARTS), new Card(Rank.TEN, Suit.HEARTS)
        };

        // Cards for Player 3
        Card[] p3Cards = {
                new Card(Rank.ACE, Suit.DIAMONDS), new Card(Rank.KING, Suit.DIAMONDS),
                new Card(Rank.QUEEN, Suit.DIAMONDS), new Card(Rank.JACK, Suit.DIAMONDS), new Card(Rank.TEN, Suit.DIAMONDS)
        };

        // Cards for Player 1 (Dealer)
        Card[] p1Cards = {
                new Card(Rank.ACE, Suit.SPADES), new Card(Rank.KING, Suit.SPADES),
                new Card(Rank.QUEEN, Suit.SPADES), new Card(Rank.JACK, Suit.SPADES), new Card(Rank.TEN, Suit.SPADES)
        };

        // 3. CONFIGURE MOCK BEHAVIOR

        // When Table requests a deck -> return our mock
        when(mockFactory.createDeck()).thenReturn(mockDeck);

        // When Table requests config -> return a real object (easier than mocking getters)
        when(mockFactory.createGameConfig()).thenReturn(
                new GameConfig(4, 2, 1000, 10, 0));

        // When Table requests evaluator -> return real one (we want actual hand evaluation logic!)
        when(mockFactory.createHandEvaluator()).thenReturn(new StandardFiveCardHandEvaluator());

        // the exact order of cards returned by Deck.deal()
        // thenReturn accepts varargs, so we list all cards sequentially
        when(mockDeck.deal()).thenReturn(
                p2Cards[0], p2Cards[1], p2Cards[2], p2Cards[3], p2Cards[4], // For P2
                p3Cards[0], p3Cards[1], p3Cards[2], p3Cards[3], p3Cards[4], // For P3
                p1Cards[0], p1Cards[1], p1Cards[2], p1Cards[3], p1Cards[4]  // For P1
        );

        // 4. INITIALIZE TABLE WITH MOCKED FACTORY
        Table table = new Table(mockFactory);

        Player p1 = new Player("p1", "Player 1", 1000);
        Player p2 = new Player("p2", "Player 2", 1000);
        Player p3 = new Player("p3", "Player 3", 1000);

        table.addPlayer(p1);
        table.addPlayer(p2);
        table.addPlayer(p3);

        // GAME START
        table.startGame();

        // Betting Round 1: Everyone Checks
        table.playerCheck(p2);
        table.playerCheck(p3);
        table.playerCheck(p1);

        // Drawing Phase: Stand Pat (no one exchanges cards)
        table.playerExchangeCards(p2, List.of());
        table.playerExchangeCards(p3, List.of());
        table.playerExchangeCards(p1, List.of());

        // Betting Round 2
        table.playerRaise(p2, 90);
        table.playerCall(p3);
        table.playerCall(p1);

        // Verification
        assertThat(table.getPot()).isEqualTo(0); // Pot should be empty (distributed)
        assertThat(p1.getChips()).isEqualTo(1000); // Back to start (1000)
        assertThat(p2.getChips()).isEqualTo(1000); // Back to start (1000)
        assertThat(p3.getChips()).isEqualTo(1000); // Back to start (1000)
    }
}