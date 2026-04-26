package com.haoyinrui.campusattendance.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.haoyinrui.campusattendance.MyRequestsActivity;
import com.haoyinrui.campusattendance.R;
import com.haoyinrui.campusattendance.adapter.CalendarAdapter;
import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.model.AttendanceRecord;
import com.haoyinrui.campusattendance.model.CalendarDayItem;
import com.haoyinrui.campusattendance.model.MonthlyReportData;
import com.haoyinrui.campusattendance.util.AttendanceStatusHelper;
import com.haoyinrui.campusattendance.util.SessionManager;
import com.haoyinrui.campusattendance.view.MonthlyStatsChartView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecordFragment extends Fragment {
    private View rootView;
    private CalendarAdapter calendarAdapter;
    private TextView textCalendarMonth;
    private TextView textMonthSummary;
    private TextView textCalendarEmpty;
    private MonthlyStatsChartView chartMonthlyStats;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private Calendar currentMonthCalendar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_record, container, false);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        currentMonthCalendar = Calendar.getInstance();
        initViews(rootView);
        bindEvents(rootView);
        loadCalendarBoard();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCalendarBoard();
    }

    public void refreshRecords() {
        loadCalendarBoard();
    }

    private void initViews(View view) {
        textCalendarMonth = view.findViewById(R.id.textCalendarMonth);
        textMonthSummary = view.findViewById(R.id.textMonthSummary);
        textCalendarEmpty = view.findViewById(R.id.textCalendarEmpty);
        chartMonthlyStats = view.findViewById(R.id.chartMonthlyStats);

        RecyclerView recyclerCalendar = view.findViewById(R.id.recyclerCalendar);
        recyclerCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        recyclerCalendar.setHasFixedSize(true);
        calendarAdapter = new CalendarAdapter();
        recyclerCalendar.setAdapter(calendarAdapter);
        calendarAdapter.setOnCalendarDayClickListener(this::showCalendarDayDetail);

        view.findViewById(R.id.buttonPrevMonth).setContentDescription("上月");
        view.findViewById(R.id.buttonNextMonth).setContentDescription("下月");
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.buttonPrevMonth).setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, -1);
            loadCalendarBoard();
        });
        view.findViewById(R.id.buttonNextMonth).setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, 1);
            loadCalendarBoard();
        });
    }

    private void loadCalendarBoard() {
        if (calendarAdapter == null || currentMonthCalendar == null) {
            return;
        }

        Calendar calendar = (Calendar) currentMonthCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        String monthPrefix = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.getTime());
        textCalendarMonth.setText(monthPrefix);

        Map<String, AttendanceRecord> recordMap = databaseHelper.getMonthlyRecordMap(
                sessionManager.getUsername(), monthPrefix);
        List<CalendarDayItem> days = buildCalendarDays(calendar, monthPrefix, recordMap);
        calendarAdapter.setDays(days);
        textCalendarEmpty.setVisibility(recordMap.isEmpty() ? View.VISIBLE : View.GONE);

        updateMonthOverview(recordMap, monthPrefix);
    }

    private List<CalendarDayItem> buildCalendarDays(Calendar monthCalendar, String monthPrefix,
                                                    Map<String, AttendanceRecord> recordMap) {
        List<CalendarDayItem> days = new ArrayList<>();
        Calendar calendar = (Calendar) monthCalendar.clone();
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        calendar.add(Calendar.DAY_OF_MONTH, -(firstDayOfWeek - 1));

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String today = dateFormat.format(new Date());

        for (int i = 0; i < 42; i++) {
            String date = dateFormat.format(calendar.getTime());
            boolean currentMonth = monthPrefix.equals(monthFormat.format(calendar.getTime()));
            days.add(new CalendarDayItem(
                    date,
                    String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)),
                    currentMonth,
                    today.equals(date),
                    recordMap.get(date)));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    private void updateMonthOverview(Map<String, AttendanceRecord> recordMap, String monthPrefix) {
        int checkInDays = 0;
        int abnormalDays = 0;
        int missingDays = 0;
        int leaveCount = 0;

        for (AttendanceRecord record : recordMap.values()) {
            if (record.hasCheckIn()) {
                checkInDays++;
            }

            String status = AttendanceStatusHelper.normalizeStatus(record.getStatus());
            if (AttendanceStatusHelper.isAbnormal(status)) {
                abnormalDays++;
            }
            if (AttendanceStatusHelper.isLeave(status)) {
                leaveCount++;
            }
            if (status.contains(DatabaseHelper.STATUS_MISSING_CARD)) {
                missingDays++;
            }
        }

        if (recordMap.isEmpty()) {
            textMonthSummary.setText("本月暂无记录");
            chartMonthlyStats.setData(0, 0, 0, 0, 0);
            return;
        }

        textMonthSummary.setText("签到 " + checkInDays + " 天 · 异常 " + abnormalDays + " · 缺卡 " + missingDays);

        MonthlyReportData reportData = databaseHelper.getMonthlyReportData(sessionManager.getUsername(), monthPrefix);
        chartMonthlyStats.setData(
                reportData.getNormalCount(),
                reportData.getLateCount(),
                reportData.getEarlyLeaveCount(),
                reportData.getMissingCount(),
                leaveCount);
    }

    private void showCalendarDayDetail(CalendarDayItem item) {
        if (item.getRecord() == null) {
            showMessage("当天暂无考勤记录");
            return;
        }
        showRecordDetail(item.getRecord());
    }

    private void showRecordDetail(AttendanceRecord record) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_record_detail, null, false);
        dialog.setContentView(sheetView);

        TextView textDetailDateTitle = sheetView.findViewById(R.id.textDetailDateTitle);
        TextView textDetailCourse = sheetView.findViewById(R.id.textDetailCourse);
        TextView textDetailTime = sheetView.findViewById(R.id.textDetailTime);
        TextView textDetailStatus = sheetView.findViewById(R.id.textDetailStatus);
        TextView textDetailRemark = sheetView.findViewById(R.id.textDetailRemark);
        TextView textDetailAppealStatus = sheetView.findViewById(R.id.textDetailAppealStatus);
        TextView textDetailAppealReason = sheetView.findViewById(R.id.textDetailAppealReason);
        TextView textDetailAppealTime = sheetView.findViewById(R.id.textDetailAppealTime);
        TextView textDetailAppealResult = sheetView.findViewById(R.id.textDetailAppealResult);
        EditText editDetailRemark = sheetView.findViewById(R.id.editDetailRemark);
        View buttonSaveRemark = sheetView.findViewById(R.id.buttonSaveRemark);
        View buttonSubmitAppeal = sheetView.findViewById(R.id.buttonSubmitAppeal);
        View buttonMakeUpRequest = sheetView.findViewById(R.id.buttonMakeUpRequest);
        View buttonReviewAppeal = sheetView.findViewById(R.id.buttonReviewAppeal);

        String status = AttendanceStatusHelper.normalizeStatus(record.getStatus());
        textDetailDateTitle.setText(record.getDate());
        textDetailCourse.setText("场景  " + safeText(record.getCourseName()) + "\n类型  " + safeText(record.getCourseType()));
        textDetailTime.setText("签到  " + displayTime(record.getCheckInTime()) + "\n签退  " + displayTime(record.getCheckOutTime()));
        textDetailStatus.setText(status);
        textDetailRemark.setText("备注  " + AttendanceStatusHelper.getRemarkOrDefault(record.getRemark()));
        textDetailAppealStatus.setText("申诉  " + safeText(record.getAppealStatus()));
        textDetailAppealReason.setText("理由  " + emptyToNone(record.getAppealReason()));
        textDetailAppealTime.setText("时间  " + emptyToNone(record.getAppealTime()));
        textDetailAppealResult.setText("结果  " + emptyToNone(record.getAppealResult()));
        editDetailRemark.setText(record.getRemark());

        if (AttendanceStatusHelper.isAbnormal(status)) {
            textDetailStatus.setBackgroundResource(R.drawable.bg_badge_abnormal);
            textDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent));
        } else if (AttendanceStatusHelper.isNormal(status)) {
            textDetailStatus.setBackgroundResource(R.drawable.bg_badge_normal);
            textDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        } else {
            textDetailStatus.setBackgroundResource(R.drawable.bg_badge_neutral);
            textDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        }

        buttonSaveRemark.setOnClickListener(v -> {
            boolean success = databaseHelper.updateRemark(record.getId(), editDetailRemark.getText().toString().trim());
            if (success) {
                showMessage("备注已保存");
                loadCalendarBoard();
                dialog.dismiss();
            } else {
                showMessage("保存失败");
            }
        });

        if (AttendanceStatusHelper.isAbnormal(status) && isAppealAvailable(record)) {
            buttonSubmitAppeal.setVisibility(View.VISIBLE);
            buttonSubmitAppeal.setOnClickListener(v -> showSubmitAppealDialog(record, dialog));
        } else {
            buttonSubmitAppeal.setVisibility(View.GONE);
        }

        if (AttendanceStatusHelper.isAbnormal(status)) {
            buttonMakeUpRequest.setVisibility(View.VISIBLE);
            buttonMakeUpRequest.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MyRequestsActivity.class);
                intent.putExtra(MyRequestsActivity.EXTRA_REQUEST_TYPE, DatabaseHelper.REQUEST_TYPE_MAKE_UP);
                intent.putExtra(MyRequestsActivity.EXTRA_TARGET_DATE, record.getDate());
                intent.putExtra(MyRequestsActivity.EXTRA_COURSE_NAME, record.getCourseName());
                startActivity(intent);
                dialog.dismiss();
            });
        } else {
            buttonMakeUpRequest.setVisibility(View.GONE);
        }

        if (DatabaseHelper.APPEAL_PENDING.equals(record.getAppealStatus())) {
            buttonReviewAppeal.setVisibility(View.VISIBLE);
            buttonReviewAppeal.setOnClickListener(v -> showReviewAppealDialog(record, dialog));
        } else {
            buttonReviewAppeal.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private boolean isAppealAvailable(AttendanceRecord record) {
        String appealStatus = record.getAppealStatus();
        return appealStatus == null
                || appealStatus.isEmpty()
                || DatabaseHelper.APPEAL_NONE.equals(appealStatus);
    }

    private void showSubmitAppealDialog(AttendanceRecord record, BottomSheetDialog dialog) {
        EditText editReason = new EditText(requireContext());
        editReason.setHint("填写申诉原因");
        editReason.setMinLines(3);
        new AlertDialog.Builder(requireContext())
                .setTitle("提交申诉")
                .setView(editReason)
                .setPositiveButton("提交", (dialogInterface, i) -> {
                    String reason = editReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        showMessage("请填写原因");
                        return;
                    }
                    boolean success = databaseHelper.submitAppeal(record.getId(), reason, getCurrentTimeText());
                    if (success) {
                        showMessage("已提交");
                        loadCalendarBoard();
                        dialog.dismiss();
                    } else {
                        showMessage("提交失败");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showReviewAppealDialog(AttendanceRecord record, BottomSheetDialog dialog) {
        new AlertDialog.Builder(requireContext())
                .setTitle("模拟审核")
                .setPositiveButton("通过", (dialogInterface, i) -> {
                    databaseHelper.updateAppealResult(
                            record.getId(),
                            DatabaseHelper.APPEAL_APPROVED,
                            "本地演示审核：已通过");
                    showMessage("已通过");
                    loadCalendarBoard();
                    dialog.dismiss();
                })
                .setNegativeButton("驳回", (dialogInterface, i) -> {
                    databaseHelper.updateAppealResult(
                            record.getId(),
                            DatabaseHelper.APPEAL_REJECTED,
                            "本地演示审核：已驳回");
                    showMessage("已驳回");
                    loadCalendarBoard();
                    dialog.dismiss();
                })
                .setNeutralButton("取消", null)
                .show();
    }

    private String displayTime(String time) {
        return time == null || time.isEmpty() ? "--" : time;
    }

    private String safeText(String text) {
        return text == null || text.isEmpty() ? "无" : text;
    }

    private String emptyToNone(String text) {
        return text == null || text.isEmpty() ? "无" : text;
    }

    private String getCurrentTimeText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private void showMessage(String message) {
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
