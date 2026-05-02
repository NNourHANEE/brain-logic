package com.reflexiongame.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GameState {
    private Long id;
    private Long playerId;
    private String playerName;
    private Difficulty difficulty;
    private GameType gameType = GameType.MEMORY;

    // MEMORY
    private List<Card> cards = new ArrayList<>();

    // SUDOKU (4x4) — values 1..4, 0 = empty. fixed[i] true = clue.
    private int[] sudokuSolution;     // length 16
    private int[] sudokuGrid;         // length 16
    private boolean[] sudokuFixed;    // length 16

    // PICTURE_PUZZLE
    private String puzzleImage;       // file name in /images/photos/
    private int puzzleSize;           // grid side (3,4,5)
    private int[] puzzleOrder;        // length size*size; value = original piece id at that slot
    private int puzzleSelected = -1;  // selected slot index, -1 if none

    // NUMBER_RUSH (Schulte table) — click numbers in order
    private int numbersSize;          // grid side
    private int[] numbersGrid;        // length size*size; value 1..N or 0 if already clicked
    private int numbersNext;          // next expected number to click (1..N or N..1 in descending mode)

    // TOWER_OF_HANOI
    private int hanoiDisks;           // total disks
    private List<List<Integer>> hanoiTowers; // 3 towers, each list bottom..top of disk sizes
    private int hanoiSelected = -1;   // selected source tower (-1 if none)

    // generic
    private int moves;
    private int matchedPairs;         // generic "progress" count (cells solved, pieces in place, etc.)
    private int score;
    private boolean completed;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private String message;
    /** Transient outcome flag for the latest action: "success", "fail", "info", or null. */
    private String outcome;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public GameType getGameType() { return gameType; }
    public void setGameType(GameType gameType) { this.gameType = gameType == null ? GameType.MEMORY : gameType; }

    public List<Card> getCards() { return cards; }
    public void setCards(List<Card> cards) { this.cards = cards == null ? new ArrayList<>() : cards; }

    public int[] getSudokuSolution() { return sudokuSolution; }
    public void setSudokuSolution(int[] sudokuSolution) { this.sudokuSolution = sudokuSolution; }
    public int[] getSudokuGrid() { return sudokuGrid; }
    public void setSudokuGrid(int[] sudokuGrid) { this.sudokuGrid = sudokuGrid; }
    public boolean[] getSudokuFixed() { return sudokuFixed; }
    public void setSudokuFixed(boolean[] sudokuFixed) { this.sudokuFixed = sudokuFixed; }

    public String getPuzzleImage() { return puzzleImage; }
    public void setPuzzleImage(String puzzleImage) { this.puzzleImage = puzzleImage; }
    public int getPuzzleSize() { return puzzleSize; }
    public void setPuzzleSize(int puzzleSize) { this.puzzleSize = puzzleSize; }
    public int[] getPuzzleOrder() { return puzzleOrder; }
    public void setPuzzleOrder(int[] puzzleOrder) { this.puzzleOrder = puzzleOrder; }
    public int getPuzzleSelected() { return puzzleSelected; }
    public void setPuzzleSelected(int puzzleSelected) { this.puzzleSelected = puzzleSelected; }

    public int getNumbersSize() { return numbersSize; }
    public void setNumbersSize(int numbersSize) { this.numbersSize = numbersSize; }
    public int[] getNumbersGrid() { return numbersGrid; }
    public void setNumbersGrid(int[] numbersGrid) { this.numbersGrid = numbersGrid; }
    public int getNumbersNext() { return numbersNext; }
    public void setNumbersNext(int numbersNext) { this.numbersNext = numbersNext; }

    public int getHanoiDisks() { return hanoiDisks; }
    public void setHanoiDisks(int hanoiDisks) { this.hanoiDisks = hanoiDisks; }
    public List<List<Integer>> getHanoiTowers() { return hanoiTowers; }
    public void setHanoiTowers(List<List<Integer>> hanoiTowers) { this.hanoiTowers = hanoiTowers; }
    public int getHanoiSelected() { return hanoiSelected; }
    public void setHanoiSelected(int hanoiSelected) { this.hanoiSelected = hanoiSelected; }

    public int getMoves() { return moves; }
    public void setMoves(int moves) { this.moves = moves; }
    public int getMatchedPairs() { return matchedPairs; }
    public void setMatchedPairs(int matchedPairs) { this.matchedPairs = matchedPairs; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    // ----- rule helpers (derived from gameType + difficulty) -----
    /** Sudoku also enforces both diagonals (X-Sudoku). */
    public boolean isXSudoku() { return gameType == GameType.SUDOKU && difficulty != null && difficulty.isAdvanced(); }
    /** Picture puzzle restricted to swaps between row/col adjacent pieces. */
    public boolean isPuzzleSlideOnly() { return gameType == GameType.PICTURE_PUZZLE && difficulty != null && difficulty.isMaster(); }
    /** Number Rush: descending order (Master) instead of ascending. */
    public boolean isNumbersDescending() { return gameType == GameType.NUMBER_RUSH && difficulty != null && difficulty.isMaster(); }
    /** Number Rush: a wrong tap reshuffles all remaining numbers (Expert/Master). */
    public boolean isNumbersShuffleOnMiss() { return gameType == GameType.NUMBER_RUSH && difficulty != null && difficulty.isAdvanced(); }
    /** Hanoi: discs can only move between adjacent towers. */
    public boolean isHanoiAdjacentOnly() { return gameType == GameType.TOWER_OF_HANOI && difficulty != null && difficulty.isMaster(); }

    public int getTotalPairs() { return gameType.getTarget(difficulty); }

    public int getProgressPercent() {
        int target = getTotalPairs();
        if (target <= 0) return 0;
        return Math.min(100, Math.round((matchedPairs * 100f) / target));
    }

    public long getElapsedSeconds() {
        if (startedAt == null) return 0;
        LocalDateTime end = completedAt != null ? completedAt : LocalDateTime.now();
        return Math.max(0, Duration.between(startedAt, end).getSeconds());
    }

    public String getElapsedLabel() {
        long elapsed = getElapsedSeconds();
        long minutes = elapsed / 60;
        long seconds = elapsed % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /** Total time budget for this (gameType, difficulty) combination, in seconds. */
    public long getTimeLimitSeconds() {
        if (difficulty == null || gameType == null) return 0;
        return Math.round(difficulty.getBaseTimeSeconds() * gameType.getTimeMultiplier());
    }

    /** How many seconds remain before auto-loss. Negative means already over. */
    public long getRemainingSeconds() {
        return getTimeLimitSeconds() - getElapsedSeconds();
    }

    public String getRemainingLabel() {
        long remaining = Math.max(0, getRemainingSeconds());
        long minutes = remaining / 60;
        long seconds = remaining % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /** A completed game with score 0 represents a loss (time-out). */
    public boolean isLost() { return completed && score == 0; }
    public boolean isWon() { return completed && score > 0; }
}
