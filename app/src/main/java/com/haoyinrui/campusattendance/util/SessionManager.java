package com.haoyinrui.campusattendance.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences 会话管理类，用于保存“记住登录状态”。
 */
public class SessionManager {
    private static final String PREF_NAME = "campus_attendance_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_USERNAME = "username";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLogin(String username) {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getUsername() {
        return preferences.getString(KEY_USERNAME, "");
    }

    public void logout() {
        preferences.edit().clear().apply();
    }
}
