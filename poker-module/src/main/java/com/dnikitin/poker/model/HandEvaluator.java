package com.dnikitin.poker.model;

import com.dnikitin.poker.common.model.Card;

import java.util.List;

public interface HandEvaluator {

    HandResult evaluate(List<Card> cards);
}
