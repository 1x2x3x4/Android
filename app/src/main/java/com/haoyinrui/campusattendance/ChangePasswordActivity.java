package com.haoyinrui.campusattendance;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.haoyinrui.campusattendance.data.DatabaseHelper;
import com.haoyinrui.campusattendance.util.SessionManager;

/**
 * 修改密码页：作为个人中心的后续功能，演示 SQLite 更新操作。
 */
public class ChangePasswordActivity extends AppCompatActivity {
    private EditText editOldPassword;
    private EditText editNewPassword;
    private EditText editConfirmNewPassword;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        editOldPassword = findViewById(R.id.editOldPassword);
        editNewPassword = findViewById(R.id.editNewPassword);
        editConfirmNewPassword = findViewById(R.id.editConfirmNewPassword);

        findViewById(R.id.buttonSavePassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changePassword();
            }
        });

        findViewById(R.id.buttonBackFromChangePassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void changePassword() {
        String oldPassword = editOldPassword.getText().toString().trim();
        String newPassword = editNewPassword.getText().toString().trim();
        String confirmPassword = editConfirmNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(oldPassword)) {
            Toast.makeText(this, "请输入原密码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "请输入新密码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPassword.length() < 4) {
            Toast.makeText(this, "新密码至少 4 位", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }
        if (oldPassword.equals(newPassword)) {
            Toast.makeText(this, "新密码不能与原密码相同", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = databaseHelper.updatePassword(
                sessionManager.getUsername(),
                oldPassword,
                newPassword);
        if (success) {
            Toast.makeText(this, "密码修改成功，请牢记新密码", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "原密码错误，修改失败", Toast.LENGTH_SHORT).show();
        }
    }
}
