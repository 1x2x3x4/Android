package com.haoyinrui.campusattendance.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 月度汇总数据，用于导出和图形化展示。
 */
public class MonthlyReportData {
    private final String username;
    private final String month;
    private final int totalCheckInDays;
    private final int normalCount;
    private final int lateCount;
    private final int earlyLeaveCount;
    private final int missingCount;
    private final int leaveCount;
    private final int makeUpRequestCount;
    private final LinkedHashMap<String, Integer> courseCountMap;

    public MonthlyReportData(String username, String month, int totalCheckInDays, int normalCount,
                             int lateCount, int earlyLeaveCount, int missingCount,
                             int leaveCount, int makeUpRequestCount,
                             LinkedHashMap<String, Integer> courseCountMap) {
        this.username = username;
        this.month = month;
        this.totalCheckInDays = totalCheckInDays;
        this.normalCount = normalCount;
        this.lateCount = lateCount;
        this.earlyLeaveCount = earlyLeaveCount;
        this.missingCount = missingCount;
        this.leaveCount = leaveCount;
        this.makeUpRequestCount = makeUpRequestCount;
        this.courseCountMap = courseCountMap;
    }

    public String getUsername() {
        return username;
    }

    public String getMonth() {
        return month;
    }

    public int getTotalCheckInDays() {
        return totalCheckInDays;
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

    public int getMissingCount() {
        return missingCount;
    }

    public int getLeaveCount() {
        return leaveCount;
    }

    public int getMakeUpRequestCount() {
        return makeUpRequestCount;
    }

    public Map<String, Integer> getCourseCountMap() {
        return courseCountMap;
    }
}
