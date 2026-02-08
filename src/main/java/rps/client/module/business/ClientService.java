package rps.client.module.business;

import rps.client.module.abstraction.IClientService;
import rps.client.module.network.UdpClient;

import java.util.Locale;
import java.util.Scanner;

public class ClientService implements IClientService {

    private final UdpClient udpClient;

    public ClientService(UdpClient udpClient) {
        this.udpClient = udpClient;
    }

    @Override
    public void start() {
        System.out.println("✅ Client started (UDP).");

        Scanner sc = new Scanner(System.in);

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Rock Paper Scissors");
        System.out.println("You can type:");
        System.out.println("  1 / rock / سنگ");
        System.out.println("  2 / paper / کاغذ");
        System.out.println("  3 / scissors / قیچی");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // انتخاب مود
        System.out.println("Select mode:");
        System.out.println("  1) Play with another player");
        System.out.println("  2) Play with Bot");
        System.out.print("Select: ");
        String modeInput = sc.nextLine().trim();

        String hello = modeInput.equals("2") ? "HELLO|BOT" : "HELLO|PLAYER";
        udpClient.send(hello);

        // Receiver thread
        Thread receiver = new Thread(() -> {
            while (true) {
                try {
                    String raw = udpClient.receive();
                    if (raw == null) continue;
                    renderServerMessage(raw);
                } catch (Exception e) {
                    System.out.println("⚠️ Connection closed: " + e.getMessage());
                    break;
                }
            }
        });
        receiver.setDaemon(true);
        receiver.start();

        System.out.println("Type your move when it's your turn. (or type 'exit')");

        while (true) {
            String line = sc.nextLine();

            if (line.equalsIgnoreCase("exit")) {
                udpClient.send("BYE");
                udpClient.close();
                System.out.println("👋 Bye!");
                break;
            }

            // 👇 تبدیل اختیاری اسم حرکت به عدد (بدون رد کردن ورودی)
            String mapped = mapMoveToNumberIfPossible(line);

            // طبق صورت سوال: اگر نامعتبر باشد همون متن خام می‌رود سمت سرور
            udpClient.send(mapped);
        }
    }

    // اگر کاربر rock/سنگ/... نوشت => "1/2/3" برگردان، وگرنه همون ورودی
    private String mapMoveToNumberIfPossible(String input) {
        if (input == null) return "";

        String s = input.trim();
        if (s.isEmpty()) return s;

        // اگر خودش عدد 1/2/3 زده، دست نزن
        if (s.equals("1") || s.equals("2") || s.equals("3")) return s;

        // نرمال‌سازی ساده (حروف کوچک + حذف فاصله‌های اضافی)
        String normalized = s.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");

        // English
        if (normalized.equals("rock")) return "1";
        if (normalized.equals("paper")) return "2";
        if (normalized.equals("scissors") || normalized.equals("scissor")) return "3";

        // Persian (چند شکل رایج)
        if (normalized.equals("سنگ")) return "1";
        if (normalized.equals("کاغذ")) return "2";
        if (normalized.equals("قیچی")) return "3";

        // اگر چیز دیگری بود، همون را بفرست (سرور خودش INVALID می‌دهد)
        return s;
    }

    // -------------------- UI helpers (همون نمایش قشنگ) --------------------

    private void renderServerMessage(String raw) {
        String msg = raw.trim();

        if (msg.equalsIgnoreCase("let's play")) {
            System.out.println();
            System.out.println("🎲 Your turn! Enter 1/2/3 or rock/paper/scissors (سنگ/کاغذ/قیچی):");
            System.out.print("> ");
            return;
        }

        if (msg.startsWith("SERVER|")) {
            System.out.println("ℹ️ " + msg.substring("SERVER|".length()));
            return;
        }

        if (msg.startsWith("INVALID|")) {
            System.out.println("❌ " + msg.substring("INVALID|".length()));
            System.out.print("> ");
            return;
        }

        if (msg.startsWith("SCORE|")) {
            System.out.println("📊 " + formatScore(msg));
            return;
        }

        if (msg.startsWith("ROUND_RESULT|")) {
            System.out.println("🧾 " + formatRoundResult(msg));
            return;
        }

        if (msg.startsWith("GAME_OVER|")) {
            System.out.println();
            System.out.println("🏁 " + msg.substring("GAME_OVER|".length()));
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }

        System.out.println("📨 " + msg);
    }

    private String formatScore(String msg) {
        String[] parts = msg.split("\\|");
        StringBuilder sb = new StringBuilder();
        sb.append("Score: ");
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) sb.append("  |  ");
            sb.append(parts[i].replace("P1", "Player1")
                    .replace("P2", "Player2")
                    .replace("YOU", "You")
                    .replace("BOT", "Bot"));
        }
        return sb.toString();
    }

    private String formatRoundResult(String msg) {
        String[] parts = msg.split("\\|");
        if (parts.length < 2) return msg;

        String outcome = parts[1];
        String niceOutcome = switch (outcome) {
            case "DRAW" -> "Draw 🤝";
            case "PLAYER1_WINS" -> "Player 1 wins ✅";
            case "PLAYER2_WINS" -> "Player 2 wins ✅";
            default -> outcome;
        };

        StringBuilder moves = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            if (i > 2) moves.append("  |  ");
            moves.append(prettyMovePair(parts[i]));
        }

        return niceOutcome + (moves.length() > 0 ? " — " + moves : "");
    }

    private String prettyMovePair(String pair) {
        String[] kv = pair.split("=");
        if (kv.length != 2) return pair;

        String who = kv[0]
                .replace("P1", "Player1")
                .replace("P2", "Player2")
                .replace("YOU", "You")
                .replace("BOT", "Bot");

        String move = prettyMoveName(kv[1]);
        return who + ": " + move;
    }

    private String prettyMoveName(String moveToken) {
        return switch (moveToken) {
            case "ROCK" -> "Rock (سنگ)";
            case "PAPER" -> "Paper (کاغذ)";
            case "SCISSORS" -> "Scissors (قیچی)";
            default -> moveToken;
        };
    }
}
