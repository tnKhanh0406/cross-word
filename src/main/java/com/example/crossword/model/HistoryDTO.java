package com.example.crossword.model;

import java.io.Serializable;

public class HistoryDTO implements Serializable {
    private int game_id;
    private String opponent_name;
    private String result;
    private String time;

    public HistoryDTO() {
    }

    public HistoryDTO(int game_id, String opponent_name, String result, String time) {
        this.game_id = game_id;
        this.opponent_name = opponent_name;
        this.result = result;
        this.time = time;
    }

    public int getGame_id() {
        return game_id;
    }

    public void setGame_id(int game_id) {
        this.game_id = game_id;
    }

    public String getOpponent_name() {
        return opponent_name;
    }

    public void setOpponent_name(String opponent_name) {
        this.opponent_name = opponent_name;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
