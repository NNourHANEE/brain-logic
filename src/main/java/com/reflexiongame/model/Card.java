package com.reflexiongame.model;

public class Card {
    private int position;
    private String symbol;
    private String image;
    private boolean revealed;
    private boolean matched;

    public Card(int position, String symbol, String image, boolean revealed, boolean matched) {
        this.position = position;
        this.symbol = symbol;
        this.image = image;
        this.revealed = revealed;
        this.matched = matched;
    }

    public int getPosition() {
        return position;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getImage() {
        return image;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }
}
