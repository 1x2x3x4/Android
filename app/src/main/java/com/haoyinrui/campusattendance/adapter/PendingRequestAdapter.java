package com.haoyinrui.campusattendance.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haoyinrui.campusattendance.R;
import com.haoyinrui.campusattendance.model.LeaveRequest;

import java.util.ArrayList;
import java.util.List;

public class PendingRequestAdapter extends RecyclerView.Adapter<PendingRequestAdapter.PendingViewHolder> {
    public interface OnActionListener {
        void onApprove(LeaveRequest request);
        void onReject(LeaveRequest request);
    }

    private final List<LeaveRequest> requests = new ArrayList<>();
    private OnActionListener actionListener;

    public void setRequests(List<LeaveRequest> newRequests) {
        requests.clear();
        requests.addAll(newRequests);
        notifyDataSetChanged();
    }

    public void setOnActionListener(OnActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public PendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_request, parent, false);
        return new PendingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingViewHolder holder, int position) {
        LeaveRequest request = requests.get(position);
        holder.textTitle.setText(request.getUsername() + " · " + request.getRequestType());
        holder.textMeta.setText(request.getTargetDate() + "  ·  " +
                (request.getRelatedCourseName().isEmpty() ? "普通到校考勤" : request.getRelatedCourseName()));
        holder.textReason.setText(request.getReason());
        holder.buttonApprove.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onApprove(request);
            }
        });
        holder.buttonReject.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onReject(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class PendingViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTitle;
        private final TextView textMeta;
        private final TextView textReason;
        private final View buttonApprove;
        private final View buttonReject;

        PendingViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textPendingTitle);
            textMeta = itemView.findViewById(R.id.textPendingMeta);
            textReason = itemView.findViewById(R.id.textPendingReason);
            buttonApprove = itemView.findViewById(R.id.buttonApproveRequest);
            buttonReject = itemView.findViewById(R.id.buttonRejectRequest);
        }
    }
}
