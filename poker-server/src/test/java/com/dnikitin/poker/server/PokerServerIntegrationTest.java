package com.dnikitin.poker.server;

import com.dnikitin.poker.client.PokerClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class RealGameIntegrationTest {

    private static final int PORT = 9999;
    private PokerServer server;
    private CompletableFuture<Void> serverThread;

    @BeforeEach
    void setUp() {
        server = new PokerServer(PORT);
        serverThread = CompletableFuture.runAsync(server::start);
        await().atMost(2, TimeUnit.SECONDS).until(server::isRunning);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
        if (serverThread != null) {
            serverThread.cancel(true);
        }
    }

    @Test
    @Timeout(15) // Zwiększyłem lekko timeout dla bezpieczeństwa
    void testFullGameScenario() throws IOException {
        // --- GRACZ 1: ALICE ---
        PipedOutputStream writeToAlice = new PipedOutputStream();
        PipedInputStream aliceIn = new PipedInputStream(writeToAlice);
        ByteArrayOutputStream aliceOutput = new ByteArrayOutputStream();

        CompletableFuture.runAsync(() -> {
            new PokerClient("localhost", PORT, aliceIn, new PrintStream(aliceOutput)).start();
        });

        // --- GRACZ 2: BOB ---
        PipedOutputStream writeToBob = new PipedOutputStream();
        PipedInputStream bobIn = new PipedInputStream(writeToBob);
        ByteArrayOutputStream bobOutput = new ByteArrayOutputStream();

        CompletableFuture.runAsync(() -> {
            new PokerClient("localhost", PORT, bobIn, new PrintStream(bobOutput)).start();
        });

        // 1. Sprawdzamy startup
        await().untilAsserted(() -> assertThat(aliceOutput.toString()).contains("STARTUP COMMANDS"));

        // 2. Alice tworzy grę
        sendCommand(writeToAlice, "CREATE");

        // Czekamy aż pojawi się ID gry.
        // UWAGA: Klient zamienia "_" na spację, więc szukamy "GAME ID="
        await().untilAsserted(() -> assertThat(aliceOutput.toString()).contains("GAME ID="));

        String gameId = extractGameId(aliceOutput.toString());

        // Dodatkowa asercja debugująca - jeśli to wybuchnie, znaczy że extractGameId jest źle napisane
        assertThat(gameId).isNotNull().withFailMessage("extractGameId zwrócił null, mimo że log zawiera 'GAME ID='");

        System.out.println("TEST INFO: Game created with ID: " + gameId);

        // 3. Alice dołącza
        sendCommand(writeToAlice, "JOIN " + gameId + " Alice");
        await().untilAsserted(() -> assertThat(aliceOutput.toString()).contains("Joined game successfully"));

        // 4. Bob dołącza
        sendCommand(writeToBob, "JOIN " + gameId + " Bob");
        await().untilAsserted(() -> assertThat(bobOutput.toString()).contains("Joined game successfully"));

        // Alice powinna widzieć Boba w Lobby
        await().untilAsserted(() -> assertThat(aliceOutput.toString()).contains("[LOBBY] Bob"));

        // 5. Start gry
        sendCommand(writeToAlice, "START");

        // 6. Weryfikacja
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Szukamy fragmentów dashboardu
            assertThat(aliceOutput.toString()).contains("HAND:");
            assertThat(bobOutput.toString()).contains("HAND:");
            assertThat(aliceOutput.toString()).contains("PHASE: ANTE");
        });

        sendCommand(writeToAlice, "QUIT");
        sendCommand(writeToBob, "QUIT");
    }

    private void sendCommand(PipedOutputStream pipe, String cmd) throws IOException {
        pipe.write((cmd + "\n").getBytes());
        pipe.flush();
    }

    private String extractGameId(String log) {
        // --- KLUCZOWA POPRAWKA ---
        // Twój klient wypisuje "GAME ID=..." (ze spacją), a nie "GAME_ID=..." (z podkreślnikiem)
        // Musimy szukać dokładnego ciągu znaków.

        String marker = "GAME ID=";
        int idx = log.lastIndexOf(marker);

        if (idx == -1) {
            // Fallback: Spróbujmy znaleźć z podkreślnikiem, na wypadek gdyby coś się zmieniło w kodzie
            marker = "GAME_ID=";
            idx = log.lastIndexOf(marker);
        }

        if (idx == -1) return null;

        String temp = log.substring(idx + marker.length());
        // Bierzemy wszystko do pierwszego białego znaku (spacji, nowej linii itp.)
        return temp.split("\\s+")[0].trim();
    }
}