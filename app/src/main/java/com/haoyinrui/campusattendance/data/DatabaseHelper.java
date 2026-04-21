package com.haoyinrui.campusattendance.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.haoyinrui.campusattendance.model.AttendanceRecord;
import com.haoyinrui.campusattendance.model.AttendanceSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite 数据库帮助类。
 *
 * 本项目面向课程设计演示，直接使用 SQLiteOpenHelper 管理用户表和考勤表。
 * 真实项目中密码应加密或哈希处理，这里保留朴素写法，便于课堂理解数据库读写流程。
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "campus_attendance.db";
    private static final int DB_VERSION = 1;

    public static final String STATUS_NOT_CHECKED = "未签到";
    public static final String STATUS_CHECKED_IN = "已签到";
    public static final String STATUS_COMPLETED = "已完成";

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
                + "UNIQUE(username, date))";

        db.execSQL(createUserTable);
        db.execSQL(createAttendanceTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTENDANCE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
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
            values.put("status", STATUS_CHECKED_IN);
            return db.insert(TABLE_ATTENDANCE, null, values) != -1;
        }

        ContentValues values = new ContentValues();
        values.put("check_in_time", time);
        values.put("status", STATUS_CHECKED_IN);
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
        values.put("status", STATUS_COMPLETED);
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
        List<AttendanceRecord> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE,
                null,
                "username=?",
                new String[]{username},
                null, null,
                "date DESC, id DESC");

        while (cursor.moveToNext()) {
            records.add(readRecord(cursor));
        }
        cursor.close();
        return records;
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
            if (STATUS_COMPLETED.equals(status)) {
                completedCount++;
            }
        }
        cursor.close();
        return new AttendanceSummary(checkInCount, completedCount);
    }

    private AttendanceRecord readRecord(Cursor cursor) {
        return new AttendanceRecord(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("username")),
                cursor.getString(cursor.getColumnIndexOrThrow("date")),
                cursor.getString(cursor.getColumnIndexOrThrow("check_in_time")),
                cursor.getString(cursor.getColumnIndexOrThrow("check_out_time")),
                cursor.getString(cursor.getColumnIndexOrThrow("status"))
        );
    }
}
