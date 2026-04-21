package com.haoyinrui.campusattendance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.model.AttendanceRecord;
import com.haoyinrui.campusattendance.model.AttendanceSummary;
import com.haoyinrui.campusattendance.util.ReminderHelper;
import com.haoyinrui.campusattendance.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 首页：显示今日考勤状态，并完成签到、签退、页面跳转和提醒演示。
 */
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private TextView textWelcome;
    private TextView textTodayDate;
    private TextView textTodayStatus;
    private TextView textMonthSummary;
    private Button buttonDailyReminder;
    private Button buttonCheckIn;
    private Button buttonCheckOut;

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        currentUsername = sessionManager.getUsername();

        if (!sessionManager.isLoggedIn() || currentUsername.isEmpty()) {
            goToLogin();
            return;
        }

        initViews();
        requestNotificationPermissionIfNeeded();
        bindEvents();
        refreshTodayStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            refreshTodayStatus();
        }
    }

    private void initViews() {
        textWelcome = findViewById(R.id.textWelcome);
        textTodayDate = findViewById(R.id.textTodayDate);
        textTodayStatus = findViewById(R.id.textTodayStatus);
        textMonthSummary = findViewById(R.id.textMonthSummary);
        buttonDailyReminder = findViewById(R.id.buttonDailyReminder);
        buttonCheckIn = findViewById(R.id.buttonCheckIn);
        buttonCheckOut = findViewById(R.id.buttonCheckOut);
    }

    private void bindEvents() {
        buttonCheckIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doCheckIn();
            }
        });

        buttonCheckOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doCheckOut();
            }
        });

        findViewById(R.id.buttonRecords).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, RecordActivity.class));
            }
        });

        findViewById(R.id.buttonProfile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            }
        });

        findViewById(R.id.buttonReminder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showReminder();
            }
        });

        buttonDailyReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleDailyReminder();
            }
        });
    }

    private void doCheckIn() {
        boolean success = databaseHelper.checkIn(currentUsername, getTodayDate(), getCurrentTime());
        if (success) {
            Toast.makeText(this, "签到成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "今天已经签到过了", Toast.LENGTH_SHORT).show();
        }
        refreshTodayStatus();
    }

    private void doCheckOut() {
        int result = databaseHelper.checkOut(currentUsername, getTodayDate(), getCurrentTime());
        if (result == 1) {
            Toast.makeText(this, "签退成功", Toast.LENGTH_SHORT).show();
        } else if (result == 0) {
            Toast.makeText(this, "今天已经签退过了", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "请先完成签到再签退", Toast.LENGTH_SHORT).show();
        }
        refreshTodayStatus();
    }

    private void refreshTodayStatus() {
        if (textWelcome == null) {
            return;
        }

        String today = getTodayDate();
        AttendanceRecord record = databaseHelper.getAttendanceRecord(currentUsername, today);

        textWelcome.setText("欢迎你，" + currentUsername);
        textTodayDate.setText("今天日期：" + today);

        String checkInText = "未签到";
        String checkOutText = "未签退";
        String statusText = DatabaseHelper.STATUS_NOT_CHECKED;
        boolean hasCheckIn = false;
        boolean hasCheckOut = false;

        if (record != null) {
            hasCheckIn = record.hasCheckIn();
            hasCheckOut = record.hasCheckOut();
            checkInText = hasCheckIn ? record.getCheckInTime() : "未签到";
            checkOutText = hasCheckOut ? record.getCheckOutTime() : "未签退";
            statusText = record.getStatus();
        }

        textTodayStatus.setText("签到时间：" + checkInText
                + "\n签退时间：" + checkOutText
                + "\n当前状态：" + statusText);

        buttonCheckIn.setEnabled(!hasCheckIn);
        buttonCheckOut.setEnabled(hasCheckIn && !hasCheckOut);
        buttonCheckIn.setText(hasCheckIn ? "已签到" : "签到");
        buttonCheckOut.setText(hasCheckOut ? "已签退" : "签退");

        refreshMonthSummary();
        refreshDailyReminderButton();
    }

    private void refreshMonthSummary() {
        String monthPrefix = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        AttendanceSummary summary = databaseHelper.getMonthlySummary(currentUsername, monthPrefix);
        textMonthSummary.setText("本月统计：已签到 " + summary.getCheckInCount()
                + " 天，完整打卡 " + summary.getCompletedCount() + " 天");
    }

    private void showReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionIfNeeded();
            Toast.makeText(this, "请先允许通知权限，再演示提醒功能", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean shown = ReminderHelper.showAttendanceNotification(
                this,
                "校园考勤提醒",
                "请记得完成今天的签到或签退。");
        ReminderHelper.scheduleDemoReminder(this);

        if (shown) {
            Toast.makeText(this, "已发送通知，并安排 10 秒后再次提醒", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "通知暂未发送，请检查系统通知权限", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleDailyReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionIfNeeded();
            Toast.makeText(this, "请先允许通知权限，再开启每日提醒", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ReminderHelper.isDailyReminderEnabled(this)) {
            ReminderHelper.disableDailyReminder(this);
            Toast.makeText(this, "已关闭每日 8:00 打卡提醒", Toast.LENGTH_SHORT).show();
        } else {
            ReminderHelper.enableDailyReminder(this);
            Toast.makeText(this, "已开启每日 8:00 打卡提醒", Toast.LENGTH_SHORT).show();
        }
        refreshDailyReminderButton();
    }

    private void refreshDailyReminderButton() {
        if (buttonDailyReminder == null) {
            return;
        }
        boolean enabled = ReminderHelper.isDailyReminderEnabled(this);
        buttonDailyReminder.setText(enabled ? "关闭每日 8:00 提醒" : "开启每日 8:00 提醒");
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
