package com.dnikitin.poker.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientGameStateTest {

    private ClientGameState gameState;

    @BeforeEach
    void setUp() {
        gameState = new ClientGameState();
    }

    @Test
    void shouldUpdatePlayerInfoCorrectly() {
        // Given
        String playerId = "p1";
        String playerName = "Dmitry";
        int chips = 1000;

        // When
        gameState.updatePlayerInfo(playerId, playerName, chips);

        // Then
        assertEquals("Dmitry", gameState.getPlayerName(playerId));
        assertTrue(gameState.getPlayerNames().containsValue("Dmitry"));
        assertEquals(1000, gameState.getPlayerChips().get(playerId));
    }

    @Test
    void shouldHandlePartialPlayerUpdate() {
        // Given
        gameState.updatePlayerInfo("p1", "OldName", 500);

        // When - update only chips
        gameState.updatePlayerInfo("p1", null, 800);
        // When - update only name
        gameState.updatePlayerInfo("p1", "NewName", -1);

        // Then
        assertEquals(800, gameState.getPlayerChips().get("p1"));
        assertEquals("NewName", gameState.getPlayerName("p1"));
    }

    @Test
    void shouldIdentifyMyself() {
        // Given
        gameState.setConnectionInfo("game1", "my-id");
        gameState.updatePlayerInfo("my-id", "Me", 100);

        // When
        String name = gameState.getPlayerName("my-id");
        String otherName = gameState.getPlayerName("other-id");

        // Then
        assertEquals("You", name);
        assertEquals("other-id", otherName); // Nieznany ID zwraca ID
    }

    @Test
    void shouldManageChips() {
        // Given
        String pid = "p1";
        gameState.updatePlayerInfo(pid, "Player", 1000);

        // When
        gameState.deductChips(pid, 200);
        gameState.addChips(pid, 500);

        // Then
        int expected = 1000 - 200 + 500;
        assertEquals(expected, gameState.getPlayerChips().get(pid));
    }

    @Test
    void shouldUpdateRoundInfo() {
        // When
        gameState.updateRoundInfo(500, 100);

        // Then
        assertEquals(500, gameState.getCurrentPot());
        assertEquals(100, gameState.getCurrentBet());
    }

    @Test
    void shouldParseHandCorrectly() {
        // When
        gameState.updateMyHand("Ah,Kh,Qh,Jh,10h");

        // Then
        List<String> hand = gameState.getMyHand();
        assertEquals(5, hand.size());
        assertEquals("Ah", hand.get(0));
        assertEquals("10h", hand.get(4));
    }

    @Test
    void shouldClearHandOnHiddenOrNone() {
        // Given
        gameState.updateMyHand("Ah,Kh");

        // When
        gameState.updateMyHand("HIDDEN");

        // Then
        assertTrue(gameState.getMyHand().isEmpty());

        // When
        gameState.updateMyHand("Ah,Kh");
        gameState.updateMyHand("NONE");

        // Then
        assertTrue(gameState.getMyHand().isEmpty());
    }

    @Test
    void shouldResetState() {
        // Given
        gameState.setConnectionInfo("g1", "p1");
        gameState.updatePlayerInfo("p1", "Name", 100);
        gameState.updateRoundInfo(500, 50);
        gameState.setLastMessage("Winner!");

        // When
        gameState.reset();

        // Then
        assertNull(gameState.getGameId());
        assertNull(gameState.getPlayerId());
        assertTrue(gameState.getPlayerNames().isEmpty());
        assertTrue(gameState.getPlayerChips().isEmpty());
        assertEquals(0, gameState.getCurrentPot());
        assertEquals(0, gameState.getCurrentBet());
        assertEquals("", gameState.getLastMessage());
        assertEquals("WAITING", gameState.getCurrentPhase());
    }
}
