package com.haoyinrui.campusattendance.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.haoyinrui.campusattendance.model.CourseScenario;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地课程/班级考勤场景管理。
 *
 * 课程设计版采用预设数据，避免引入后端和复杂课表系统。
 */
public class CourseScenarioManager {
    private static final String PREF_NAME = "course_scenario_pref";
    private static final String KEY_SELECTED_ID = "selected_id";

    private final SharedPreferences preferences;
    private final List<CourseScenario> scenarios;

    public CourseScenarioManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        scenarios = buildDefaultScenarios();
    }

    public List<CourseScenario> getScenarios() {
        return scenarios;
    }

    public CourseScenario getSelectedScenario() {
        String selectedId = preferences.getString(KEY_SELECTED_ID, scenarios.get(0).getId());
        for (CourseScenario scenario : scenarios) {
            if (scenario.getId().equals(selectedId)) {
                return scenario;
            }
        }
        return scenarios.get(0);
    }

    public void saveSelectedScenario(CourseScenario scenario) {
        if (scenario != null) {
            preferences.edit().putString(KEY_SELECTED_ID, scenario.getId()).apply();
        }
    }

    private List<CourseScenario> buildDefaultScenarios() {
        List<CourseScenario> list = new ArrayList<>();
        list.add(new CourseScenario("daily", "普通到校考勤", "日常考勤", "每天",
                "08:00", "17:00", "07:30", "08:10", "17:00", "22:00"));
        list.add(new CourseScenario("first", "第一节课签到", "课程考勤", "周一至周五",
                "08:00", "09:40", "07:40", "08:05", "09:35", "10:00"));
        list.add(new CourseScenario("second", "第二节课签到", "课程考勤", "周一至周五",
                "10:10", "11:50", "09:50", "10:15", "11:45", "12:05"));
        list.add(new CourseScenario("lab", "实验课签到", "实验课", "按课表",
                "14:00", "16:30", "13:40", "14:10", "16:20", "16:50"));
        list.add(new CourseScenario("evening", "晚自习签到", "晚自习", "周日至周四",
                "19:00", "21:00", "18:40", "19:10", "20:50", "21:20"));
        return list;
    }
}
