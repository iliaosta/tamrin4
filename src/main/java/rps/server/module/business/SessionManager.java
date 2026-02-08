package rps.server.module.business;

import rps.server.core.config.ConfigManager;
import rps.server.core.logging.Logger;
import rps.server.module.abstraction.ISessionManager;
import rps.server.module.model.GameSession;
import rps.server.module.model.Move;
import rps.server.module.model.PlayerInfo;

import java.util.*;

public class SessionManager implements ISessionManager {

    private final Logger logger;
    private final ConfigManager config;

    private final Queue<PlayerInfo> waitingPlayers = new ArrayDeque<>();
    private final Map<String, GameSession> playerToSession = new HashMap<>();

    // deadline per session (session -> epochMillis)
    private final Map<GameSession, Long> roundDeadline = new HashMap<>();

    // Bot-related: which sessions are bot sessions
    private final Set<GameSession> botSessions = new HashSet<>();
    private final Random random = new Random();

    public SessionManager(Logger logger, ConfigManager config) {
        this.logger = logger;
        this.config = config;
    }

    @Override
    public List<OutboundMessage> onMessage(PlayerInfo player, String message) {
        List<OutboundMessage> out = new ArrayList<>();

        GameSession session = playerToSession.get(player.key());

        // اگر بازیکن در هیچ session نیست → پیام باید HELLO باشد یا در صف برود
        if (session == null) {
            handleHelloOrWaiting(player, message, out);
            return out;
        }

        // اگر session bot هست، فقط از بازیکن انسانی پیام می‌گیریم
        // (bot خودش توسط سرور تولید می‌شود)
        return handleInSession(player, message, session, out);
    }

    @Override
    public List<OutboundMessage> checkTimeouts() {
        List<OutboundMessage> out = new ArrayList<>();
        long now = System.currentTimeMillis();

        List<GameSession> timedOutSessions = new ArrayList<>();
        for (Map.Entry<GameSession, Long> e : roundDeadline.entrySet()) {
            if (now >= e.getValue()) timedOutSessions.add(e.getKey());
        }

        for (GameSession session : timedOutSessions) {
            boolean p1Moved = session.getP1Move() != null;
            boolean p2Moved = session.getP2Move() != null;

            if (p1Moved && p2Moved) {
                roundDeadline.remove(session);
                continue;
            }

            // Bot session: اگر انسان جواب نده، انسان بازنده است
            if (botSessions.contains(session)) {
                PlayerInfo human = session.getP1(); // در bot session، p1 انسان است
                out.add(new OutboundMessage(human, "Timeout! You lose the game."));
                logger.log("Timeout (BOT). Human lost: " + human.key());
                roundDeadline.remove(session);
                cleanupSession(session);
                botSessions.remove(session);
                continue;
            }

            PlayerInfo loser;
            PlayerInfo winner;

            if (!p1Moved && p2Moved) {
                loser = session.getP1();
                winner = session.getP2();
            } else if (!p2Moved && p1Moved) {
                loser = session.getP2();
                winner = session.getP1();
            } else {
                out.add(new OutboundMessage(session.getP1(), "Timeout! Game ended (no moves)."));
                out.add(new OutboundMessage(session.getP2(), "Timeout! Game ended (no moves)."));
                logger.log("Timeout: both players did not respond. Session ended: " +
                        session.getP1().key() + " vs " + session.getP2().key());
                roundDeadline.remove(session);
                cleanupSession(session);
                continue;
            }

            out.add(new OutboundMessage(loser, "Timeout! You lose the game."));
            out.add(new OutboundMessage(winner, "GAME_OVER|Opponent timeout. You win!"));

            logger.log("Timeout! Loser: " + loser.key() + " Winner: " + winner.key() +
                    " | Session: " + session.getP1().key() + " vs " + session.getP2().key());

            roundDeadline.remove(session);
            cleanupSession(session);
        }

        return out;
    }

    // -------------------- internal helpers --------------------

    private void handleHelloOrWaiting(PlayerInfo player, String message, List<OutboundMessage> out) {
        String msg = (message == null) ? "" : message.trim();

        if (msg.equalsIgnoreCase("HELLO|BOT")) {
            createBotSession(player, out);
            return;
        }

        // برای حالت عادی، اگر پیام HELLO|PLAYER یا هر چیز دیگری بود → وارد matchmaking معمولی
        handleWaiting(player, out);
    }

