package com.haoyinrui.campusattendance.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.haoyinrui.campusattendance.util.ReminderHelper;

/**
 * 开机广播接收器：设备重启后，如果用户开启了每日提醒，则重新注册闹钟。
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                && ReminderHelper.isDailyReminderEnabled(context)) {
            ReminderHelper.scheduleDailyReminder(context);
        }
    }
}
