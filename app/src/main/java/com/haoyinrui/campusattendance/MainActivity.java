package com.haoyinrui.campusattendance;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.haoyinrui.campusattendance.fragment.HomeFragment;
import com.haoyinrui.campusattendance.fragment.ProfileFragment;
import com.haoyinrui.campusattendance.fragment.RecordFragment;
import com.haoyinrui.campusattendance.fragment.SettingsFragment;
import com.haoyinrui.campusattendance.util.SessionManager;

/**
 * 主页面容器：登录成功后进入，使用底部导航切换四个顶层 Fragment。
 */
public class MainActivity extends AppCompatActivity {
    private HomeFragment homeFragment;
    private RecordFragment recordFragment;
    private SettingsFragment settingsFragment;
    private ProfileFragment profileFragment;
    private Fragment currentFragment;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || sessionManager.getUsername().isEmpty()) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        initFragments();
        bindBottomNavigation();

        if (savedInstanceState == null) {
            switchToFragment(homeFragment);
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentFragment instanceof HomeFragment) {
            ((HomeFragment) currentFragment).refreshTodayStatus();
        } else if (currentFragment instanceof RecordFragment) {
            ((RecordFragment) currentFragment).refreshRecords();
        }
    }

    private void initFragments() {
        homeFragment = new HomeFragment();
        recordFragment = new RecordFragment();
        settingsFragment = new SettingsFragment();
        profileFragment = new ProfileFragment();
    }

    private void bindBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                switchToFragment(homeFragment);
                homeFragment.refreshTodayStatus();
                return true;
            } else if (itemId == R.id.nav_record) {
                switchToFragment(recordFragment);
                recordFragment.refreshRecords();
                return true;
            } else if (itemId == R.id.nav_settings) {
                switchToFragment(settingsFragment);
                return true;
            } else if (itemId == R.id.nav_profile) {
                switchToFragment(profileFragment);
                return true;
            }
            return false;
        });
    }

    /**
     * 使用 show/hide 缓存 Fragment，避免重复创建页面，尽量保留筛选输入等状态。
     */
    private void switchToFragment(@NonNull Fragment targetFragment) {
        if (currentFragment == targetFragment) {
            return;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (!targetFragment.isAdded()) {
            transaction.add(R.id.mainFragmentContainer, targetFragment);
        }
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }
        transaction.show(targetFragment).commit();
        currentFragment = targetFragment;
    }

    @Override
    public void onBackPressed() {
        if (bottomNavigationView != null && bottomNavigationView.getSelectedItemId() != R.id.nav_home) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            return;
        }
        super.onBackPressed();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
