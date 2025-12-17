package com.dnikitin.poker.client;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

public class ConsoleUI {
    private final Scanner scanner;
    private final PrintStream out;

    private static final String BORDER_HOR = "═";
    private static final String BORDER_VER = "║";
    private static final String CORNER_TL = "╔";
    private static final String CORNER_TR = "╗";
    private static final String CORNER_BL = "╚";
    private static final String CORNER_BR = "╝";
    private static final String SEP_L = "╠";
    private static final String SEP_R = "╣";

    private static final int WIDTH = 50;

    //Test constructor
    public ConsoleUI(InputStream in, PrintStream out) {
        this.scanner = new Scanner(in);
        this.out = out;
    }

    //default
    public ConsoleUI() {
        this(System.in, System.out);
    }

    public String readLine() {
        if (scanner.hasNextLine()) {
            return scanner.nextLine();
        }
        return null;
    }

    public synchronized void printMessage(String msg) {
        out.println(msg);
    }

    public synchronized void printError(String msg) {
        out.println(" [!] " + msg);
    }

    public synchronized void printDashboard(ClientGameState state) {
        out.println(); // Odstęp
        printLine(CORNER_TL, CORNER_TR);

        printRow(" Phase: " + state.getCurrentPhase());
        printRow(" Pot:   " + state.getCurrentPot());

        printLine(SEP_L, SEP_R);

        // ZMIANA: Używamy getMyName() zamiast getName()
        // Metoda getMyName() sama dba o zwrócenie "Unknown" lub ID w razie braku imienia
        printRow(" Player: " + state.getMyName());

        // ZMIANA: Używamy getMyChips() zamiast getChips() (pobiera z mapy)
        printRow(" Chips:  " + state.getMyChips());

        printRow(" To Call: " + state.getAmountToCall());

        printLine(SEP_L, SEP_R);

        // ZMIANA: Używamy getMyHand() zamiast getHand()
        String handStr = formatHand(state.getMyHand());
        printRow(" HAND: " + handStr);

        printLine(CORNER_BL, CORNER_BR);

        if (!state.getLastMessage().isEmpty()) {
            System.out.println(" > SYSTEM: " + state.getLastMessage());
        }
    }

    public synchronized void printHelp(ClientGameState state) {
        if (state.getGameId() == null || state.getPlayerId() == null) {
            printStartupHelp();
        } else {
            printGameHelp();
        }
    }

    public synchronized void printPrompt() {
        out.print("\n> "); // Uses the injected stream, not System.out
    }


    private String formatHand(List<String> hand) {
        if (hand.isEmpty()) return "[ NO CARDS ]";

        // Kopia listy, żeby nie mieszać w oryginale
        List<String> sortedHand = new ArrayList<>(hand);

        // Sortowanie malejące (v2 porównywane do v1)
        sortedHand.sort((c1, c2) -> Integer.compare(getCardValue(c2), getCardValue(c1)));

        StringBuilder sb = new StringBuilder();
        for (String card : sortedHand) {
            sb.append("[").append(card).append("] ");
        }
        return sb.toString().trim();
    }

    private void printLine(String left, String right) {
        out.print(left);
        for (int i = 0; i < WIDTH; i++) System.out.print(BORDER_HOR);
        out.println(right);
    }

    private void printRow(String content) {
        out.print(BORDER_VER);
        String format = "%-" + WIDTH + "s";

        if (content.length() > WIDTH) content = content.substring(0, WIDTH);
        out.printf(format, content);
        out.println(BORDER_VER);
    }

    private void printStartupHelp() {
        out.println();
        printLine(CORNER_TL, CORNER_TR);
        printRow("              STARTUP COMMANDS");
        printLine(SEP_L, SEP_R);

        printRow(" create             - Create a new game");
        printRow(" join <id> <name>   - Join game (e.g. join 123 Alice)");
        printRow(" help               - Show this menu");
        printRow(" quit               - Exit application");

        printLine(CORNER_BL, CORNER_BR);
    }


    private void printGameHelp() {
        out.println();
        printLine(CORNER_TL, CORNER_TR);
        printRow("              AVAILABLE COMMANDS");

        printLine(SEP_L, SEP_R);

        printRow(" check           - Pass turn (if no bet)");
        printRow(" call            - Match current bet");
        printRow(" raise <amount>  - Increase bet (e.g. raise 50)");
        printRow(" fold            - Give up hand");
        printRow(" draw <indexes>  - Exchange cards (e.g. draw 0,2)");
        printRow(" help            - Show this menu");
        printRow(" quit            - Exit game");

        printLine(CORNER_BL, CORNER_BR);
    }

    private int getCardValue(String card) {
        if (card == null || card.length() < 2) return 0;
        // Ostatni znak to kolor, reszta to figura (np. "10" albo "K")
        String rank = card.substring(0, card.length() - 1);

        return switch (rank) {
            case "A" -> 14;
            case "K" -> 13;
            case "Q" -> 12;
            case "J" -> 11;
            default -> {
                try {
                    yield Integer.parseInt(rank);
                } catch (NumberFormatException e) {
                    yield 0;
                }
            }
        };
    }

}