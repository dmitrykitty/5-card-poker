package com.dnikitin.poker.model;

import com.dnikitin.poker.common.model.game.Card;
import com.dnikitin.poker.common.model.game.HandRank;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.dnikitin.poker.common.model.game.Rank.*;
import static com.dnikitin.poker.common.model.game.Suit.*;
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

    @Test
    void compareHighCardStrictly() {
        // A, J, 9, 7, 4
        HandResult hand1 = HandResult.builder()
                .handRank(HandRank.HIGH_CARD)
                .mainCards(List.of(new Card(ACE, SPADES)))
                .otherCards(List.of(
                        new Card(JACK, HEARTS), new Card(NINE, CLUBS),
                        new Card(SEVEN, DIAMONDS), new Card(FOUR, SPADES)
                ))
                .build();

        // A, J, 9, 7, 2
        HandResult hand2 = HandResult.builder()
                .handRank(HandRank.HIGH_CARD)
                .mainCards(List.of(new Card(ACE, CLUBS)))
                .otherCards(List.of(
                        new Card(JACK, DIAMONDS), new Card(NINE, SPADES),
                        new Card(SEVEN, CLUBS), new Card(TWO, HEARTS)
                ))
                .build();

        assertThat(hand1.compareTo(hand2)).isPositive();
    }

    @Test
    void compareFlushByCardsOrder() {
        // A, 10, 8, 4, 3
        HandResult flushAce = HandResult.builder()
                .handRank(HandRank.FLUSH)
                .mainCards(List.of(
                        new Card(ACE, HEARTS), new Card(TEN, HEARTS), new Card(EIGHT, HEARTS),
                        new Card(FOUR, HEARTS), new Card(THREE, HEARTS)
                ))
                .build();

        // K, Q, J, 10, 8
        HandResult flushKing = HandResult.builder()
                .handRank(HandRank.FLUSH)
                .mainCards(List.of(
                        new Card(KING, SPADES), new Card(QUEEN, SPADES), new Card(JACK, SPADES),
                        new Card(TEN, SPADES), new Card(EIGHT, SPADES)
                ))
                .build();

        assertThat(flushAce.compareTo(flushKing)).isPositive();
    }

    @Test
    void ensureImmutabilityOfLists() {
        List<Card> mutableList = new ArrayList<>();
        mutableList.add(new Card(ACE, SPADES));

        HandResult result = HandResult.builder()
                .handRank(HandRank.HIGH_CARD)
                .mainCards(mutableList)
                .build();

        mutableList.clear();

        assertThat(result.getMainCards().size()).isEqualTo(1);
    }
}
