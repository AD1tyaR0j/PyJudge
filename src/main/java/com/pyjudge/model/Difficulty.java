package com.pyjudge.model;

/** Exercise difficulty, with the leaderboard points a solve is worth. */
public enum Difficulty {
    EASY("Easy", 10),
    MEDIUM("Medium", 20),
    HARD("Hard", 40);

    private final String label;
    private final int points;

    Difficulty(String label, int points) {
        this.label = label;
        this.points = points;
    }

    public String getLabel() { return label; }
    public int getPoints() { return points; }
    public String getCss() { return label.toLowerCase(); }

    public static Difficulty fromLabel(String label) {
        for (Difficulty d : values()) {
            if (d.label.equalsIgnoreCase(label)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown difficulty: " + label);
    }
}
