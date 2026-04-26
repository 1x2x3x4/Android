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
import com.haoyinrui.campusattendance.model.LeaveRequest;

import java.util.ArrayList;
import java.util.List;

public class LeaveRequestAdapter extends RecyclerView.Adapter<LeaveRequestAdapter.RequestViewHolder> {
    public interface OnRequestClickListener {
        void onRequestClick(LeaveRequest request);
    }

    private final List<LeaveRequest> requests = new ArrayList<>();
    private OnRequestClickListener requestClickListener;

    public void setRequests(List<LeaveRequest> newRequests) {
        requests.clear();
        requests.addAll(newRequests);
        notifyDataSetChanged();
    }

    public void setOnRequestClickListener(OnRequestClickListener requestClickListener) {
        this.requestClickListener = requestClickListener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leave_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        LeaveRequest request = requests.get(position);
        holder.textType.setText(request.getRequestType());
        holder.textDate.setText(request.getTargetDate());
        holder.textCourse.setText(request.getRelatedCourseName().isEmpty() ? "普通到校考勤" : request.getRelatedCourseName());
        holder.textReason.setText(request.getReason());
        holder.textStatus.setText(request.getStatus());

        int background = R.drawable.bg_badge_neutral;
        int textColor = R.color.text_secondary;
        if (DatabaseHelper.REQUEST_STATUS_APPROVED.equals(request.getStatus())) {
            background = R.drawable.bg_badge_normal;
            textColor = R.color.primary;
        } else if (DatabaseHelper.REQUEST_STATUS_REJECTED.equals(request.getStatus())) {
            background = R.drawable.bg_badge_abnormal;
            textColor = R.color.accent;
        }
        holder.textStatus.setBackgroundResource(background);
        holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), textColor));
        holder.itemView.setOnClickListener(v -> {
            if (requestClickListener != null) {
                requestClickListener.onRequestClick(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        private final TextView textType;
        private final TextView textDate;
        private final TextView textCourse;
        private final TextView textReason;
        private final TextView textStatus;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            textType = itemView.findViewById(R.id.textRequestType);
            textDate = itemView.findViewById(R.id.textRequestDate);
            textCourse = itemView.findViewById(R.id.textRequestCourse);
            textReason = itemView.findViewById(R.id.textRequestReason);
            textStatus = itemView.findViewById(R.id.textRequestStatus);
        }
    }
}
