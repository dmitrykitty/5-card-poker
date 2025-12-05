package com.dnikitin.poker.model;


import com.dnikitin.poker.common.model.Card;
import com.dnikitin.poker.common.model.HandRank;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
public class HandResult implements Comparable<HandResult> {
    private final HandRank handRank;
    private final List<Card> mainCards;
    private final List<Card> otherCards;
    private final List<Card> allCards;

    @Builder
    public HandResult(HandRank handRank, List<Card> mainCards, List<Card> kickers, List<Card> allCards) {
        this.handRank = handRank;
        this.mainCards = mainCards != null ? List.copyOf(mainCards) : List.of();
        this.otherCards = kickers != null ? List.copyOf(kickers) : List.of();
        this.allCards = allCards != null ? List.copyOf(allCards) : List.of();
    }

    @Override
    public int compareTo(HandResult other) {
        int handRankComparison = Integer.compare(handRank.getStrength(), other.getHandRank().getStrength());

        if (handRankComparison != 0) {
            return handRankComparison;
        }
        int mainCardsComparison = compareCardLists(mainCards, other.getMainCards());

        if (mainCardsComparison != 0) {
            return mainCardsComparison;
        }
        return compareCardLists(otherCards, other.otherCards);
    }

    private int compareCardLists(List<Card> mainCards, List<Card> otherCards) {
        int size = Math.min(mainCards.size(), otherCards.size());
        int result = 0;

        for (int i = 0; i < size; i++) {
            result = mainCards.get(i).compareTo(otherCards.get(i));
            if (result != 0) {
                return result;
            }
        }
        return result;
    }
}
