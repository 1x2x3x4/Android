package com.haoyinrui.campusattendance;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.util.AttendanceRuleManager;
import com.haoyinrui.campusattendance.util.CampusLocationHelper;
import com.haoyinrui.campusattendance.util.ReminderHelper;
import com.haoyinrui.campusattendance.util.SessionManager;

/**
 * 考勤设置页：配置时间规则、提醒开关和校园范围校验。
 */
public class AttendanceSettingsActivity extends AppCompatActivity {
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

    private AttendanceRuleManager ruleManager;
    private CampusLocationHelper campusLocationHelper;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

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

    private void initViews() {
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
    }

    private void bindEvents() {
        findViewById(R.id.buttonSaveSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettings();
            }
        });

        findViewById(R.id.buttonBackFromSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.buttonResetDefaultRules).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetDefaults();
            }
        });

        findViewById(R.id.buttonGenerateDemoData).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generateDemoData();
            }
        });

        findViewById(R.id.buttonClearAttendanceData).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmClearAttendanceData();
            }
        });
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
            Toast.makeText(this, "时间格式应为 HH:mm，例如 08:10", Toast.LENGTH_SHORT).show();
            return;
        }
        if (AttendanceRuleManager.compareTime(signInStart, signInEnd) > 0
                || AttendanceRuleManager.compareTime(checkOutStart, checkOutEnd) > 0) {
            Toast.makeText(this, "开始时间不能晚于截止时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(editCampusLatitude.getText())
                || TextUtils.isEmpty(editCampusLongitude.getText())
                || TextUtils.isEmpty(editCampusRadius.getText())) {
            Toast.makeText(this, "请填写校园经纬度和半径", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isLocationConfigValid()) {
            Toast.makeText(this, "校园经纬度或半径格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }

        ruleManager.saveRules(signInStart, signInEnd, checkOutStart, checkOutEnd);
        campusLocationHelper.saveSettings(
                switchLocationCheck.isChecked(),
                editCampusLatitude.getText().toString().trim(),
                editCampusLongitude.getText().toString().trim(),
                editCampusRadius.getText().toString().trim());

        if (switchSignInReminder.isChecked()) {
            ReminderHelper.enableSignInReminder(this, ruleManager.getSignInStart());
        } else {
            ReminderHelper.disableSignInReminder(this);
        }

        if (switchCheckOutReminder.isChecked()) {
            ReminderHelper.enableCheckOutReminder(this, ruleManager.getCheckOutStart());
        } else {
            ReminderHelper.disableCheckOutReminder(this);
        }

        Toast.makeText(this, "考勤设置已保存", Toast.LENGTH_SHORT).show();
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

    private void resetDefaults() {
        ruleManager.resetDefaults();
        campusLocationHelper.resetDefaults();
        ReminderHelper.disableSignInReminder(this);
        ReminderHelper.disableCheckOutReminder(this);
        ReminderHelper.disableDailyReminder(this);
        loadSettings();
        Toast.makeText(this, "已恢复默认考勤规则", Toast.LENGTH_SHORT).show();
    }

    private void generateDemoData() {
        databaseHelper.generateDemoRecords(sessionManager.getUsername());
        Toast.makeText(this, "已生成最近 7 天演示考勤数据", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmClearAttendanceData() {
        new AlertDialog.Builder(this)
                .setTitle("清空考勤记录")
                .setMessage("将清空当前用户的考勤记录，但不会删除账号。是否继续？")
                .setPositiveButton("确认清空", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        databaseHelper.clearAttendanceRecords(sessionManager.getUsername());
                        Toast.makeText(AttendanceSettingsActivity.this, "当前用户考勤记录已清空", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
