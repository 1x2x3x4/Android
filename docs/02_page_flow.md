# 页面跳转流程说明

## 1. 页面列表

系统共包含 6 个主要页面：

| 页面 | 类名 | 主要职责 |
| --- | --- | --- |
| 启动页 | SplashActivity | 判断用户是否已登录 |
| 登录页 | LoginActivity | 输入用户名和密码并登录 |
| 注册页 | RegisterActivity | 创建新用户 |
| 首页 | MainActivity | 显示今日状态，完成签到和签退 |
| 历史记录页 | RecordActivity | 展示当前用户的考勤记录 |
| 个人中心页 | ProfileActivity | 展示用户信息，退出登录 |

## 2. 启动流程

应用启动后首先进入 `SplashActivity`。该页面读取 SharedPreferences 中保存的登录状态：

1. 如果用户已登录，自动跳转到 `MainActivity`。
2. 如果用户未登录，跳转到 `LoginActivity`。

这样可以实现“记住登录状态”的效果，用户关闭应用后再次打开时无需重复登录。

## 3. 登录与注册流程

登录页提供用户名输入框、密码输入框、登录按钮和注册入口。

登录流程如下：

```text
LoginActivity
输入用户名和密码
调用 DatabaseHelper.validateUser()
校验成功：SessionManager.saveLogin()，进入 MainActivity
校验失败：Toast 提示用户名或密码错误
```

注册流程如下：

```text
LoginActivity 点击注册入口
进入 RegisterActivity
输入用户名、密码、确认密码
调用 DatabaseHelper.registerUser()
注册成功：返回登录页
注册失败：提示用户名已存在或输入不合法
```

## 4. 首页功能流程

首页显示欢迎信息、今天日期、今日签到时间、签退时间和当前状态。

用户点击“签到”时：

1. 获取当前日期和当前时间。
2. 查询当天是否已有签到记录。
3. 如果未签到，则向 attendance 表插入记录。
4. 如果已签到，则提示“今天已经签到过了”。

用户点击“签退”时：

1. 查询当天记录。
2. 如果尚未签到，提示先签到。
3. 如果已签退，提示重复签退。
4. 如果符合条件，则更新签退时间和状态。

## 5. 历史记录与个人中心流程

从首页点击“查看历史打卡记录”进入 `RecordActivity`，页面通过 RecyclerView 展示当前登录用户的考勤记录。如果没有数据，则显示“暂无打卡记录”。

从首页点击“个人中心”进入 `ProfileActivity`，页面显示当前用户名，并提供退出登录按钮。退出登录后清空 SharedPreferences，并返回登录页。

## 6. 提醒流程

首页点击“演示打卡提醒”后：

1. 立即发送一条系统通知。
2. 使用 AlarmManager 安排 10 秒后的广播。
3. `AttendanceReminderReceiver` 接收广播后再次发送提醒通知。

该功能覆盖了 BroadcastReceiver、AlarmManager 和 Notification 的基础使用方式。
