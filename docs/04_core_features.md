# 核心功能说明

## 1. 用户注册

用户在注册页输入用户名、密码和确认密码。系统首先检查输入是否为空，再检查两次密码是否一致，最后调用 `DatabaseHelper.registerUser()` 写入 SQLite。

如果用户名重复，数据库不会插入新记录，并提示“用户名已存在，请更换”。

## 2. 用户登录与记住状态

登录时，系统调用 `DatabaseHelper.validateUser()` 查询 `user` 表。如果用户名和密码匹配，则调用 `SessionManager.saveLogin()` 将登录状态和用户名保存到 SharedPreferences。

应用再次启动时，`SplashActivity` 会读取 SharedPreferences。如果检测到已登录用户，则直接进入首页。

## 3. 每日签到

用户点击首页“签到”按钮后，系统获取当前日期和时间，并查询当前用户当天是否已有记录。

如果没有记录，则插入一条 attendance 数据，签到时间为当前时间，签退时间为空，状态为“已签到”。如果当天已经签到，则 Toast 提示重复签到。

## 4. 每日签退

用户点击首页“签退”按钮后，系统查询当天考勤记录。

如果没有签到记录，提示“请先完成签到再签退”。如果已经签退，提示“今天已经签退过了”。如果符合条件，则更新签退时间，并将状态改为“已完成”。

## 5. 历史记录查询

历史记录页通过 `DatabaseHelper.getAttendanceRecords(username)` 查询当前用户的全部考勤记录，并按照日期倒序排列。

页面使用 RecyclerView 展示列表，每条记录显示日期、签到时间、签退时间和状态。如果没有记录，则显示空状态提示。

## 6. 个人中心

个人中心页面显示当前登录用户名，提供退出登录按钮。点击退出后，系统清空 SharedPreferences 中的登录状态，并跳转回登录页。

“修改密码”按钮目前作为扩展入口保留，后续可以继续通过 SQLite 更新 user 表中的 password 字段。

## 7. 打卡提醒

提醒功能位于首页“演示打卡提醒”按钮。点击后系统会立即发送通知，并使用 AlarmManager 安排 10 秒后的广播提醒。

`AttendanceReminderReceiver` 接收到广播后调用 `ReminderHelper.showAttendanceNotification()` 再次发送通知。该实现便于课堂演示 BroadcastReceiver 和通知功能。

首页还提供每日 8:00 提醒开关。开启后，系统将提醒状态保存到 SharedPreferences，并通过 AlarmManager 注册每日提醒；设备重启后，BootReceiver 会重新注册提醒。

## 8. 修改密码与统计功能

个人中心提供修改密码入口。用户输入原密码、新密码和确认新密码后，系统校验原密码是否正确，并更新 SQLite 中 user 表的 password 字段。

首页新增本月统计区域，用于展示当前用户本月已签到天数和完整打卡天数。该功能通过 SQLite 条件查询和状态统计实现。

## 9. 可扩展方向

当前项目没有使用 Service 和 ContentProvider，因为本系统数据规模较小，主要功能集中在本地页面和数据库操作中。后续如果扩展后台定位、长期提醒或跨应用数据共享，可以增加前台 Service 或 ContentProvider。
