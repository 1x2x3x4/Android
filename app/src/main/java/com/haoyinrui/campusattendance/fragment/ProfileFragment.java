package com.haoyinrui.campusattendance.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.haoyinrui.campusattendance.ChangePasswordActivity;
import com.haoyinrui.campusattendance.LoginActivity;
import com.haoyinrui.campusattendance.R;
import com.haoyinrui.campusattendance.util.SessionManager;

/**
 * 我的 Fragment：集中展示用户信息、修改密码和退出登录。
 */
public class ProfileFragment extends Fragment {
    private SessionManager sessionManager;
    private TextView textProfileUsername;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        sessionManager = new SessionManager(requireContext());
        textProfileUsername = view.findViewById(R.id.textProfileUsername);
        bindEvents(view);
        refreshUserInfo();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUserInfo();
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.buttonChangePassword).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChangePasswordActivity.class)));
        view.findViewById(R.id.buttonLogout).setOnClickListener(v -> logout());
    }

    private void refreshUserInfo() {
        if (textProfileUsername != null && sessionManager != null) {
            textProfileUsername.setText("当前用户：" + sessionManager.getUsername());
        }
    }

    private void logout() {
        sessionManager.logout();
        Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
