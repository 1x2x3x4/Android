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
import com.haoyinrui.campusattendance.model.CalendarDayItem;
import com.haoyinrui.campusattendance.util.AttendanceStatusHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 月历网格适配器：使用日期、淡色背景和今日边框表达状态。
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
    private final List<CalendarDayItem> days = new ArrayList<>();
    private OnCalendarDayClickListener listener;

    public interface OnCalendarDayClickListener {
        void onDayClick(CalendarDayItem item);
    }

    public void setDays(List<CalendarDayItem> newDays) {
        days.clear();
        days.addAll(newDays);
        notifyDataSetChanged();
    }

    public void setOnCalendarDayClickListener(OnCalendarDayClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDayItem item = days.get(position);
        AttendanceRecord record = item.getRecord();

        holder.textDay.setText(item.getDayText());

        if (!item.isCurrentMonth()) {
            holder.layoutCell.setBackgroundResource(R.drawable.bg_calendar_day_outside);
            holder.textDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.border));
        } else {
            applyCurrentMonthStyle(holder, item, record);
        }

        holder.itemView.setContentDescription(buildContentDescription(item));
        holder.itemView.setFocusable(item.isCurrentMonth());
        holder.itemView.setEnabled(item.isCurrentMonth());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && item.isCurrentMonth()) {
                listener.onDayClick(item);
            }
        });
    }

    private void applyCurrentMonthStyle(CalendarViewHolder holder, CalendarDayItem item, AttendanceRecord record) {
        String status = record == null ? "" : AttendanceStatusHelper.normalizeStatus(record.getStatus());

        if (item.isToday()) {
            if (record == null) {
                holder.layoutCell.setBackgroundResource(R.drawable.bg_calendar_day_today_default);
            } else if (status.contains(DatabaseHelper.STATUS_MISSING_CARD)) {
                holder.layoutCell.setBackgroundResource(R.drawable.bg_calendar_day_today_missing);
            } else if (AttendanceStatusHelper.isAbnormal(status)) {
                holder.layoutCell.setBackgroundResource(R.drawable.bg_calendar_day_today_abnormal);
            } else {
                holder.layoutCell.setBackgroundResource(R.drawable.bg_calendar_day_today_normal);
            }
            holder.textDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
            return;
        }

        if (record == null) {
            holder.layoutCell.setBackgroundResource(R.drawable.bg_calendar_day_default);
            holder.textDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
        } else if (status.contains(DatabaseHelper.STATUS_MISSING_CARD)) {
            holder.layoutCell.setBackgroundResource(R.drawable.bg_calendar_day_missing);
            holder.textDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.danger));
        } else if (AttendanceStatusHelper.isAbnormal(status)) {
            holder.layoutCell.setBackgroundResource(R.drawable.bg_calendar_day_abnormal);
            holder.textDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent));
        } else {
            holder.layoutCell.setBackgroundResource(R.drawable.bg_calendar_day_normal);
            holder.textDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
        }
    }

    private String buildContentDescription(CalendarDayItem item) {
        if (!item.isCurrentMonth()) {
            return item.getDate() + "，非本月";
        }

        AttendanceRecord record = item.getRecord();
        if (record == null) {
            return item.getDate() + (item.isToday() ? "，今天，无记录" : "，无记录");
        }

        String prefix = item.isToday() ? "今天，" : "";
        String status = AttendanceStatusHelper.normalizeStatus(record.getStatus());
        return item.getDate() + "，" + prefix + status;
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        View layoutCell;
        TextView textDay;

        CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCell = itemView.findViewById(R.id.layoutCalendarCell);
            textDay = itemView.findViewById(R.id.textCalendarDay);
        }
    }
}
