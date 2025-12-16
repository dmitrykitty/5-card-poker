package com.dnikitin.poker.model;

import com.dnikitin.poker.common.model.game.Card;
import com.dnikitin.poker.common.model.game.Rank;
import com.dnikitin.poker.common.model.game.Suit;
import com.dnikitin.poker.exceptions.moves.NotEnoughChipsException;
import com.dnikitin.poker.game.state.PlayerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    private Player player;
    private final int INITIAL_CHIPS = 1000;

    @BeforeEach
    void setUp() {
        player = new Player("uuid-1", "TestPlayer", INITIAL_CHIPS);
    }

    @Test
    void shouldReduceChipsWhenBetting() {
        // given
        int betAmount = 200;

        // when
        player.bet(betAmount);

        // then
        assertThat(player.getChips()).isEqualTo(INITIAL_CHIPS - betAmount);
        assertThat(player.getCurrentBet()).isEqualTo(betAmount);
    }

    @Test
    void shouldThrowExceptionWhenNotEnoughChips() {
        assertThatThrownBy(() -> player.bet(INITIAL_CHIPS + 1))
                .isInstanceOf(NotEnoughChipsException.class)
                .hasMessage("Not enough chips");
    }


    @Test
    void shouldAccumulateCurrentBet() {
        player.bet(100);
        player.bet(200); // Raise

        assertThat(player.getCurrentBet()).isEqualTo(300);
        assertThat(player.getChips()).isEqualTo(INITIAL_CHIPS - 300);
    }

    @Test
    void shouldDiscardCardsCorrectlyHandlingIndexShifts() {
        // given: [A, K, Q, J, 10]
        List<Card> initialHand = new ArrayList<>();
        initialHand.add(new Card(Rank.ACE, Suit.SPADES));   // index 0
        initialHand.add(new Card(Rank.KING, Suit.SPADES));  // index 1
        initialHand.add(new Card(Rank.QUEEN, Suit.SPADES)); // index 2
        initialHand.add(new Card(Rank.JACK, Suit.SPADES));  // index 3
        initialHand.add(new Card(Rank.TEN, Suit.SPADES));   // index 4
        player.receiveCards(initialHand);

        // when: delete[A, Q]
        List<Integer> indexesToDiscard = List.of(0, 2);
        player.discardCards(indexesToDiscard);

        // then: [K, J, 10]
        assertThat(player.getHand()).hasSize(3);
        assertThat(player.getHand()).containsExactly(
                new Card(Rank.KING, Suit.SPADES),
                new Card(Rank.JACK, Suit.SPADES),
                new Card(Rank.TEN, Suit.SPADES)
        );
    }

    @Test
    void shouldMarkAsFolded() {
        player.fold();

        assertThat(player.isFolded()).isTrue();
        assertThat(player.isActive()).isFalse();
    }

    @Test
    void shouldClearHandAndResetStatus() {
        player.receiveCards(List.of(new Card(Rank.ACE, Suit.HEARTS)));
        player.fold();
        player.bet(100);

        player.clearHand();
        player.resetRoundBet();

        assertThat(player.getHand()).isEmpty();
        assertThat(player.isFolded()).isFalse();
        assertThat(player.getCurrentBet()).isZero();
    }

    @Test
    void shouldChangePlayerStatusDuringTheGame() {
        //active from the begining
        assertAll(
                () -> assertThat(player.getStatus()).isEqualTo(PlayerStatus.ACTIVE),
                () -> assertThat(player.isActive()).isTrue()
        );

        player.bet(1000);
        assertAll(
                () -> assertThat(player.getStatus()).isEqualTo(PlayerStatus.ALL_IN),
                () -> assertThat(player.isAllIn()).isTrue()
        );

        player.fold();
        assertAll(
                () -> assertThat(player.getStatus()).isEqualTo(PlayerStatus.FOLDED),
                () -> assertThat(player.isFolded()).isTrue()
        );
    }
}