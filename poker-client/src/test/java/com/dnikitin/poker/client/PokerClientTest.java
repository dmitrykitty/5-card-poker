package com.dnikitin.poker.client;

import com.dnikitin.poker.common.protocol.serverclient.ServerMessageParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PokerClientTest {

    @Mock
    private ConsoleUI ui;

    @Mock
    private PrintWriter serverOut;

    @Spy
    private ClientGameState gameState = new ClientGameState();
    private final ServerMessageParser parser = new ServerMessageParser();

    private PokerClient client;

    @BeforeEach
    void setUp() {
        client = new PokerClient("localhost", 9998, gameState, ui, parser);
        client.setOut(serverOut);
    }


    @Test
    void shouldHandleWelcomeMessage() {
        // Given
        String msg = "WELCOME GAME=g1 PLAYER=p1 NAME=Dmitry";

        // When
        client.handleServerMessage(msg);

        // Then
        assertThat(gameState.getGameId()).isEqualTo("g1");
        assertThat(gameState.getPlayerId()).isEqualTo("p1");
        assertThat(gameState.getMyName()).isEqualTo("Dmitry");

        verify(ui).printMessage(contains("Joined game successfully as Dmitry"));
    }

    @Test
    void shouldHandleTurnMessage_WhenItIsMyTurn() {
        // Given - logged
        gameState.setConnectionInfo("g1", "my-id");
        String msg = "TURN PLAYER=my-id CALL=50 MINRAISE=100";

        client.handleServerMessage(msg);

        // Then
        assertThat(gameState.getAmountToCall()).isEqualTo(50);
        assertThat(gameState.getLastMessage()).contains("YOUR TURN");

        verify(ui).printDashboard(gameState);
        verify(ui).printPrompt();
    }

    @Test
    void shouldHandleTurnMessage_WhenOpponentTurn() {
        // Given
        gameState.setConnectionInfo("g1", "my-id");
        gameState.updatePlayerInfo("opp-id", "Opponent", 1000);
        String msg = "TURN PLAYER=opp-id";

        // When
        client.handleServerMessage(msg);

        // Then
        verify(ui).printMessage(contains("Waiting for Opponent"));
        verify(ui, never()).printDashboard(any());
    }

    @Test
    void shouldHandleDealCards() {
        // Given
        gameState.setConnectionInfo("g1", "my-id");
        String msg = "DEAL PLAYER=my-id CARDS=Ah,Kh,Qh,Jh,10h";

        // When
        client.handleServerMessage(msg);

        // Then
        assertThat(gameState.getMyHand())
                .hasSize(5)
                .containsExactly("Ah", "Kh", "Qh", "Jh", "10h");

        verify(ui).printDashboard(gameState);
    }

    @Test
    void shouldHandleWinner() {
        // Given
        gameState.updatePlayerInfo("winner-id", "Champion", 100);
        String msg = "WINNER PLAYER=winner-id RANK=Flush POT=500 CARDS=Ah,Kh,Qh,Jh,10h";

        // When
        client.handleServerMessage(msg);

        // Then
        assertThat(gameState.getPlayerChips().get("winner-id")).isEqualTo(600);
        verify(ui).printMessage(contains("WINNER: Champion"));
        verify(ui).printMessage(contains("Flush"));
    }

    // --- processCommand ---

    @Test
    void shouldSendCreateCommand() {
        // When
        client.processCommand("create");

        // Then
        verify(serverOut).println("CREATE ANTE=10 BET=10 LIMIT=FIXED");
    }

    @Test
    void shouldSendJoinCommand() {
        // When
        client.processCommand("join 123 Alice");

        // Then
        verify(serverOut).println("JOIN GAME=123 NAME=Alice");
    }

    @Test
    void shouldBlockGameCommandsIfNotJoined() {
        // Given

        // When
        client.processCommand("call");

        // Then
        verify(serverOut, never()).println(anyString());
        verify(ui).printError(contains("must join a game first"));
    }

    @Test
    void shouldSendFoldCommand() {
        // Given
        gameState.setConnectionInfo("g1", "p1");

        // When
        client.processCommand("fold");

        // Then
        verify(serverOut).println("g1 p1 FOLD");
    }

    @Test
    void shouldSendRaiseCommand() {
        // Given
        gameState.setConnectionInfo("g1", "p1");

        // When
        client.processCommand("raise 200");

        // Then
        verify(serverOut).println("g1 p1 RAISE AMOUNT=200");
    }

    @Test
    void shouldHandleRaiseErrorUsage() {
        // Given
        gameState.setConnectionInfo("g1", "p1");

        // When
        client.processCommand("raise"); // Brak kwoty

        // Then
        verify(serverOut, never()).println(contains("RAISE"));
        verify(ui).printError(contains("Usage: raise"));
    }

    @Test
    void shouldSendDrawCommand() {
        // Given
        gameState.setConnectionInfo("g1", "p1");

        // When
        client.processCommand("draw 0, 1, 4");

        // Then
        verify(serverOut).println("g1 p1 DRAW CARDS=0,1,4");
    }
}