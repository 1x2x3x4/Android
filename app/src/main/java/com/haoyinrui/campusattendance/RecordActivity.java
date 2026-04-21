package com.haoyinrui.campusattendance;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.haoyinrui.campusattendance.adapter.AttendanceAdapter;
import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.model.AttendanceRecord;
import com.haoyinrui.campusattendance.util.SessionManager;

import java.util.List;

/**
 * 历史记录页：使用 RecyclerView 展示当前登录用户的全部考勤记录。
 */
public class RecordActivity extends AppCompatActivity {
    private AttendanceAdapter adapter;
    private TextView textEmptyRecord;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        textEmptyRecord = findViewById(R.id.textEmptyRecord);
        RecyclerView recyclerView = findViewById(R.id.recyclerRecords);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter();
        recyclerView.setAdapter(adapter);

        findViewById(R.id.buttonBackFromRecord).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        loadRecords();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecords();
    }

    private void loadRecords() {
        if (sessionManager == null) {
            return;
        }
        List<AttendanceRecord> records = databaseHelper.getAttendanceRecords(sessionManager.getUsername());
        adapter.setRecords(records);
        textEmptyRecord.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
