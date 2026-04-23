package com.haoyinrui.campusattendance.model;

/**
 * 考勤记录实体类，对应 SQLite 中 attendance 表的一行数据。
 */
public class AttendanceRecord {
    private final int id;
    private final String username;
    private final String date;
    private final String checkInTime;
    private final String checkOutTime;
    private final String status;
    private final String remark;

    public AttendanceRecord(int id, String username, String date, String checkInTime,
                            String checkOutTime, String status) {
        this(id, username, date, checkInTime, checkOutTime, status, "");
    }

    public AttendanceRecord(int id, String username, String date, String checkInTime,
                            String checkOutTime, String status, String remark) {
        this.id = id;
        this.username = username;
        this.date = date;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.status = status;
        this.remark = remark;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDate() {
        return date;
    }

    public String getCheckInTime() {
        return checkInTime;
    }

    public String getCheckOutTime() {
        return checkOutTime;
    }

    public String getStatus() {
        return status;
    }

    public String getRemark() {
        return remark;
    }

    public boolean hasCheckIn() {
        return checkInTime != null && !checkInTime.isEmpty();
    }

    public boolean hasCheckOut() {
        return checkOutTime != null && !checkOutTime.isEmpty();
    }

    public boolean isAbnormal() {
        return status != null
                && (status.contains("迟到") || status.contains("早退") || status.contains("缺卡"));
    }
}
