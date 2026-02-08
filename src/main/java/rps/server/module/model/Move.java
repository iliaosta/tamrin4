package rps.server.module.model;

public enum Move {
    ROCK(1), PAPER(2), SCISSORS(3);

    private final int code;

    Move(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Move fromString(String s) {
        if (s == null) return null;
        s = s.trim();
        try {
            int v = Integer.parseInt(s);
            return switch (v) {
                case 1 -> ROCK;
                case 2 -> PAPER;
                case 3 -> SCISSORS;
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
