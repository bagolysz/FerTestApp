package com.example.szabi.fertestapp.view.chatroom;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
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

import java.util.LinkedList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText senderText;
    private ProgressBar progressBar;
    private boolean dataLoaded;
    private TextureView previewWindow;
    private boolean previewVisible;

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

        previewVisible = false;
        previewWindow = findViewById(R.id.preview_window);
        previewWindow.setSurfaceTextureListener(textureListener);

        progressBar = findViewById(R.id.chat_progress_bar);
        recyclerView = findViewById(R.id.message_list_layout);
        senderText = findViewById(R.id.chat_box_edit_text);
        ImageView sendButton = findViewById(R.id.chat_box_send_button);
        ImageView quickPredictionButton = findViewById(R.id.chat_box_quick_predict);


        chatService = new ChatService(this, conversationId);

        dataLoaded = false;
        sendButton.setOnClickListener(sendButtonClickListener);
        quickPredictionButton.setOnClickListener(v -> {
            if (cameraPredictionService != null)
                cameraPredictionService.predictOne();
        });

        messageList = new LinkedList<>();
        messageListAdapter = new MessageListAdapter(messageList, chatService.getFirebaseUser());
        recyclerView.setAdapter(messageListAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
    }

    // when preview Window becomes available start the cameraPredictionService
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            cameraPredictionService = new CameraPredictionService(ChatActivity.this);
            cameraPredictionService.startBackgroundThread();
            cameraPredictionService.openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


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

    public void addMessageToList(Message message) {
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
        runOnUiThread(() -> senderText.append(EmojiMapper.getInstance().getEmojiCode(labelsType)));
    }

    public TextureView getPreviewWindow() {
        return previewWindow;
    }

    // SYSTEM_CALLBACK SECTION
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                break;

            case R.id.menu_chat_preview:
                previewVisible = !previewVisible;
                previewWindow.setVisibility(previewVisible ? View.VISIBLE : View.INVISIBLE);
                if (previewVisible) {
                    item.setTitle(R.string.menu_chat_hide_preview);
                } else {
                    item.setTitle(R.string.menu_chat_show_preview);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraPredictionService != null) {
            cameraPredictionService.startBackgroundThread();
            cameraPredictionService.openCamera();
        }
    }

    @Override
    protected void onPause() {
        if (cameraPredictionService != null) {
            cameraPredictionService.closeCamera();
            cameraPredictionService.stopBackgroundThread();
        }
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
