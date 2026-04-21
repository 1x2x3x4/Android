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
        ReminderHelper.showAttendanceNotification(
                context,
                "校园考勤提醒",
                "这是定时提醒：请检查今天是否已完成签到和签退。");
    }
}
