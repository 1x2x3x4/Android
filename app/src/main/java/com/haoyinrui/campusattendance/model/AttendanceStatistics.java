package com.haoyinrui.campusattendance.model;

/**
 * 考勤统计实体，集中保存总览、本月和最近 7 天统计结果。
 */
public class AttendanceStatistics {
    private final int totalCheckInCount;
    private final int normalCount;
    private final int lateCount;
    private final int earlyLeaveCount;
    private final int missingCardCount;
    private final int monthCheckInCount;
    private final int monthNormalCount;
    private final int monthAbnormalCount;
    private final String recentSevenDaysText;

    public AttendanceStatistics(int totalCheckInCount, int normalCount, int lateCount,
                                int earlyLeaveCount, int missingCardCount,
                                int monthCheckInCount, int monthNormalCount,
                                int monthAbnormalCount, String recentSevenDaysText) {
        this.totalCheckInCount = totalCheckInCount;
        this.normalCount = normalCount;
        this.lateCount = lateCount;
        this.earlyLeaveCount = earlyLeaveCount;
        this.missingCardCount = missingCardCount;
        this.monthCheckInCount = monthCheckInCount;
        this.monthNormalCount = monthNormalCount;
        this.monthAbnormalCount = monthAbnormalCount;
        this.recentSevenDaysText = recentSevenDaysText;
    }

    public int getTotalCheckInCount() {
        return totalCheckInCount;
    }

    public int getNormalCount() {
        return normalCount;
    }

    public int getLateCount() {
        return lateCount;
    }

    public int getEarlyLeaveCount() {
        return earlyLeaveCount;
    }

    public int getMissingCardCount() {
        return missingCardCount;
    }

    public int getMonthCheckInCount() {
        return monthCheckInCount;
    }

    public int getMonthNormalCount() {
        return monthNormalCount;
    }

    public int getMonthAbnormalCount() {
        return monthAbnormalCount;
    }

    public String getRecentSevenDaysText() {
        return recentSevenDaysText;
    }
}
