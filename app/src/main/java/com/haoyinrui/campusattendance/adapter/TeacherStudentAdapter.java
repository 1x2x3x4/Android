package com.haoyinrui.campusattendance.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.haoyinrui.campusattendance.R;
import com.haoyinrui.campusattendance.model.TeacherStudentStatus;

import java.util.ArrayList;
import java.util.List;

public class TeacherStudentAdapter extends RecyclerView.Adapter<TeacherStudentAdapter.StudentViewHolder> {
    private final List<TeacherStudentStatus> students = new ArrayList<>();

    public void setStudents(List<TeacherStudentStatus> newStudents) {
        students.clear();
        students.addAll(newStudents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_teacher_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        TeacherStudentStatus student = students.get(position);
        holder.textName.setText(student.getStudentName());
        holder.textTime.setText("签到 " + displayTime(student.getCheckInTime())
                + "  ·  签退 " + displayTime(student.getCheckOutTime()));
        holder.textStatus.setText(student.getStatus());
        holder.textStatus.setBackgroundResource(student.isAbnormal()
                ? R.drawable.bg_badge_abnormal : R.drawable.bg_badge_normal);
        holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                student.isAbnormal() ? R.color.accent : R.color.primary));
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    private String displayTime(String value) {
        return value == null || value.isEmpty() ? "--" : value;
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textTime;
        private final TextView textStatus;

        StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textTeacherStudentName);
            textTime = itemView.findViewById(R.id.textTeacherStudentTime);
            textStatus = itemView.findViewById(R.id.textTeacherStudentStatus);
        }
    }
}
