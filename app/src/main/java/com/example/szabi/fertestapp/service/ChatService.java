package com.example.szabi.fertestapp.service;

import android.support.annotation.NonNull;

import com.example.szabi.fertestapp.model.messages.Message;
import com.example.szabi.fertestapp.view.chatroom.ChatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

import static com.example.szabi.fertestapp.Configs.DB_CONVERSATION_MESSAGES;

public class ChatService {

    private static final int LOAD_LIMIT = 30;

    private ChatActivity view;
    private DatabaseReference databaseReference;
    private FirebaseUser firebaseUser;

    public ChatService(ChatActivity view, String conversationId) {
        this.view = view;

        databaseReference = FirebaseDatabase.getInstance()
                .getReference(DB_CONVERSATION_MESSAGES).child(conversationId);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        databaseReference.orderByChild("creationTime").limitToLast(LOAD_LIMIT).addChildEventListener(messageEventListener);
    }

    private ChildEventListener messageEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            view.checkDataLoaded();
            Message message = dataSnapshot.getValue(Message.class);
            view.addMessageToList(message);
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    };

    public void pushMessage(String text) {
        Message newMessage = new Message(firebaseUser.getDisplayName(),
                text,
                Objects.requireNonNull(firebaseUser.getPhotoUrl()).toString(),
                System.currentTimeMillis());

        databaseReference.push().setValue(newMessage);
    }

    public FirebaseUser getFirebaseUser() {
        return firebaseUser;
    }
}
