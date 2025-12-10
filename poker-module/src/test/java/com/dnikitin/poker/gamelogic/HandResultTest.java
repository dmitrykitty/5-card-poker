package com.dnikitin.poker.gamelogic;

import com.dnikitin.poker.common.model.Card;
import com.dnikitin.poker.common.model.HandRank;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.dnikitin.poker.common.model.Rank.*;
import static com.dnikitin.poker.common.model.Suit.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class HandResultTest {
    @Test
    void compareDifferentRanks() {
        HandResult flush = HandResult.builder()
                .handRank(HandRank.FLUSH)
                .mainCards(List.of(new Card(ACE, HEARTS))) // simlify, because HandRank in more important
                .build();

        HandResult straight = HandResult.builder()
                .handRank(HandRank.STRAIGHT)
                .mainCards(List.of(new Card(ACE, SPADES)))
                .build();

        assertThat(flush.compareTo(straight)).isPositive();
        assertThat(straight.compareTo(flush)).isNegative();
    }

    @Test
    void compareFullHousesByTrio() {
        HandResult kingsFull = HandResult.builder()
                .handRank(HandRank.FULL_HOUSE)
                .mainCards(List.of(
                        new Card(KING, SPADES), new Card(KING, HEARTS), new Card(KING, CLUBS),
                        new Card(TWO, DIAMONDS), new Card(TWO, CLUBS)
                ))
                .build();

        HandResult queensFull = HandResult.builder()
                .handRank(HandRank.FULL_HOUSE)
                .mainCards(List.of(
                        new Card(QUEEN, SPADES), new Card(QUEEN, HEARTS), new Card(QUEEN, CLUBS),
                        new Card(ACE, DIAMONDS), new Card(ACE, CLUBS)
                ))
                .build();
        assertThat(kingsFull.compareTo(queensFull)).isPositive();
    }

    @Test
    void comparePairsByOtherCards() {
        HandResult pair10KickerA = HandResult.builder()
                .handRank(HandRank.ONE_PAIR)
                .mainCards(List.of(new Card(TEN, SPADES), new Card(TEN, HEARTS)))
                .otherCards(List.of(new Card(ACE, CLUBS), new Card(FIVE, DIAMONDS), new Card(TWO, SPADES)))
                .build();

        HandResult pair10KickerK = HandResult.builder()
                .handRank(HandRank.ONE_PAIR)
                .mainCards(List.of(new Card(TEN, CLUBS), new Card(TEN, DIAMONDS)))
                .otherCards(List.of(new Card(KING, CLUBS), new Card(FIVE, SPADES), new Card(TWO, HEARTS)))
                .build();

        assertThat(pair10KickerA.compareTo(pair10KickerK)).isPositive();
    }

    @Test
    void compareEqualHands() {
        HandResult pairA = HandResult.builder()
                .handRank(HandRank.ONE_PAIR)
                .mainCards(List.of(new Card(TEN, SPADES), new Card(TEN, HEARTS)))
                .otherCards(List.of(new Card(ACE, CLUBS), new Card(FIVE, DIAMONDS), new Card(TWO, SPADES)))
                .build();

        HandResult pairB = HandResult.builder()
                .handRank(HandRank.ONE_PAIR)
                .mainCards(List.of(new Card(TEN, SPADES), new Card(TEN, HEARTS)))
                .otherCards(List.of(new Card(ACE, CLUBS), new Card(FIVE, DIAMONDS), new Card(TWO, SPADES)))
                .build();

        assertThat(pairA.compareTo(pairB)).isZero();
    }
}
