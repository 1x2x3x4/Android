package com.haoyinrui.campusattendance.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.haoyinrui.campusattendance.R;
import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.model.AttendanceRecord;
import com.haoyinrui.campusattendance.model.AttendanceStatistics;
import com.haoyinrui.campusattendance.model.AttendanceSummary;
import com.haoyinrui.campusattendance.util.AttendanceRuleManager;
import com.haoyinrui.campusattendance.util.AttendanceStatusHelper;
import com.haoyinrui.campusattendance.util.CampusLocationHelper;
import com.haoyinrui.campusattendance.util.ReminderHelper;
import com.haoyinrui.campusattendance.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 首页 Fragment：承载今日考勤、签到签退、统计和提醒入口。
 */
public class HomeFragment extends Fragment {
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private static final int REQUEST_LOCATION_PERMISSION = 1002;

    private TextView textWelcome;
    private TextView textTodayDate;
    private TextView textRuleInfo;
    private TextView textTodayStatus;
    private TextView textMonthSummary;
    private TextView textStatisticsSummary;
    private TextView textRecentSevenDays;
    private Button buttonDailyReminder;
    private Button buttonCheckIn;
    private Button buttonCheckOut;

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private AttendanceRuleManager ruleManager;
    private CampusLocationHelper campusLocationHelper;
    private String currentUsername;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        ruleManager = new AttendanceRuleManager(requireContext());
        campusLocationHelper = new CampusLocationHelper(requireContext());
        currentUsername = sessionManager.getUsername();
        initViews(view);
        bindEvents(view);
        requestNotificationPermissionIfNeeded();
        refreshTodayStatus();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            ruleManager = new AttendanceRuleManager(requireContext());
            campusLocationHelper = new CampusLocationHelper(requireContext());
            refreshTodayStatus();
        }
    }

    private void initViews(View view) {
        textWelcome = view.findViewById(R.id.textWelcome);
        textTodayDate = view.findViewById(R.id.textTodayDate);
        textRuleInfo = view.findViewById(R.id.textRuleInfo);
        textTodayStatus = view.findViewById(R.id.textTodayStatus);
        textMonthSummary = view.findViewById(R.id.textMonthSummary);
        textStatisticsSummary = view.findViewById(R.id.textStatisticsSummary);
        textRecentSevenDays = view.findViewById(R.id.textRecentSevenDays);
        buttonDailyReminder = view.findViewById(R.id.buttonDailyReminder);
        buttonCheckIn = view.findViewById(R.id.buttonCheckIn);
        buttonCheckOut = view.findViewById(R.id.buttonCheckOut);
    }

    private void bindEvents(View view) {
        buttonCheckIn.setOnClickListener(v -> doCheckIn());
        buttonCheckOut.setOnClickListener(v -> doCheckOut());
        view.findViewById(R.id.buttonReminder).setOnClickListener(v -> showReminder());
        buttonDailyReminder.setOnClickListener(v -> toggleDailyReminder());
    }

    private void doCheckIn() {
        String currentTime = getCurrentTime();
        if (ruleManager.isBeforeSignInStart(currentTime)) {
            Toast.makeText(requireContext(), "还未到签到开始时间：" + ruleManager.getSignInStart(), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isCampusLocationAllowed()) {
            return;
        }

        boolean late = ruleManager.isLate(currentTime);
        String status = late ? DatabaseHelper.STATUS_LATE : DatabaseHelper.STATUS_CHECKED_IN;
        String remark = late ? "超过签到截止时间，系统自动判定为迟到" : "";
        boolean success = databaseHelper.checkIn(currentUsername, getTodayDate(), currentTime, status, remark);
        Toast.makeText(requireContext(), success
                ? (late ? "迟到签到已记录" : "签到成功")
                : "今天已经签到过了", Toast.LENGTH_SHORT).show();
        refreshTodayStatus();
    }

    private void doCheckOut() {
        String today = getTodayDate();
        String currentTime = getCurrentTime();
        AttendanceRecord record = databaseHelper.getAttendanceRecord(currentUsername, today);
        String finalStatus = buildFinalStatus(record, currentTime);
        String remark = buildFinalRemark(record, finalStatus);
        int result = databaseHelper.checkOut(currentUsername, today, currentTime, finalStatus, remark);
        if (result == 1) {
            Toast.makeText(requireContext(), "签退成功，考勤结果：" + finalStatus, Toast.LENGTH_SHORT).show();
        } else if (result == 0) {
            Toast.makeText(requireContext(), "今天已经签退过了", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "请先完成签到再签退", Toast.LENGTH_SHORT).show();
        }
        refreshTodayStatus();
    }

    public void refreshTodayStatus() {
        if (textWelcome == null || currentUsername == null || currentUsername.isEmpty()) {
            return;
        }

        String today = getTodayDate();
        databaseHelper.markMissingCards(currentUsername, today, getCurrentTime(), ruleManager.getCheckOutEnd());
        AttendanceRecord record = databaseHelper.getAttendanceRecord(currentUsername, today);

        textWelcome.setText("欢迎你，" + currentUsername);
        textTodayDate.setText("今天日期：" + today);
        textRuleInfo.setText("考勤规则：" + ruleManager.formatForDisplay()
                + (campusLocationHelper.isEnabled() ? "\n定位校验：已开启，" + campusLocationHelper.getDescription() : "\n定位校验：未开启"));

        String checkInText = "未签到";
        String checkOutText = "未签退";
        String statusText = buildNoRecordStatus();
        String remarkText = "无备注";
        boolean hasCheckIn = false;
        boolean hasCheckOut = false;

        if (record != null) {
            hasCheckIn = record.hasCheckIn();
            hasCheckOut = record.hasCheckOut();
            checkInText = hasCheckIn ? record.getCheckInTime() : "未签到";
            checkOutText = hasCheckOut ? record.getCheckOutTime() : "未签退";
            statusText = AttendanceStatusHelper.normalizeStatus(record.getStatus());
            remarkText = AttendanceStatusHelper.getRemarkOrDefault(record.getRemark());
        }

        textTodayStatus.setText("签到时间：" + checkInText
                + "\n签退时间：" + checkOutText
                + "\n今日考勤结果：" + statusText
                + "\n备注：" + remarkText);

        buttonCheckIn.setEnabled(!hasCheckIn);
        buttonCheckOut.setEnabled(hasCheckIn && !hasCheckOut);
        buttonCheckIn.setText(hasCheckIn ? "已签到" : "签到");
        buttonCheckOut.setText(hasCheckOut ? "已签退" : "签退");
        refreshStatistics();
        refreshDailyReminderButton();
    }

    private void refreshStatistics() {
        String monthPrefix = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        AttendanceSummary summary = databaseHelper.getMonthlySummary(currentUsername, monthPrefix);
        textMonthSummary.setText("本月统计：已签到 " + summary.getCheckInCount()
                + " 天，完整打卡 " + summary.getCompletedCount() + " 天");

        AttendanceStatistics statistics = databaseHelper.getAttendanceStatistics(currentUsername, monthPrefix, getRecentSevenDates());
        textStatisticsSummary.setText("累计签到：" + statistics.getTotalCheckInCount()
                + " 次\n正常：" + statistics.getNormalCount()
                + " 次，迟到：" + statistics.getLateCount()
                + " 次，早退：" + statistics.getEarlyLeaveCount()
                + " 次，缺卡：" + statistics.getMissingCardCount()
                + " 次\n本月正常：" + statistics.getMonthNormalCount()
                + " 天，本月异常：" + statistics.getMonthAbnormalCount() + " 天");
        textRecentSevenDays.setText(statistics.getRecentSevenDaysText());
    }

    private void showReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionIfNeeded();
            Toast.makeText(requireContext(), "请先允许通知权限，再演示提醒功能", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean shown = ReminderHelper.showAttendanceNotification(requireContext(), "校园考勤提醒", "请记得完成今天的签到或签退。");
        ReminderHelper.scheduleDemoReminder(requireContext());
        Toast.makeText(requireContext(), shown
                ? "已发送通知，并安排 10 秒后再次提醒"
                : "通知暂未发送，请检查系统通知权限", Toast.LENGTH_LONG).show();
    }

    private void toggleDailyReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionIfNeeded();
            Toast.makeText(requireContext(), "请先允许通知权限，再开启每日提醒", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ReminderHelper.isDailyReminderEnabled(requireContext())) {
            ReminderHelper.disableDailyReminder(requireContext());
            ReminderHelper.disableSignInReminder(requireContext());
            ReminderHelper.disableCheckOutReminder(requireContext());
            Toast.makeText(requireContext(), "已关闭签到和签退提醒", Toast.LENGTH_SHORT).show();
        } else {
            ReminderHelper.enableDailyReminder(requireContext());
            ReminderHelper.enableSignInReminder(requireContext(), ruleManager.getSignInStart());
            ReminderHelper.enableCheckOutReminder(requireContext(), ruleManager.getCheckOutStart());
            Toast.makeText(requireContext(), "已开启签到和签退提醒", Toast.LENGTH_SHORT).show();
        }
        refreshDailyReminderButton();
    }

    private void refreshDailyReminderButton() {
        boolean enabled = ReminderHelper.isSignInReminderEnabled(requireContext())
                || ReminderHelper.isCheckOutReminderEnabled(requireContext())
                || ReminderHelper.isDailyReminderEnabled(requireContext());
        buttonDailyReminder.setText(enabled ? "关闭签到/签退提醒" : "开启签到/签退提醒");
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            Toast.makeText(requireContext(),
                    grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                            ? "通知权限已允许，可正常接收考勤提醒"
                            : "通知权限被拒绝，提醒功能可能无法显示通知",
                    Toast.LENGTH_LONG).show();
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            Toast.makeText(requireContext(),
                    grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                            ? "位置权限已允许，请再次点击签到"
                            : "位置权限被拒绝，开启校园范围校验时无法签到",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String buildNoRecordStatus() {
        String currentTime = getCurrentTime();
        if (ruleManager.isAfterCheckOutEnd(currentTime)) {
            return DatabaseHelper.STATUS_MISSING_CARD;
        }
        if (ruleManager.isLate(currentTime)) {
            return "未签到（已超过签到截止，签到将记为迟到）";
        }
        return DatabaseHelper.STATUS_NOT_CHECKED;
    }

    private String buildFinalStatus(AttendanceRecord record, String checkOutTime) {
        boolean late = record != null && record.getStatus() != null && record.getStatus().contains(DatabaseHelper.STATUS_LATE);
        boolean earlyLeave = ruleManager.isEarlyLeave(checkOutTime);
        boolean missing = ruleManager.isAfterCheckOutEnd(checkOutTime);
        if (missing) {
            return DatabaseHelper.STATUS_MISSING_CARD;
        }
        if (late && earlyLeave) {
            return DatabaseHelper.STATUS_LATE_EARLY;
        }
        if (late) {
            return DatabaseHelper.STATUS_LATE;
        }
        if (earlyLeave) {
            return DatabaseHelper.STATUS_EARLY_LEAVE;
        }
        return DatabaseHelper.STATUS_NORMAL;
    }

    private String buildFinalRemark(AttendanceRecord record, String finalStatus) {
        StringBuilder builder = new StringBuilder();
        if (record != null && record.getRemark() != null && !record.getRemark().isEmpty()) {
            builder.append(record.getRemark());
        }
        if (DatabaseHelper.STATUS_EARLY_LEAVE.equals(finalStatus)
                || DatabaseHelper.STATUS_LATE_EARLY.equals(finalStatus)) {
            appendRemark(builder, "早于签退允许开始时间，系统自动判定为早退");
        }
        if (DatabaseHelper.STATUS_MISSING_CARD.equals(finalStatus)) {
            appendRemark(builder, "超过签退截止时间，系统自动判定为缺卡");
        }
        return builder.toString();
    }

    private void appendRemark(StringBuilder builder, String text) {
        if (builder.length() > 0 && builder.indexOf(text) < 0) {
            builder.append("；");
        }
        if (builder.indexOf(text) < 0) {
            builder.append(text);
        }
    }

    private boolean isCampusLocationAllowed() {
        if (!campusLocationHelper.isEnabled()) {
            return true;
        }
        if (!campusLocationHelper.hasLocationPermission()) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            Toast.makeText(requireContext(), "请允许位置权限后再签到", Toast.LENGTH_SHORT).show();
            return false;
        }
        Location location = campusLocationHelper.getLastKnownLocation();
        if (location == null) {
            Toast.makeText(requireContext(), "暂时无法获取当前位置，请开启系统定位后重试", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!campusLocationHelper.isInCampus(location)) {
            Toast.makeText(requireContext(), "当前位置不在校园打卡范围内", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private List<String> getRecentSevenDates() {
        List<String> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -6);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (int i = 0; i < 7; i++) {
            dates.add(format.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dates;
    }
}
