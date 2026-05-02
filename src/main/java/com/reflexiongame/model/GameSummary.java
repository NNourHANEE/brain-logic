package com.reflexiongame.model;

import java.time.LocalDateTime;

public class GameSummary {
    private Long id;
    private String playerName;
    private Difficulty difficulty;
    private GameType gameType = GameType.MEMORY;
    private int moves;
    private int matchedPairs;
    private int totalPairs;
    private int score;
    private boolean completed;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public GameType getGameType() { return gameType; }
    public void setGameType(GameType gameType) { this.gameType = gameType == null ? GameType.MEMORY : gameType; }
    public int getMoves() { return moves; }
    public void setMoves(int moves) { this.moves = moves; }
    public int getMatchedPairs() { return matchedPairs; }
    public void setMatchedPairs(int matchedPairs) { this.matchedPairs = matchedPairs; }
    public int getTotalPairs() { return totalPairs; }
    public void setTotalPairs(int totalPairs) { this.totalPairs = totalPairs; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getProgressPercent() {
        if (totalPairs == 0) return 0;
        return Math.round((matchedPairs * 100f) / totalPairs);
    }
}
