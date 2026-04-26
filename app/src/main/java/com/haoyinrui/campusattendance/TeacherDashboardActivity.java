package com.haoyinrui.campusattendance;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.haoyinrui.campusattendance.adapter.PendingRequestAdapter;
import com.haoyinrui.campusattendance.adapter.TeacherStudentAdapter;
import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.model.AttendanceRecord;
import com.haoyinrui.campusattendance.model.LeaveRequest;
import com.haoyinrui.campusattendance.model.TeacherStudentStatus;
import com.haoyinrui.campusattendance.util.AttendanceStatusHelper;
import com.haoyinrui.campusattendance.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 教师/管理员演示页：展示班级概览和本地审批流程。
 */
public class TeacherDashboardActivity extends AppCompatActivity {
    private android.view.View rootView;
    private TextView textPresentCount;
    private TextView textLateCount;
    private TextView textMissingCount;
    private TextView textLeaveCount;
    private TextView textAbnormalCount;
    private TextView textStudentEmpty;
    private TextView textPendingEmpty;
    private TeacherStudentAdapter studentAdapter;
    private PendingRequestAdapter requestAdapter;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private android.widget.Spinner spinnerFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        rootView = findViewById(R.id.layoutTeacherRoot);

        textPresentCount = findViewById(R.id.textTeacherPresentCount);
        textLateCount = findViewById(R.id.textTeacherLateCount);
        textMissingCount = findViewById(R.id.textTeacherMissingCount);
        textLeaveCount = findViewById(R.id.textTeacherLeaveCount);
        textAbnormalCount = findViewById(R.id.textTeacherAbnormalCount);
        textStudentEmpty = findViewById(R.id.textTeacherStudentEmpty);
        textPendingEmpty = findViewById(R.id.textTeacherPendingEmpty);
        spinnerFilter = findViewById(R.id.spinnerTeacherFilter);

        RecyclerView recyclerStudents = findViewById(R.id.recyclerTeacherStudents);
        recyclerStudents.setLayoutManager(new LinearLayoutManager(this));
        studentAdapter = new TeacherStudentAdapter();
        recyclerStudents.setAdapter(studentAdapter);

        RecyclerView recyclerPending = findViewById(R.id.recyclerPendingRequests);
        recyclerPending.setLayoutManager(new LinearLayoutManager(this));
        requestAdapter = new PendingRequestAdapter();
        recyclerPending.setAdapter(requestAdapter);
        requestAdapter.setOnActionListener(new PendingRequestAdapter.OnActionListener() {
            @Override
            public void onApprove(LeaveRequest request) {
                confirmRequestAction(request, true);
            }

            @Override
            public void onReject(LeaveRequest request) {
                confirmRequestAction(request, false);
            }
        });

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"全部", "异常", "迟到", "缺卡", "请假"});
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);
        spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                loadTeacherData();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        findViewById(R.id.buttonBackFromTeacher).setOnClickListener(v -> finish());
        loadTeacherData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTeacherData();
    }

    private void loadTeacherData() {
        List<TeacherStudentStatus> allStudents = buildDemoStudents();
        updateSummary(allStudents);
        List<TeacherStudentStatus> filtered = applyFilter(allStudents, spinnerFilter.getSelectedItemPosition());
        studentAdapter.setStudents(filtered);
        textStudentEmpty.setVisibility(filtered.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);

        List<LeaveRequest> pendingRequests = databaseHelper.getPendingLeaveRequests();
        requestAdapter.setRequests(pendingRequests);
        textPendingEmpty.setVisibility(pendingRequests.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private List<TeacherStudentStatus> buildDemoStudents() {
        List<TeacherStudentStatus> students = new ArrayList<>();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        AttendanceRecord record = databaseHelper.getAttendanceRecord(sessionManager.getUsername(), today);
        String status = record == null ? DatabaseHelper.STATUS_NOT_CHECKED
                : AttendanceStatusHelper.normalizeStatus(record.getStatus());
        students.add(new TeacherStudentStatus(
                sessionManager.getUsername(),
                status,
                record == null ? "" : record.getCheckInTime(),
                record == null ? "" : record.getCheckOutTime(),
                AttendanceStatusHelper.isAbnormal(status)));
        students.add(new TeacherStudentStatus("李明", DatabaseHelper.STATUS_NORMAL, "08:01:00", "17:15:00", false));
        students.add(new TeacherStudentStatus("王芳", DatabaseHelper.STATUS_LATE, "08:19:00", "17:16:00", true));
        students.add(new TeacherStudentStatus("张强", DatabaseHelper.STATUS_MISSING_CARD, "08:03:00", "", true));
        students.add(new TeacherStudentStatus("陈雪", DatabaseHelper.STATUS_LEAVE, "", "", false));
        students.add(new TeacherStudentStatus("赵婷", DatabaseHelper.STATUS_EARLY_LEAVE, "08:00:00", "16:10:00", true));
        return students;
    }

    private List<TeacherStudentStatus> applyFilter(List<TeacherStudentStatus> source, int filterIndex) {
        if (filterIndex == 0) {
            return source;
        }
        List<TeacherStudentStatus> result = new ArrayList<>();
        for (TeacherStudentStatus item : source) {
            String status = item.getStatus();
            if (filterIndex == 1 && item.isAbnormal()) {
                result.add(item);
            } else if (filterIndex == 2 && status.contains(DatabaseHelper.STATUS_LATE)) {
                result.add(item);
            } else if (filterIndex == 3 && status.contains(DatabaseHelper.STATUS_MISSING_CARD)) {
                result.add(item);
            } else if (filterIndex == 4 && DatabaseHelper.STATUS_LEAVE.equals(status)) {
                result.add(item);
            }
        }
        return result;
    }

    private void updateSummary(List<TeacherStudentStatus> students) {
        int present = 0;
        int late = 0;
        int missing = 0;
        int leave = 0;
        int abnormal = 0;
        for (TeacherStudentStatus student : students) {
            String status = student.getStatus();
            if (DatabaseHelper.STATUS_NORMAL.equals(status)) {
                present++;
            }
            if (status.contains(DatabaseHelper.STATUS_LATE)) {
                late++;
            }
            if (status.contains(DatabaseHelper.STATUS_MISSING_CARD)) {
                missing++;
            }
            if (DatabaseHelper.STATUS_LEAVE.equals(status)) {
                leave++;
            }
            if (student.isAbnormal()) {
                abnormal++;
            }
        }
        textPresentCount.setText("出勤\n" + present);
        textLateCount.setText("迟到\n" + late);
        textMissingCount.setText("缺卡\n" + missing);
        textLeaveCount.setText("请假\n" + leave);
        textAbnormalCount.setText("异常\n" + abnormal);
    }

    private void confirmRequestAction(LeaveRequest request, boolean approve) {
        String actionText = approve ? "通过" : "驳回";
        new AlertDialog.Builder(this)
                .setTitle(actionText + "申请")
                .setMessage("确认" + actionText + "这条申请？")
                .setPositiveButton(actionText, (dialog, which) -> {
                    boolean success = databaseHelper.updateLeaveRequestStatus(
                            request.getId(),
                            approve ? DatabaseHelper.REQUEST_STATUS_APPROVED : DatabaseHelper.REQUEST_STATUS_REJECTED,
                            getNowText(),
                            "本地演示审批：" + actionText);
                    showMessage(success ? "已" + actionText : "处理失败");
                    if (success) {
                        loadTeacherData();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String getNowText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private void showMessage(String message) {
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
}
