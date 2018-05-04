package com.example.szabi.fertestapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.szabi.fertestapp.model.messages.User;
import com.google.gson.Gson;

public class PreferencesManager {

    private static final String SETTINGS_NAME = "default_settings";
    private static PreferencesManager preferencesManager;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private enum Key {LOGIN_USER, TEST_INT}

    private PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
    }

    public static PreferencesManager getInstance(Context context) {
        if (preferencesManager == null) {
            preferencesManager = new PreferencesManager(context.getApplicationContext());
        }
        return preferencesManager;
    }

    public static PreferencesManager getInstance() {
        if (preferencesManager != null) {
            return preferencesManager;
        }

        throw new IllegalArgumentException("Should use getInstance(Context) at least once before using this method.");
    }

    public void putUser(User user){
        doEdit();
        String json = new Gson().toJson(user);
        editor.putString(Key.LOGIN_USER.toString(), json);
        doCommit();
    }

    public User getUser(){
        String json = prefs.getString(Key.LOGIN_USER.toString(), null);
        Gson gson = new Gson();
        return gson.fromJson(json, User.class);
    }

    public void putNumber(int number) {
        doEdit();
        editor.putInt(Key.TEST_INT.toString(), number);
        doCommit();
    }

    public int getNumber() {
        return prefs.getInt(Key.TEST_INT.toString(), 0);
    }


    private void doEdit() {
        if (editor == null) {
            editor = prefs.edit();
        }
    }

    private void doCommit() {
        if (editor != null) {
            editor.commit();
            editor = null;
        }
    }
}

