package com.haoyinrui.campusattendance.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
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

import com.google.android.material.snackbar.Snackbar;
import com.haoyinrui.campusattendance.R;
import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.model.AttendanceRecord;
import com.haoyinrui.campusattendance.model.AttendanceStatistics;
import com.haoyinrui.campusattendance.model.CourseSchedule;
import com.haoyinrui.campusattendance.util.AttendanceRuleManager;
import com.haoyinrui.campusattendance.util.AttendanceStatusHelper;
import com.haoyinrui.campusattendance.util.CampusLocationHelper;
import com.haoyinrui.campusattendance.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private static final int REQUEST_LOCATION_PERMISSION = 1002;

    private View rootView;
    private TextView textWelcome;
    private TextView textTodayDate;
    private TextView textStatusSummary;
    private TextView textTodayResult;
    private TextView textTodayScene;
    private TextView textTodayCourseWindow;
    private TextView textTodayCheckIn;
    private TextView textTodayCheckOut;
    private TextView textTodayRemark;
    private TextView textOverviewSummary;
    private Button buttonCheckIn;
    private Button buttonCheckOut;

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private CampusLocationHelper campusLocationHelper;
    private String currentUsername;
    private CourseSchedule currentCourseSchedule;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        campusLocationHelper = new CampusLocationHelper(requireContext());
        currentUsername = sessionManager.getUsername();
        initViews(rootView);
        bindEvents();
        refreshTodayStatus();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            currentUsername = sessionManager.getUsername();
            campusLocationHelper = new CampusLocationHelper(requireContext());
            refreshTodayStatus();
        }
    }

    private void initViews(View view) {
        textWelcome = view.findViewById(R.id.textWelcome);
        textTodayDate = view.findViewById(R.id.textTodayDate);
        textStatusSummary = view.findViewById(R.id.textStatusSummary);
        textTodayResult = view.findViewById(R.id.textTodayResult);
        textTodayScene = view.findViewById(R.id.textTodayScene);
        textTodayCourseWindow = view.findViewById(R.id.textTodayCourseWindow);
        textTodayCheckIn = view.findViewById(R.id.textTodayCheckIn);
        textTodayCheckOut = view.findViewById(R.id.textTodayCheckOut);
        textTodayRemark = view.findViewById(R.id.textTodayRemark);
        textOverviewSummary = view.findViewById(R.id.textOverviewSummary);
        buttonCheckIn = view.findViewById(R.id.buttonCheckIn);
        buttonCheckOut = view.findViewById(R.id.buttonCheckOut);

        buttonCheckIn.setContentDescription("签到");
        buttonCheckOut.setContentDescription("签退");
    }

    private void bindEvents() {
        buttonCheckIn.setOnClickListener(v -> doCheckIn());
        buttonCheckOut.setOnClickListener(v -> doCheckOut());
    }

    private void doCheckIn() {
        CourseSchedule schedule = resolveActiveScheduleForCheckIn();
        String currentTime = getCurrentTime();
        if (AttendanceRuleManager.compareTime(currentTime, schedule.getCheckInStart()) < 0) {
            showMessage("还未到签到时间");
            return;
        }
        if (!isCampusLocationAllowed()) {
            return;
        }

        boolean late = AttendanceRuleManager.compareTime(currentTime, schedule.getCheckInDeadline()) > 0;
        String status = late ? DatabaseHelper.STATUS_LATE : DatabaseHelper.STATUS_CHECKED_IN;
        String remark = late ? "超过签到截止时间" : "";
        boolean success = databaseHelper.checkIn(
                currentUsername,
                getTodayDate(),
                currentTime,
                status,
                remark,
                schedule.getCourseName(),
                getCourseType(schedule),
                schedule.getWeekdayLabel());

        showMessage(success ? (late ? "签到成功，已记为迟到" : "签到成功") : "今天已签到");
        refreshTodayStatus();
    }

    private void doCheckOut() {
        String today = getTodayDate();
        String currentTime = getCurrentTime();
        AttendanceRecord record = databaseHelper.getAttendanceRecord(currentUsername, today);
        if (record == null || !record.hasCheckIn()) {
            showMessage("请先签到");
            return;
        }

        CourseSchedule schedule = resolveScheduleForRecord(record);
        String finalStatus = buildFinalStatus(record, schedule, currentTime);
        String remark = buildFinalRemark(record, finalStatus);
        int result = databaseHelper.checkOut(currentUsername, today, currentTime, finalStatus, remark);

        if (result == 1) {
            showMessage("签退成功");
        } else if (result == 0) {
            showMessage("今天已签退");
        } else {
            showMessage("请先签到");
        }
        refreshTodayStatus();
    }

    public void refreshTodayStatus() {
        if (textWelcome == null || currentUsername == null || currentUsername.isEmpty()) {
            return;
        }

        String today = getTodayDate();
        AttendanceRecord originalRecord = databaseHelper.getAttendanceRecord(currentUsername, today);
        CourseSchedule markSchedule = originalRecord == null
                ? resolveActiveScheduleForCheckIn()
                : resolveScheduleForRecord(originalRecord);
        databaseHelper.markMissingCards(currentUsername, today, getCurrentTime(), markSchedule.getCheckOutDeadline());

        AttendanceRecord record = databaseHelper.getAttendanceRecord(currentUsername, today);
        currentCourseSchedule = record == null
                ? resolveActiveScheduleForCheckIn()
                : resolveScheduleForRecord(record);
        boolean autoMatched = currentCourseSchedule.getId() != -1;
        boolean hasCheckIn = record != null && record.hasCheckIn();
        boolean hasCheckOut = record != null && record.hasCheckOut();
        String statusText = record == null
                ? DatabaseHelper.STATUS_NOT_CHECKED
                : AttendanceStatusHelper.normalizeStatus(record.getStatus());

        textWelcome.setText("你好，" + currentUsername);
        textTodayDate.setText(today);
        textStatusSummary.setText(buildStatusSummary(hasCheckIn, hasCheckOut, statusText, autoMatched));
        textTodayResult.setText(buildTodayResultText(hasCheckIn, hasCheckOut, statusText));
        textTodayScene.setText(currentCourseSchedule.getCourseName());
        textTodayCourseWindow.setText(buildScheduleWindowText(currentCourseSchedule, autoMatched));
        textTodayCheckIn.setText(hasCheckIn ? formatShortTime(record.getCheckInTime()) : "--");
        textTodayCheckOut.setText(hasCheckOut ? formatShortTime(record.getCheckOutTime()) : "--");

        String remark = record == null ? "" : AttendanceStatusHelper.getRemarkOrDefault(record.getRemark());
        if (record != null && AttendanceStatusHelper.isAbnormal(statusText) && remark != null && !remark.isEmpty()) {
            textTodayRemark.setVisibility(View.VISIBLE);
            textTodayRemark.setText(remark);
        } else {
            textTodayRemark.setVisibility(View.GONE);
        }

        applyResultStyle(statusText, hasCheckIn, hasCheckOut);
        buttonCheckIn.setEnabled(!hasCheckIn);
        buttonCheckOut.setEnabled(hasCheckIn && !hasCheckOut);
        buttonCheckIn.setText(hasCheckIn ? "已签到" : "签到");
        buttonCheckOut.setText(hasCheckOut ? "已签退" : "签退");
        refreshOverview();
    }

    private void refreshOverview() {
        String monthPrefix = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        AttendanceStatistics statistics = databaseHelper.getAttendanceStatistics(
                currentUsername,
                monthPrefix,
                getRecentSevenDates());
        int monthAbnormal = statistics.getMonthAbnormalCount();
        int totalAbnormal = statistics.getLateCount()
                + statistics.getEarlyLeaveCount()
                + statistics.getMissingCardCount();

        if (statistics.getTotalCheckInCount() == 0) {
            textOverviewSummary.setText("还没有考勤记录");
            return;
        }

        textOverviewSummary.setText(
                "本月 " + statistics.getMonthCheckInCount() + " 天 · 异常 " + monthAbnormal + " · 缺卡 "
                        + statistics.getMissingCardCount()
                        + "\n累计 " + statistics.getTotalCheckInCount() + " 次 · 异常 " + totalAbnormal
                        + " · 近7天正常 " + countRecentNormalDays());
    }

    private int countRecentNormalDays() {
        int count = 0;
        for (String date : getRecentSevenDates()) {
            AttendanceRecord record = databaseHelper.getAttendanceRecord(currentUsername, date);
            if (record != null && AttendanceStatusHelper.isNormal(record.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private void applyResultStyle(String statusText, boolean hasCheckIn, boolean hasCheckOut) {
        if (!hasCheckIn) {
            textTodayResult.setBackgroundResource(R.drawable.bg_badge_neutral);
            textTodayResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            return;
        }

        if (!hasCheckOut) {
            textTodayResult.setBackgroundResource(R.drawable.bg_badge_normal);
            textTodayResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            return;
        }

        if (AttendanceStatusHelper.isAbnormal(statusText)) {
            textTodayResult.setBackgroundResource(R.drawable.bg_badge_abnormal);
            textTodayResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent));
        } else {
            textTodayResult.setBackgroundResource(R.drawable.bg_badge_normal);
            textTodayResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        }
    }

    private String buildStatusSummary(boolean hasCheckIn, boolean hasCheckOut, String statusText,
                                      boolean autoMatched) {
        if (!hasCheckIn) {
            return autoMatched ? "当前课程待签到" : "当前无匹配课程";
        }
        if (!hasCheckOut) {
            return "已签到，待签退";
        }
        return AttendanceStatusHelper.isAbnormal(statusText) ? "今日异常" : "今日正常";
    }

    private String buildTodayResultText(boolean hasCheckIn, boolean hasCheckOut, String statusText) {
        if (!hasCheckIn) {
            return "未打卡";
        }
        if (!hasCheckOut) {
            return "已签到";
        }
        return AttendanceStatusHelper.normalizeStatus(statusText);
    }

    private String buildScheduleWindowText(CourseSchedule schedule, boolean autoMatched) {
        return (autoMatched ? "自动匹配" : "普通到校")
                + " · 签到 " + formatShortTime(schedule.getCheckInStart())
                + "-" + formatShortTime(schedule.getCheckInDeadline())
                + " · 签退 " + formatShortTime(schedule.getCheckOutStart())
                + "-" + formatShortTime(schedule.getCheckOutDeadline());
    }

    private CourseSchedule resolveActiveScheduleForCheckIn() {
        CourseSchedule schedule = databaseHelper.getRecommendedCourseSchedule(
                Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
                getCurrentTime());
        return schedule != null ? schedule : databaseHelper.getFallbackCourseSchedule();
    }

    private CourseSchedule resolveScheduleForRecord(AttendanceRecord record) {
        if (record != null && record.getCourseName() != null && !record.getCourseName().trim().isEmpty()) {
            List<CourseSchedule> schedules = databaseHelper.getCourseSchedules();
            for (CourseSchedule schedule : schedules) {
                if (record.getCourseName().equals(schedule.getCourseName())) {
                    return schedule;
                }
            }
        }
        return databaseHelper.getFallbackCourseSchedule();
    }

    private String getCourseType(CourseSchedule schedule) {
        return schedule.getId() == -1 ? "日常考勤" : "课程考勤";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            showMessage(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    ? "位置权限已开启"
                    : "未开启位置权限");
        }
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String buildFinalStatus(AttendanceRecord record, CourseSchedule schedule, String checkOutTime) {
        boolean late = record.getStatus() != null && record.getStatus().contains(DatabaseHelper.STATUS_LATE);
        boolean earlyLeave = AttendanceRuleManager.compareTime(checkOutTime, schedule.getCheckOutStart()) < 0;
        boolean missing = AttendanceRuleManager.compareTime(checkOutTime, schedule.getCheckOutDeadline()) > 0;

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
            appendRemark(builder, "早于签退开始时间");
        }
        if (DatabaseHelper.STATUS_MISSING_CARD.equals(finalStatus)) {
            appendRemark(builder, "超过签退截止时间");
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
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            showMessage("请先开启位置权限");
            return false;
        }
        Location location = campusLocationHelper.getLastKnownLocation();
        if (location == null) {
            showMessage("无法获取当前位置");
            return false;
        }
        if (!campusLocationHelper.isInCampus(location)) {
            showMessage("当前位置不在校园打卡范围内");
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

    private String formatShortTime(String timeText) {
        if (timeText == null || timeText.trim().isEmpty() || "--".equals(timeText)) {
            return "--";
        }
        return timeText.length() >= 5 ? timeText.substring(0, 5) : timeText;
    }

    private void showMessage(String message) {
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
