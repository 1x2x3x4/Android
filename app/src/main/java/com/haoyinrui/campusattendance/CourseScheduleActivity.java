package com.haoyinrui.campusattendance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.haoyinrui.campusattendance.adapter.CourseScheduleAdapter;
import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.model.CourseSchedule;
import com.haoyinrui.campusattendance.util.AttendanceRuleManager;

/**
 * 课程表管理页：查看并轻量编辑本地课表。
 */
public class CourseScheduleActivity extends AppCompatActivity {
    private View rootView;
    private TextView textEmptyView;
    private CourseScheduleAdapter adapter;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_schedule);
        databaseHelper = new DatabaseHelper(this);
        rootView = findViewById(R.id.layoutCourseScheduleRoot);
        textEmptyView = findViewById(R.id.textCourseScheduleEmpty);

        RecyclerView recyclerView = findViewById(R.id.recyclerCourseSchedule);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseScheduleAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setOnScheduleActionListener(this::showEditDialog);

        findViewById(R.id.buttonBackFromCourseSchedule).setOnClickListener(v -> finish());
        loadSchedules();
    }

    private void loadSchedules() {
        adapter.setSchedules(databaseHelper.getCourseSchedules());
        textEmptyView.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void showEditDialog(CourseSchedule schedule) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_course_schedule_edit, null, false);
        TextView textTitle = dialogView.findViewById(R.id.textCourseDialogTitle);
        EditText editTeacher = dialogView.findViewById(R.id.editScheduleTeacher);
        EditText editClassroom = dialogView.findViewById(R.id.editScheduleClassroom);
        EditText editStart = dialogView.findViewById(R.id.editScheduleStartTime);
        EditText editEnd = dialogView.findViewById(R.id.editScheduleEndTime);
        EditText editCheckInStart = dialogView.findViewById(R.id.editScheduleCheckInStart);
        EditText editCheckInEnd = dialogView.findViewById(R.id.editScheduleCheckInEnd);
        EditText editCheckOutStart = dialogView.findViewById(R.id.editScheduleCheckOutStart);
        EditText editCheckOutEnd = dialogView.findViewById(R.id.editScheduleCheckOutEnd);
        SwitchCompat switchEnabled = dialogView.findViewById(R.id.switchScheduleEnabled);

        textTitle.setText(schedule.getCourseName() + " · " + schedule.getWeekdayLabel());
        editTeacher.setText(schedule.getTeacherName());
        editClassroom.setText(schedule.getClassroom());
        editStart.setText(schedule.getStartTime());
        editEnd.setText(schedule.getEndTime());
        editCheckInStart.setText(schedule.getCheckInStart());
        editCheckInEnd.setText(schedule.getCheckInDeadline());
        editCheckOutStart.setText(schedule.getCheckOutStart());
        editCheckOutEnd.setText(schedule.getCheckOutDeadline());
        switchEnabled.setChecked(schedule.isEnabled());

        new AlertDialog.Builder(this)
                .setTitle("编辑课程表")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String startTime = editStart.getText().toString().trim();
                    String endTime = editEnd.getText().toString().trim();
                    String checkInStart = editCheckInStart.getText().toString().trim();
                    String checkInEnd = editCheckInEnd.getText().toString().trim();
                    String checkOutStart = editCheckOutStart.getText().toString().trim();
                    String checkOutEnd = editCheckOutEnd.getText().toString().trim();

                    if (!isValidTimeGroup(startTime, endTime, checkInStart, checkInEnd, checkOutStart, checkOutEnd)) {
                        showMessage("时间格式不正确");
                        return;
                    }
                    if (AttendanceRuleManager.compareTime(startTime, endTime) > 0
                            || AttendanceRuleManager.compareTime(checkInStart, checkInEnd) > 0
                            || AttendanceRuleManager.compareTime(checkOutStart, checkOutEnd) > 0) {
                        showMessage("开始时间不能晚于结束时间");
                        return;
                    }

                    CourseSchedule updated = new CourseSchedule(
                            schedule.getId(),
                            schedule.getCourseName(),
                            editTeacher.getText().toString().trim(),
                            schedule.getWeekday(),
                            startTime,
                            endTime,
                            checkInStart,
                            checkInEnd,
                            checkOutStart,
                            checkOutEnd,
                            editClassroom.getText().toString().trim(),
                            switchEnabled.isChecked());
                    boolean success = databaseHelper.updateCourseSchedule(updated);
                    showMessage(success ? "已保存" : "保存失败");
                    if (success) {
                        loadSchedules();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean isValidTimeGroup(String... values) {
        for (String value : values) {
            if (!AttendanceRuleManager.isValidTime(value)) {
                return false;
            }
        }
        return true;
    }

    private void showMessage(String message) {
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
}
