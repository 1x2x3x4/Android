package com.haoyinrui.campusattendance.util;

import com.haoyinrui.campusattendance.data.DatabaseHelper;

/**
 * 统一考勤状态展示与分类，避免首页、列表、详情和统计出现不同叫法。
 */
public class AttendanceStatusHelper {
    private AttendanceStatusHelper() {
    }

    public static String normalizeStatus(String status) {
        if (status == null || status.isEmpty()) {
            return DatabaseHelper.STATUS_NOT_CHECKED;
        }
        if ("已完成".equals(status)) {
            return DatabaseHelper.STATUS_NORMAL;
        }
        return status;
    }

    public static String getRemarkOrDefault(String remark) {
        return remark == null || remark.trim().isEmpty() ? "无备注" : remark;
    }

    public static boolean isNormal(String status) {
        return DatabaseHelper.STATUS_NORMAL.equals(normalizeStatus(status));
    }

    public static boolean isAbnormal(String status) {
        String value = normalizeStatus(status);
        return value.contains(DatabaseHelper.STATUS_LATE)
                || value.contains(DatabaseHelper.STATUS_EARLY_LEAVE)
                || value.contains(DatabaseHelper.STATUS_MISSING_CARD);
    }
}
