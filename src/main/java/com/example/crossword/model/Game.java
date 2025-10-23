package com.example.crossword.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Game implements Serializable {
    private int id;
    private User player1;
    private User player2;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private User winner;
    private String result;

    public Game() {
    }

    public Game(int id, User player1, User player2, LocalDateTime startTime, LocalDateTime endTime, String status, User winner, String result) {
        this.id = id;
        this.player1 = player1;
        this.player2 = player2;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.winner = winner;
        this.result = result;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getPlayer1() {
        return player1;
    }

    public void setPlayer1(User player1) {
        this.player1 = player1;
    }

    public User getPlayer2() {
        return player2;
    }

    public void setPlayer2(User player2) {
        this.player2 = player2;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getWinner() {
        return winner;
    }

    public void setWinner(User winner) {
        this.winner = winner;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
