package com.haoyinrui.campusattendance.model;

/**
 * 月度考勤统计实体，用于首页展示本月签到和完成情况。
 */
public class AttendanceSummary {
    private final int checkInCount;
    private final int completedCount;

    public AttendanceSummary(int checkInCount, int completedCount) {
        this.checkInCount = checkInCount;
        this.completedCount = completedCount;
    }

    public int getCheckInCount() {
        return checkInCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }
}
