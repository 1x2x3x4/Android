package com.haoyinrui.campusattendance.model;

/**
 * 本地课程表实体，用于首页自动匹配课程考勤规则。
 */
public class CourseSchedule {
    private final int id;
    private final String courseName;
    private final String teacherName;
    private final int weekday;
    private final String startTime;
    private final String endTime;
    private final String checkInStart;
    private final String checkInDeadline;
    private final String checkOutStart;
    private final String checkOutDeadline;
    private final String classroom;
    private final boolean enabled;

    public CourseSchedule(int id, String courseName, String teacherName, int weekday,
                          String startTime, String endTime, String checkInStart,
                          String checkInDeadline, String checkOutStart, String checkOutDeadline,
                          String classroom, boolean enabled) {
        this.id = id;
        this.courseName = courseName;
        this.teacherName = teacherName;
        this.weekday = weekday;
        this.startTime = startTime;
        this.endTime = endTime;
        this.checkInStart = checkInStart;
        this.checkInDeadline = checkInDeadline;
        this.checkOutStart = checkOutStart;
        this.checkOutDeadline = checkOutDeadline;
        this.classroom = classroom;
        this.enabled = enabled;
    }

    public int getId() {
        return id;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public int getWeekday() {
        return weekday;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getCheckInStart() {
        return checkInStart;
    }

    public String getCheckInDeadline() {
        return checkInDeadline;
    }

    public String getCheckOutStart() {
        return checkOutStart;
    }

    public String getCheckOutDeadline() {
        return checkOutDeadline;
    }

    public String getClassroom() {
        return classroom;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getWeekdayLabel() {
        switch (weekday) {
            case 1:
                return "周日";
            case 2:
                return "周一";
            case 3:
                return "周二";
            case 4:
                return "周三";
            case 5:
                return "周四";
            case 6:
                return "周五";
            default:
                return "周六";
        }
    }

    public String getTimeWindowText() {
        return checkInStart + " - " + checkOutDeadline;
    }
}
