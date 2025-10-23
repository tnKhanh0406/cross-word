package com.example.crossword.model;

import java.io.Serializable;

public class Message implements Serializable {
    private String type;
    private Object content;

    public Message(String type, Object content) {
        this.type = type;
        this.content = content;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public Object getContent() {
        return content;
    }
}
