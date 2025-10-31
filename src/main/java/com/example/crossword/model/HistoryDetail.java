package com.example.crossword.model;

import java.io.Serializable;

public class HistoryDetail implements Serializable {
    private int round;
    private String word;
    private String myAnswer;
    private String opponentAnswer;

    public HistoryDetail() {
    }

    public HistoryDetail(int round, String word, String myAnswer, String opponentAnswer) {
        this.round = round;
        this.word = word;
        this.myAnswer = myAnswer;
        this.opponentAnswer = opponentAnswer;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getMyAnswer() {
        return myAnswer;
    }

    public void setMyAnswer(String myAnswer) {
        this.myAnswer = myAnswer;
    }

    public String getOpponentAnswer() {
        return opponentAnswer;
    }

    public void setOpponentAnswer(String opponentAnswer) {
        this.opponentAnswer = opponentAnswer;
    }
}
