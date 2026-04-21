# CampusAttendance 项目功能结构说明

## 1. 项目概述

CampusAttendance 是一个基于 Android 的校园考勤打卡应用，主要面向学生用户。系统提供用户注册、登录、每日签到、每日签退、历史记录查询、个人中心和打卡提醒等功能。

项目采用 Java + XML + SQLite + SharedPreferences 的原生 Android 实现方式，符合课程设计对 Activity、RecyclerView、SQLite、SharedPreferences、BroadcastReceiver 和 Intent 等知识点的要求。

## 2. 技术结构

项目保留 Android Studio 默认的 `app` 模块，包名为 `com.haoyinrui.campusattendance`。

主要目录结构如下：

```text
app/src/main/java/com/haoyinrui/campusattendance
├── SplashActivity.java              启动页，判断登录状态
├── LoginActivity.java               登录页
├── RegisterActivity.java            注册页
├── MainActivity.java                首页，负责签到、签退和状态展示
├── RecordActivity.java              历史记录页
├── ProfileActivity.java             个人中心页
├── adapter/AttendanceAdapter.java   RecyclerView 适配器
├── data/DatabaseHelper.java         SQLite 数据库帮助类
├── model/AttendanceRecord.java      考勤记录实体类
├── receiver/AttendanceReminderReceiver.java  提醒广播接收器
└── util/
    ├── ReminderHelper.java          通知和闹钟提醒工具类
    └── SessionManager.java          登录状态管理类
```

资源文件位于 `app/src/main/res`，其中 `layout` 目录保存页面布局，`drawable` 目录保存按钮、输入框、面板背景和通知图标，`values` 目录保存颜色、字符串和主题配置。

## 3. 功能模块划分

1. 用户模块：注册、登录、保存登录状态、退出登录。
2. 考勤模块：每日签到、每日签退、重复操作判断、首页状态刷新。
3. 记录模块：从 SQLite 查询当前用户历史记录，并通过 RecyclerView 展示。
4. 个人中心模块：展示当前用户名，提供退出登录和修改密码预留入口。
5. 提醒模块：使用 Notification 展示提醒，并通过 AlarmManager + BroadcastReceiver 演示定时提醒。

## 4. 设计特点

项目没有引入 Kotlin、Room、Jetpack Compose、Hilt、MVVM 等复杂技术，整体保持课程作业常见的 Activity + SQLiteOpenHelper 结构。代码中对关键逻辑添加了注释，便于报告撰写和答辩讲解。
