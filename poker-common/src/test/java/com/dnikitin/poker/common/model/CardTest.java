package com.dnikitin.poker.common.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;


public class CardTest {

    @Test
    void isComparingCardsCorrectly() {
        Card ace1 = new Card(Rank.ACE, Suit.DIAMONDS);
        Card ace2 = new Card(Rank.ACE, Suit.HEARTS);
        Card ten = new Card(Rank.TEN, Suit.CLUBS);

        assertAll(
                () -> assertThat(ace1.compareTo(ace2)).isNotZero(),
                () -> assertThat(ace1.compareTo(ten)).isPositive()
        );
    }

    @Test
    void isAddingToHashSetWorksCorrectly(){
        Card ace = new Card(Rank.ACE, Suit.DIAMONDS);
        Card theSameAce = new Card(Rank.ACE, Suit.DIAMONDS);

        Set<Card> cards = new HashSet<>();
        cards.add(ace);
        cards.add(theSameAce);

        assertThat(cards.size()).isEqualTo(1);
    }
}
