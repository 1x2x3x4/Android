package com.haoyinrui.campusattendance.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.haoyinrui.campusattendance.model.AttendanceRecord;
import com.haoyinrui.campusattendance.model.AttendanceStatistics;
import com.haoyinrui.campusattendance.model.AttendanceSummary;
import com.haoyinrui.campusattendance.model.CourseSchedule;
import com.haoyinrui.campusattendance.model.LeaveRequest;
import com.haoyinrui.campusattendance.model.MonthlyReportData;
import com.haoyinrui.campusattendance.util.AttendanceRuleManager;
import com.haoyinrui.campusattendance.util.AttendanceStatusHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQLite 数据库帮助类。
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "campus_attendance.db";
    private static final int DB_VERSION = 6;

    public static final String STATUS_NOT_CHECKED = "未签到";
    public static final String STATUS_CHECKED_IN = "已签到";
    public static final String STATUS_NORMAL = "正常";
    public static final String STATUS_LATE = "迟到";
    public static final String STATUS_EARLY_LEAVE = "早退";
    public static final String STATUS_LATE_EARLY = "迟到早退";
    public static final String STATUS_MISSING_CARD = "缺卡";
    public static final String STATUS_LEAVE = "请假";
    public static final String STATUS_COMPLETED = STATUS_NORMAL;

    public static final String APPEAL_NONE = "未申诉";
    public static final String APPEAL_PENDING = "待处理";
    public static final String APPEAL_APPROVED = "已通过";
    public static final String APPEAL_REJECTED = "已驳回";

    public static final String REQUEST_TYPE_LEAVE = "请假";
    public static final String REQUEST_TYPE_MAKE_UP = "补签";
    public static final String REQUEST_TYPE_CANCEL_LEAVE = "销假";
    public static final String REQUEST_STATUS_PENDING = "待处理";
    public static final String REQUEST_STATUS_APPROVED = "已通过";
    public static final String REQUEST_STATUS_REJECTED = "已驳回";

    private static final String TABLE_USER = "user";
    private static final String TABLE_ATTENDANCE = "attendance";
    private static final String TABLE_COURSE_SCHEDULE = "course_schedule";
    private static final String TABLE_LEAVE_REQUEST = "leave_request";

    private final Context appContext;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USER + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT UNIQUE, "
                + "password TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_ATTENDANCE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT, "
                + "date TEXT, "
                + "check_in_time TEXT, "
                + "check_out_time TEXT, "
                + "status TEXT, "
                + "remark TEXT DEFAULT '', "
                + "appeal_status TEXT DEFAULT '" + APPEAL_NONE + "', "
                + "appeal_reason TEXT DEFAULT '', "
                + "appeal_result TEXT DEFAULT '', "
                + "appeal_time TEXT DEFAULT '', "
                + "course_name TEXT DEFAULT '普通到校考勤', "
                + "course_type TEXT DEFAULT '日常考勤', "
                + "weekday_label TEXT DEFAULT '', "
                + "UNIQUE(username, date))");

        db.execSQL("CREATE TABLE " + TABLE_COURSE_SCHEDULE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "course_name TEXT, "
                + "teacher_name TEXT DEFAULT '', "
                + "weekday INTEGER, "
                + "start_time TEXT, "
                + "end_time TEXT, "
                + "check_in_start TEXT, "
                + "check_in_deadline TEXT, "
                + "check_out_start TEXT, "
                + "check_out_deadline TEXT, "
                + "classroom TEXT DEFAULT '', "
                + "is_enabled INTEGER DEFAULT 1)");

        db.execSQL("CREATE TABLE " + TABLE_LEAVE_REQUEST + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT, "
                + "request_type TEXT, "
                + "target_date TEXT, "
                + "related_course_name TEXT DEFAULT '', "
                + "reason TEXT, "
                + "status TEXT DEFAULT '" + REQUEST_STATUS_PENDING + "', "
                + "created_at TEXT, "
                + "processed_at TEXT DEFAULT '', "
                + "result_remark TEXT DEFAULT '')");

        seedDefaultCourseSchedules(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            addColumnIfNeeded(db, TABLE_ATTENDANCE, "remark TEXT DEFAULT ''");
        }
        if (oldVersion < 3) {
            addColumnIfNeeded(db, TABLE_ATTENDANCE, "appeal_status TEXT DEFAULT '" + APPEAL_NONE + "'");
            addColumnIfNeeded(db, TABLE_ATTENDANCE, "appeal_reason TEXT DEFAULT ''");
            addColumnIfNeeded(db, TABLE_ATTENDANCE, "appeal_result TEXT DEFAULT ''");
            addColumnIfNeeded(db, TABLE_ATTENDANCE, "appeal_time TEXT DEFAULT ''");
            addColumnIfNeeded(db, TABLE_ATTENDANCE, "course_name TEXT DEFAULT '普通到校考勤'");
            addColumnIfNeeded(db, TABLE_ATTENDANCE, "course_type TEXT DEFAULT '日常考勤'");
            addColumnIfNeeded(db, TABLE_ATTENDANCE, "weekday_label TEXT DEFAULT ''");
        }
        if (oldVersion < 6) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_COURSE_SCHEDULE + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "course_name TEXT, "
                    + "teacher_name TEXT DEFAULT '', "
                    + "weekday INTEGER, "
                    + "start_time TEXT, "
                    + "end_time TEXT, "
                    + "check_in_start TEXT, "
                    + "check_in_deadline TEXT, "
                    + "check_out_start TEXT, "
                    + "check_out_deadline TEXT, "
                    + "classroom TEXT DEFAULT '', "
                    + "is_enabled INTEGER DEFAULT 1)");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LEAVE_REQUEST + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT, "
                    + "request_type TEXT, "
                    + "target_date TEXT, "
                    + "related_course_name TEXT DEFAULT '', "
                    + "reason TEXT, "
                    + "status TEXT DEFAULT '" + REQUEST_STATUS_PENDING + "', "
                    + "created_at TEXT, "
                    + "processed_at TEXT DEFAULT '', "
                    + "result_remark TEXT DEFAULT '')");

            seedDefaultCourseSchedules(db);
        }
    }

    public boolean registerUser(String username, String password) {
        if (isUsernameExists(username)) {
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        return db.insert(TABLE_USER, null, values) != -1;
    }

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

    public boolean updatePassword(String username, String oldPassword, String newPassword) {
        if (!validateUser(username, oldPassword)) {
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", newPassword);
        return db.update(TABLE_USER, values, "username=?", new String[]{username}) > 0;
    }

    public boolean checkIn(String username, String date, String time) {
        return checkIn(username, date, time, STATUS_CHECKED_IN, "");
    }

    public boolean checkIn(String username, String date, String time, String status, String remark) {
        return checkIn(username, date, time, status, remark, "普通到校考勤", "日常考勤", "");
    }

    public boolean checkIn(String username, String date, String time, String status, String remark,
                           String courseName, String courseType, String weekdayLabel) {
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
            values.put("appeal_status", APPEAL_NONE);
            values.put("appeal_reason", "");
            values.put("appeal_result", "");
            values.put("appeal_time", "");
            values.put("course_name", courseName);
            values.put("course_type", courseType);
            values.put("weekday_label", weekdayLabel);
            return db.insert(TABLE_ATTENDANCE, null, values) != -1;
        }

        ContentValues values = new ContentValues();
        values.put("check_in_time", time);
        values.put("status", status);
        values.put("remark", remark == null ? "" : remark);
        values.put("course_name", courseName);
        values.put("course_type", courseType);
        values.put("weekday_label", weekdayLabel);
        return db.update(TABLE_ATTENDANCE, values, "username=? AND date=?", new String[]{username, date}) > 0;
    }

    public int checkOut(String username, String date, String time) {
        return checkOut(username, date, time, STATUS_NORMAL, "");
    }

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
        int rows = db.update(TABLE_ATTENDANCE, values, "username=? AND date=?", new String[]{username, date});
        return rows > 0 ? 1 : -1;
    }

    public AttendanceRecord getAttendanceRecord(String username, String date) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE, null,
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

    public List<AttendanceRecord> getAttendanceRecords(String username) {
        return getAttendanceRecords(username, "", false);
    }

    public List<AttendanceRecord> getAttendanceRecords(String username, String dateFilter, boolean abnormalOnly) {
        List<AttendanceRecord> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder selection = new StringBuilder("username=?");
        List<String> args = new ArrayList<>();
        args.add(username);

        if (!TextUtils.isEmpty(dateFilter)) {
            selection.append(" AND date LIKE ?");
            args.add(dateFilter.trim() + "%");
        }

        if (abnormalOnly) {
            selection.append(" AND (status LIKE ? OR status LIKE ? OR status LIKE ?)");
            args.add("%" + STATUS_LATE + "%");
            args.add("%" + STATUS_EARLY_LEAVE + "%");
            args.add("%" + STATUS_MISSING_CARD + "%");
        }

        Cursor cursor = db.query(TABLE_ATTENDANCE, null,
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
        return db.update(TABLE_ATTENDANCE, values, "id=?", new String[]{String.valueOf(recordId)}) > 0;
    }

    public boolean submitAppeal(int recordId, String reason, String appealTime) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("appeal_status", APPEAL_PENDING);
        values.put("appeal_reason", reason == null ? "" : reason);
        values.put("appeal_result", "");
        values.put("appeal_time", appealTime == null ? "" : appealTime);
        return db.update(TABLE_ATTENDANCE, values, "id=?", new String[]{String.valueOf(recordId)}) > 0;
    }

    public boolean updateAppealResult(int recordId, String appealStatus, String appealResult) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("appeal_status", appealStatus);
        values.put("appeal_result", appealResult == null ? "" : appealResult);
        return db.update(TABLE_ATTENDANCE, values, "id=?", new String[]{String.valueOf(recordId)}) > 0;
    }

    public Map<String, AttendanceRecord> getMonthlyRecordMap(String username, String monthPrefix) {
        Map<String, AttendanceRecord> map = new HashMap<>();
        List<AttendanceRecord> records = getAttendanceRecords(username, monthPrefix, false);
        for (AttendanceRecord record : records) {
            map.put(record.getDate(), record);
        }
        return map;
    }

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
                ContentValues values = new ContentValues();
                values.put("status", STATUS_MISSING_CARD);
                values.put("remark", mergeRemark(todayRecord.getRemark(), "超过签退截止时间未签退"));
                db.update(TABLE_ATTENDANCE, values,
                        "username=? AND date=?",
                        new String[]{username, today});
            }
        }
    }

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
            if (!TextUtils.isEmpty(checkInTime)) {
                checkInCount++;
            }
            if (AttendanceStatusHelper.isNormal(status)) {
                completedCount++;
            }
        }
        cursor.close();
        return new AttendanceSummary(checkInCount, completedCount);
    }

    public AttendanceStatistics getAttendanceStatistics(String username, String monthPrefix, List<String> recentDates) {
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
        Cursor cursor = db.query(TABLE_ATTENDANCE, null, "username=?",
                new String[]{username}, null, null, "date ASC");

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
            if (TextUtils.isEmpty(status)) {
                status = "无记录";
            }
            recentBuilder.append(date.substring(5))
                    .append("：")
                    .append(AttendanceStatusHelper.normalizeStatus(status))
                    .append("\n");
        }

        return new AttendanceStatistics(
                totalCheckIn, normal, late, early, missing,
                monthCheckIn, monthNormal, monthAbnormal,
                recentBuilder.toString().trim());
    }

    public String getCourseStatisticsText(String username) {
        LinkedHashMap<String, Integer> map = getCourseStatisticsMap(username, "");
        if (map.isEmpty()) {
            return "暂无课程统计";
        }
        StringBuilder builder = new StringBuilder("课程统计");
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            builder.append("\n")
                    .append(entry.getKey())
                    .append(" ")
                    .append(entry.getValue())
                    .append(" 次");
        }
        return builder.toString();
    }

    public void generateDemoRecords(String username) {
        clearAttendanceRecords(username);
        SQLiteDatabase db = getWritableDatabase();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -6);

        String[][] samples = new String[][]{
                {"08:00:00", "17:20:00", STATUS_NORMAL, "课堂正常", "普通到校考勤", "日常考勤"},
                {"08:18:00", "17:25:00", STATUS_LATE, "超过签到截止时间", "周一第一节课", "课程考勤"},
                {"07:55:00", "16:35:00", STATUS_EARLY_LEAVE, "早于签退开始时间", "周二实验课", "实验课"},
                {"08:20:00", "16:40:00", STATUS_LATE_EARLY, "迟到且早退", "周三晚自习", "晚自习"},
                {"08:03:00", "", STATUS_MISSING_CARD, "超过签退截止时间未签退", "周四到校考勤", "日常考勤"},
                {"", "", STATUS_LEAVE, "请假通过", "周五课程签到", "课程考勤"},
                {"08:12:00", "17:10:00", STATUS_LATE, "轻微迟到", "周五课程签到", "课程考勤"}
        };

        for (String[] sample : samples) {
            ContentValues values = new ContentValues();
            values.put("username", username);
            values.put("date", formatDate(calendar));
            values.put("check_in_time", sample[0]);
            values.put("check_out_time", sample[1]);
            values.put("status", sample[2]);
            values.put("remark", sample[3]);
            values.put("appeal_status", APPEAL_NONE);
            values.put("appeal_reason", "");
            values.put("appeal_result", "");
            values.put("appeal_time", "");
            values.put("course_name", sample[4]);
            values.put("course_type", sample[5]);
            values.put("weekday_label", "演示");
            db.insert(TABLE_ATTENDANCE, null, values);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        submitLeaveRequest(username, REQUEST_TYPE_LEAVE, formatDate(addDays(-1)), "周五课程签到", "演示请假", nowText());
        submitLeaveRequest(username, REQUEST_TYPE_MAKE_UP, formatDate(addDays(-3)), "周三晚自习", "演示补签", nowText());
        submitLeaveRequest(username, REQUEST_TYPE_CANCEL_LEAVE, formatDate(addDays(-1)), "周五课程签到", "演示销假", nowText());
    }

    public void generateQuarterDemoRecords(String username) {
        clearAttendanceRecords(username);
        SQLiteDatabase db = getWritableDatabase();

        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.add(Calendar.MONTH, -2);
        normalizeToStartOfDay(start);

        Calendar end = Calendar.getInstance();
        normalizeToStartOfDay(end);

        int sampleIndex = 0;
        while (!start.after(end)) {
            int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                insertQuarterDemoAttendance(db, username, start, sampleIndex);
                sampleIndex++;
            }
            start.add(Calendar.DAY_OF_MONTH, 1);
        }

        createQuarterDemoRequest(username, REQUEST_TYPE_LEAVE, getQuarterDemoDate(2, 8),
                "周一第一节课", "演示请假：校外竞赛", REQUEST_STATUS_APPROVED, "教师审批：已通过");
        createQuarterDemoRequest(username, REQUEST_TYPE_MAKE_UP, getQuarterDemoDate(1, 12),
                "周三晚自习", "演示补签：活动结束后补签", REQUEST_STATUS_PENDING, "");
        createQuarterDemoRequest(username, REQUEST_TYPE_CANCEL_LEAVE, getQuarterDemoDate(1, 5),
                "周五课程签到", "演示销假：提前返校", REQUEST_STATUS_REJECTED, "教师审批：暂不通过");
        createQuarterDemoRequest(username, REQUEST_TYPE_LEAVE, getQuarterDemoDate(0, 3),
                "周二实验课", "演示请假：身体不适", REQUEST_STATUS_PENDING, "");
        createQuarterDemoRequest(username, REQUEST_TYPE_MAKE_UP, getQuarterDemoDate(0, 9),
                "周四到校考勤", "演示补签：网络异常漏打卡", REQUEST_STATUS_APPROVED, "教师审批：已通过");
    }

    public void clearAttendanceRecords(String username) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_ATTENDANCE, "username=?", new String[]{username});
        db.delete(TABLE_LEAVE_REQUEST, "username=?", new String[]{username});
    }

    public List<CourseSchedule> getCourseSchedules() {
        List<CourseSchedule> schedules = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_COURSE_SCHEDULE, null, null, null, null, null,
                "weekday ASC, start_time ASC");
        while (cursor.moveToNext()) {
            schedules.add(readCourseSchedule(cursor));
        }
        cursor.close();
        return schedules;
    }

    public CourseSchedule getRecommendedCourseSchedule(int weekday, String currentTime) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_COURSE_SCHEDULE, null,
                "weekday=? AND is_enabled=1",
                new String[]{String.valueOf(weekday)},
                null, null,
                "check_in_start ASC");

        CourseSchedule result = null;
        while (cursor.moveToNext()) {
            CourseSchedule schedule = readCourseSchedule(cursor);
            if (AttendanceRuleManager.compareTime(currentTime, schedule.getCheckInStart()) >= 0
                    && AttendanceRuleManager.compareTime(currentTime, schedule.getCheckOutDeadline()) <= 0) {
                result = schedule;
                break;
            }
        }
        cursor.close();
        return result;
    }

    public CourseSchedule getFallbackCourseSchedule() {
        AttendanceRuleManager ruleManager = new AttendanceRuleManager(appContext);
        int weekday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return new CourseSchedule(
                -1,
                "普通到校考勤",
                "",
                weekday,
                ruleManager.getSignInStart(),
                ruleManager.getCheckOutEnd(),
                ruleManager.getSignInStart(),
                ruleManager.getSignInEnd(),
                ruleManager.getCheckOutStart(),
                ruleManager.getCheckOutEnd(),
                "校园",
                true
        );
    }

    public boolean updateCourseSchedule(CourseSchedule schedule) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("course_name", schedule.getCourseName());
        values.put("teacher_name", schedule.getTeacherName());
        values.put("weekday", schedule.getWeekday());
        values.put("start_time", schedule.getStartTime());
        values.put("end_time", schedule.getEndTime());
        values.put("check_in_start", schedule.getCheckInStart());
        values.put("check_in_deadline", schedule.getCheckInDeadline());
        values.put("check_out_start", schedule.getCheckOutStart());
        values.put("check_out_deadline", schedule.getCheckOutDeadline());
        values.put("classroom", schedule.getClassroom());
        values.put("is_enabled", schedule.isEnabled() ? 1 : 0);
        return db.update(TABLE_COURSE_SCHEDULE, values, "id=?", new String[]{String.valueOf(schedule.getId())}) > 0;
    }

    public boolean submitLeaveRequest(String username, String requestType, String targetDate,
                                      String relatedCourseName, String reason, String createdAt) {
        SQLiteDatabase db = getWritableDatabase();
        if (hasPendingRequest(username, requestType, targetDate)) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("request_type", requestType);
        values.put("target_date", targetDate);
        values.put("related_course_name", relatedCourseName == null ? "" : relatedCourseName);
        values.put("reason", reason == null ? "" : reason);
        values.put("status", REQUEST_STATUS_PENDING);
        values.put("created_at", createdAt);
        values.put("processed_at", "");
        values.put("result_remark", "");
        return db.insert(TABLE_LEAVE_REQUEST, null, values) != -1;
    }

    public List<LeaveRequest> getUserLeaveRequests(String username) {
        return queryLeaveRequests("username=?", new String[]{username});
    }

    public List<LeaveRequest> getPendingLeaveRequests() {
        return queryLeaveRequests("status=?", new String[]{REQUEST_STATUS_PENDING});
    }

    public boolean updateLeaveRequestStatus(int requestId, String status, String processedAt, String resultRemark) {
        LeaveRequest request = getLeaveRequestById(requestId);
        if (request == null) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        values.put("processed_at", processedAt == null ? "" : processedAt);
        values.put("result_remark", resultRemark == null ? "" : resultRemark);
        boolean success = db.update(TABLE_LEAVE_REQUEST, values, "id=?", new String[]{String.valueOf(requestId)}) > 0;
        if (!success || !REQUEST_STATUS_APPROVED.equals(status)) {
            return success;
        }

        if (REQUEST_TYPE_LEAVE.equals(request.getRequestType())) {
            applyApprovedLeave(request);
        } else if (REQUEST_TYPE_MAKE_UP.equals(request.getRequestType())) {
            applyApprovedMakeUp(request);
        } else if (REQUEST_TYPE_CANCEL_LEAVE.equals(request.getRequestType())) {
            applyApprovedCancelLeave(request);
        }
        return true;
    }

    public int getPendingRequestCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LEAVE_REQUEST + " WHERE status=?",
                new String[]{REQUEST_STATUS_PENDING});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getApprovedLeaveCount(String username, String monthPrefix) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LEAVE_REQUEST
                        + " WHERE username=? AND request_type=? AND status=? AND target_date LIKE ?",
                new String[]{username, REQUEST_TYPE_LEAVE, REQUEST_STATUS_APPROVED, monthPrefix + "%"});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getMakeUpRequestCount(String username, String monthPrefix) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LEAVE_REQUEST
                        + " WHERE username=? AND request_type=? AND target_date LIKE ?",
                new String[]{username, REQUEST_TYPE_MAKE_UP, monthPrefix + "%"});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public MonthlyReportData getMonthlyReportData(String username, String monthPrefix) {
        int totalCheckInDays = 0;
        int normalCount = 0;
        int lateCount = 0;
        int earlyCount = 0;
        int missingCount = 0;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE, null,
                "username=? AND date LIKE ?",
                new String[]{username, monthPrefix + "%"},
                null, null, "date ASC");

        while (cursor.moveToNext()) {
            AttendanceRecord record = readRecord(cursor);
            String status = record.getStatus();
            if (record.hasCheckIn()) {
                totalCheckInDays++;
            }
            if (AttendanceStatusHelper.isNormal(status)) {
                normalCount++;
            }
            if (status != null && status.contains(STATUS_LATE)) {
                lateCount++;
            }
            if (status != null && status.contains(STATUS_EARLY_LEAVE)) {
                earlyCount++;
            }
            if (status != null && status.contains(STATUS_MISSING_CARD)) {
                missingCount++;
            }
        }
        cursor.close();

        LinkedHashMap<String, Integer> courseCountMap = getCourseStatisticsMap(username, monthPrefix);
        return new MonthlyReportData(
                username,
                monthPrefix,
                totalCheckInDays,
                normalCount,
                lateCount,
                earlyCount,
                missingCount,
                getApprovedLeaveCount(username, monthPrefix),
                getMakeUpRequestCount(username, monthPrefix),
                courseCountMap
        );
    }

    private LinkedHashMap<String, Integer> getCourseStatisticsMap(String username, String monthPrefix) {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        String selection = "username=?";
        String[] args;
        if (TextUtils.isEmpty(monthPrefix)) {
            args = new String[]{username};
        } else {
            selection += " AND date LIKE ?";
            args = new String[]{username, monthPrefix + "%"};
        }
        Cursor cursor = db.query(TABLE_ATTENDANCE,
                new String[]{"course_name"},
                selection,
                args,
                null, null,
                "course_name ASC");
        while (cursor.moveToNext()) {
            String courseName = cursor.getString(0);
            if (TextUtils.isEmpty(courseName)) {
                courseName = "普通到校考勤";
            }
            Integer count = map.get(courseName);
            map.put(courseName, count == null ? 1 : count + 1);
        }
        cursor.close();
        return map;
    }

    private List<LeaveRequest> queryLeaveRequests(String selection, String[] args) {
        List<LeaveRequest> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_LEAVE_REQUEST, null,
                selection, args, null, null,
                "created_at DESC, id DESC");
        while (cursor.moveToNext()) {
            list.add(readLeaveRequest(cursor));
        }
        cursor.close();
        return list;
    }

    private LeaveRequest getLeaveRequestById(int requestId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_LEAVE_REQUEST, null,
                "id=?", new String[]{String.valueOf(requestId)},
                null, null, null);
        LeaveRequest request = null;
        if (cursor.moveToFirst()) {
            request = readLeaveRequest(cursor);
        }
        cursor.close();
        return request;
    }

    private boolean hasPendingRequest(String username, String requestType, String targetDate) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_LEAVE_REQUEST,
                new String[]{"id"},
                "username=? AND request_type=? AND target_date=? AND status=?",
                new String[]{username, requestType, targetDate, REQUEST_STATUS_PENDING},
                null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    private void applyApprovedLeave(LeaveRequest request) {
        SQLiteDatabase db = getWritableDatabase();
        AttendanceRecord record = getAttendanceRecord(request.getUsername(), request.getTargetDate());
        ContentValues values = new ContentValues();
        values.put("status", STATUS_LEAVE);
        values.put("remark", request.getReason());
        values.put("course_name", TextUtils.isEmpty(request.getRelatedCourseName()) ? "请假申请" : request.getRelatedCourseName());
        values.put("course_type", "请假");
        values.put("weekday_label", "");

        if (record == null) {
            values.put("username", request.getUsername());
            values.put("date", request.getTargetDate());
            values.put("check_in_time", "");
            values.put("check_out_time", "");
            values.put("appeal_status", APPEAL_NONE);
            values.put("appeal_reason", "");
            values.put("appeal_result", "");
            values.put("appeal_time", "");
            db.insert(TABLE_ATTENDANCE, null, values);
        } else {
            db.update(TABLE_ATTENDANCE, values,
                    "username=? AND date=?",
                    new String[]{request.getUsername(), request.getTargetDate()});
        }
    }

    private void applyApprovedMakeUp(LeaveRequest request) {
        AttendanceRecord record = getAttendanceRecord(request.getUsername(), request.getTargetDate());
        if (record == null) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", STATUS_NORMAL);
        values.put("remark", mergeRemark(record.getRemark(), "补签申请已通过"));
        db.update(TABLE_ATTENDANCE, values,
                "username=? AND date=?",
                new String[]{request.getUsername(), request.getTargetDate()});
    }

    private void applyApprovedCancelLeave(LeaveRequest request) {
        AttendanceRecord record = getAttendanceRecord(request.getUsername(), request.getTargetDate());
        if (record == null || !STATUS_LEAVE.equals(record.getStatus())) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", STATUS_NOT_CHECKED);
        values.put("remark", mergeRemark(record.getRemark(), "销假申请已通过"));
        db.update(TABLE_ATTENDANCE, values,
                "username=? AND date=?",
                new String[]{request.getUsername(), request.getTargetDate()});
    }

    private void seedDefaultCourseSchedules(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_COURSE_SCHEDULE, null);
        boolean hasData = false;
        if (cursor.moveToFirst()) {
            hasData = cursor.getInt(0) > 0;
        }
        cursor.close();
        if (hasData) {
            return;
        }

        insertCourse(db, "周一第一节课", "王老师", Calendar.MONDAY,
                "08:00", "09:40", "07:40", "08:05", "09:35", "10:00", "教学楼 A101");
        insertCourse(db, "周二实验课", "刘老师", Calendar.TUESDAY,
                "14:00", "16:30", "13:40", "14:10", "16:20", "16:50", "实验楼 204");
        insertCourse(db, "周三晚自习", "班主任", Calendar.WEDNESDAY,
                "19:00", "21:00", "18:40", "19:10", "20:50", "21:20", "自习教室 301");
        insertCourse(db, "周四到校考勤", "辅导员", Calendar.THURSDAY,
                "08:00", "17:00", "07:30", "08:10", "17:00", "22:00", "校园");
        insertCourse(db, "周五课程签到", "陈老师", Calendar.FRIDAY,
                "10:10", "11:50", "09:50", "10:15", "11:45", "12:05", "教学楼 B203");
    }

    private void insertCourse(SQLiteDatabase db, String courseName, String teacherName, int weekday,
                              String startTime, String endTime, String checkInStart,
                              String checkInDeadline, String checkOutStart,
                              String checkOutDeadline, String classroom) {
        ContentValues values = new ContentValues();
        values.put("course_name", courseName);
        values.put("teacher_name", teacherName);
        values.put("weekday", weekday);
        values.put("start_time", startTime);
        values.put("end_time", endTime);
        values.put("check_in_start", checkInStart);
        values.put("check_in_deadline", checkInDeadline);
        values.put("check_out_start", checkOutStart);
        values.put("check_out_deadline", checkOutDeadline);
        values.put("classroom", classroom);
        values.put("is_enabled", 1);
        db.insert(TABLE_COURSE_SCHEDULE, null, values);
    }

    private AttendanceRecord readRecord(Cursor cursor) {
        return new AttendanceRecord(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("username")),
                cursor.getString(cursor.getColumnIndexOrThrow("date")),
                getStringOrDefault(cursor, "check_in_time", ""),
                getStringOrDefault(cursor, "check_out_time", ""),
                getStringOrDefault(cursor, "status", STATUS_NOT_CHECKED),
                getStringOrDefault(cursor, "remark", ""),
                getStringOrDefault(cursor, "appeal_status", APPEAL_NONE),
                getStringOrDefault(cursor, "appeal_reason", ""),
                getStringOrDefault(cursor, "appeal_result", ""),
                getStringOrDefault(cursor, "appeal_time", ""),
                getStringOrDefault(cursor, "course_name", "普通到校考勤"),
                getStringOrDefault(cursor, "course_type", "日常考勤"),
                getStringOrDefault(cursor, "weekday_label", "")
        );
    }

    private CourseSchedule readCourseSchedule(Cursor cursor) {
        return new CourseSchedule(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                getStringOrDefault(cursor, "course_name", "普通到校考勤"),
                getStringOrDefault(cursor, "teacher_name", ""),
                cursor.getInt(cursor.getColumnIndexOrThrow("weekday")),
                getStringOrDefault(cursor, "start_time", "08:00"),
                getStringOrDefault(cursor, "end_time", "17:00"),
                getStringOrDefault(cursor, "check_in_start", "07:30"),
                getStringOrDefault(cursor, "check_in_deadline", "08:10"),
                getStringOrDefault(cursor, "check_out_start", "17:00"),
                getStringOrDefault(cursor, "check_out_deadline", "22:00"),
                getStringOrDefault(cursor, "classroom", ""),
                cursor.getInt(cursor.getColumnIndexOrThrow("is_enabled")) == 1
        );
    }

    private LeaveRequest readLeaveRequest(Cursor cursor) {
        return new LeaveRequest(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                getStringOrDefault(cursor, "username", ""),
                getStringOrDefault(cursor, "request_type", REQUEST_TYPE_LEAVE),
                getStringOrDefault(cursor, "target_date", ""),
                getStringOrDefault(cursor, "related_course_name", ""),
                getStringOrDefault(cursor, "reason", ""),
                getStringOrDefault(cursor, "status", REQUEST_STATUS_PENDING),
                getStringOrDefault(cursor, "created_at", ""),
                getStringOrDefault(cursor, "processed_at", ""),
                getStringOrDefault(cursor, "result_remark", "")
        );
    }

    private String mergeRemark(String oldRemark, String newRemark) {
        if (TextUtils.isEmpty(newRemark)) {
            return oldRemark == null ? "" : oldRemark;
        }
        if (TextUtils.isEmpty(oldRemark)) {
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
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day);
    }

    private Calendar addDays(int offset) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, offset);
        return calendar;
    }

    private void insertQuarterDemoAttendance(SQLiteDatabase db, String username, Calendar calendar, int sampleIndex) {
        String courseName = getQuarterDemoCourseName(calendar.get(Calendar.DAY_OF_WEEK));
        String courseType = courseName.contains("实验") ? "实验课" : "课程考勤";
        String remark;
        String checkInTime;
        String checkOutTime;
        String status;

        switch (sampleIndex % 9) {
            case 1:
                checkInTime = "08:16:00";
                checkOutTime = "17:20:00";
                status = STATUS_LATE;
                remark = "超过签到截止时间";
                break;
            case 2:
                checkInTime = "07:58:00";
                checkOutTime = "16:36:00";
                status = STATUS_EARLY_LEAVE;
                remark = "早于签退开始时间";
                break;
            case 3:
                checkInTime = "08:18:00";
                checkOutTime = "16:38:00";
                status = STATUS_LATE_EARLY;
                remark = "迟到且早退";
                break;
            case 4:
                checkInTime = "08:05:00";
                checkOutTime = "";
                status = STATUS_MISSING_CARD;
                remark = "超过签退截止时间未签退";
                break;
            case 5:
                checkInTime = "";
                checkOutTime = "";
                status = STATUS_LEAVE;
                remark = "演示请假记录";
                courseType = "请假";
                break;
            case 6:
                checkInTime = "08:10:00";
                checkOutTime = "17:05:00";
                status = STATUS_LATE;
                remark = "轻微迟到";
                break;
            case 7:
                checkInTime = "07:52:00";
                checkOutTime = "17:18:00";
                status = STATUS_NORMAL;
                remark = "课堂正常";
                break;
            case 8:
                checkInTime = "08:01:00";
                checkOutTime = "17:08:00";
                status = STATUS_NORMAL;
                remark = "到课正常";
                break;
            default:
                checkInTime = "07:56:00";
                checkOutTime = "17:15:00";
                status = STATUS_NORMAL;
                remark = "课堂正常";
                break;
        }

        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("date", formatDate(calendar));
        values.put("check_in_time", checkInTime);
        values.put("check_out_time", checkOutTime);
        values.put("status", status);
        values.put("remark", remark);
        values.put("appeal_status", APPEAL_NONE);
        values.put("appeal_reason", "");
        values.put("appeal_result", "");
        values.put("appeal_time", "");
        values.put("course_name", courseName);
        values.put("course_type", courseType);
        values.put("weekday_label", getQuarterDemoWeekdayLabel(calendar.get(Calendar.DAY_OF_WEEK)));
        db.insert(TABLE_ATTENDANCE, null, values);
    }

    private void createQuarterDemoRequest(String username, String requestType, String targetDate,
                                          String relatedCourseName, String reason,
                                          String finalStatus, String resultRemark) {
        boolean created = submitLeaveRequest(username, requestType, targetDate, relatedCourseName, reason, nowText());
        if (!created || REQUEST_STATUS_PENDING.equals(finalStatus)) {
            return;
        }

        List<LeaveRequest> requests = getUserLeaveRequests(username);
        for (LeaveRequest request : requests) {
            if (requestType.equals(request.getRequestType())
                    && targetDate.equals(request.getTargetDate())
                    && REQUEST_STATUS_PENDING.equals(request.getStatus())) {
                updateLeaveRequestStatus(request.getId(), finalStatus, nowText(), resultRemark);
                break;
            }
        }
    }

    private String getQuarterDemoCourseName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return "周一第一节课";
            case Calendar.TUESDAY:
                return "周二实验课";
            case Calendar.WEDNESDAY:
                return "周三晚自习";
            case Calendar.THURSDAY:
                return "周四到校考勤";
            case Calendar.FRIDAY:
                return "周五课程签到";
            default:
                return "普通到校考勤";
        }
    }

    private String getQuarterDemoWeekdayLabel(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return "周一";
            case Calendar.TUESDAY:
                return "周二";
            case Calendar.WEDNESDAY:
                return "周三";
            case Calendar.THURSDAY:
                return "周四";
            case Calendar.FRIDAY:
                return "周五";
            case Calendar.SATURDAY:
                return "周六";
            default:
                return "周日";
        }
    }

    private String getQuarterDemoDate(int monthsAgo, int dayOffset) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, -monthsAgo);
        calendar.add(Calendar.DAY_OF_MONTH, dayOffset);
        return formatDate(calendar);
    }

    private void normalizeToStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private String nowText() {
        Calendar calendar = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d:%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
    }

    private void addColumnIfNeeded(SQLiteDatabase db, String tableName, String columnSql) {
        try {
            db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnSql);
        } catch (Exception ignored) {
        }
    }

    private String getStringOrDefault(Cursor cursor, String columnName, String defaultValue) {
        int index = cursor.getColumnIndex(columnName);
        if (index < 0) {
            return defaultValue;
        }
        String value = cursor.getString(index);
        return value == null ? defaultValue : value;
    }
}
