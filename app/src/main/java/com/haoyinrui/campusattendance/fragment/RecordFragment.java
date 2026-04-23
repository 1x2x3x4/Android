package com.haoyinrui.campusattendance.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.haoyinrui.campusattendance.R;
import com.haoyinrui.campusattendance.adapter.AttendanceAdapter;
import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.model.AttendanceRecord;
import com.haoyinrui.campusattendance.util.AttendanceStatusHelper;
import com.haoyinrui.campusattendance.util.SessionManager;

import java.util.List;

/**
 * 记录 Fragment：保留筛选、异常查看、详情和备注能力。
 */
public class RecordFragment extends Fragment {
    private AttendanceAdapter adapter;
    private TextView textEmptyRecord;
    private EditText editFilterDate;
    private CheckBox checkOnlyAbnormal;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record, container, false);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        initViews(view);
        bindEvents(view);
        loadRecords();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecords();
    }

    public void refreshRecords() {
        loadRecords();
    }

    private void initViews(View view) {
        textEmptyRecord = view.findViewById(R.id.textEmptyRecord);
        editFilterDate = view.findViewById(R.id.editFilterDate);
        checkOnlyAbnormal = view.findViewById(R.id.checkOnlyAbnormal);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerRecords);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AttendanceAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setOnRecordClickListener(this::showRecordDetail);
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.buttonSearchRecord).setOnClickListener(v -> loadRecords());
        view.findViewById(R.id.buttonResetRecordFilter).setOnClickListener(v -> {
            editFilterDate.setText("");
            checkOnlyAbnormal.setChecked(false);
            loadRecords();
        });
    }

    private void loadRecords() {
        if (sessionManager == null || adapter == null) {
            return;
        }
        String dateFilter = editFilterDate == null ? "" : editFilterDate.getText().toString().trim();
        boolean abnormalOnly = checkOnlyAbnormal != null && checkOnlyAbnormal.isChecked();
        List<AttendanceRecord> records = databaseHelper.getAttendanceRecords(sessionManager.getUsername(), dateFilter, abnormalOnly);
        adapter.setRecords(records);
        if (records.isEmpty()) {
            textEmptyRecord.setText(TextUtils.isEmpty(dateFilter) && !abnormalOnly
                    ? "暂无打卡记录，可在设置页生成演示数据"
                    : "没有符合条件的考勤记录");
            textEmptyRecord.setVisibility(View.VISIBLE);
        } else {
            textEmptyRecord.setVisibility(View.GONE);
        }
    }

    private void showRecordDetail(AttendanceRecord record) {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, 0);

        TextView detailText = new TextView(requireContext());
        detailText.setText("日期：" + record.getDate()
                + "\n签到时间：" + displayTime(record.getCheckInTime())
                + "\n签退时间：" + displayTime(record.getCheckOutTime())
                + "\n考勤状态：" + AttendanceStatusHelper.normalizeStatus(record.getStatus())
                + "\n备注信息：" + AttendanceStatusHelper.getRemarkOrDefault(record.getRemark()));
        detailText.setTextSize(15);
        detailText.setLineSpacing(4, 1);
        container.addView(detailText);

        EditText editRemark = new EditText(requireContext());
        editRemark.setHint(record.isAbnormal() ? "可填写异常原因，便于后续申诉说明" : "可补充备注");
        editRemark.setMinLines(2);
        editRemark.setText(record.getRemark());
        container.addView(editRemark);

        new AlertDialog.Builder(requireContext())
                .setTitle("考勤详情")
                .setView(container)
                .setPositiveButton("保存备注", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        boolean success = databaseHelper.updateRemark(record.getId(), editRemark.getText().toString().trim());
                        Toast.makeText(requireContext(), success ? "备注已保存" : "备注保存失败", Toast.LENGTH_SHORT).show();
                        loadRecords();
                    }
                })
                .setNegativeButton("提交申诉", (dialogInterface, i) ->
                        Toast.makeText(requireContext(), "申诉入口已预留：后续可扩展为提交给教师审核", Toast.LENGTH_SHORT).show())
                .setNeutralButton("关闭", null)
                .show();
    }

    private String displayTime(String time) {
        return time == null || time.isEmpty() ? "未记录" : time;
    }
}