    private void createBotSession(PlayerInfo human, List<OutboundMessage> out) {
        // یک Bot "مجازی" با کلید خاص می‌سازیم (بدون IP/Port واقعی)
        // اما چون ارسال UDP به Bot نداریم، فقط p2 را نگه می‌داریم برای state
        PlayerInfo bot = new PlayerInfo(human.getAddress(), -1) {
            @Override public String key() { return "BOT"; }
        };

        GameSession session = new GameSession(human, bot, config.winsRequired());

        playerToSession.put(human.key(), session);
        // bot را در map نمی‌گذاریم چون از UDP پیام نمی‌گیرد

        botSessions.add(session);

        logger.log("Bot session created for human: " + human.key());

        out.add(new OutboundMessage(human, "SERVER|Game started! You are playing with BOT."));
        startRound(session, out);
    }

    private void handleWaiting(PlayerInfo player, List<OutboundMessage> out) {
        for (PlayerInfo p : waitingPlayers) {
            if (p.key().equals(player.key())) {
                out.add(new OutboundMessage(player, "SERVER|Waiting for another player..."));
                return;
            }
        }

        waitingPlayers.add(player);
        out.add(new OutboundMessage(player, "SERVER|Registered. Waiting for another player..."));
        logger.log("Client connected: " + player.key());

        if (waitingPlayers.size() >= 2) {
            PlayerInfo p1 = waitingPlayers.poll();
            PlayerInfo p2 = waitingPlayers.poll();

            GameSession session = new GameSession(p1, p2, config.winsRequired());

            playerToSession.put(p1.key(), session);
            playerToSession.put(p2.key(), session);

            logger.log("Game session created: " + p1.key() + " vs " + p2.key());

            out.add(new OutboundMessage(p1, "SERVER|Game started! You are Player 1."));
            out.add(new OutboundMessage(p2, "SERVER|Game started! You are Player 2."));

            startRound(session, out);
        }
    }

    private List<OutboundMessage> handleInSession(PlayerInfo player, String message, GameSession session, List<OutboundMessage> out) {
        // اگر bot session هست و پیام از bot بیاید، نادیده (اصلاً نباید بیاید)
        if (botSessions.contains(session) && !session.getP1().key().equals(player.key())) {
            out.add(new OutboundMessage(player, "SERVER|Invalid sender."));
            return out;
        }

        Move move = Move.fromString(message);
        if (move == null) {
            out.add(new OutboundMessage(player, "INVALID|Please send 1, 2 or 3"));
            logger.log("Invalid input from " + player.key() + " -> " + message);
            return out;
        }

        session.setMove(player, move);
        out.add(new OutboundMessage(player, "SERVER|Move received."));
        logger.log("Move: " + player.key() + " -> " + move);

        // اگر session bot باشد، همینجا حرکت Bot را هم تولید می‌کنیم
        if (botSessions.contains(session)) {
            Move botMove = randomBotMove();
            session.setMove(session.getP2(), botMove);
            logger.log("Move: BOT -> " + botMove);
        }

        if (session.bothMoved()) {
            roundDeadline.remove(session);

            Move m1 = session.getP1Move();
            Move m2 = session.getP2Move();

            int roundWinner = determineRoundWinner(m1, m2);
            if (roundWinner == 1) session.addPointToP1();
            else if (roundWinner == 2) session.addPointToP2();

            String roundResultText = switch (roundWinner) {
                case 0 -> "DRAW";
                case 1 -> "PLAYER1_WINS";
                case 2 -> "PLAYER2_WINS";
                default -> "DRAW";
            };

            if (botSessions.contains(session)) {
                // فقط به انسان ارسال می‌کنیم
                out.add(new OutboundMessage(session.getP1(),
                        "ROUND_RESULT|" + roundResultText + "|YOU=" + m1 + "|BOT=" + m2));
                out.add(new OutboundMessage(session.getP1(),
                        "SCORE|YOU=" + session.getP1Score() + "|BOT=" + session.getP2Score()));
            } else {
                out.add(new OutboundMessage(session.getP1(),
                        "ROUND_RESULT|" + roundResultText + "|P1=" + m1 + "|P2=" + m2));
                out.add(new OutboundMessage(session.getP2(),
                        "ROUND_RESULT|" + roundResultText + "|P1=" + m1 + "|P2=" + m2));

                out.add(new OutboundMessage(session.getP1(),
                        "SCORE|P1=" + session.getP1Score() + "|P2=" + session.getP2Score()));
                out.add(new OutboundMessage(session.getP2(),
                        "SCORE|P1=" + session.getP1Score() + "|P2=" + session.getP2Score()));
            }

            logger.log("Round result: " + roundResultText +
                    " | P1=" + m1 + " P2=" + m2 +
                    " | Score P1=" + session.getP1Score() + " P2=" + session.getP2Score());

            if (session.isGameOver()) {
                int winner = session.winnerPlayerNumber();

                if (botSessions.contains(session)) {
                    if (winner == 1) out.add(new OutboundMessage(session.getP1(), "GAME_OVER|You win against BOT!"));
                    else out.add(new OutboundMessage(session.getP1(), "GAME_OVER|You lose against BOT!"));

                    logger.log("Game over (BOT). Winner playerNumber=" + winner +
                            " | Human=" + session.getP1().key());
                    cleanupSession(session);
                    botSessions.remove(session);
                } else {
                    endGame(session, winner, out);
                    cleanupSession(session);
                }

            } else {
                session.resetRound();
                startRound(session, out);
            }
        }

        return out;
    }

