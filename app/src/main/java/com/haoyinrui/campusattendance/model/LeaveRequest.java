package com.haoyinrui.campusattendance.model;

/**
 * 请假、补签、销假申请实体。
 */
public class LeaveRequest {
    private final int id;
    private final String username;
    private final String requestType;
    private final String targetDate;
    private final String relatedCourseName;
    private final String reason;
    private final String status;
    private final String createdAt;
    private final String processedAt;
    private final String resultRemark;

    public LeaveRequest(int id, String username, String requestType, String targetDate,
                        String relatedCourseName, String reason, String status,
                        String createdAt, String processedAt, String resultRemark) {
        this.id = id;
        this.username = username;
        this.requestType = requestType;
        this.targetDate = targetDate;
        this.relatedCourseName = relatedCourseName;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.resultRemark = resultRemark;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRequestType() {
        return requestType;
    }

    public String getTargetDate() {
        return targetDate;
    }

    public String getRelatedCourseName() {
        return relatedCourseName;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public String getResultRemark() {
        return resultRemark;
    }
}
