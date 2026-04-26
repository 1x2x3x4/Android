package com.haoyinrui.campusattendance;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.util.AttendanceRuleManager;
import com.haoyinrui.campusattendance.util.CampusLocationHelper;
import com.haoyinrui.campusattendance.util.ReminderHelper;
import com.haoyinrui.campusattendance.util.SessionManager;

/**
 * 设置页：保留规则、提醒、定位和演示数据配置。
 */
public class AttendanceSettingsActivity extends AppCompatActivity {
    private static final int REQUEST_NOTIFICATION_PERMISSION = 3001;

    private View rootView;
    private EditText editSignInStart;
    private EditText editSignInEnd;
    private EditText editCheckOutStart;
    private EditText editCheckOutEnd;
    private EditText editCampusLatitude;
    private EditText editCampusLongitude;
    private EditText editCampusRadius;
    private SwitchCompat switchSignInReminder;
    private SwitchCompat switchCheckOutReminder;
    private SwitchCompat switchLocationCheck;
    private TextView textReminderPermissionStatus;
    private TextView textLocationPermissionStatus;

    private AttendanceRuleManager ruleManager;
    private CampusLocationHelper campusLocationHelper;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private boolean pendingSaveAfterPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_settings);

        ruleManager = new AttendanceRuleManager(this);
        campusLocationHelper = new CampusLocationHelper(this);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        initViews();
        loadSettings();
        bindEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionStatus();
    }

    private void initViews() {
        rootView = findViewById(R.id.layoutSettingsRoot);
        editSignInStart = findViewById(R.id.editSignInStart);
        editSignInEnd = findViewById(R.id.editSignInEnd);
        editCheckOutStart = findViewById(R.id.editCheckOutStart);
        editCheckOutEnd = findViewById(R.id.editCheckOutEnd);
        editCampusLatitude = findViewById(R.id.editCampusLatitude);
        editCampusLongitude = findViewById(R.id.editCampusLongitude);
        editCampusRadius = findViewById(R.id.editCampusRadius);
        switchSignInReminder = findViewById(R.id.switchSignInReminder);
        switchCheckOutReminder = findViewById(R.id.switchCheckOutReminder);
        switchLocationCheck = findViewById(R.id.switchLocationCheck);
        textReminderPermissionStatus = findViewById(R.id.textReminderPermissionStatus);
        textLocationPermissionStatus = findViewById(R.id.textLocationPermissionStatus);
    }

    private void loadSettings() {
        editSignInStart.setText(ruleManager.getSignInStart());
        editSignInEnd.setText(ruleManager.getSignInEnd());
        editCheckOutStart.setText(ruleManager.getCheckOutStart());
        editCheckOutEnd.setText(ruleManager.getCheckOutEnd());
        editCampusLatitude.setText(campusLocationHelper.getLatitude());
        editCampusLongitude.setText(campusLocationHelper.getLongitude());
        editCampusRadius.setText(campusLocationHelper.getRadius());
        switchSignInReminder.setChecked(ReminderHelper.isSignInReminderEnabled(this));
        switchCheckOutReminder.setChecked(ReminderHelper.isCheckOutReminderEnabled(this));
        switchLocationCheck.setChecked(campusLocationHelper.isEnabled());
        refreshPermissionStatus();
    }

    private void bindEvents() {
        findViewById(R.id.buttonSaveSettings).setOnClickListener(v -> saveSettings());
        findViewById(R.id.buttonBackFromSettings).setOnClickListener(v -> finish());
        findViewById(R.id.buttonOpenCourseSchedule).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, CourseScheduleActivity.class)));
        findViewById(R.id.buttonResetDefaultRules).setOnClickListener(v -> confirmResetDefaults());
        findViewById(R.id.buttonGenerateDemoData).setOnClickListener(v -> confirmGenerateDemoData());
        findViewById(R.id.buttonClearAttendanceData).setOnClickListener(v -> confirmClearAttendanceData());

        switchSignInReminder.setOnCheckedChangeListener((buttonView, isChecked) -> refreshPermissionStatus());
        switchCheckOutReminder.setOnCheckedChangeListener((buttonView, isChecked) -> refreshPermissionStatus());
        switchLocationCheck.setOnCheckedChangeListener((buttonView, isChecked) -> refreshPermissionStatus());
    }

    private void saveSettings() {
        String signInStart = editSignInStart.getText().toString().trim();
        String signInEnd = editSignInEnd.getText().toString().trim();
        String checkOutStart = editCheckOutStart.getText().toString().trim();
        String checkOutEnd = editCheckOutEnd.getText().toString().trim();

        if (!AttendanceRuleManager.isValidTime(signInStart)
                || !AttendanceRuleManager.isValidTime(signInEnd)
                || !AttendanceRuleManager.isValidTime(checkOutStart)
                || !AttendanceRuleManager.isValidTime(checkOutEnd)) {
            showMessage("时间格式错误");
            return;
        }
        if (AttendanceRuleManager.compareTime(signInStart, signInEnd) > 0
                || AttendanceRuleManager.compareTime(checkOutStart, checkOutEnd) > 0) {
            showMessage("开始时间不能晚于结束时间");
            return;
        }
        if (TextUtils.isEmpty(editCampusLatitude.getText())
                || TextUtils.isEmpty(editCampusLongitude.getText())
                || TextUtils.isEmpty(editCampusRadius.getText())) {
            showMessage("请完善定位参数");
            return;
        }
        if (!isLocationConfigValid()) {
            showMessage("定位参数错误");
            return;
        }

        if (needNotificationPermission()) {
            pendingSaveAfterPermission = true;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            showMessage("请先开启通知权限");
            return;
        }

        applySettings(signInStart, signInEnd, checkOutStart, checkOutEnd);
    }

    private void applySettings(String signInStart, String signInEnd, String checkOutStart, String checkOutEnd) {
        ruleManager.saveRules(signInStart, signInEnd, checkOutStart, checkOutEnd);
        campusLocationHelper.saveSettings(
                switchLocationCheck.isChecked(),
                editCampusLatitude.getText().toString().trim(),
                editCampusLongitude.getText().toString().trim(),
                editCampusRadius.getText().toString().trim());

        if (switchSignInReminder.isChecked()) {
            ReminderHelper.enableSignInReminder(this, signInStart);
        } else {
            ReminderHelper.disableSignInReminder(this);
        }

        if (switchCheckOutReminder.isChecked()) {
            ReminderHelper.enableCheckOutReminder(this, checkOutStart);
        } else {
            ReminderHelper.disableCheckOutReminder(this);
        }

        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean isLocationConfigValid() {
        try {
            double latitude = Double.parseDouble(editCampusLatitude.getText().toString().trim());
            double longitude = Double.parseDouble(editCampusLongitude.getText().toString().trim());
            double radius = Double.parseDouble(editCampusRadius.getText().toString().trim());
            return latitude >= -90 && latitude <= 90
                    && longitude >= -180 && longitude <= 180
                    && radius > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean needNotificationPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && (switchSignInReminder.isChecked() || switchCheckOutReminder.isChecked())
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    private void refreshPermissionStatus() {
        if (textReminderPermissionStatus != null) {
            boolean remindersEnabled = switchSignInReminder.isChecked() || switchCheckOutReminder.isChecked();
            boolean notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
            if (!remindersEnabled) {
                textReminderPermissionStatus.setText("");
            } else if (notificationGranted) {
                textReminderPermissionStatus.setText("通知已开启");
            } else {
                textReminderPermissionStatus.setText("未开启通知权限");
            }
        }

        if (textLocationPermissionStatus != null) {
            if (!switchLocationCheck.isChecked()) {
                textLocationPermissionStatus.setText("");
            } else if (campusLocationHelper.hasLocationPermission()) {
                textLocationPermissionStatus.setText("位置权限已开启");
            } else {
                textLocationPermissionStatus.setText("未开启位置权限");
            }
        }
    }

    private void resetDefaults() {
        ruleManager.resetDefaults();
        campusLocationHelper.resetDefaults();
        ReminderHelper.disableSignInReminder(this);
        ReminderHelper.disableCheckOutReminder(this);
        ReminderHelper.disableDailyReminder(this);
        loadSettings();
        showMessage("已恢复默认");
    }

    private void confirmResetDefaults() {
        new AlertDialog.Builder(this)
                .setTitle("恢复默认")
                .setMessage("确定恢复默认设置？")
                .setPositiveButton("恢复", (dialogInterface, i) -> resetDefaults())
                .setNegativeButton("取消", null)
                .show();
    }

    private void generateDemoData() {
        databaseHelper.generateQuarterDemoRecords(sessionManager.getUsername());
        showMessage("已写入最近三个月演示数据");
    }

    private void confirmGenerateDemoData() {
        new AlertDialog.Builder(this)
                .setTitle("写入演示数据")
                .setMessage("将清空当前用户现有考勤与申请记录，并写入最近三个月模拟数据。是否继续？")
                .setPositiveButton("写入", (dialog, which) -> generateDemoData())
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmClearAttendanceData() {
        new AlertDialog.Builder(this)
                .setTitle("清空记录")
                .setMessage("确定清空当前用户记录？")
                .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        databaseHelper.clearAttendanceRecords(sessionManager.getUsername());
                        showMessage("已清空");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingSaveAfterPermission) {
                    pendingSaveAfterPermission = false;
                    saveSettings();
                }
            } else {
                pendingSaveAfterPermission = false;
                refreshPermissionStatus();
                showMessage("通知权限未开启");
            }
        }
    }

    private void showMessage(String message) {
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
}
