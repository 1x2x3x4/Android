package com.haoyinrui.campusattendance.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.haoyinrui.campusattendance.util.ReminderHelper;

/**
 * BroadcastReceiver：接收 AlarmManager 广播后发送打卡提醒通知。
 */
public class AttendanceReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        String message = "这是定时提醒：请检查今天是否已完成签到和签退。";
        String channelId = ReminderHelper.CHANNEL_ID;
        if (ReminderHelper.ACTION_SIGN_IN_REMINDER.equals(action)) {
            message = "签到时间到了，请及时完成今日校园签到。";
            channelId = ReminderHelper.CHANNEL_SIGN_IN_ID;
        } else if (ReminderHelper.ACTION_CHECK_OUT_REMINDER.equals(action)) {
            message = "签退时间到了，请记得完成今日校园签退。";
            channelId = ReminderHelper.CHANNEL_CHECK_OUT_ID;
        }
        ReminderHelper.showAttendanceNotification(
                context,
                "校园考勤提醒",
                message,
                channelId);
    }
}
