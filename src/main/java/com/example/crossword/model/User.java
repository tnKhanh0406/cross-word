package com.example.crossword.model;

import java.io.Serializable;

public class User implements Serializable {
    private int id;
    private String username;
    private String password;
    private String displayName;
    private double totalPoints;
    private int totalWins;
    private String status;

    public User() {
    }

    public User(int id, String username, String password, String displayName, double totalPoints, int totalWins, String status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.totalPoints = totalPoints;
        this.totalWins = totalWins;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public double getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(double totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
