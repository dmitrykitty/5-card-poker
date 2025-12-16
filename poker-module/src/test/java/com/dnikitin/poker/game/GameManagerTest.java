package com.dnikitin.poker.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameManagerTest {

    @Test
    @DisplayName("Should return the same instance (Singleton pattern)")
    void testSingleton() {
        GameManager instance1 = GameManager.getInstance();
        GameManager instance2 = GameManager.getInstance();

        assertThat(instance1).isSameAs(instance2);
    }

    @Test
    @DisplayName("Should create a new game and retrieve it by ID")
    void testCreateAndFindGame() {
        // given
        GameManager manager = GameManager.getInstance();

        // when
        String gameId = manager.createGame();

        // then
        assertThat(gameId).isNotNull().isNotBlank();
        assertThat(manager.findGame(gameId)).isPresent();
        assertThat(manager.findGame(gameId).get().getId()).isEqualTo(gameId);
    }

    @Test
    @DisplayName("Should create unique games on subsequent calls")
    void testCreateMultipleGames() {
        // given
        GameManager manager = GameManager.getInstance();

        // when
        String id1 = manager.createGame();
        String id2 = manager.createGame();

        // then
        assertThat(id1).isNotEqualTo(id2);
        assertThat(manager.findGame(id1)).isPresent();
        assertThat(manager.findGame(id2)).isPresent();
    }

    @Test
    @DisplayName("Should return empty for non-existent game ID")
    void testFindNonExistentGame() {
        // given
        GameManager manager = GameManager.getInstance();

        // when
        var result = manager.findGame("some-fake-id-123-xyz");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should remove game correctly")
    void testRemoveGame() {
        // given
        GameManager manager = GameManager.getInstance();
        String gameId = manager.createGame();

        assertThat(manager.findGame(gameId)).isPresent();

        // when
        manager.removeGame(gameId);

        // then
        assertThat(manager.findGame(gameId)).isEmpty();
    }
}
