package com.example.szabi.fertestapp.service;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.example.szabi.fertestapp.model.messages.Conversation;
import com.example.szabi.fertestapp.model.messages.User;
import com.example.szabi.fertestapp.utils.PreferencesManager;
import com.example.szabi.fertestapp.view.chatroom.ChatActivity;
import com.example.szabi.fertestapp.view.home.HomeActivity;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import static com.example.szabi.fertestapp.Configs.DB_CONVERSATIONS;
import static com.example.szabi.fertestapp.Configs.DB_GROUPS;
import static com.example.szabi.fertestapp.Configs.DB_NUMBER_OF_CONVERSATIONS;
import static com.example.szabi.fertestapp.Configs.DB_NUMBER_OF_GROUPS;
import static com.example.szabi.fertestapp.Configs.DB_USERS;
import static com.example.szabi.fertestapp.Configs.DB_UTILS;
import static com.example.szabi.fertestapp.view.home.HomeActivity.DB_PATH_EXTRA;
import static com.example.szabi.fertestapp.view.home.HomeActivity.GROUP_EXTRA;
import static com.example.szabi.fertestapp.view.home.HomeActivity.NAME_EXTRA;

public class UserService {

    private HomeActivity view;
    private User currentUser;
    private long numberOfConversations;
    private long numberOfGroups;
    private long groupCounter;
    private long conversationCounter;

    private DatabaseReference userRef;
    private DatabaseReference conversationRef;
    private DatabaseReference groupRef;
    private DatabaseReference utilRef;

    public UserService(HomeActivity view) {
        this.view = view;
        currentUser = PreferencesManager.getInstance().getUser();

        userRef = FirebaseDatabase.getInstance().getReference(DB_USERS);
        conversationRef = FirebaseDatabase.getInstance().getReference(DB_CONVERSATIONS);
        groupRef = FirebaseDatabase.getInstance().getReference(DB_GROUPS);
        utilRef = FirebaseDatabase.getInstance().getReference(DB_UTILS).child(DB_NUMBER_OF_CONVERSATIONS);

        loadNumberOfConversations();
    }

