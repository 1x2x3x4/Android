package com.haoyinrui.campusattendance;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.haoyinrui.campusattendance.util.SessionManager;

/**
 * 启动页：根据 SharedPreferences 中的登录状态决定进入登录页或首页。
 */
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                SessionManager sessionManager = new SessionManager(SplashActivity.this);
                Class<?> targetActivity = sessionManager.isLoggedIn()
                        ? MainActivity.class
                        : LoginActivity.class;
                startActivity(new Intent(SplashActivity.this, targetActivity));
                finish();
            }
        }, 700);
    }
}
