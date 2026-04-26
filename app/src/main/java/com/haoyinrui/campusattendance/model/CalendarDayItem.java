package com.haoyinrui.campusattendance.model;

/**
 * 月历网格中的单个日期数据。
 */
public class CalendarDayItem {
    private final String date;
    private final String dayText;
    private final boolean currentMonth;
    private final boolean today;
    private final AttendanceRecord record;

    public CalendarDayItem(String date, String dayText, boolean currentMonth,
                           boolean today, AttendanceRecord record) {
        this.date = date;
        this.dayText = dayText;
        this.currentMonth = currentMonth;
        this.today = today;
        this.record = record;
    }

    public String getDate() {
        return date;
    }

    public String getDayText() {
        return dayText;
    }

    public boolean isCurrentMonth() {
        return currentMonth;
    }

    public boolean isToday() {
        return today;
    }

    public AttendanceRecord getRecord() {
        return record;
    }
}