    private Move randomBotMove() {
        int v = random.nextInt(3) + 1;
        return switch (v) {
            case 1 -> Move.ROCK;
            case 2 -> Move.PAPER;
            default -> Move.SCISSORS;
        };
    }

    private void startRound(GameSession session, List<OutboundMessage> out) {
        if (botSessions.contains(session)) {
            out.add(new OutboundMessage(session.getP1(), "let's play"));
            long deadline = System.currentTimeMillis() + (long) config.roundTimeoutSeconds() * 1000L;
            roundDeadline.put(session, deadline);

            logger.log("Round started (BOT, timeout=" + config.roundTimeoutSeconds() + "s) for human: " + session.getP1().key());
            return;
        }

        out.add(new OutboundMessage(session.getP1(), "let's play"));
        out.add(new OutboundMessage(session.getP2(), "let's play"));

        long deadline = System.currentTimeMillis() + (long) config.roundTimeoutSeconds() * 1000L;
        roundDeadline.put(session, deadline);

        logger.log("Round started (timeout=" + config.roundTimeoutSeconds() + "s) for session: " +
                session.getP1().key() + " vs " + session.getP2().key());
    }

    private int determineRoundWinner(Move p1, Move p2) {
        if (p1 == p2) return 0;

        if (p1 == Move.ROCK && p2 == Move.SCISSORS) return 1;
        if (p2 == Move.ROCK && p1 == Move.SCISSORS) return 2;

        if (p1 == Move.PAPER && p2 == Move.ROCK) return 1;
        if (p2 == Move.PAPER && p1 == Move.ROCK) return 2;

        if (p1 == Move.SCISSORS && p2 == Move.PAPER) return 1;
        if (p2 == Move.SCISSORS && p1 == Move.PAPER) return 2;

        return 0;
    }

    private void endGame(GameSession session, int winnerPlayerNumber, List<OutboundMessage> out) {
        if (winnerPlayerNumber == 1) {
            out.add(new OutboundMessage(session.getP1(), "GAME_OVER|You win!"));
            out.add(new OutboundMessage(session.getP2(), "GAME_OVER|You lose!"));
        } else if (winnerPlayerNumber == 2) {
            out.add(new OutboundMessage(session.getP1(), "GAME_OVER|You lose!"));
            out.add(new OutboundMessage(session.getP2(), "GAME_OVER|You win!"));
        }

        logger.log("Game over. Winner: Player " + winnerPlayerNumber +
                " | Final Score P1=" + session.getP1Score() + " P2=" + session.getP2Score() +
                " | Session: " + session.getP1().key() + " vs " + session.getP2().key());
    }

    private void cleanupSession(GameSession session) {
        playerToSession.remove(session.getP1().key());
        if (session.getP2().getPort() != -1) { // bot port = -1, map ندارد
            playerToSession.remove(session.getP2().key());
        }
        roundDeadline.remove(session);
    }
}
