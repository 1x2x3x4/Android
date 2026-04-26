package com.haoyinrui.campusattendance;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.haoyinrui.campusattendance.fragment.HomeFragment;
import com.haoyinrui.campusattendance.fragment.ProfileFragment;
import com.haoyinrui.campusattendance.fragment.RecordFragment;
import com.haoyinrui.campusattendance.util.SessionManager;

/**
 * 主页面容器：登录成功后进入，使用底部导航切换三个顶层 Fragment。
 */
public class MainActivity extends AppCompatActivity {
    private static final String STATE_SELECTED_TAB = "selected_tab";
    private static final String TAG_HOME = "tag_home";
    private static final String TAG_RECORD = "tag_record";
    private static final String TAG_PROFILE = "tag_profile";

    private HomeFragment homeFragment;
    private RecordFragment recordFragment;
    private ProfileFragment profileFragment;
    private Fragment currentFragment;
    private BottomNavigationView bottomNavigationView;
    private OnBackPressedCallback backPressedCallback;

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
        bindBackNavigation();
        currentFragment = findVisibleFragment();

        int selectedTab = savedInstanceState == null
                ? R.id.nav_home
                : savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.nav_home);
        Fragment selectedFragment = getFragmentForMenuItem(selectedTab);

        if (currentFragment == null) {
            switchToFragment(selectedFragment);
        }
        if (bottomNavigationView.getSelectedItemId() != selectedTab) {
            bottomNavigationView.setSelectedItemId(selectedTab);
        } else {
            refreshSelectedTab(selectedTab);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            refreshSelectedTab(bottomNavigationView.getSelectedItemId());
        }
    }

    private void initFragments() {
        homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(TAG_HOME);
        recordFragment = (RecordFragment) getSupportFragmentManager().findFragmentByTag(TAG_RECORD);
        profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag(TAG_PROFILE);

        if (homeFragment == null) {
            homeFragment = new HomeFragment();
        }
        if (recordFragment == null) {
            recordFragment = new RecordFragment();
        }
        if (profileFragment == null) {
            profileFragment = new ProfileFragment();
        }
    }

    private void bindBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment targetFragment = getFragmentForMenuItem(itemId);
            switchToFragment(targetFragment);
            refreshSelectedTab(itemId);
            return true;
        });
        bottomNavigationView.setOnItemReselectedListener(item -> refreshSelectedTab(item.getItemId()));
    }

    private void bindBackNavigation() {
        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (bottomNavigationView != null && bottomNavigationView.getSelectedItemId() != R.id.nav_home) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    /**
     * 使用 show/hide 缓存 Fragment，避免重复创建页面，尽量保留页面状态。
     */
    private void switchToFragment(@NonNull Fragment targetFragment) {
        if (currentFragment == targetFragment) {
            return;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (!targetFragment.isAdded()) {
            transaction.add(R.id.mainFragmentContainer, targetFragment, getFragmentTag(targetFragment));
        }
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }
        transaction.show(targetFragment).commit();
        currentFragment = targetFragment;
    }

    private Fragment getFragmentForMenuItem(int itemId) {
        if (itemId == R.id.nav_record) {
            return recordFragment;
        }
        if (itemId == R.id.nav_profile) {
            return profileFragment;
        }
        return homeFragment;
    }

    private Fragment findVisibleFragment() {
        if (homeFragment != null && homeFragment.isAdded() && !homeFragment.isHidden()) {
            return homeFragment;
        }
        if (recordFragment != null && recordFragment.isAdded() && !recordFragment.isHidden()) {
            return recordFragment;
        }
        if (profileFragment != null && profileFragment.isAdded() && !profileFragment.isHidden()) {
            return profileFragment;
        }
        return null;
    }

    private void refreshSelectedTab(int itemId) {
        if (itemId == R.id.nav_home) {
            homeFragment.refreshTodayStatus();
        } else if (itemId == R.id.nav_record) {
            recordFragment.refreshRecords();
        }
    }

    private String getFragmentTag(@NonNull Fragment fragment) {
        if (fragment instanceof HomeFragment) {
            return TAG_HOME;
        }
        if (fragment instanceof RecordFragment) {
            return TAG_RECORD;
        }
        return TAG_PROFILE;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNavigationView != null) {
            outState.putInt(STATE_SELECTED_TAB, bottomNavigationView.getSelectedItemId());
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
