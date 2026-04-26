package com.haoyinrui.campusattendance.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.haoyinrui.campusattendance.AttendanceSettingsActivity;
import com.haoyinrui.campusattendance.ChangePasswordActivity;
import com.haoyinrui.campusattendance.LoginActivity;
import com.haoyinrui.campusattendance.MyRequestsActivity;
import com.haoyinrui.campusattendance.R;
import com.haoyinrui.campusattendance.TeacherDashboardActivity;
import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.model.MonthlyReportData;
import com.haoyinrui.campusattendance.util.AttendanceExportHelper;
import com.haoyinrui.campusattendance.util.SessionManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {
    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper;
    private android.view.View rootView;
    private TextView textProfileUsername;
    private TextView textAvatarInitial;
    private TextView textProfileHint;

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull android.view.LayoutInflater inflater,
                                          @Nullable android.view.ViewGroup container,
                                          @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_profile, container, false);
        sessionManager = new SessionManager(requireContext());
        databaseHelper = new DatabaseHelper(requireContext());
        textProfileUsername = rootView.findViewById(R.id.textProfileUsername);
        textAvatarInitial = rootView.findViewById(R.id.textAvatarInitial);
        textProfileHint = rootView.findViewById(R.id.textProfileHint);
        bindEvents(rootView);
        refreshUserInfo();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUserInfo();
    }

    private void bindEvents(android.view.View view) {
        android.view.View.OnClickListener openSettingsListener = v ->
                startActivity(new Intent(requireContext(), AttendanceSettingsActivity.class));

        view.findViewById(R.id.buttonOpenSettings).setOnClickListener(openSettingsListener);
        view.findViewById(R.id.itemOpenSettings).setOnClickListener(openSettingsListener);
        view.findViewById(R.id.itemMyRequests).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MyRequestsActivity.class)));
        view.findViewById(R.id.itemExportReport).setOnClickListener(v -> exportMonthlyReport());
        view.findViewById(R.id.itemTeacherDemo).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TeacherDashboardActivity.class)));
        view.findViewById(R.id.itemChangePassword).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChangePasswordActivity.class)));
        view.findViewById(R.id.itemProjectIntro).setOnClickListener(v -> showProjectDialog());
        view.findViewById(R.id.itemAboutApp).setOnClickListener(v -> showAboutDialog());
        view.findViewById(R.id.itemLogout).setOnClickListener(v -> logout());
    }

    private void refreshUserInfo() {
        if (sessionManager == null) {
            return;
        }
        String username = sessionManager.getUsername();
        textProfileUsername.setText(username);
        textProfileHint.setText("校园考勤系统用户");
        if (username != null && !username.isEmpty()) {
            textAvatarInitial.setText(username.substring(0, 1).toUpperCase(Locale.getDefault()));
        } else {
            textAvatarInitial.setText("U");
        }
    }

    private void exportMonthlyReport() {
        try {
            String monthPrefix = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
            MonthlyReportData reportData = databaseHelper.getMonthlyReportData(sessionManager.getUsername(), monthPrefix);
            File file = AttendanceExportHelper.exportMonthlyCsv(requireContext(), reportData);
            new AlertDialog.Builder(requireContext())
                    .setTitle("导出成功")
                    .setMessage("文件已生成：\n" + file.getAbsolutePath())
                    .setPositiveButton("知道了", null)
                    .show();
        } catch (Exception e) {
            showMessage("导出失败");
        }
    }

    private void showProjectDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("项目说明")
                .setMessage("CampusAttendance 是基于 Android 的校园考勤课程设计项目。")
                .setPositiveButton("知道了", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("关于应用")
                .setMessage("版本 1.0\nJava + XML + SQLite")
                .setPositiveButton("确定", null)
                .show();
    }

    private void logout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("退出登录")
                .setMessage("确定退出？")
                .setPositiveButton("退出", (dialog, which) -> {
                    sessionManager.logout();
                    Toast.makeText(requireContext(), "已退出", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showMessage(String message) {
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
