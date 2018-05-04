package com.example.szabi.fertestapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.szabi.fertestapp.R;
import com.example.szabi.fertestapp.model.messages.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.LinkedList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private static final String MESSAGES = "messages";

    private RecyclerView recyclerView;
    private EditText senderText;
    private Button sendButton;
    private ProgressBar progressBar;
    private boolean dataLoaded;

    private MessageListAdapter messageListAdapter;
    private LinearLayoutManager layoutManager;
    private List<Message> messageList;

    private DatabaseReference databaseReference;
    private FirebaseUser firebaseUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        dataLoaded = false;
        databaseReference = FirebaseDatabase.getInstance().getReference(MESSAGES);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();


        progressBar = findViewById(R.id.progress_bar);
        recyclerView = findViewById(R.id.message_list_layout);
        senderText = findViewById(R.id.chat_box_edit_text);
        sendButton = findViewById(R.id.chat_box_send_button);
        sendButton.setOnClickListener(sendButtonClickListener);

        messageList = new LinkedList<>();

        messageListAdapter = new MessageListAdapter(this, messageList, firebaseUser);
        recyclerView.setAdapter(messageListAdapter);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);


        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (!dataLoaded) {
                    dataLoaded = true;
                    progressBar.setVisibility(View.INVISIBLE);
                }

                Log.d(TAG, dataSnapshot.toString());
                Message message = dataSnapshot.getValue(Message.class);
                messageList.add(message);
                updateRecyclerViewElements();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "child event error");
            }
        };
        databaseReference.addChildEventListener(childEventListener);

    }

    View.OnClickListener sendButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String text = String.valueOf(senderText.getText());
            if (!text.equals("")) {
                senderText.setText("");
                Message newMessage = new Message(firebaseUser.getDisplayName(), text, System.currentTimeMillis());
                databaseReference.push().setValue(newMessage);
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_home_camera_test:
                startActivity(new Intent(HomeActivity.this, CameraTestActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
    }

    private void updateRecyclerViewElements() {
        messageListAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void showToast(String msg) {
        Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
}
