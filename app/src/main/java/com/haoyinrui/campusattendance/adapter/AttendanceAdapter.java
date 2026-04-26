package com.haoyinrui.campusattendance.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.haoyinrui.campusattendance.R;
import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.model.AttendanceRecord;
import com.haoyinrui.campusattendance.util.AttendanceStatusHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 考勤记录列表适配器：突出日期、状态、课程场景和签到签退摘要，便于快速扫读。
 */
public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.RecordViewHolder> {
    private final List<AttendanceRecord> records = new ArrayList<>();
    private OnRecordClickListener onRecordClickListener;

    public interface OnRecordClickListener {
        void onRecordClick(AttendanceRecord record);
    }

    public void setRecords(List<AttendanceRecord> newRecords) {
        records.clear();
        records.addAll(newRecords);
        notifyDataSetChanged();
    }

    public void setOnRecordClickListener(OnRecordClickListener listener) {
        onRecordClickListener = listener;
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
        String status = AttendanceStatusHelper.normalizeStatus(record.getStatus());

        holder.textDate.setText(record.getDate());
        holder.textStatus.setText(status);
        holder.textCourse.setText("场景：" + safeText(record.getCourseName()));
        holder.textTimeSummary.setText("签到 " + displayTime(record.getCheckInTime())
                + "  ·  签退 " + displayTime(record.getCheckOutTime()));

        if (AttendanceStatusHelper.isNormal(status)) {
            holder.textStatus.setBackgroundResource(R.drawable.bg_badge_normal);
            holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
        } else if (AttendanceStatusHelper.isAbnormal(status)) {
            holder.textStatus.setBackgroundResource(R.drawable.bg_badge_abnormal);
            holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent));
        } else {
            holder.textStatus.setBackgroundResource(R.drawable.bg_badge_neutral);
            holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
        }

        if (record.getAppealStatus() != null
                && !record.getAppealStatus().isEmpty()
                && !DatabaseHelper.APPEAL_NONE.equals(record.getAppealStatus())) {
            holder.textAppeal.setVisibility(View.VISIBLE);
            holder.textAppeal.setText("申诉：" + record.getAppealStatus());
        } else {
            holder.textAppeal.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onRecordClickListener != null) {
                onRecordClickListener.onRecordClick(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    private String displayTime(String time) {
        return time == null || time.isEmpty() ? "--" : time;
    }

    private String safeText(String text) {
        return text == null || text.isEmpty() ? "普通到校考勤" : text;
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView textDate;
        TextView textStatus;
        TextView textCourse;
        TextView textTimeSummary;
        TextView textAppeal;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textRecordDate);
            textStatus = itemView.findViewById(R.id.textRecordStatus);
            textCourse = itemView.findViewById(R.id.textRecordCourse);
            textTimeSummary = itemView.findViewById(R.id.textRecordTimeSummary);
            textAppeal = itemView.findViewById(R.id.textRecordAppeal);
        }
    }
}