    private void loadNumberOfConversations() {
        utilRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Long val = dataSnapshot.getValue(Long.class);
                if (val != null)
                    numberOfConversations = val;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference(DB_UTILS).child(DB_NUMBER_OF_GROUPS);
        groupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Long val = dataSnapshot.getValue(Long.class);
                if (val != null)
                    numberOfGroups = val;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void openOrCreateGroupConversation(List<User> selectedUsers) {
        if (selectedUsers.size() > 1) {

            boolean contains = false;
            for (User u : selectedUsers) {
                if (u.getEmail().equals(currentUser.getEmail())) {
                    contains = true;
                }
            }
            if (!contains) {
                selectedUsers.add(currentUser);
            }

            if (numberOfGroups == 0) {
                // create new group
                Conversation c = new Conversation(selectedUsers);
                groupRef.push().setValue(c);
                FirebaseDatabase.getInstance().getReference(DB_UTILS).child(DB_NUMBER_OF_GROUPS).setValue(++numberOfGroups);
            } else {
                groupCounter = 0;
                groupRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                        Conversation conversation = dataSnapshot.getValue(Conversation.class);
                        groupCounter++;

                        int c = 0;
                        if (conversation != null) {
                            for (User u : conversation.users) {
                                for (User v : selectedUsers) {
                                    if (u.getEmail().equals(v.getEmail())) {
                                        c++;
                                    }
                                }
                            }
                        }

                        if (c == selectedUsers.size()) {
                            //conversation found
                            //Toast.makeText(view, dataSnapshot.getRef().getKey(), Toast.LENGTH_SHORT).show();
                            groupRef.removeEventListener(this);

                            StringBuilder builder = new StringBuilder("");
                            int size = selectedUsers.size();
                            for (int i = 0; i < size - 1; i++) {
                                builder.append(selectedUsers.get(i).getName()).append(", ");
                            }
                            builder.append(selectedUsers.get(size - 1).getName());

                            Intent intent = new Intent(view, ChatActivity.class);
                            intent.putExtra(DB_PATH_EXTRA, dataSnapshot.getRef().getKey());
                            intent.putExtra(NAME_EXTRA, "group");
                            intent.putExtra(GROUP_EXTRA, builder.toString());
                            view.startActivity(intent);

                        } else {
                            //check if all conversations were tracked
                            if (groupCounter >= numberOfGroups) {
                                groupRef.removeEventListener(this);

                                //initiate new conversation
                                FirebaseDatabase.getInstance().getReference(DB_UTILS).child(DB_NUMBER_OF_GROUPS).setValue(++numberOfGroups);

                                DatabaseReference gRef = groupRef.push();
                                gRef.setValue(new Conversation(selectedUsers));


                                // open the conversation page
                                StringBuilder builder = new StringBuilder("");
                                int size = selectedUsers.size();
                                for (int i = 0; i < size - 1; i++) {
                                    builder.append(selectedUsers.get(i).getName()).append(", ");
                                }
                                builder.append(selectedUsers.get(size - 1).getName());

                                Intent intent = new Intent(view, ChatActivity.class);
                                intent.putExtra(DB_PATH_EXTRA, gRef.getKey());
                                intent.putExtra(NAME_EXTRA, "group");
                                intent.putExtra(GROUP_EXTRA, builder.toString());
                                view.startActivity(intent);
                            }
                        }
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
                });
            }
        }
    }

    public void openOrCreateDualConversation(User nextUser) {
        ArrayList<User> impliedUsers = new ArrayList<>();
        impliedUsers.add(PreferencesManager.getInstance().getUser());
        impliedUsers.add(nextUser);

        if (numberOfConversations == 0) {
            // create new conversation
            Conversation c = new Conversation(impliedUsers);
            conversationRef.push().setValue(c);
            FirebaseDatabase.getInstance().getReference(DB_UTILS).child(DB_NUMBER_OF_CONVERSATIONS).setValue(++numberOfConversations);
        } else {
            conversationCounter = 0;
            conversationRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                    Conversation conversation = dataSnapshot.getValue(Conversation.class);
                    conversationCounter++;

                    int c = 0;
                    if (conversation != null && conversation.users.size() == 2) {
                        for (User u : conversation.users) {
                            for (User v : impliedUsers) {
                                if (u.getEmail().equals(v.getEmail())) {
                                    c++;
                                }
                            }
                        }
                    }

                    if (c == 2) {
                        //conversation found
                        //Toast.makeText(HomeActivity.this, dataSnapshot.getRef().getKey(), Toast.LENGTH_SHORT).show();
                        conversationRef.removeEventListener(this);

                        Intent intent = new Intent(view, ChatActivity.class);
                        intent.putExtra(DB_PATH_EXTRA, dataSnapshot.getRef().getKey());
                        intent.putExtra(NAME_EXTRA, nextUser.getName());
                        view.startActivity(intent);

                    } else {
                        //check if all conversations were tracked
                        if (conversationCounter >= numberOfConversations) {
                            conversationRef.removeEventListener(this);

                            //initiate new conversation
                            FirebaseDatabase.getInstance().getReference(DB_UTILS).child(DB_NUMBER_OF_CONVERSATIONS).setValue(++numberOfConversations);

                            DatabaseReference convRef = conversationRef.push();
                            convRef.setValue(new Conversation(impliedUsers));

                            //start new conversation
                            Intent intent = new Intent(view, ChatActivity.class);
                            intent.putExtra(DB_PATH_EXTRA, convRef.getKey());
                            intent.putExtra(NAME_EXTRA, nextUser.getName());
                            view.startActivity(intent);
                        }
                    }
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
            });
        }
    }

    public void loadUsers() {
        userRef.orderByChild("name").addChildEventListener(userEventListener);
        groupRef.addChildEventListener(groupConversationEventListener);
    }

    private ChildEventListener groupConversationEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            Conversation conversation = dataSnapshot.getValue(Conversation.class);
            if (conversation != null && conversation.users.size() > 2) {
                boolean isInConversation = false;
                for (User u : conversation.users) {
                    if (u.getEmail().equals(currentUser.getEmail())) {
                        isInConversation = true;
                    }
                }

                if (isInConversation) {
                    view.addConversationToList(conversation);
                }
            }
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

    private ChildEventListener userEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            view.checkIfDataLoaded();

            User user = dataSnapshot.getValue(User.class);
            if (!user.getEmail().equals(currentUser.getEmail())) {
                view.addUserToList(user);
            }
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

}
