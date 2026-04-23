package com.haoyinrui.campusattendance.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.haoyinrui.campusattendance.model.AttendanceRecord;
import com.haoyinrui.campusattendance.model.AttendanceSummary;
import com.haoyinrui.campusattendance.util.AttendanceRuleManager;
import com.haoyinrui.campusattendance.util.AttendanceStatusHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite 数据库帮助类。
 *
 * 本项目面向课程设计演示，直接使用 SQLiteOpenHelper 管理用户表和考勤表。
 * 真实项目中密码应加密或哈希处理，这里保留朴素写法，便于课堂理解数据库读写流程。
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "campus_attendance.db";
    private static final int DB_VERSION = 2;

    public static final String STATUS_NOT_CHECKED = "未签到";
    /** 已签到：表示当天已完成签到，但尚未完成签退。 */
    public static final String STATUS_CHECKED_IN = "已签到";
    /** 正常：签到和签退均符合时间规则。 */
    public static final String STATUS_NORMAL = "正常";
    /** 迟到：签到时间晚于签到截止时间。 */
    public static final String STATUS_LATE = "迟到";
    /** 早退：签退时间早于签退允许开始时间。 */
    public static final String STATUS_EARLY_LEAVE = "早退";
    /** 迟到早退：同一天既迟到又早退。 */
    public static final String STATUS_LATE_EARLY = "迟到早退";
    /** 缺卡：超过签退截止时间仍未完成有效签退。 */
    public static final String STATUS_MISSING_CARD = "缺卡";
    public static final String STATUS_COMPLETED = STATUS_NORMAL;

    private static final String TABLE_USER = "user";
    private static final String TABLE_ATTENDANCE = "attendance";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUserTable = "CREATE TABLE " + TABLE_USER + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT UNIQUE, "
                + "password TEXT)";

        String createAttendanceTable = "CREATE TABLE " + TABLE_ATTENDANCE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT, "
                + "date TEXT, "
                + "check_in_time TEXT, "
                + "check_out_time TEXT, "
                + "status TEXT, "
                + "remark TEXT DEFAULT '', "
                + "UNIQUE(username, date))";

        db.execSQL(createUserTable);
        db.execSQL(createAttendanceTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_ATTENDANCE + " ADD COLUMN remark TEXT DEFAULT ''");
            } catch (Exception ignored) {
                // 如果字段已经存在，忽略异常，避免升级时影响原有数据。
            }
        }
    }

    /**
     * 注册用户。用户名唯一，重复用户名会返回 false。
     */
    public boolean registerUser(String username, String password) {
        if (isUsernameExists(username)) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        long result = db.insert(TABLE_USER, null, values);
        return result != -1;
    }

    /**
     * 登录校验：查询 user 表中是否存在匹配的用户名和密码。
     */
    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER,
                new String[]{"id"},
                "username=? AND password=?",
                new String[]{username, password},
                null, null, null);
        boolean isValid = cursor.moveToFirst();
        cursor.close();
        return isValid;
    }

    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER,
                new String[]{"id"},
                "username=?",
                new String[]{username},
                null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    /**
     * 修改密码：先校验旧密码，再更新 user 表中的 password 字段。
     */
    public boolean updatePassword(String username, String oldPassword, String newPassword) {
        if (!validateUser(username, oldPassword)) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", newPassword);
        int rows = db.update(TABLE_USER, values, "username=?", new String[]{username});
        return rows > 0;
    }

    /**
     * 每日签到：同一用户同一天只能签到一次。
     */
    public boolean checkIn(String username, String date, String time) {
        return checkIn(username, date, time, STATUS_CHECKED_IN, "");
    }

    /**
     * 带规则结果的签到：由 Activity 根据时间规则传入“已签到/迟到”等状态。
     */
    public boolean checkIn(String username, String date, String time, String status, String remark) {
        AttendanceRecord record = getAttendanceRecord(username, date);
        SQLiteDatabase db = getWritableDatabase();

        if (record != null && record.hasCheckIn()) {
            return false;
        }

        if (record == null) {
            ContentValues values = new ContentValues();
            values.put("username", username);
            values.put("date", date);
            values.put("check_in_time", time);
            values.put("check_out_time", "");
            values.put("status", status);
            values.put("remark", remark == null ? "" : remark);
            return db.insert(TABLE_ATTENDANCE, null, values) != -1;
        }

        ContentValues values = new ContentValues();
        values.put("check_in_time", time);
        values.put("status", status);
        values.put("remark", remark == null ? "" : remark);
        int rows = db.update(TABLE_ATTENDANCE, values,
                "username=? AND date=?", new String[]{username, date});
        return rows > 0;
    }

    /**
     * 每日签退：需要先签到，且同一天只能签退一次。
     *
     * @return 1 表示成功，0 表示重复签退，-1 表示尚未签到。
     */
    public int checkOut(String username, String date, String time) {
        return checkOut(username, date, time, STATUS_NORMAL, "");
    }

    /**
     * 带规则结果的签退：状态可为正常、早退、迟到早退或缺卡。
     */
    public int checkOut(String username, String date, String time, String status, String remark) {
        AttendanceRecord record = getAttendanceRecord(username, date);
        if (record == null || !record.hasCheckIn()) {
            return -1;
        }
        if (record.hasCheckOut()) {
            return 0;
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("check_out_time", time);
        values.put("status", status);
        values.put("remark", mergeRemark(record.getRemark(), remark));
        int rows = db.update(TABLE_ATTENDANCE, values,
                "username=? AND date=?", new String[]{username, date});
        return rows > 0 ? 1 : -1;
    }

    /**
     * 查询某个用户某一天的考勤记录。
     */
    public AttendanceRecord getAttendanceRecord(String username, String date) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE,
                null,
                "username=? AND date=?",
                new String[]{username, date},
                null, null, null);

        AttendanceRecord record = null;
        if (cursor.moveToFirst()) {
            record = readRecord(cursor);
        }
        cursor.close();
        return record;
    }

    /**
     * 查询当前用户全部考勤记录，按日期倒序显示。
     */
    public List<AttendanceRecord> getAttendanceRecords(String username) {
        return getAttendanceRecords(username, "", false);
    }

    /**
     * 按日期和异常类型筛选记录。
     */
    public List<AttendanceRecord> getAttendanceRecords(String username, String dateFilter, boolean abnormalOnly) {
        List<AttendanceRecord> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder selection = new StringBuilder("username=?");
        List<String> args = new ArrayList<>();
        args.add(username);

        if (dateFilter != null && !dateFilter.trim().isEmpty()) {
            selection.append(" AND date LIKE ?");
            args.add(dateFilter.trim() + "%");
        }

        if (abnormalOnly) {
            selection.append(" AND (status LIKE ? OR status LIKE ? OR status LIKE ?)");
            args.add("%迟到%");
            args.add("%早退%");
            args.add("%缺卡%");
        }

        Cursor cursor = db.query(TABLE_ATTENDANCE,
                null,
                selection.toString(),
                args.toArray(new String[0]),
                null, null,
                "date DESC, id DESC");

        while (cursor.moveToNext()) {
            records.add(readRecord(cursor));
        }
        cursor.close();
        return records;
    }

    public boolean updateRemark(int recordId, String remark) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("remark", remark == null ? "" : remark);
        int rows = db.update(TABLE_ATTENDANCE, values, "id=?", new String[]{String.valueOf(recordId)});
        return rows > 0;
    }

    /**
     * 当超过签退截止时间仍未签退时，将记录标记为缺卡。
     */
    public void markMissingCards(String username, String today, String currentTime, String checkOutEndTime) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues pastValues = new ContentValues();
        pastValues.put("status", STATUS_MISSING_CARD);
        pastValues.put("remark", "超过签退截止时间未签退");
        db.update(TABLE_ATTENDANCE,
                pastValues,
                "username=? AND date<? AND check_in_time<>'' AND check_out_time=''",
                new String[]{username, today});

        if (AttendanceRuleManager.compareTime(currentTime, checkOutEndTime) > 0) {
            AttendanceRecord todayRecord = getAttendanceRecord(username, today);
            if (todayRecord != null && todayRecord.hasCheckIn() && !todayRecord.hasCheckOut()) {
                ContentValues todayValues = new ContentValues();
                todayValues.put("status", STATUS_MISSING_CARD);
                todayValues.put("remark", mergeRemark(todayRecord.getRemark(), "超过签退截止时间未签退"));
                db.update(TABLE_ATTENDANCE,
                        todayValues,
                        "username=? AND date=?",
                        new String[]{username, today});
            }
        }
    }

    /**
     * 查询某月考勤统计。
     *
     * @param monthPrefix 月份前缀，格式为 yyyy-MM
     */
    public AttendanceSummary getMonthlySummary(String username, String monthPrefix) {
        int checkInCount = 0;
        int completedCount = 0;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE,
                new String[]{"check_in_time", "status"},
                "username=? AND date LIKE ?",
                new String[]{username, monthPrefix + "%"},
                null, null, null);

        while (cursor.moveToNext()) {
            String checkInTime = cursor.getString(cursor.getColumnIndexOrThrow("check_in_time"));
            String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
            if (checkInTime != null && !checkInTime.isEmpty()) {
                checkInCount++;
            }
            if (AttendanceStatusHelper.isNormal(status)) {
                completedCount++;
            }
        }
        cursor.close();
        return new AttendanceSummary(checkInCount, completedCount);
    }

    /**
     * 统计总签到、迟到、早退、缺卡、本月概览和最近 7 天情况。
     */
    public com.haoyinrui.campusattendance.model.AttendanceStatistics getAttendanceStatistics(
            String username, String monthPrefix, List<String> recentDates) {
        int totalCheckIn = 0;
        int normal = 0;
        int late = 0;
        int early = 0;
        int missing = 0;
        int monthCheckIn = 0;
        int monthNormal = 0;
        int monthAbnormal = 0;
        Map<String, String> recentStatusMap = new HashMap<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE,
                null,
                "username=?",
                new String[]{username},
                null, null,
                "date ASC");

        while (cursor.moveToNext()) {
            AttendanceRecord record = readRecord(cursor);
            String status = record.getStatus() == null ? "" : record.getStatus();
            if (record.hasCheckIn()) {
                totalCheckIn++;
            }
            if (AttendanceStatusHelper.isNormal(status)) {
                normal++;
            }
            if (status.contains(STATUS_LATE)) {
                late++;
            }
            if (status.contains(STATUS_EARLY_LEAVE)) {
                early++;
            }
            if (status.contains(STATUS_MISSING_CARD)) {
                missing++;
            }
            if (record.getDate().startsWith(monthPrefix)) {
                if (record.hasCheckIn()) {
                    monthCheckIn++;
                }
                if (AttendanceStatusHelper.isNormal(status)) {
                    monthNormal++;
                }
                if (AttendanceStatusHelper.isAbnormal(status)) {
                    monthAbnormal++;
                }
            }
            if (recentDates.contains(record.getDate())) {
                recentStatusMap.put(record.getDate(), status);
            }
        }
        cursor.close();

        StringBuilder recentBuilder = new StringBuilder();
        for (String date : recentDates) {
            String status = recentStatusMap.get(date);
            if (status == null || status.isEmpty()) {
                status = "无记录";
            }
            recentBuilder.append(date.substring(5)).append("：")
                    .append(AttendanceStatusHelper.normalizeStatus(status))
                    .append("\n");
        }

        return new com.haoyinrui.campusattendance.model.AttendanceStatistics(
                totalCheckIn, normal, late, early, missing,
                monthCheckIn, monthNormal, monthAbnormal,
                recentBuilder.toString().trim());
    }

    /**
     * 为课程答辩生成最近 7 天演示数据，覆盖正常、迟到、早退、缺卡等典型场景。
     */
    public void generateDemoRecords(String username) {
        clearAttendanceRecords(username);
        SQLiteDatabase db = getWritableDatabase();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -6);

        String[][] samples = new String[][]{
                {"08:00:00", "17:20:00", STATUS_NORMAL, "演示数据：正常打卡"},
                {"08:18:00", "17:25:00", STATUS_LATE, "演示数据：超过签到截止时间，判定为迟到"},
                {"07:55:00", "16:35:00", STATUS_EARLY_LEAVE, "演示数据：早于签退开始时间，判定为早退"},
                {"08:20:00", "16:40:00", STATUS_LATE_EARLY, "演示数据：迟到且早退"},
                {"08:03:00", "", STATUS_MISSING_CARD, "演示数据：已签到但未签退，判定为缺卡"},
                {"07:58:00", "17:15:00", STATUS_NORMAL, "演示数据：正常打卡"},
                {"08:12:00", "17:10:00", STATUS_LATE, "演示数据：轻微迟到"}
        };

        for (String[] sample : samples) {
            ContentValues values = new ContentValues();
            values.put("username", username);
            values.put("date", formatDate(calendar));
            values.put("check_in_time", sample[0]);
            values.put("check_out_time", sample[1]);
            values.put("status", sample[2]);
            values.put("remark", sample[3]);
            db.insert(TABLE_ATTENDANCE, null, values);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    /**
     * 清空当前用户考勤记录，保留账号信息，便于答辩时重置演示环境。
     */
    public void clearAttendanceRecords(String username) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_ATTENDANCE, "username=?", new String[]{username});
    }

    private AttendanceRecord readRecord(Cursor cursor) {
        return new AttendanceRecord(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("username")),
                cursor.getString(cursor.getColumnIndexOrThrow("date")),
                cursor.getString(cursor.getColumnIndexOrThrow("check_in_time")),
                cursor.getString(cursor.getColumnIndexOrThrow("check_out_time")),
                cursor.getString(cursor.getColumnIndexOrThrow("status")),
                cursor.getString(cursor.getColumnIndexOrThrow("remark"))
        );
    }

    private String mergeRemark(String oldRemark, String newRemark) {
        if (newRemark == null || newRemark.isEmpty()) {
            return oldRemark == null ? "" : oldRemark;
        }
        if (oldRemark == null || oldRemark.isEmpty()) {
            return newRemark;
        }
        if (oldRemark.contains(newRemark)) {
            return oldRemark;
        }
        return oldRemark + "；" + newRemark;
    }

    private String formatDate(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", year, month, day);
    }
}
