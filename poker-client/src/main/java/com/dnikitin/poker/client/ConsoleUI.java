package com.dnikitin.poker.client;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

public class ConsoleUI {
    private final Scanner scanner;
    private final PrintStream out;

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m"; // Gold/Yellow
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    private static final String BOLD = "\u001B[1m";

    private static final String FRAME_COLOR = BLUE;
    private static final String BORDER_HOR = FRAME_COLOR + "═" + RESET;
    private static final String BORDER_VER = FRAME_COLOR + "║" + RESET;
    private static final String CORNER_TL = FRAME_COLOR + "╔" + RESET;
    private static final String CORNER_TR = FRAME_COLOR + "╗" + RESET;
    private static final String CORNER_BL = FRAME_COLOR + "╚" + RESET;
    private static final String CORNER_BR = FRAME_COLOR + "╝" + RESET;
    private static final String SEP_L = FRAME_COLOR + "╠" + RESET;
    private static final String SEP_R = FRAME_COLOR + "╣" + RESET;

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
        if (msg.trim().startsWith("✓")) {
            out.println(GREEN + msg + RESET);
        }
        else if (msg.contains("[LOBBY]")) {
            out.println(CYAN + msg + RESET);
        }
        else {
            out.println(msg);
        }
    }

    public synchronized void printError(String msg) {
        out.println(" " + RED + "[!] " + msg + RESET);
    }

    public synchronized void printDashboard(ClientGameState state) {
        out.println();
        printLine(CORNER_TL, CORNER_TR);

        // Faza gry na żółto
        printRow(" Phase: " + YELLOW + state.getCurrentPhase() + RESET);
        printRow(" Pot:   " + GREEN + state.getCurrentPot() + RESET);

        printLine(SEP_L, SEP_R);

        // Dane gracza
        printRow(" Player: " + BOLD + state.getMyName() + RESET);
        printRow(" Chips:  " + YELLOW + state.getMyChips() + RESET);
        printRow(" To Call: " + RED + state.getAmountToCall() + RESET);


        printLine(SEP_L, SEP_R);

        // Ręka z kolorowaniem kart
        String handStr = formatHand(state.getMyHand());
        printRow(" HAND: " + handStr);

        printLine(CORNER_BL, CORNER_BR);

        if (!state.getLastMessage().isEmpty()) {
            // Systemowe komunikaty (np. Twoja Tura) na fioletowo/jasno
            out.println(" > SYSTEM: " + PURPLE + state.getLastMessage() + RESET);
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
        out.print("\n" + GREEN + BOLD + "> " + RESET); // Uses the injected stream, not System.out
    }


    private String formatHand(List<String> hand) {
        if (hand.isEmpty()) return "[ NO CARDS ]";

        // Kopia i sortowanie
        List<String> sortedHand = new ArrayList<>(hand);
        sortedHand.sort((c1, c2) -> Integer.compare(getCardValue(c2), getCardValue(c1)));

        StringBuilder sb = new StringBuilder();
        for (String card : sortedHand) {
            sb.append((card)).append(" ");
        }
        return sb.toString().trim();
    }

    private void printLine(String left, String right) {
        out.print(left);
        // Środek ramki
        for (int i = 0; i < WIDTH; i++) out.print(FRAME_COLOR + "═" + RESET);
        out.println(right);
    }

    // Przeciążona metoda printRow, która przyjmuje etykietę i wartość (dla łatwiejszego kolorowania)
    private void printRow(String label, String value) {
        // Obliczamy faktyczną długość tekstu bez kodów sterujących (żeby ramka się nie rozjechała)
        int visibleLength = stripAnsi(label + value).length();
        int padding = WIDTH - visibleLength;

        out.print(BORDER_VER);
        out.print(label + value);

        // Dopełniamy spacjami
        for (int i = 0; i < padding; i++) out.print(" ");

        out.println(BORDER_VER);
    }

    private void printRow(String content) {
        printRow(content, "");
    }

    private String stripAnsi(String str) {
        return str.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    private void printStartupHelp() {
        out.println();
        printLine(CORNER_TL, CORNER_TR);
        printRow(BOLD + "              STARTUP COMMANDS" + RESET, "");
        printLine(SEP_L, SEP_R);
        printRow(" create             - Create a new game", "");
        printRow(" join <id> <name>   - Join game", "");
        printRow(" help               - Show this menu", "");
        printRow(" quit               - Exit application", "");
        printLine(CORNER_BL, CORNER_BR);
    }


    private void printGameHelp() {
        out.println();
        printLine(CORNER_TL, CORNER_TR);
        printRow(BOLD + "              AVAILABLE COMMANDS" + RESET, "");
        printLine(SEP_L, SEP_R);
        printRow(" start           - Start game (creator)", "");
        printRow(" check           - Pass turn", "");
        printRow(" call            - Match current bet", "");
        printRow(" raise <amount>  - Increase bet", "");
        printRow(" fold            - Give up hand", "");
        printRow(" draw <indexes>  - Exchange cards", "");
        printRow(" status          - Refresh info", "");
        printRow(" help            - Show this menu", "");
        printRow(" quit            - Exit game", "");
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