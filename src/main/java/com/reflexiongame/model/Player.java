package com.reflexiongame.model;

public class Player {
    private Long id;
    private String name;
    private int bestScore;
    private int progression;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getBestScore() { return bestScore; }
    public void setBestScore(int bestScore) { this.bestScore = bestScore; }
    public int getProgression() { return progression; }
    public void setProgression(int progression) { this.progression = progression; }
}
