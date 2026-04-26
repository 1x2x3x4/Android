package com.haoyinrui.campusattendance.util;

import android.content.Context;
import android.os.Environment;

import com.haoyinrui.campusattendance.model.MonthlyReportData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 月度考勤导出工具，当前提供 CSV 导出。
 */
public final class AttendanceExportHelper {
    private AttendanceExportHelper() {
    }

    public static File exportMonthlyCsv(Context context, MonthlyReportData data) throws IOException {
        File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (directory == null) {
            directory = context.getFilesDir();
        }
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("无法创建导出目录");
        }

        String safeMonth = data.getMonth().replace("-", "");
        File file = new File(directory, "attendance_report_" + data.getUsername() + "_" + safeMonth + ".csv");
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(buildCsvContent(data).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
        return file;
    }

    private static String buildCsvContent(MonthlyReportData data) {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("用户名,月份,总签到天数,正常次数,迟到次数,早退次数,缺卡次数,请假次数,补签申请次数\n");
        builder.append(csv(data.getUsername())).append(',')
                .append(csv(data.getMonth())).append(',')
                .append(data.getTotalCheckInDays()).append(',')
                .append(data.getNormalCount()).append(',')
                .append(data.getLateCount()).append(',')
                .append(data.getEarlyLeaveCount()).append(',')
                .append(data.getMissingCount()).append(',')
                .append(data.getLeaveCount()).append(',')
                .append(data.getMakeUpRequestCount()).append('\n');

        builder.append('\n');
        builder.append("课程统计\n");
        builder.append("课程名称,次数\n");
        for (Map.Entry<String, Integer> entry : data.getCourseCountMap().entrySet()) {
            builder.append(csv(entry.getKey())).append(',')
                    .append(entry.getValue()).append('\n');
        }
        return builder.toString();
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
