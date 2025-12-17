package com.dnikitin.poker.server;

import com.dnikitin.poker.common.model.events.GameEvent;
import com.dnikitin.poker.common.model.game.Card;
import com.dnikitin.poker.common.model.game.Rank;
import com.dnikitin.poker.common.model.game.Suit;
import com.dnikitin.poker.game.GameManager;
import com.dnikitin.poker.game.Table;
import com.dnikitin.poker.model.Player;
import com.dnikitin.poker.server.security.RateLimiter;
import com.dnikitin.poker.server.security.TimeoutManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientHandlerTest {

    @Mock
    private SocketChannel socketChannel;
    @Mock
    private RateLimiter rateLimiter;
    @Mock
    private TimeoutManager timeoutManager;
    @Mock
    private GameManager gameManager;
    @Mock
    private Table table;

    private MockedStatic<GameManager> mockedGameManagerStatic;
    private ClientHandler clientHandler;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        mockedGameManagerStatic = mockStatic(GameManager.class);
        mockedGameManagerStatic.when(GameManager::getInstance).thenReturn(gameManager);

        clientHandler = new ClientHandler(socketChannel, rateLimiter, timeoutManager);

        responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter, true);

        Field outField = ClientHandler.class.getDeclaredField("out");
        outField.setAccessible(true);
        outField.set(clientHandler, printWriter);
    }

    @AfterEach
    void tearDown() {
        mockedGameManagerStatic.close();
    }

    // --- Client -> SERVER ---

    @Test
    @DisplayName("Should handle HELLO command")
    void testHandleHello() throws Exception {
        invokeHandleCommand("HELLO VERSION=1.0");
        assertThat(responseWriter.toString()).contains("OK MESSAGE=Connected_to_poker_server");
    }

    @Test
    @DisplayName("Should handle CREATE command")
    void testHandleCreate() throws Exception {
        String newGameId = "game-123";
        when(gameManager.createGame()).thenReturn(newGameId);

        invokeHandleCommand("CREATE ANTE=10 BET=10");

        verify(gameManager).createGame();
        assertThat(responseWriter.toString()).contains("OK MESSAGE=GAME_ID=" + newGameId);
    }

    @Test
    @DisplayName("Should handle JOIN command successfully")
    void testHandleJoinSuccess() throws Exception {
        String gameId = "game-123";
        String playerName = "Alice";
        when(gameManager.findGame(gameId)).thenReturn(Optional.of(table));

        invokeHandleCommand("JOIN GAME=" + gameId + " NAME=" + playerName);

        verify(table).addPlayer(any(Player.class));
        verify(table).addObserver(clientHandler);
        assertThat(responseWriter.toString())
                .contains("WELCOME GAME=" + gameId)
                .contains("NAME=" + playerName);
    }

    @Test
    @DisplayName("Should return error when JOINing fails (Validation or No Game)")
    void testHandleJoinFail() throws Exception {
        // 1. Invalid Name
        invokeHandleCommand("JOIN GAME=valid-id NAME=Bob!");
        assertThat(responseWriter.toString()).contains("ERR CODE=INVALID_NAME");
        verify(gameManager, never()).findGame(any());

        // Reset buffer
        responseWriter.getBuffer().setLength(0);

        // 2. Invalid Game ID
        invokeHandleCommand("JOIN GAME=short NAME=Bob");
        assertThat(responseWriter.toString()).contains("ERR CODE=INVALID_GAME_ID");
        verify(gameManager, never()).findGame(any());

        responseWriter.getBuffer().setLength(0);

        // 3. Game Not Found
        when(gameManager.findGame("invalid-id")).thenReturn(Optional.empty());
        invokeHandleCommand("JOIN GAME=invalid-id NAME=Bob");
        assertThat(responseWriter.toString()).contains("ERR CODE=GAME_NOT_FOUND");
        verify(table, never()).addPlayer(any());
    }

    @Test
    @DisplayName("Should handle START command")
    void testHandleStart() throws Exception {
        Player player = joinGameHelper();
        String gameId = "game-123";

        // ProtocolParser wymaga formatu: GAME_ID PLAYER_ID ACTION [PARAMS]
        invokeHandleCommand(gameId + " " + player.getId() + " START");

        verify(table).startGame();
        assertThat(responseWriter.toString()).contains("OK MESSAGE=Game_started");
    }

    @Test
    @DisplayName("Should handle RAISE command")
    void testHandleRaise() throws Exception {
        Player player = joinGameHelper();
        String gameId = "game-123";

        invokeHandleCommand(gameId + " " + player.getId() + " RAISE AMOUNT=100");

        verify(table).playerRaise(any(Player.class), eq(100));
        assertThat(responseWriter.toString()).contains("OK");
    }

    @Test
    @DisplayName("Should handle FOLD command")
    void testHandleFold() throws Exception {
        Player player = joinGameHelper();
        String gameId = "game-123";

        invokeHandleCommand(gameId + " " + player.getId() + " FOLD");

        verify(table).playerFold(any(Player.class));
        assertThat(responseWriter.toString()).contains("OK");
    }

    @Test
    @DisplayName("Should handle CALL command")
    void testHandleCall() throws Exception {
        // given
        String gId = "game-1234"; //
        when(gameManager.findGame(gId)).thenReturn(Optional.of(table));

        // 1. JOIN
        invokeHandleCommand("JOIN GAME=" + gId + " NAME=Alice");
        responseWriter.getBuffer().setLength(0); // czyścimy bufor po welcome

        // 2. Get ID using reflection
        String pId = getPlayer().getId();

        // when
        invokeHandleCommand(gId + " " + pId + " CALL");

        // then
        verify(table).playerCall(any());
        assertThat(responseWriter.toString()).contains("OK");
    }

    @Test
    @DisplayName("Should handle CHECK command")
    void testHandleCheck() throws Exception {
        // given
        String gId = "game-1234"; // ZMIANA
        when(gameManager.findGame(gId)).thenReturn(Optional.of(table));

        invokeHandleCommand("JOIN GAME=" + gId + " NAME=Bob");
        responseWriter.getBuffer().setLength(0);

        String pId = getPlayer().getId();

        // when
        invokeHandleCommand(gId + " " + pId + " CHECK");

        // then
        verify(table).playerCheck(any());
        assertThat(responseWriter.toString()).contains("OK");
    }

    @Test
    @DisplayName("Should handle DRAW command with valid cards")
    void testHandleDraw() throws Exception {
        // given
        String gId = "game-1234"; // ZMIANA
        when(gameManager.findGame(gId)).thenReturn(Optional.of(table));

        invokeHandleCommand("JOIN GAME=" + gId + " NAME=Charlie");
        responseWriter.getBuffer().setLength(0);

        String pId = getPlayer().getId();

        // when
        invokeHandleCommand(gId + " " + pId + " DRAW CARDS=0,2,4");

        // then
        verify(table).playerExchangeCards(any(), eq(List.of(0, 2, 4)));
        assertThat(responseWriter.toString()).contains("OK");
    }

    // --- Server -> Client Events ---

    @Test
    @DisplayName("Should send PlayerJoined event to client")
    void testOnPlayerJoined() {
        GameEvent event = new GameEvent.PlayerJoined("p-1", "Bob", 1000);

        clientHandler.onGameEvent(event);

        assertThat(responseWriter.toString())
                .contains("LOBBY PLAYER=p-1 CHIPS=1000 NAME=Bob");
    }

    @Test
    @DisplayName("Should send CardsDealt event (HIDDEN for others)")
    void testOnCardsDealtOthers() throws Exception {
        joinGameHelper();
        responseWriter.getBuffer().setLength(0);

        List<Card> cards = List.of(new Card(Rank.ACE, Suit.SPADES));
        GameEvent event = new GameEvent.CardsDealt("other-player-id", cards);

        clientHandler.onGameEvent(event);

        assertThat(responseWriter.toString())
                .contains("DEAL PLAYER=other-player-id CARDS=HIDDEN");
    }

    @Test
    @DisplayName("Should send CardsDealt event (VISIBLE for me)")
    void testOnCardsDealtMe() throws Exception {
        Player myPlayer = joinGameHelper();
        responseWriter.getBuffer().setLength(0);

        List<Card> cards = List.of(new Card(Rank.ACE, Suit.SPADES));
        GameEvent event = new GameEvent.CardsDealt(myPlayer.getId(), cards);

        clientHandler.onGameEvent(event);

        assertThat(responseWriter.toString())
                .contains("DEAL PLAYER=" + myPlayer.getId() + " CARDS=A♠");
    }

    // --- HELPERS ---

    private Player joinGameHelper() throws Exception {
        String gameId = "game-123";
        // Upewniamy się, że mockowanie jest ustawione tylko jeśli jeszcze nie było (w przypadku wielokrotnego użycia w teście)
        // Ale tutaj każdy test jest izolowany, więc jest OK.
        when(gameManager.findGame(gameId)).thenReturn(Optional.of(table));

        invokeHandleCommand("JOIN GAME=" + gameId + " NAME=Alice");

        responseWriter.getBuffer().setLength(0);
        return getPlayer();
    }

    private Player getPlayer() throws Exception {
        Field playerField = ClientHandler.class.getDeclaredField("player");
        playerField.setAccessible(true);
        return (Player) playerField.get(clientHandler);
    }

    private void invokeHandleCommand(String line) throws Exception {
        clientHandler.handleCommand(line);
    }
}