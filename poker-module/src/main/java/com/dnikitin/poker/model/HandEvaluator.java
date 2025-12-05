package com.dnikitin.poker.model;

import com.dnikitin.poker.common.model.Card;
import com.dnikitin.poker.common.model.HandRank;
import com.dnikitin.poker.common.model.Rank;
import com.dnikitin.poker.common.model.Suit;
import com.dnikitin.poker.exceptions.NoFiveCardsException;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class HandEvaluator {
    public static HandResult evaluate(List<Card> cardsFromPlayer) {
        ArrayList<Card> cards = new ArrayList<>(cardsFromPlayer);

        if (cards.size() != 5) {
            throw new NoFiveCardsException("Required 5 cards in the en of game.");
        }

        cards.sort(Collections.reverseOrder());
        boolean flush = isFlush(cards);
        boolean straight = isStraight(cards);
        Map<Rank, List<Card>> rankCounts = groupCardsByRank(cards);

        //ROYAL_FLUSH(10), STRAIGHT_FLUSH(9), //FLUSH(6), STRAIGHT(5)
        if (flush || straight) {
            return getHandResultWhenAllCardsAreMain(flush, straight, cards);
        }

        //FOUR_OF_A_KIND(8), FULL_HOUSE(7)
        if (rankCounts.size() == 2) {

        }
        //THREE_OF_A_KIND(4), TWO_PAIRS(3)
        if (rankCounts.size() == 3) {

        }


    }

    private HandResult getHandResultWhenAllCardsAreMain(boolean flush, boolean straight, ArrayList<Card> cards) {
        HandRank handRank;
        if (flush && straight) {
            //ROYAL_FLUSH(10), STRAIGHT_FLUSH(9)
            handRank = cards.getFirst().rank().equals(Rank.ACE)
                    ? HandRank.ROYAL_FLUSH
                    : HandRank.STRAIGHT_FLUSH;
        } else {
            //FLUSH(6), STRAIGHT(5)
            handRank = flush ? HandRank.FLUSH : HandRank.STRAIGHT;
        }

        return HandResult.builder()
                .handRank(handRank)
                .mainCards(cards)
                .otherCards(List.of())
                .build();
    }

//    private Map<Rank, Integer> groupCountsByRank(List<Card> cards) {
//        Map<Rank, Integer> rankMap = new HashMap<>();
//        for (Card card : cards) {
//            Integer count = rankMap.getOrDefault(card.rank(), 0);
//            rankMap.put(card.rank(), count + 1);
//        }
//        return rankMap;
//    }

    private Map<Rank, List<Card>> groupCardsByRank(List<Card> cards) {
        return cards.stream()
                .collect(Collectors.groupingBy(Card::rank));
    }

    private boolean isFlush(List<Card> cards) {
        Suit suit = cards.getFirst().suit();
        for (int i = 1; i < cards.size(); i++) {
            if (cards.get(i).suit() != suit)
                return false;
        }
        return true;
    }

    private boolean isStraight(List<Card> cards) {
        for (int i = 1; i < cards.size(); i++) {
            if (cards.get(i - 1).rank().getPower() - cards.get(i).rank().getPower() != 1) {
                return false;
            }
        }
        return true;
    }
}
