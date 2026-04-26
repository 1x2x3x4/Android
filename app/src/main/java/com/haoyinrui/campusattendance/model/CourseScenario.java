package com.haoyinrui.campusattendance.model;

/**
 * 本地预设课程/考勤场景。
 */
public class CourseScenario {
    private final String id;
    private final String name;
    private final String type;
    private final String weekdayLabel;
    private final String classStartTime;
    private final String classEndTime;
    private final String signInStart;
    private final String signInEnd;
    private final String checkOutStart;
    private final String checkOutEnd;

    public CourseScenario(String id, String name, String type, String weekdayLabel,
                          String classStartTime, String classEndTime,
                          String signInStart, String signInEnd,
                          String checkOutStart, String checkOutEnd) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.weekdayLabel = weekdayLabel;
        this.classStartTime = classStartTime;
        this.classEndTime = classEndTime;
        this.signInStart = signInStart;
        this.signInEnd = signInEnd;
        this.checkOutStart = checkOutStart;
        this.checkOutEnd = checkOutEnd;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getWeekdayLabel() { return weekdayLabel; }
    public String getClassStartTime() { return classStartTime; }
    public String getClassEndTime() { return classEndTime; }
    public String getSignInStart() { return signInStart; }
    public String getSignInEnd() { return signInEnd; }
    public String getCheckOutStart() { return checkOutStart; }
    public String getCheckOutEnd() { return checkOutEnd; }

    @Override
    public String toString() {
        return name;
    }
}
