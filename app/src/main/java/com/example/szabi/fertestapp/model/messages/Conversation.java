package com.example.szabi.fertestapp.model.messages;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class Conversation {

    public List<User> users;

    public Conversation() {
        users = new ArrayList<>();
    }

    public Conversation(List<User> users) {
        this.users = users;
    }

}
