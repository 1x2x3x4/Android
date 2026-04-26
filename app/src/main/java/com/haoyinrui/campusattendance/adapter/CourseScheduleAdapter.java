package com.haoyinrui.campusattendance.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.haoyinrui.campusattendance.R;
import com.haoyinrui.campusattendance.model.CourseSchedule;

import java.util.ArrayList;
import java.util.List;

public class CourseScheduleAdapter extends RecyclerView.Adapter<CourseScheduleAdapter.ScheduleViewHolder> {
    public interface OnScheduleActionListener {
        void onEdit(CourseSchedule schedule);
    }

    private final List<CourseSchedule> schedules = new ArrayList<>();
    private OnScheduleActionListener actionListener;

    public void setSchedules(List<CourseSchedule> newSchedules) {
        schedules.clear();
        schedules.addAll(newSchedules);
        notifyDataSetChanged();
    }

    public void setOnScheduleActionListener(OnScheduleActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        CourseSchedule schedule = schedules.get(position);
        holder.textTitle.setText(schedule.getCourseName());
        holder.textSubtitle.setText(schedule.getWeekdayLabel() + "  " + schedule.getStartTime() + " - " + schedule.getEndTime());
        holder.textMeta.setText(schedule.getClassroom() + "  ·  签到 " + schedule.getCheckInStart()
                + "-" + schedule.getCheckInDeadline()
                + "  ·  签退 " + schedule.getCheckOutStart() + "-" + schedule.getCheckOutDeadline());
        holder.textStatus.setText(schedule.isEnabled() ? "已启用" : "已停用");
        holder.textStatus.setBackgroundResource(schedule.isEnabled()
                ? R.drawable.bg_badge_normal : R.drawable.bg_badge_neutral);
        holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                schedule.isEnabled() ? R.color.primary : R.color.text_secondary));
        holder.buttonEdit.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEdit(schedule);
            }
        });
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTitle;
        private final TextView textSubtitle;
        private final TextView textMeta;
        private final TextView textStatus;
        private final View buttonEdit;

        ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textScheduleTitle);
            textSubtitle = itemView.findViewById(R.id.textScheduleSubtitle);
            textMeta = itemView.findViewById(R.id.textScheduleMeta);
            textStatus = itemView.findViewById(R.id.textScheduleStatus);
            buttonEdit = itemView.findViewById(R.id.buttonEditSchedule);
        }
    }
}
