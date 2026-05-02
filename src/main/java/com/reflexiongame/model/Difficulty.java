package com.reflexiongame.model;

public enum Difficulty {
    EASY    ("Facile",    4,  2, 100, 3, 3, 5,  180),
    MEDIUM  ("Moyen",     6,  3, 150, 4, 4, 8,  240),
    HARD    ("Difficile", 8,  4, 220, 5, 5, 12, 300),
    EXPERT  ("Expert",    10, 4, 320, 6, 6, 14, 360),
    MASTER  ("Maître",    12, 4, 460, 6, 7, 16, 480);

    private final String label;
    private final int pairs;
    private final int columns;
    private final int multiplier;
    private final int gridSize;
    private final int disks;
    private final int sudokuHidden;
    /** Base time budget in seconds before a game is auto-lost. */
    private final int baseTimeSeconds;

    Difficulty(String label, int pairs, int columns, int multiplier,
               int gridSize, int disks, int sudokuHidden, int baseTimeSeconds) {
        this.label = label;
        this.pairs = pairs;
        this.columns = columns;
        this.multiplier = multiplier;
        this.gridSize = gridSize;
        this.disks = disks;
        this.sudokuHidden = sudokuHidden;
        this.baseTimeSeconds = baseTimeSeconds;
    }

    public String getLabel() { return label; }
    public int getPairs() { return pairs; }
    public int getColumns() { return columns; }
    public int getMultiplier() { return multiplier; }
    public int getGridSize() { return gridSize; }
    public int getDisks() { return disks; }
    public int getSudokuHidden() { return sudokuHidden; }
    public int getBaseTimeSeconds() { return baseTimeSeconds; }

    /** True for Expert and Master — used to enable extra rules. */
    public boolean isAdvanced() { return this == EXPERT || this == MASTER; }
    public boolean isMaster()   { return this == MASTER; }

    public Difficulty next() {
        Difficulty[] all = values();
        int i = ordinal();
        return i + 1 < all.length ? all[i + 1] : null;
    }
}
