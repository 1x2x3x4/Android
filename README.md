# CampusAttendance

基于 Android 的校园考勤打卡应用，使用 Java + XML + SQLite + SharedPreferences 实现。

## 项目说明

- 项目名称：`CampusAttendance`
- 包名：`com.haoyinrui.campusattendance`
- 最低版本：Android 7.0（API 24）
- 构建方式：Gradle

## 主要功能

- 用户注册、登录、记住登录状态、退出登录
- 首页签到、签退、今日状态展示
- 课程表联动考勤与普通到校考勤回退
- 月历式考勤记录查看
- 异常申诉、请假、补签、销假申请
- 教师视角演示页
- 月度统计与图形化展示
- 提醒、定位校验、导出月报

## 页面结构

- `LoginActivity`：登录
- `RegisterActivity`：注册
- `MainActivity`：主容器
  - `HomeFragment`：首页
  - `RecordFragment`：记录
  - `ProfileFragment`：我的
- `AttendanceSettingsActivity`：设置
- `MyRequestsActivity`：我的申请
- `TeacherDashboardActivity`：教师视角
- `CourseScheduleActivity`：课程表

## 运行方式

1. 用 Android Studio 打开本项目根目录。
2. 等待 Gradle 同步完成。
3. 连接模拟器或真机。
4. 运行应用。

## 构建命令

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
```

## 演示数据

设置页提供“写入三个月演示数据”入口，也可以使用仓库中的脚本直接导入 SQLite 数据库：

```powershell
python tools\import_quarter_demo.py --db "数据库文件路径" --username admin
```

## 目录说明

- `app/`：Android 应用源码
- `gradle/`：Gradle 包装器配置
- `tools/`：辅助脚本

