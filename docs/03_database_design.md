# 数据库表结构说明

## 1. 数据库概述

项目使用 Android 原生 SQLite 存储数据，数据库帮助类为：

```text
app/src/main/java/com/haoyinrui/campusattendance/data/DatabaseHelper.java
```

数据库名称为 `campus_attendance.db`，当前版本号为 1。数据库中包含两张核心表：`user` 表和 `attendance` 表。

## 2. user 表

`user` 表用于保存注册用户信息。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 用户编号 |
| username | TEXT | UNIQUE | 用户名，不能重复 |
| password | TEXT | 无 | 用户密码 |

建表语句核心内容如下：

```sql
CREATE TABLE user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE,
    password TEXT
);
```

注册时，系统会先查询用户名是否已存在。如果不存在，则插入新用户；如果已存在，则通过 Toast 提示用户更换用户名。

## 3. attendance 表

`attendance` 表用于保存学生每日签到和签退记录。

| 字段名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | 记录编号 |
| username | TEXT | 与用户关联 | 当前登录用户名 |
| date | TEXT | 与 username 组成唯一约束 | 打卡日期 |
| check_in_time | TEXT | 无 | 签到时间 |
| check_out_time | TEXT | 无 | 签退时间 |
| status | TEXT | 无 | 当前状态 |

建表语句核心内容如下：

```sql
CREATE TABLE attendance (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT,
    date TEXT,
    check_in_time TEXT,
    check_out_time TEXT,
    status TEXT,
    UNIQUE(username, date)
);
```

`UNIQUE(username, date)` 用于保证同一用户同一天只有一条考勤记录。签到时插入记录，签退时更新同一条记录。

## 4. 状态设计

系统中使用三个主要状态：

| 状态 | 含义 |
| --- | --- |
| 未签到 | 当天还没有生成签到记录 |
| 已签到 | 已完成签到，但尚未签退 |
| 已完成 | 当天签到和签退均已完成 |

通过状态字段可以在首页和历史记录页中直观展示考勤结果。
