package com.example.crossword.model;

import java.io.Serializable;

public class Word implements Serializable {
    private int id;
    private String word;
    private String hint;

    public Word(int id, String word, String hint) {
        this.id = id;
        this.word = word;
        this.hint = hint;
    }

    public Word() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
}
