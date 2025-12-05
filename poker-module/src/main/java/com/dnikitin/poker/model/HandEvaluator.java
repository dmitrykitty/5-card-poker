package com.dnikitin.poker.model;

import com.dnikitin.poker.common.model.Card;
import com.dnikitin.poker.common.model.HandRank;
import com.dnikitin.poker.common.model.Rank;
import com.dnikitin.poker.common.model.Suit;
import com.dnikitin.poker.exceptions.NoFiveCardsException;
import com.dnikitin.poker.exceptions.WrongRankException;
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

        //ROYAL_FLUSH(10), STRAIGHT_FLUSH(9), //FLUSH(6), STRAIGHT(5)
        if (flush || straight) {
            return getHandResultWhenAllCardsAreMain(flush, straight, cards);
        }

        Map<Rank, List<Card>> rankCards = cards.stream()
                .collect(Collectors.groupingBy(Card::rank));

        return switch (rankCards.size()) {
            case 2 -> getFourOfKingOrFullHouse(rankCards);     // FOUR_OF_A_KIND(4+1), FULL_HOUSE(3+2)
            case 3 -> getThreeOfAKindOrTwoPairs(rankCards);    // THREE_OF_A_KIND(3+1+1), TWO_PAIRS(2+2+1)
            case 4 -> getOnePair(rankCards);                   // ONE_PAIR(2+1+1+1)
            case 5 -> getHighCard(cards);                      // HIGH_CARD(1+1+1+1+1)
            default -> throw new WrongRankException("Impossible state for 5 cards");

        };
    }


    private static HandResult getHandResultWhenAllCardsAreMain(boolean flush, boolean straight, ArrayList<Card> cards) {
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

    private static HandResult getFourOfKingOrFullHouse(Map<Rank, List<Card>> rankCards) {
        List<List<Card>> groups = getGroupsSortedBySize(rankCards);

        List<Card> mainCards = new ArrayList<>(groups.getFirst());  // 3 or 4
        List<Card> secondaryGroup = groups.get(1); // 2 or 1

        boolean isFullHouse = mainCards.size() == 3;
        if (isFullHouse) {
            mainCards.addAll(secondaryGroup);
        }

        return HandResult.builder()
                .handRank(isFullHouse ? HandRank.FULL_HOUSE : HandRank.FOUR_OF_A_KIND)
                .mainCards(mainCards)
                .otherCards(isFullHouse ? List.of() : secondaryGroup)
                .build();
    }

    private static HandResult getThreeOfAKindOrTwoPairs(Map<Rank, List<Card>> rankCards) {
        List<List<Card>> groups = getGroupsSortedBySize(rankCards);

        List<Card> mainCards = groups.getFirst();
        List<Card> otherCards = groups.getLast();

        boolean isThreeOfKing = mainCards.size() == 3;
        if (isThreeOfKing) {
            otherCards.addAll(groups.get(1));
            otherCards.sort(Collections.reverseOrder());
        } else {
            mainCards.addAll(groups.get(1));
            mainCards.sort(Collections.reverseOrder());
        }

        return HandResult.builder()
                .handRank(isThreeOfKing ? HandRank.THREE_OF_A_KIND : HandRank.TWO_PAIRS)
                .mainCards(mainCards)
                .otherCards(otherCards)
                .build();
    }

    private static HandResult getOnePair(Map<Rank, List<Card>> rankCards) {
        List<List<Card>> groups = getGroupsSortedBySize(rankCards);

        List<Card> pair = groups.getFirst();

        List<Card> kickers = groups.stream()
                .skip(1)
                .flatMap(List::stream)//to convert all lists into one stream
                .sorted(Collections.reverseOrder())
                .toList();

        return HandResult.builder()
                .handRank(HandRank.ONE_PAIR)
                .mainCards(pair)
                .otherCards(kickers)
                .build();
    }

    private static List<List<Card>> getGroupsSortedBySize(Map<Rank, List<Card>> rankCards) {
        return rankCards.values().stream()
                .sorted(Comparator.comparingInt((List<Card> list) -> list.size()).reversed())
                .toList();
    }

    private static boolean isFlush(List<Card> cards) {
        Suit suit = cards.getFirst().suit();
        for (int i = 1; i < cards.size(); i++) {
            if (cards.get(i).suit() != suit)
                return false;
        }
        return true;
    }

    private static boolean isStraight(List<Card> cards) {
        for (int i = 1; i < cards.size(); i++) {
            if (cards.get(i - 1).rank().getPower() - cards.get(i).rank().getPower() != 1) {
                return false;
            }
        }
        return true;
    }

    private static HandResult getHighCard(List<Card> cards) {
        return HandResult.builder()
                .handRank(HandRank.HIGH_CARD)
                .mainCards(List.of(cards.getFirst()))
                .otherCards(cards.subList(1, 5))
                .build();
    }
}
