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

    //mocking
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
    private StringWriter responseWriter; // response from server - HERE

    @BeforeEach
    void setUp() throws Exception {
        // mocking singleton behavior
        mockedGameManagerStatic = mockStatic(GameManager.class);
        mockedGameManagerStatic.when(GameManager::getInstance).thenReturn(gameManager);

        clientHandler = new ClientHandler(socketChannel, rateLimiter, timeoutManager);

        // inject PrintWriter via reflection to capture what the handler sends to the client
        // (Because the 'out' field is private and normally initialized in run())
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

    // Commands from client to SERVER

    @Test
    @DisplayName("Should handle HELLO command")
    void testHandleHello() throws Exception {
        // given
        String command = "HELLO VERSION=1.0";

        // when
        invokeHandleCommand(command);

        // then
        assertThat(responseWriter.toString()).contains("OK MESSAGE=Connected_to_poker_server");
    }

    @Test
    @DisplayName("Should handle CREATE command")
    void testHandleCreate() throws Exception {
        // given
        String newGameId = "game-123";
        when(gameManager.createGame()).thenReturn(newGameId);

        // when
        invokeHandleCommand("CREATE ANTE=10 BET=10");

        // then
        verify(gameManager).createGame();
        assertThat(responseWriter.toString()).contains("OK MESSAGE=GAME_ID=" + newGameId);
    }

    @Test
    @DisplayName("Should handle JOIN command successfully")
    void testHandleJoinSuccess() throws Exception {
        // given
        String gameId = "game-123";
        String playerName = "Alice";

        when(gameManager.findGame(gameId)).thenReturn(Optional.of(table));

        // when
        invokeHandleCommand("JOIN GAME=" + gameId + " NAME=" + playerName);

        // then
        // Weryfikujemy, że dodano gracza do stołu
        verify(table).addPlayer(any(Player.class));
        verify(table).addObserver(clientHandler);

        // Sprawdzamy odpowiedź WELCOME
        assertThat(responseWriter.toString()).contains("WELCOME GAME=" + gameId);
    }

    @Test
    @DisplayName("Should return error when JOINing non-existent game")
    void testHandleJoinFail() throws Exception {
        // given
        when(gameManager.findGame("invalid-id")).thenReturn(Optional.empty());

        // when
        invokeHandleCommand("JOIN GAME=invalid-id NAME=Bob");

        // then
        verify(table, never()).addPlayer(any());
        assertThat(responseWriter.toString()).contains("ERR CODE=GAME_NOT_FOUND");
    }

    @Test
    @DisplayName("Should handle START command")
    void testHandleStart() throws Exception {
        // given - musimy najpierw dołączyć do gry, aby ustawić pole 'table' w handlerze
        joinGameHelper();

        // when
        invokeHandleCommand("game-id player-id START");

        // then
        verify(table).startGame();
        assertThat(responseWriter.toString()).contains("OK MESSAGE=Game_started");
    }

    @Test
    @DisplayName("Should handle BET/RAISE command")
    void testHandleRaise() throws Exception {
        // given
        joinGameHelper();

        // when
        invokeHandleCommand("game-id player-id RAISE AMOUNT=100");

        // then
        verify(table).playerRaise(any(Player.class), eq(100));
        assertThat(responseWriter.toString()).contains("OK");
    }

    @Test
    @DisplayName("Should handle FOLD command")
    void testHandleFold() throws Exception {
        // given
        joinGameHelper();

        // when
        invokeHandleCommand("game-id player-id FOLD");

        // then
        verify(table).playerFold(any(Player.class));
        assertThat(responseWriter.toString()).contains("OK");
    }

    // --- TESTY OBSERWERA (Serwer -> Klient) ---

    @Test
    @DisplayName("Should send PlayerJoined event to client")
    void testOnPlayerJoined() {
        // given
        GameEvent event = new GameEvent.PlayerJoined("p-1", "Bob", 1000);

        // when
        clientHandler.onGameEvent(event);

        // then
        assertThat(responseWriter.toString())
                .contains("LOBBY PLAYER=p-1 CHIPS=1000 NAME=Bob");
    }

    @Test
    @DisplayName("Should send CardsDealt event (HIDDEN for others)")
    void testOnCardsDealtOthers() throws Exception {
        // given
        // Ustawiamy gracza w handlerze (poprzez JOIN)
        joinGameHelper();
        // Gracz w handlerze to "Alice" (stworzona w joinGameHelper)

        // Zdarzenie dotyczy INNEGO gracza
        List<Card> cards = List.of(new Card(Rank.ACE, Suit.SPADES));
        GameEvent event = new GameEvent.CardsDealt("other-player-id", cards);

        // when
        clientHandler.onGameEvent(event);

        // then
        // Powinniśmy dostać HIDDEN, bo to nie nasze karty
        assertThat(responseWriter.toString())
                .contains("DEAL PLAYER=other-player-id CARDS=HIDDEN");
    }

    @Test
    @DisplayName("Should send CardsDealt event (VISIBLE for me)")
    void testOnCardsDealtMe() throws Exception {
        // given
        Player myPlayer = joinGameHelper();

        List<Card> cards = List.of(new Card(Rank.ACE, Suit.SPADES));
        GameEvent event = new GameEvent.CardsDealt(myPlayer.getId(), cards);

        // when
        clientHandler.onGameEvent(event);

        // then
        // Powinniśmy widzieć karty
        assertThat(responseWriter.toString())
                .contains("DEAL PLAYER=" + myPlayer.getId() + " CARDS=A♠");
    }

    // --- HELPERY ---

    /**
     * Symuluje dołączenie do gry, aby ustawić wewnętrzne pola 'table' i 'player' w ClientHandler.
     */
    private Player joinGameHelper() throws Exception {
        String gameId = "game-123";
        when(gameManager.findGame(gameId)).thenReturn(Optional.of(table));

        // Wywołujemy prawdziwą logikę JOIN
        invokeHandleCommand("JOIN GAME=" + gameId + " NAME=Alice");

        // Resetujemy bufor odpowiedzi, żeby testy właściwe miały czysto
        responseWriter.getBuffer().setLength(0);

        // Pobieramy gracza, który został utworzony wewnątrz handlera (przez refleksję lub getter package-private)
        Field playerField = ClientHandler.class.getDeclaredField("player");
        playerField.setAccessible(true);
        return (Player) playerField.get(clientHandler);
    }

    /**
     * Wywołuje prywatną/package-private metodę handleCommand przez refleksję.
     * Jeśli zmieniłeś widoczność metody na package-private, możesz to wywołać bezpośrednio:
     * clientHandler.handleCommand(line);
     */
    private void invokeHandleCommand(String line) throws Exception {
        clientHandler.handleCommand(line);
    }
}