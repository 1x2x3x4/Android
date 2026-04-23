package com.haoyinrui.campusattendance.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/**
 * 考勤时间规则管理类。
 *
 * 规则说明：
 * 1. 签到开始时间之前不允许签到，避免过早打卡。
 * 2. 签到截止时间之后签到，记录为“迟到”。
 * 3. 签退开始时间之前签退，记录为“早退”。
 * 4. 签退截止时间之后仍未签退，记录为“缺卡”。
 */
public class AttendanceRuleManager {
    private static final String PREF_NAME = "attendance_rule_pref";
    private static final String KEY_SIGN_IN_START = "sign_in_start";
    private static final String KEY_SIGN_IN_END = "sign_in_end";
    private static final String KEY_CHECK_OUT_START = "check_out_start";
    private static final String KEY_CHECK_OUT_END = "check_out_end";

    private static final String DEFAULT_SIGN_IN_START = "07:30";
    private static final String DEFAULT_SIGN_IN_END = "08:10";
    private static final String DEFAULT_CHECK_OUT_START = "17:00";
    private static final String DEFAULT_CHECK_OUT_END = "22:00";

    private final SharedPreferences preferences;

    public AttendanceRuleManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getSignInStart() {
        return preferences.getString(KEY_SIGN_IN_START, DEFAULT_SIGN_IN_START);
    }

    public String getSignInEnd() {
        return preferences.getString(KEY_SIGN_IN_END, DEFAULT_SIGN_IN_END);
    }

    public String getCheckOutStart() {
        return preferences.getString(KEY_CHECK_OUT_START, DEFAULT_CHECK_OUT_START);
    }

    public String getCheckOutEnd() {
        return preferences.getString(KEY_CHECK_OUT_END, DEFAULT_CHECK_OUT_END);
    }

    public void saveRules(String signInStart, String signInEnd, String checkOutStart, String checkOutEnd) {
        preferences.edit()
                .putString(KEY_SIGN_IN_START, normalizeTime(signInStart))
                .putString(KEY_SIGN_IN_END, normalizeTime(signInEnd))
                .putString(KEY_CHECK_OUT_START, normalizeTime(checkOutStart))
                .putString(KEY_CHECK_OUT_END, normalizeTime(checkOutEnd))
                .apply();
    }

    public void resetDefaults() {
        saveRules(DEFAULT_SIGN_IN_START, DEFAULT_SIGN_IN_END, DEFAULT_CHECK_OUT_START, DEFAULT_CHECK_OUT_END);
    }

    public boolean isBeforeSignInStart(String currentTime) {
        return compareTime(currentTime, getSignInStart()) < 0;
    }

    public boolean isLate(String checkInTime) {
        return compareTime(checkInTime, getSignInEnd()) > 0;
    }

    public boolean isEarlyLeave(String checkOutTime) {
        return compareTime(checkOutTime, getCheckOutStart()) < 0;
    }

    public boolean isAfterCheckOutEnd(String currentTime) {
        return compareTime(currentTime, getCheckOutEnd()) > 0;
    }

    public String getRuleDescription() {
        return "签到：" + getSignInStart() + "-" + getSignInEnd()
                + "，签退：" + getCheckOutStart() + "-" + getCheckOutEnd();
    }

    public static boolean isValidTime(String time) {
        return time != null && time.matches("([01]\\d|2[0-3]):[0-5]\\d");
    }

    public static String normalizeTime(String time) {
        if (time == null) {
            return "00:00";
        }
        String value = time.trim();
        if (value.length() >= 5) {
            return value.substring(0, 5);
        }
        return value;
    }

    public static int compareTime(String left, String right) {
        return normalizeTime(left).compareTo(normalizeTime(right));
    }

    public String formatForDisplay() {
        return String.format(Locale.getDefault(), "签到 %s-%s / 签退 %s-%s",
                getSignInStart(), getSignInEnd(), getCheckOutStart(), getCheckOutEnd());
    }
}
