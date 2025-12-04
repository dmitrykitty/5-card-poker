package com.dnikitin.poker.model;

import com.dnikitin.poker.common.model.Card;
import com.dnikitin.poker.common.model.Rank;
import com.dnikitin.poker.common.model.Suit;

import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.random.RandomGenerator;

public class Deck {

    private final LinkedList<Card> cards;
    private final RandomGenerator random; //use of Secure Random

    private Deck(List<Card> cards, RandomGenerator random) {
        this.cards = new LinkedList<>(cards);
        this.random = random;
    }

    public static Deck createDeck() {
        List<Card> cards = new LinkedList<>();

        for (Suit suit: Suit.values()) {
            for (Rank rank:Rank.values()){
                Card card = new Card(rank, suit);
                cards.add(card);
            }
        }
        return new Deck(cards, new SecureRandom());
    }
}
