package com.haoyinrui.campusattendance.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.haoyinrui.campusattendance.R;
import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.util.AttendanceRuleManager;
import com.haoyinrui.campusattendance.util.CampusLocationHelper;
import com.haoyinrui.campusattendance.util.ReminderHelper;
import com.haoyinrui.campusattendance.util.SessionManager;

/**
 * 设置 Fragment：配置规则、提醒、定位校验和答辩演示数据。
 */
public class SettingsFragment extends Fragment {
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        ruleManager = new AttendanceRuleManager(requireContext());
        campusLocationHelper = new CampusLocationHelper(requireContext());
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        initViews(view);
        bindEvents(view);
        loadSettings();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ruleManager != null) {
            loadSettings();
        }
    }

    private void initViews(View view) {
        editSignInStart = view.findViewById(R.id.editSignInStart);
        editSignInEnd = view.findViewById(R.id.editSignInEnd);
        editCheckOutStart = view.findViewById(R.id.editCheckOutStart);
        editCheckOutEnd = view.findViewById(R.id.editCheckOutEnd);
        editCampusLatitude = view.findViewById(R.id.editCampusLatitude);
        editCampusLongitude = view.findViewById(R.id.editCampusLongitude);
        editCampusRadius = view.findViewById(R.id.editCampusRadius);
        switchSignInReminder = view.findViewById(R.id.switchSignInReminder);
        switchCheckOutReminder = view.findViewById(R.id.switchCheckOutReminder);
        switchLocationCheck = view.findViewById(R.id.switchLocationCheck);
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.buttonSaveSettings).setOnClickListener(v -> saveSettings());
        view.findViewById(R.id.buttonResetDefaultRules).setOnClickListener(v -> resetDefaults());
        view.findViewById(R.id.buttonGenerateDemoData).setOnClickListener(v -> generateDemoData());
        view.findViewById(R.id.buttonClearAttendanceData).setOnClickListener(v -> confirmClearAttendanceData());
    }

    private void loadSettings() {
        editSignInStart.setText(ruleManager.getSignInStart());
        editSignInEnd.setText(ruleManager.getSignInEnd());
        editCheckOutStart.setText(ruleManager.getCheckOutStart());
        editCheckOutEnd.setText(ruleManager.getCheckOutEnd());
        editCampusLatitude.setText(campusLocationHelper.getLatitude());
        editCampusLongitude.setText(campusLocationHelper.getLongitude());
        editCampusRadius.setText(campusLocationHelper.getRadius());
        switchSignInReminder.setChecked(ReminderHelper.isSignInReminderEnabled(requireContext()));
        switchCheckOutReminder.setChecked(ReminderHelper.isCheckOutReminderEnabled(requireContext()));
        switchLocationCheck.setChecked(campusLocationHelper.isEnabled());
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
            Toast.makeText(requireContext(), "时间格式应为 HH:mm，例如 08:10", Toast.LENGTH_SHORT).show();
            return;
        }
        if (AttendanceRuleManager.compareTime(signInStart, signInEnd) > 0
                || AttendanceRuleManager.compareTime(checkOutStart, checkOutEnd) > 0) {
            Toast.makeText(requireContext(), "开始时间不能晚于截止时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(editCampusLatitude.getText())
                || TextUtils.isEmpty(editCampusLongitude.getText())
                || TextUtils.isEmpty(editCampusRadius.getText())) {
            Toast.makeText(requireContext(), "请填写校园经纬度和半径", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isLocationConfigValid()) {
            Toast.makeText(requireContext(), "校园经纬度或半径格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }

        ruleManager.saveRules(signInStart, signInEnd, checkOutStart, checkOutEnd);
        campusLocationHelper.saveSettings(
                switchLocationCheck.isChecked(),
                editCampusLatitude.getText().toString().trim(),
                editCampusLongitude.getText().toString().trim(),
                editCampusRadius.getText().toString().trim());

        if (switchSignInReminder.isChecked()) {
            ReminderHelper.enableSignInReminder(requireContext(), ruleManager.getSignInStart());
        } else {
            ReminderHelper.disableSignInReminder(requireContext());
        }

        if (switchCheckOutReminder.isChecked()) {
            ReminderHelper.enableCheckOutReminder(requireContext(), ruleManager.getCheckOutStart());
        } else {
            ReminderHelper.disableCheckOutReminder(requireContext());
        }

        Toast.makeText(requireContext(), "考勤设置已保存，切回首页会自动刷新", Toast.LENGTH_SHORT).show();
        loadSettings();
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
        ReminderHelper.disableSignInReminder(requireContext());
        ReminderHelper.disableCheckOutReminder(requireContext());
        ReminderHelper.disableDailyReminder(requireContext());
        loadSettings();
        Toast.makeText(requireContext(), "已恢复默认考勤规则", Toast.LENGTH_SHORT).show();
    }

    private void generateDemoData() {
        databaseHelper.generateQuarterDemoRecords(sessionManager.getUsername());
        Toast.makeText(requireContext(), "已生成最近 7 天演示考勤数据，可切到首页或记录查看", Toast.LENGTH_LONG).show();
    }

    private void confirmClearAttendanceData() {
        new AlertDialog.Builder(requireContext())
                .setTitle("清空考勤记录")
                .setMessage("将清空当前用户的考勤记录，但不会删除账号。是否继续？")
                .setPositiveButton("确认清空", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        databaseHelper.clearAttendanceRecords(sessionManager.getUsername());
                        Toast.makeText(requireContext(), "当前用户考勤记录已清空", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
