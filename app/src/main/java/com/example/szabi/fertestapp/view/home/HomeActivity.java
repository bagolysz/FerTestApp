package com.example.szabi.fertestapp.view.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.szabi.fertestapp.R;
import com.example.szabi.fertestapp.model.messages.Conversation;
import com.example.szabi.fertestapp.model.messages.User;
import com.example.szabi.fertestapp.service.UserService;
import com.example.szabi.fertestapp.utils.PreferencesManager;
import com.example.szabi.fertestapp.view.CameraTestActivity;
import com.example.szabi.fertestapp.view.FeedbackActivity;
import com.example.szabi.fertestapp.view.FeedbackResultsActivity;
import com.example.szabi.fertestapp.view.chatroom.ChatActivity;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HomeActivity extends AppCompatActivity implements
        UserListAdapter.ItemClickListener,
        ConversationListAdapter.ItemClickListener {

    private static final String TAG = "HomeActivity";
    public static final String DB_PATH_EXTRA = "dbPathExtra";
    public static final String NAME_EXTRA = "nameExtra";
    public static final String GROUP_EXTRA = "groupExtra";

    private GoogleApiClient googleApiClient;
    private ProgressBar progressBar;
    private FloatingActionButton fab;
    private Button allUsersButton;
    private Button groupsButton;
    private RecyclerView usersListRecyclerView;
    private RecyclerView groupsListRecyclerView;
    private UserListAdapter userListAdapter;
    private ConversationListAdapter conversationListAdapter;
    private List<User> userList;
    private List<Conversation> conversationList;

    private boolean dataLoaded;

    private UserService userService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setSupportActionBar(findViewById(R.id.home_toolbar));

        userService = new UserService(this);

        dataLoaded = false;
        progressBar = findViewById(R.id.home_progress_bar);
        fab = findViewById(R.id.home_fab);
        allUsersButton = findViewById(R.id.tab_button_users);
        groupsButton = findViewById(R.id.tab_button_groups);
        usersListRecyclerView = findViewById(R.id.users_list_layout);
        groupsListRecyclerView = findViewById(R.id.groups_list_layout);

        userList = new ArrayList<>();
        conversationList = new ArrayList<>();

        userListAdapter = new UserListAdapter(userList, this);
        conversationListAdapter = new ConversationListAdapter(conversationList, this);

        usersListRecyclerView.setAdapter(userListAdapter);
        usersListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        groupsListRecyclerView.setAdapter(conversationListAdapter);
        groupsListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        fab.setOnClickListener(fabClickListener);
        allUsersButton.setOnClickListener(allUsersButtonClickListener);
        groupsButton.setOnClickListener(groupsButtonClickListener);

        userService.loadUsers();

        // set progress bar hide
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                            if (!dataLoaded) {
                                dataLoaded = true;
                                showToast("No data to display.");
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                },
                3000
        );
    }

    View.OnClickListener allUsersButtonClickListener = v -> {
        allUsersButton.setTextColor(getResources().getColor(R.color.white));
        groupsButton.setTextColor(getResources().getColor(R.color.trans_white));
        fab.setVisibility(View.INVISIBLE);

        groupsListRecyclerView.setVisibility(View.INVISIBLE);
        usersListRecyclerView.setVisibility(View.VISIBLE);
    };


    View.OnClickListener groupsButtonClickListener = v -> {
        groupsButton.setTextColor(getResources().getColor(R.color.white));
        allUsersButton.setTextColor(getResources().getColor(R.color.trans_white));
        fab.setVisibility(View.VISIBLE);

        usersListRecyclerView.setVisibility(View.INVISIBLE);
        groupsListRecyclerView.setVisibility(View.VISIBLE);
    };


    View.OnClickListener fabClickListener = v -> {
        List<String> nameList = new ArrayList<>();
        for (User u : userList) {
            nameList.add(u.getName());
        }
        boolean[] selectedItems = new boolean[nameList.size()];

        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        builder.setTitle("Add people to conversation:")
                .setMultiChoiceItems(nameList.toArray(new CharSequence[nameList.size()]),
                        selectedItems,
                        (dialog, which, isChecked) -> selectedItems[which] = isChecked)
                .setCancelable(true)
                .setPositiveButton("Create group", (dialog, which) -> {
                    int counter = 0;
                    for (boolean selectedItem : selectedItems) {
                        if (selectedItem)
                            counter++;
                    }
                    if (counter > 1) {
                        List<User> users = new ArrayList<>();
                        for (int i = 0; i < selectedItems.length; i++) {
                            if (selectedItems[i]) {
                                users.add(userList.get(i));
                            }
                        }
                        userService.openOrCreateGroupConversation(users);
                    } else {
                        Toast.makeText(HomeActivity.this, "Please add more people.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    };

    public void checkIfDataLoaded() {
        if (!dataLoaded) {
            dataLoaded = true;
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public void addUserToList(User user) {
        userList.add(user);
        userListAdapter.notifyDataSetChanged();
    }

    public void addConversationToList(Conversation conversation) {
        conversationList.add(conversation);
        conversationListAdapter.notifyDataSetChanged();
    }

    public void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onUserClicked(int position) {
        userService.openOrCreateDualConversation(userList.get(position));
    }

    @Override
    public void onConversationClicked(int position) {
        userService.openOrCreateGroupConversation(conversationList.get(position).users);
    }


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

            case R.id.menu_home_feedback:
                startActivity(new Intent(HomeActivity.this, FeedbackActivity.class));
                break;

            case R.id.menu_home_feedback_results:
                startActivity(new Intent(HomeActivity.this, FeedbackResultsActivity.class));
                break;

            case R.id.menu_home_sign_out:
                Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(status -> {

                });
                PreferencesManager.getInstance().putUser(null);
                finish();

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onStart() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestProfile()
                .requestEmail()
                .build();
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        googleApiClient.connect();
        super.onStart();
    }
}
