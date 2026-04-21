package com.haoyinrui.campusattendance.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haoyinrui.campusattendance.R;
import com.haoyinrui.campusattendance.model.AttendanceRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView 适配器，用于展示历史考勤记录。
 */
public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.RecordViewHolder> {
    private final List<AttendanceRecord> records = new ArrayList<>();

    public void setRecords(List<AttendanceRecord> newRecords) {
        records.clear();
        records.addAll(newRecords);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        AttendanceRecord record = records.get(position);
        holder.textDate.setText("日期：" + record.getDate());
        holder.textCheckIn.setText("签到：" + displayTime(record.getCheckInTime()));
        holder.textCheckOut.setText("签退：" + displayTime(record.getCheckOutTime()));
        holder.textStatus.setText("状态：" + record.getStatus());
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    private String displayTime(String time) {
        return time == null || time.isEmpty() ? "未记录" : time;
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView textDate;
        TextView textCheckIn;
        TextView textCheckOut;
        TextView textStatus;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textRecordDate);
            textCheckIn = itemView.findViewById(R.id.textRecordCheckIn);
            textCheckOut = itemView.findViewById(R.id.textRecordCheckOut);
            textStatus = itemView.findViewById(R.id.textRecordStatus);
        }
    }
}
