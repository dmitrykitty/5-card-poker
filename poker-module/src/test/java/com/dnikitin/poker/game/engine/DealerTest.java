package com.dnikitin.poker.game.engine;

import com.dnikitin.poker.common.model.game.Card;
import com.dnikitin.poker.game.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DealerTest {

    private Dealer dealer;
    private Player player;

    @BeforeEach
    void setUp() {
        dealer = new Dealer();
        player = new Player("1", "Test", 1000);
    }

    @Test
    @DisplayName("Should generate a audit seed for deck")
    void testAuditSeed() {
        String seed = dealer.getDeckSeedForAudit();
        assertAll(
                () -> assertThat(seed).startsWith("DECK_SEED:"),
                () -> assertThat(dealer.getDeck()).isNotNull()
        );

    }

    @Test
    @DisplayName("Should deal initial hands correctly")
    void testDealInitialHands() {
        List<Player> players = List.of(
                new Player("1", "A", 100),
                new Player("2", "B", 100)
        );

        dealer.dealInitialHands(players, 5);

        assertAll(
                () -> assertThat(players.getFirst().getHand()).hasSize(5),
                () -> assertThat(players.get(1).getHand()).hasSize(5),
                () -> assertThat(dealer.getRemainingCards()).isEqualTo(42)
                );
    }

    @Test
    @DisplayName("Should exchange cards correctly")
    void testExchangeCards() {
        // Deal initial hand
        dealer.dealInitialHands(List.of(player), 5);
        List<Card> initialHand = List.copyOf(player.getHand());

        // Exchange index 0 and 2
        List<Integer> toDiscard = List.of(0, 2);
        dealer.exchangeCards(player, toDiscard);
        assertAll(
                () -> assertThat(player.getHand()).hasSize(5),
                () -> assertThat(player.getHand()).doesNotContain(initialHand.getFirst(), initialHand.get(2)),
                () -> assertThat(player.getHand()).contains(initialHand.get(1), initialHand.get(3), initialHand.get(4))
        );
    }
}
