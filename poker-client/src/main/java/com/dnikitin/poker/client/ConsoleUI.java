package com.dnikitin.poker.client;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * Handles the Text User Interface (TUI) for the Poker Client.
 * <p>
 * This class is responsible for rendering the game dashboard, handling user input via Scanner,
 * and formatting output using ANSI escape codes for colors and frames.
 * </p>
 * <p>
 * Synchronization: Output methods are synchronized to prevent text interleaving
 * between the server listener thread and the user input thread.
 * </p>
 */
public class ConsoleUI {
    private final Scanner scanner;
    private final PrintStream out;

    // ANSI Escape Codes for coloring output
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m"; // Gold/Yellow
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    private static final String BOLD = "\u001B[1m";

    // Frame drawing characters
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

    /**
     * Constructs a UI with custom input and output streams.
     * Useful for testing or redirecting I/O.
     *
     * @param in  The input stream (e.g., System.in).
     * @param out The output stream (e.g., System.out).
     */
    public ConsoleUI(InputStream in, PrintStream out) {
        this.scanner = new Scanner(in);
        this.out = out;
    }

    /**
     * Constructs a UI using the standard system streams (stdin/stdout).
     */
    public ConsoleUI() {
        this(System.in, System.out);
    }

    /**
     * Reads a line of text from the input stream.
     *
     * @return The trimmed string entered by the user, or null if the stream is closed.
     */
    public String readLine() {
        if (scanner.hasNextLine()) {
            return scanner.nextLine();
        }
        return null;
    }

    /**
     * Prints a standard message to the console.
     * Includes basic heuristics for coloring success or lobby messages.
     *
     * @param msg The message to print.
     */
    public synchronized void printMessage(String msg) {
        if (msg.trim().startsWith("✓")) {
            out.println(GREEN + msg + RESET);
        } else if (msg.contains("[LOBBY]")) {
            out.println(CYAN + msg + RESET);
        } else {
            out.println(msg);
        }
    }

    /**
     * Prints an error message, prefixed with a red exclamation mark.
     *
     * @param msg The error description.
     */
    public synchronized void printError(String msg) {
        out.println(" " + RED + "[!] " + msg + RESET);
    }

    /**
     * Renders the complete game dashboard, including the frame,
     * player stats, pot info, and current hand.
     *
     * @param state The current {@link ClientGameState} to render.
     */
    public synchronized void printDashboard(ClientGameState state) {
        out.println();
        printLine(CORNER_TL, CORNER_TR);

        // Phase (Yellow)
        printRow(" Phase: " + YELLOW + state.getCurrentPhase() + RESET);
        printRow(" Pot:   " + GREEN + state.getCurrentPot() + RESET);

        printLine(SEP_L, SEP_R);

        // Player Data
        printRow(" Player: " + BOLD + state.getMyName() + RESET);
        printRow(" Chips:  " + YELLOW + state.getMyChips() + RESET);
        printRow(" To Call: " + RED + state.getAmountToCall() + RESET);

        printLine(SEP_L, SEP_R);

        // Hand
        String handStr = formatHand(state.getMyHand());
        printRow(" HAND: " + handStr);

        printLine(CORNER_BL, CORNER_BR);

        if (!state.getLastMessage().isEmpty()) {
            out.println(" > SYSTEM: " + PURPLE + state.getLastMessage() + RESET);
        }
    }

    /**
     * Displays contextual help commands based on connection status.
     *
     * @param state The current game state.
     */
    public synchronized void printHelp(ClientGameState state) {
        if (state.getGameId() == null || state.getPlayerId() == null) {
            printStartupHelp();
        } else {
            printGameHelp();
        }
    }

    /**
     * Prints the input prompt indicator (e.g. "> ").
     */
    public synchronized void printPrompt() {
        out.print("\n" + GREEN + BOLD + "> " + RESET);
    }

    /**
     * Formats and sorts the player's hand for display.
     * Sorts cards by value descending (e.g., Ace before King).
     *
     * @param hand List of card strings (e.g., "Ah", "Ks").
     * @return A formatted string representation of the hand.
     */
    private String formatHand(List<String> hand) {
        if (hand.isEmpty()) return "[ NO CARDS ]";

        // Copy and sort
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
        // Middle of the frame
        for (int i = 0; i < WIDTH; i++) out.print(FRAME_COLOR + "═" + RESET);
        out.println(right);
    }

    // Overloaded printRow for easier coloring
    private void printRow(String label, String value) {
        // Calculate visible length stripping ANSI codes to align the frame correctly
        int visibleLength = stripAnsi(label + value).length();
        int padding = WIDTH - visibleLength;

        out.print(BORDER_VER);
        out.print(label + value);

        // Fill with spaces
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

    /**
     * Determines the numerical value of a card for sorting purposes.
     *
     * @param card The card string (e.g., "10h", "As").
     * @return Integer value (2-14), or 0 if invalid.
     */
    private int getCardValue(String card) {
        if (card == null || card.length() < 2) return 0;
        // Last char is suit, rest is rank (e.g. "10" or "K")
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