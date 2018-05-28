package com.example.szabi.fertestapp.view.chatroom;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.szabi.fertestapp.R;
import com.example.szabi.fertestapp.model.face.LabelsType;
import com.example.szabi.fertestapp.model.messages.Message;
import com.example.szabi.fertestapp.service.CameraPredictionService;
import com.example.szabi.fertestapp.service.ChatService;
import com.example.szabi.fertestapp.utils.EmojiMapper;
import com.example.szabi.fertestapp.view.home.HomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.example.szabi.fertestapp.Configs.DB_CONVERSATIONS;
import static com.example.szabi.fertestapp.Configs.DB_MESSAGES;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private RecyclerView recyclerView;
    private EditText senderText;
    private ProgressBar progressBar;
    private boolean dataLoaded;

    private MessageListAdapter messageListAdapter;
    private List<Message> messageList;

    private ChatService chatService;
    private CameraPredictionService cameraPredictionService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent creator = getIntent();
        String name = creator.getStringExtra(HomeActivity.NAME_EXTRA);
        String conversationId = creator.getStringExtra(HomeActivity.DB_PATH_EXTRA);
        String users = "";
        if (creator.hasExtra(HomeActivity.GROUP_EXTRA)) {
            users = creator.getStringExtra(HomeActivity.GROUP_EXTRA);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chat with " + name);
            getSupportActionBar().setSubtitle(users);
        }

        cameraPredictionService = new CameraPredictionService(this);
        chatService = new ChatService(this, conversationId);

        dataLoaded = false;

        progressBar = findViewById(R.id.chat_progress_bar);
        recyclerView = findViewById(R.id.message_list_layout);
        senderText = findViewById(R.id.chat_box_edit_text);
        ImageView sendButton = findViewById(R.id.chat_box_send_button);
        sendButton.setOnClickListener(sendButtonClickListener);
        ImageView quickPredictionButton = findViewById(R.id.chat_box_quick_predict);
        quickPredictionButton.setOnClickListener(v -> cameraPredictionService.predictOne());

        messageList = new LinkedList<>();

        messageListAdapter = new MessageListAdapter(messageList, chatService.getFirebaseUser());
        recyclerView.setAdapter(messageListAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
    }

    View.OnClickListener sendButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String text = String.valueOf(senderText.getText());
            if (!text.equals("")) {
                senderText.setText("");
                chatService.pushMessage(text);
            }
        }
    };

    public void addMessageToList(Message message){
        messageList.add(message);
        updateRecyclerViewElements();
    }

    public void checkDataLoaded() {
        if (!dataLoaded) {
            dataLoaded = true;
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public void onPredictionComplete(LabelsType labelsType) {
        runOnUiThread(() -> senderText.setText(EmojiMapper.getInstance().getEmojiCode(labelsType)));
    }

    // SYSTEM_CALLBACK SECTION
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraPredictionService.startBackgroundThread();
        cameraPredictionService.openCamera();
    }

    @Override
    protected void onPause() {
        cameraPredictionService.closeCamera();
        cameraPredictionService.stopBackgroundThread();
        super.onPause();
    }

    private void updateRecyclerViewElements() {
        messageListAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    public void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(ChatActivity.this, msg, Toast.LENGTH_SHORT).show());
    }
}
