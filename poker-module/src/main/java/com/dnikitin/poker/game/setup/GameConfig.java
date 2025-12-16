package com.dnikitin.poker.game.setup;

import lombok.Builder;

@Builder
public record GameConfig(
        int maxPlayers,
        int minPlayers,
        int startingChips,
        int ante,
        int maxDrawCount) {
}
