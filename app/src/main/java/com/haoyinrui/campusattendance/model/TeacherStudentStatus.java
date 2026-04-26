package com.haoyinrui.campusattendance.model;

/**
 * 教师视角中的学生考勤概览项。
 */
public class TeacherStudentStatus {
    private final String studentName;
    private final String status;
    private final String checkInTime;
    private final String checkOutTime;
    private final boolean abnormal;

    public TeacherStudentStatus(String studentName, String status, String checkInTime,
                                String checkOutTime, boolean abnormal) {
        this.studentName = studentName;
        this.status = status;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.abnormal = abnormal;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStatus() {
        return status;
    }

    public String getCheckInTime() {
        return checkInTime;
    }

    public String getCheckOutTime() {
        return checkOutTime;
    }

    public boolean isAbnormal() {
        return abnormal;
    }
}
