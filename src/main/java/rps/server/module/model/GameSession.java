package rps.server.module.model;

public class GameSession {
    private final PlayerInfo p1;
    private final PlayerInfo p2;

    private Move p1Move;
    private Move p2Move;

    private int p1Score = 0;
    private int p2Score = 0;

    private final int winsRequired;

    public GameSession(PlayerInfo p1, PlayerInfo p2, int winsRequired) {
        this.p1 = p1;
        this.p2 = p2;
        this.winsRequired = winsRequired;
    }

    public PlayerInfo getP1() { return p1; }
    public PlayerInfo getP2() { return p2; }

    public boolean contains(PlayerInfo p) {
        return p1.key().equals(p.key()) || p2.key().equals(p.key());
    }

    public boolean isP1(PlayerInfo p) { return p1.key().equals(p.key()); }
    public boolean isP2(PlayerInfo p) { return p2.key().equals(p.key()); }

    public void setMove(PlayerInfo p, Move move) {
        if (isP1(p)) p1Move = move;
        else if (isP2(p)) p2Move = move;
    }

    public boolean bothMoved() {
        return p1Move != null && p2Move != null;
    }

    public Move getP1Move() { return p1Move; }
    public Move getP2Move() { return p2Move; }

    public void resetRound() {
        p1Move = null;
        p2Move = null;
    }

    public int getP1Score() { return p1Score; }
    public int getP2Score() { return p2Score; }

    public void addPointToP1() { p1Score++; }
    public void addPointToP2() { p2Score++; }

    public int getWinsRequired() { return winsRequired; }

    public boolean isGameOver() {
        return p1Score >= winsRequired || p2Score >= winsRequired;
    }

    public int winnerPlayerNumber() {
        if (p1Score >= winsRequired) return 1;
        if (p2Score >= winsRequired) return 2;
        return 0;
    }
}
