package com.example.szabi.fertestapp.model.messages;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Message {

    private String name;
    private String text;
    private long creationTime;

    public Message() {

    }

    public Message(String name, String text, long creationTime) {
        this.name = name;
        this.text = text;
        this.creationTime = creationTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }
}
