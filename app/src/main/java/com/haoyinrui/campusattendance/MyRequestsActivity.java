package com.haoyinrui.campusattendance;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.haoyinrui.campusattendance.adapter.LeaveRequestAdapter;
import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.model.LeaveRequest;
import com.haoyinrui.campusattendance.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 学生端申请页：请假、补签、销假统一查看。
 */
public class MyRequestsActivity extends AppCompatActivity {
    public static final String EXTRA_REQUEST_TYPE = "extra_request_type";
    public static final String EXTRA_TARGET_DATE = "extra_target_date";
    public static final String EXTRA_COURSE_NAME = "extra_course_name";

    private View rootView;
    private TextView textEmptyView;
    private TextView textRequestSummary;
    private LeaveRequestAdapter adapter;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private boolean requestLaunchedFromIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        rootView = findViewById(R.id.layoutMyRequestsRoot);
        textEmptyView = findViewById(R.id.textRequestEmpty);
        textRequestSummary = findViewById(R.id.textRequestSummary);

        RecyclerView recyclerView = findViewById(R.id.recyclerMyRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaveRequestAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setOnRequestClickListener(this::showRequestDetail);

        findViewById(R.id.buttonBackFromRequests).setOnClickListener(v -> finish());
        findViewById(R.id.buttonApplyLeave).setOnClickListener(v ->
                showRequestForm(DatabaseHelper.REQUEST_TYPE_LEAVE, null, ""));
        findViewById(R.id.buttonApplyCancelLeave).setOnClickListener(v ->
                showRequestForm(DatabaseHelper.REQUEST_TYPE_CANCEL_LEAVE, null, ""));
        loadRequests();
        handleIntentRequest();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }

    private void handleIntentRequest() {
        if (requestLaunchedFromIntent) {
            return;
        }
        String type = getIntent().getStringExtra(EXTRA_REQUEST_TYPE);
        if (TextUtils.isEmpty(type)) {
            return;
        }
        requestLaunchedFromIntent = true;
        showRequestForm(type,
                getIntent().getStringExtra(EXTRA_TARGET_DATE),
                getIntent().getStringExtra(EXTRA_COURSE_NAME));
    }

    private void loadRequests() {
        List<LeaveRequest> requests = databaseHelper.getUserLeaveRequests(sessionManager.getUsername());
        adapter.setRequests(requests);
        textEmptyView.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);

        int pendingCount = 0;
        int approvedCount = 0;
        for (LeaveRequest request : requests) {
            if (DatabaseHelper.REQUEST_STATUS_PENDING.equals(request.getStatus())) {
                pendingCount++;
            }
            if (DatabaseHelper.REQUEST_STATUS_APPROVED.equals(request.getStatus())) {
                approvedCount++;
            }
        }
        textRequestSummary.setText("共 " + requests.size() + " 条 · 待处理 " + pendingCount + " · 已通过 " + approvedCount);
    }

    private void showRequestForm(String requestType, String presetDate, String presetCourseName) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_request_form, null, false);
        TextView textSelectedDate = dialogView.findViewById(R.id.textSelectedRequestDate);
        EditText editCourse = dialogView.findViewById(R.id.editRequestCourse);
        EditText editReason = dialogView.findViewById(R.id.editRequestReason);

        String defaultDate = TextUtils.isEmpty(presetDate) ? getTodayDate() : presetDate;
        textSelectedDate.setText(defaultDate);
        editCourse.setText(presetCourseName == null ? "" : presetCourseName);
        textSelectedDate.setOnClickListener(v -> showDatePicker(textSelectedDate));
        if (!TextUtils.isEmpty(presetDate)) {
            textSelectedDate.setEnabled(false);
        }
        if (DatabaseHelper.REQUEST_TYPE_MAKE_UP.equals(requestType)) {
            editCourse.setEnabled(false);
        }

        new AlertDialog.Builder(this)
                .setTitle(requestType + "申请")
                .setView(dialogView)
                .setPositiveButton("提交", (dialog, which) -> {
                    String targetDate = textSelectedDate.getText().toString().trim();
                    String reason = editReason.getText().toString().trim();
                    String courseName = editCourse.getText().toString().trim();
                    if (targetDate.isEmpty()) {
                        showMessage("请选择日期");
                        return;
                    }
                    if (reason.isEmpty()) {
                        showMessage("请填写原因");
                        return;
                    }
                    boolean success = databaseHelper.submitLeaveRequest(
                            sessionManager.getUsername(),
                            requestType,
                            targetDate,
                            courseName,
                            reason,
                            getNowText());
                    showMessage(success ? "已提交" : "已有待处理申请");
                    if (success) {
                        loadRequests();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDatePicker(TextView targetView) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                targetView.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d",
                        year, month + 1, dayOfMonth)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showRequestDetail(LeaveRequest request) {
        String courseName = request.getRelatedCourseName().isEmpty() ? "普通到校考勤" : request.getRelatedCourseName();
        String message = "日期：" + request.getTargetDate()
                + "\n类型：" + request.getRequestType()
                + "\n课程：" + courseName
                + "\n状态：" + request.getStatus()
                + "\n原因：" + request.getReason()
                + "\n提交时间：" + request.getCreatedAt()
                + "\n处理时间：" + emptyToDash(request.getProcessedAt())
                + "\n处理说明：" + emptyToDash(request.getResultRemark());
        new AlertDialog.Builder(this)
                .setTitle("申请详情")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    private String emptyToDash(String value) {
        return value == null || value.isEmpty() ? "--" : value;
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
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
