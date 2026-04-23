package com.haoyinrui.campusattendance.util;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.haoyinrui.campusattendance.MainActivity;
import com.haoyinrui.campusattendance.R;
import com.haoyinrui.campusattendance.receiver.AttendanceReminderReceiver;

import java.util.Calendar;

/**
 * 提醒工具类：封装通知渠道、通知发送和 AlarmManager 演示提醒。
 */
public class ReminderHelper {
    public static final String CHANNEL_ID = "attendance_reminder_channel";
    public static final String ACTION_DEMO_REMINDER = "com.haoyinrui.campusattendance.DEMO_REMINDER";
    public static final String ACTION_SIGN_IN_REMINDER = "com.haoyinrui.campusattendance.SIGN_IN_REMINDER";
    public static final String ACTION_CHECK_OUT_REMINDER = "com.haoyinrui.campusattendance.CHECK_OUT_REMINDER";
    private static final String PREF_NAME = "attendance_reminder_pref";
    private static final String KEY_DAILY_ENABLED = "daily_enabled";
    private static final String KEY_SIGN_IN_ENABLED = "sign_in_enabled";
    private static final String KEY_CHECK_OUT_ENABLED = "check_out_enabled";
    private static final String KEY_SIGN_IN_TIME = "sign_in_time";
    private static final String KEY_CHECK_OUT_TIME = "check_out_time";
    private static final int NOTIFICATION_ID = 2001;
    private static final int REQUEST_OPEN_MAIN = 3001;
    private static final int REQUEST_ALARM_REMINDER = 3002;
    private static final int REQUEST_DAILY_REMINDER = 3003;
    private static final int REQUEST_SIGN_IN_REMINDER = 3004;
    private static final int REQUEST_CHECK_OUT_REMINDER = 3005;

    private ReminderHelper() {
    }

    public static boolean showAttendanceNotification(Context context, String title, String message) {
        createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_OPEN_MAIN,
                intent,
                getPendingIntentFlag());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_attendance)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        return true;
    }

    /**
     * 安排一个 10 秒后的演示提醒，便于课堂现场看到 BroadcastReceiver 被触发。
     */
    public static void scheduleDemoReminder(Context context) {
        Intent intent = new Intent(context, AttendanceReminderReceiver.class);
        intent.setAction(ACTION_DEMO_REMINDER);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_ALARM_REMINDER,
                intent,
                getPendingIntentFlag());

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            long triggerAtMillis = System.currentTimeMillis() + 10 * 1000;
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    /**
     * 开启每日 8:00 打卡提醒，状态保存到 SharedPreferences。
     */
    public static void enableDailyReminder(Context context) {
        getPreferences(context).edit()
                .putBoolean(KEY_DAILY_ENABLED, true)
                .putBoolean(KEY_SIGN_IN_ENABLED, true)
                .apply();
        scheduleDailyReminder(context);
    }

    /**
     * 关闭每日提醒，同时取消 AlarmManager 中的 PendingIntent。
     */
    public static void disableDailyReminder(Context context) {
        getPreferences(context).edit().putBoolean(KEY_DAILY_ENABLED, false).apply();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(createDailyReminderPendingIntent(context));
        }
    }

    public static boolean isDailyReminderEnabled(Context context) {
        return getPreferences(context).getBoolean(KEY_DAILY_ENABLED, false);
    }

    public static void enableSignInReminder(Context context, String time) {
        getPreferences(context).edit()
                .putBoolean(KEY_SIGN_IN_ENABLED, true)
                .putString(KEY_SIGN_IN_TIME, time)
                .apply();
        scheduleRepeatingReminder(context, ACTION_SIGN_IN_REMINDER, REQUEST_SIGN_IN_REMINDER, time);
    }

    public static void disableSignInReminder(Context context) {
        getPreferences(context).edit().putBoolean(KEY_SIGN_IN_ENABLED, false).apply();
        cancelReminder(context, ACTION_SIGN_IN_REMINDER, REQUEST_SIGN_IN_REMINDER);
    }

    public static boolean isSignInReminderEnabled(Context context) {
        return getPreferences(context).getBoolean(KEY_SIGN_IN_ENABLED, false);
    }

    public static void enableCheckOutReminder(Context context, String time) {
        getPreferences(context).edit()
                .putBoolean(KEY_CHECK_OUT_ENABLED, true)
                .putString(KEY_CHECK_OUT_TIME, time)
                .apply();
        scheduleRepeatingReminder(context, ACTION_CHECK_OUT_REMINDER, REQUEST_CHECK_OUT_REMINDER, time);
    }

    public static void disableCheckOutReminder(Context context) {
        getPreferences(context).edit().putBoolean(KEY_CHECK_OUT_ENABLED, false).apply();
        cancelReminder(context, ACTION_CHECK_OUT_REMINDER, REQUEST_CHECK_OUT_REMINDER);
    }

    public static boolean isCheckOutReminderEnabled(Context context) {
        return getPreferences(context).getBoolean(KEY_CHECK_OUT_ENABLED, false);
    }

    public static void restoreEnabledReminders(Context context) {
        SharedPreferences preferences = getPreferences(context);
        if (preferences.getBoolean(KEY_SIGN_IN_ENABLED, false)) {
            scheduleRepeatingReminder(
                    context,
                    ACTION_SIGN_IN_REMINDER,
                    REQUEST_SIGN_IN_REMINDER,
                    preferences.getString(KEY_SIGN_IN_TIME, "07:30"));
        }
        if (preferences.getBoolean(KEY_CHECK_OUT_ENABLED, false)) {
            scheduleRepeatingReminder(
                    context,
                    ACTION_CHECK_OUT_REMINDER,
                    REQUEST_CHECK_OUT_REMINDER,
                    preferences.getString(KEY_CHECK_OUT_TIME, "17:00"));
        }
        if (preferences.getBoolean(KEY_DAILY_ENABLED, false)) {
            scheduleDailyReminder(context);
        }
    }

    /**
     * 安排每天早上 8 点提醒。使用 setInexactRepeating，稳定性较好且不需要额外精确闹钟权限。
     */
    public static void scheduleDailyReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                createDailyReminderPendingIntent(context));
    }

    private static PendingIntent createDailyReminderPendingIntent(Context context) {
        Intent intent = new Intent(context, AttendanceReminderReceiver.class);
        intent.setAction(ACTION_SIGN_IN_REMINDER);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_DAILY_REMINDER,
                intent,
                getPendingIntentFlag());
    }

    private static void scheduleRepeatingReminder(Context context, String action, int requestCode, String time) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        String normalizedTime = AttendanceRuleManager.normalizeTime(time);
        String[] parts = normalizedTime.split(":");
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                createReminderPendingIntent(context, action, requestCode));
    }

    private static void cancelReminder(Context context, String action, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(createReminderPendingIntent(context, action, requestCode));
        }
    }

    private static PendingIntent createReminderPendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, AttendanceReminderReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                getPendingIntentFlag());
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "校园考勤提醒",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("用于提示学生完成签到或签退");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private static int getPendingIntentFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.FLAG_UPDATE_CURRENT;
    }
}
