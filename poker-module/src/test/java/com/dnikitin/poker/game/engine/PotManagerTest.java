package com.dnikitin.poker.game.engine;

import com.dnikitin.poker.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PotManagerTest {

    private PotManager potManager;
    private Player p1, p2, p3;

    @BeforeEach
    void setUp() {
        potManager = new PotManager();
        p1 = new Player("1", "Alice", 1000);
        p2 = new Player("2", "Bob", 1000);
        p3 = new Player("3", "Charlie", 100);
    }

    @Test
    @DisplayName("Should collect simple bets into one main pot")
    void testSimplePotCollection() {
        // given
        p1.bet(100);
        p2.bet(100);

        // when
        potManager.distributeBets(List.of(p1, p2));

        // then
        assertThat(potManager.getPotCount()).isEqualTo(1);
        assertThat(potManager.getTotalPot()).isEqualTo(200);
        assertThat(potManager.getPots().getFirst().getEligiblePlayerIds())
                .containsExactlyInAnyOrder("1", "2");
    }

    @Test
    @DisplayName("Should create Side Pot when a player is All-In with less chips")
    void testSidePotCreation() {
        // given
        // Charlie - all-in - 100.
        // Alice and Bob - 300 (total).
        // W puli głównej (dostępnej dla Charliego) powinno być 3x100 = 300.
        // W puli bocznej (tylko Alice i Bob) powinno być 2x200 = 400.

        p3.bet(100); // All-in
        p1.bet(300);
        p2.bet(300);

        // when
        potManager.distributeBets(List.of(p1, p2, p3));

        // then
        assertThat(potManager.getPotCount()).isEqualTo(2); // Main + Side

        // Main Pot
        PotManager.Pot mainPot = potManager.getPots().getFirst();
        assertThat(mainPot.getAmount()).isEqualTo(300);
        assertThat(mainPot.getEligiblePlayerIds()).containsExactlyInAnyOrder("1", "2", "3");

        // Side Pot
        PotManager.Pot sidePot = potManager.getPots().get(1);
        assertThat(sidePot.getAmount()).isEqualTo(400); // (300-100) + (300-100)
        assertThat(sidePot.getEligiblePlayerIds()).containsExactlyInAnyOrder("1", "2");
        assertThat(sidePot.getEligiblePlayerIds()).doesNotContain("3");
    }

    @Test
    @DisplayName("Should correct split pot odd chips")
    void testSplitPotWithOddChips() {
        // given pot = 3. Winners = 2. Share = 1, Remainder = 1.
        potManager.addToMainPot(3);

        // when
        potManager.splitPot(0, List.of(p1, p2));
        // then
        int totalDistributed = (1000 - p1.getChips()) * -1 + (1000 - p2.getChips()) * -1;
        // Player starts with 1000. p1.chips -> 1002, p2.chips -> 1001 (lub odwrotnie)

        assertThat(p1.getChips() + p2.getChips()).isEqualTo(2003);
    }

    @Test
    @DisplayName("Should handle folded players correctly (dead money)")
    void testFoldedPlayerMoney() {
        // given
        p1.bet(100);
        p2.fold();
        p2.bet(50);
        p3.bet(100);

        // when
        potManager.distributeBets(List.of(p1, p2, p3));

        // then
        // 250 (100+50+100)
        assertThat(potManager.getTotalPot()).isEqualTo(250);
        assertThat(potManager.getPots().getFirst().getEligiblePlayerIds())
                .containsExactlyInAnyOrder("1", "3");
    }

    @Test
    @DisplayName("Should award pot to eligible winner correctly")
    void testAwardPotSuccess() {
        // given
        // p1, p2, 200
        p1.bet(100);
        p2.bet(100);
        potManager.distributeBets(List.of(p1, p2)); //  potIndex=0

        int initialWinnerChips = p1.getChips(); // 900

        // when
        int awarded = potManager.awardPot(0, p1);

        // then
        assertAll(
                () -> assertThat(awarded).isEqualTo(200),
                () -> assertThat(p1.getChips()).isEqualTo(initialWinnerChips + awarded)
        );

    }

    @Test
    @DisplayName("Should return 0 and not award chips for invalid pot index")
    void testAwardPotInvalidIndex() {
        // given
        p1.bet(100);
        potManager.distributeBets(List.of(p1));
        int initialChips = p1.getChips();

        // when
        int awardedNegative = potManager.awardPot(-1, p1);
        int awardedTooBig = potManager.awardPot(999, p1);

        // then
        assertThat(awardedNegative).isZero();
        assertThat(awardedTooBig).isZero();
        assertThat(p1.getChips()).isEqualTo(initialChips);
    }

    @Test
    @DisplayName("Should not award pot if player is not eligible (e.g. Side Pot)")
    void testAwardPotNotEligible() {
        // given
        // p3 (Charlie) - All-in 100
        // p1 and p2 continue for Side Pot (index 1).
        p3.bet(100); // All-in
        p1.bet(300);
        p2.bet(300);
        potManager.distributeBets(List.of(p1, p2, p3));

        // 2 different pots
        assertThat(potManager.getPotCount()).isEqualTo(2);

        int p3InitialChips = p3.getChips(); // 0

        // when
        // Charlie (p3) -> side pot (index 1)
        int awarded = potManager.awardPot(1, p3);
        // then
        assertThat(awarded).isZero();
        assertThat(p3.getChips()).isEqualTo(p3InitialChips);


        int eligibleAward = potManager.awardPot(0, p3);
        assertThat(eligibleAward).isEqualTo(300);
        assertThat(p3.getChips()).isEqualTo(p3InitialChips +  eligibleAward);

    }
}
