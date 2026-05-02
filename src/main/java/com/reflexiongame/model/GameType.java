package com.reflexiongame.model;

public enum GameType {
    MEMORY        ("Mémoire",          "Retrouvez toutes les paires",                                "paires",  1.0),
    SUDOKU        ("Sudoku 4×4",       "Remplissez la grille avec les chiffres 1 à 4, sans répétition", "cases",   1.2),
    PICTURE_PUZZLE("Puzzle photo",     "Reconstituez l'image en échangeant les pièces",              "pièces",  1.5),
    NUMBER_RUSH   ("Grille des nombres", "Tapez les nombres dans le bon ordre, le plus vite possible", "nombres", 0.6),
    TOWER_OF_HANOI("Tours de Hanoï",   "Déplacez tous les disques sur la tour de droite",            "disques", 1.4);

    private final String label;
    private final String description;
    private final String unitLabel;
    /** Per-game-type multiplier applied to the difficulty's base time budget. */
    private final double timeMultiplier;

    GameType(String label, String description, String unitLabel, double timeMultiplier) {
        this.label = label;
        this.description = description;
        this.unitLabel = unitLabel;
        this.timeMultiplier = timeMultiplier;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public String getUnitLabel() { return unitLabel; }
    public double getTimeMultiplier() { return timeMultiplier; }

    /** Total steps to win (used for progress). */
    public int getTarget(Difficulty difficulty) {
        return switch (this) {
            case MEMORY -> difficulty.getPairs();
            case SUDOKU -> 16;
            case PICTURE_PUZZLE -> {
                int s = difficulty.getGridSize();
                yield s * s;
            }
            case NUMBER_RUSH -> {
                int s = difficulty.getGridSize();
                yield s * s;
            }
            case TOWER_OF_HANOI -> difficulty.getDisks();
        };
    }
}
