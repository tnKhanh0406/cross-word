package com.example.crossword.model;

import java.io.Serializable;

public class GameDetail implements Serializable {
    private int id;
    private Game game;
    private Word word;
    private int round;
    private String player1Answer;
    private String player2Answer;
    private boolean player1Correct;
    private boolean player2Correct;

    public GameDetail() {}

    public GameDetail(int id, Game game, Word word, int round, String player1Answer, String player2Answer, boolean player1Correct, boolean player2Correct) {
        this.id = id;
        this.game = game;
        this.word = word;
        this.round = round;
        this.player1Answer = player1Answer;
        this.player2Answer = player2Answer;
        this.player1Correct = player1Correct;
        this.player2Correct = player2Correct;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        this.word = word;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getPlayer1Answer() {
        return player1Answer;
    }

    public void setPlayer1Answer(String player1Answer) {
        this.player1Answer = player1Answer;
    }

    public String getPlayer2Answer() {
        return player2Answer;
    }

    public void setPlayer2Answer(String player2Answer) {
        this.player2Answer = player2Answer;
    }

    public boolean isPlayer1Correct() {
        return player1Correct;
    }

    public void setPlayer1Correct(boolean player1Correct) {
        this.player1Correct = player1Correct;
    }

    public boolean isPlayer2Correct() {
        return player2Correct;
    }

    public void setPlayer2Correct(boolean player2Correct) {
        this.player2Correct = player2Correct;
    }
}
