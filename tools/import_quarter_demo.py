#!/usr/bin/env python3
import argparse
import sqlite3
from dataclasses import dataclass
from datetime import date, datetime, timedelta
from pathlib import Path


STATUS_NOT_CHECKED = "未签到"
STATUS_CHECKED_IN = "已签到"
STATUS_NORMAL = "正常"
STATUS_LATE = "迟到"
STATUS_EARLY_LEAVE = "早退"
STATUS_LATE_EARLY = "迟到早退"
STATUS_MISSING_CARD = "缺卡"
STATUS_LEAVE = "请假"

APPEAL_NONE = "未申诉"

REQUEST_TYPE_LEAVE = "请假"
REQUEST_TYPE_MAKE_UP = "补签"
REQUEST_TYPE_CANCEL_LEAVE = "销假"
REQUEST_STATUS_PENDING = "待处理"
REQUEST_STATUS_APPROVED = "已通过"
REQUEST_STATUS_REJECTED = "已驳回"


@dataclass
class DemoAttendance:
    check_in_time: str
    check_out_time: str
    status: str
    remark: str
    course_name: str
    course_type: str
    weekday_label: str


def parse_args():
    parser = argparse.ArgumentParser(
        description="向 CampusAttendance SQLite 数据库写入最近三个月模拟数据。"
    )
    parser.add_argument("--db", required=True, help="SQLite 数据库文件路径")
    parser.add_argument("--username", required=True, help="要写入的用户名")
    parser.add_argument(
        "--keep-existing",
        action="store_true",
        help="保留当前用户已有考勤和申请记录；默认会先清空",
    )
    return parser.parse_args()


def ensure_database_exists(db_path: Path):
    if not db_path.exists():
        raise FileNotFoundError(f"数据库文件不存在: {db_path}")


def ensure_tables(conn: sqlite3.Connection):
    tables = {row[0] for row in conn.execute("SELECT name FROM sqlite_master WHERE type='table'")}
    required = {"attendance", "leave_request"}
    missing = required - tables
    if missing:
        raise RuntimeError(f"数据库缺少必要数据表: {', '.join(sorted(missing))}")


def fmt_day(day: date) -> str:
    return day.strftime("%Y-%m-%d")


def now_text() -> str:
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def first_day_months_ago(months_ago: int) -> date:
    today = date.today()
    year = today.year
    month = today.month - months_ago
    while month <= 0:
        year -= 1
        month += 12
    return date(year, month, 1)


def weekday_label(py_weekday: int) -> str:
    labels = {
        0: "周一",
        1: "周二",
        2: "周三",
        3: "周四",
        4: "周五",
        5: "周六",
        6: "周日",
    }
    return labels[py_weekday]


def course_name_for_weekday(py_weekday: int) -> str:
    mapping = {
        0: "周一第一节课",
        1: "周二实验课",
        2: "周三晚自习",
        3: "周四到校考勤",
        4: "周五课程签到",
    }
    return mapping.get(py_weekday, "普通到校考勤")


def course_type_for_name(course_name: str) -> str:
    if "实验" in course_name:
        return "实验课"
    return "课程考勤"


def build_demo_attendance(day: date, sample_index: int) -> DemoAttendance:
    course_name = course_name_for_weekday(day.weekday())
    course_type = course_type_for_name(course_name)
    label = weekday_label(day.weekday())

    slot = sample_index % 9
    if slot == 1:
        return DemoAttendance("08:16:00", "17:20:00", STATUS_LATE, "超过签到截止时间", course_name, course_type, label)
    if slot == 2:
        return DemoAttendance("07:58:00", "16:36:00", STATUS_EARLY_LEAVE, "早于签退开始时间", course_name, course_type, label)
    if slot == 3:
        return DemoAttendance("08:18:00", "16:38:00", STATUS_LATE_EARLY, "迟到且早退", course_name, course_type, label)
    if slot == 4:
        return DemoAttendance("08:05:00", "", STATUS_MISSING_CARD, "超过签退截止时间未签退", course_name, course_type, label)
    if slot == 5:
        return DemoAttendance("", "", STATUS_LEAVE, "演示请假记录", course_name, "请假", label)
    if slot == 6:
        return DemoAttendance("08:10:00", "17:05:00", STATUS_LATE, "轻微迟到", course_name, course_type, label)
    if slot == 7:
        return DemoAttendance("07:52:00", "17:18:00", STATUS_NORMAL, "课堂正常", course_name, course_type, label)
    if slot == 8:
        return DemoAttendance("08:01:00", "17:08:00", STATUS_NORMAL, "到课正常", course_name, course_type, label)
    return DemoAttendance("07:56:00", "17:15:00", STATUS_NORMAL, "课堂正常", course_name, course_type, label)


def upsert_attendance(conn: sqlite3.Connection, username: str, day: date, record: DemoAttendance):
    conn.execute(
        """
        INSERT OR REPLACE INTO attendance
        (id, username, date, check_in_time, check_out_time, status, remark,
         appeal_status, appeal_reason, appeal_result, appeal_time,
         course_name, course_type, weekday_label)
        VALUES (
            (SELECT id FROM attendance WHERE username=? AND date=?),
            ?, ?, ?, ?, ?, ?, ?, '', '', '', ?, ?, ?
        )
        """,
        (
            username, fmt_day(day),
            username, fmt_day(day), record.check_in_time, record.check_out_time,
            record.status, record.remark, APPEAL_NONE,
            record.course_name, record.course_type, record.weekday_label,
        ),
    )


def insert_request(
    conn: sqlite3.Connection,
    username: str,
    request_type: str,
    target_date: str,
    related_course_name: str,
    reason: str,
    status: str,
    result_remark: str,
):
    created_at = now_text()
    processed_at = "" if status == REQUEST_STATUS_PENDING else created_at
    conn.execute(
        """
        INSERT INTO leave_request
        (username, request_type, target_date, related_course_name, reason, status,
         created_at, processed_at, result_remark)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            username,
            request_type,
            target_date,
            related_course_name,
            reason,
            status,
            created_at,
            processed_at,
            result_remark,
        ),
    )


def append_remark(old_remark: str, extra: str) -> str:
    if not old_remark:
        return extra
    if extra in old_remark:
        return old_remark
    return old_remark + "；" + extra


def apply_approved_make_up(conn: sqlite3.Connection, username: str, target_date: str):
    row = conn.execute(
        "SELECT remark FROM attendance WHERE username=? AND date=?",
        (username, target_date),
    ).fetchone()
    if not row:
        return
    conn.execute(
        """
        UPDATE attendance
        SET status=?, remark=?
        WHERE username=? AND date=?
        """,
        (
            STATUS_NORMAL,
            append_remark(row[0] or "", "补签申请已通过"),
            username,
            target_date,
        ),
    )


def apply_approved_cancel_leave(conn: sqlite3.Connection, username: str, target_date: str):
    row = conn.execute(
        "SELECT status, remark FROM attendance WHERE username=? AND date=?",
        (username, target_date),
    ).fetchone()
    if not row or row[0] != STATUS_LEAVE:
        return
    conn.execute(
        """
        UPDATE attendance
        SET status=?, remark=?
        WHERE username=? AND date=?
        """,
        (
            STATUS_NOT_CHECKED,
            append_remark(row[1] or "", "销假申请已通过"),
            username,
            target_date,
        ),
    )


def build_target_dates() -> dict:
    dates = {}
    for months_ago, offset in [(2, 8), (1, 12), (1, 5), (0, 3), (0, 9)]:
        day = first_day_months_ago(months_ago) + timedelta(days=offset)
        dates[(months_ago, offset)] = day
    return dates


def clear_user_data(conn: sqlite3.Connection, username: str):
    conn.execute("DELETE FROM attendance WHERE username=?", (username,))
    conn.execute("DELETE FROM leave_request WHERE username=?", (username,))


def import_quarter_demo(conn: sqlite3.Connection, username: str, clear_first: bool):
    if clear_first:
        clear_user_data(conn, username)

    start = first_day_months_ago(2)
    end = date.today()
    current = start
    sample_index = 0

    while current <= end:
        if current.weekday() < 5:
            record = build_demo_attendance(current, sample_index)
            upsert_attendance(conn, username, current, record)
            sample_index += 1
        current += timedelta(days=1)

    targets = build_target_dates()

    insert_request(
        conn,
        username,
        REQUEST_TYPE_LEAVE,
        fmt_day(targets[(2, 8)]),
        "周一第一节课",
        "演示请假：校外竞赛",
        REQUEST_STATUS_APPROVED,
        "教师审批：已通过",
    )

    insert_request(
        conn,
        username,
        REQUEST_TYPE_MAKE_UP,
        fmt_day(targets[(1, 12)]),
        "周三晚自习",
        "演示补签：活动结束后补签",
        REQUEST_STATUS_PENDING,
        "",
    )

    insert_request(
        conn,
        username,
        REQUEST_TYPE_CANCEL_LEAVE,
        fmt_day(targets[(1, 5)]),
        "周五课程签到",
        "演示销假：提前返校",
        REQUEST_STATUS_REJECTED,
        "教师审批：暂不通过",
    )

    insert_request(
        conn,
        username,
        REQUEST_TYPE_LEAVE,
        fmt_day(targets[(0, 3)]),
        "周二实验课",
        "演示请假：身体不适",
        REQUEST_STATUS_PENDING,
        "",
    )

    approved_make_up_date = fmt_day(targets[(0, 9)])
    insert_request(
        conn,
        username,
        REQUEST_TYPE_MAKE_UP,
        approved_make_up_date,
        "周四到校考勤",
        "演示补签：网络异常漏打卡",
        REQUEST_STATUS_APPROVED,
        "教师审批：已通过",
    )
    apply_approved_make_up(conn, username, approved_make_up_date)


def main():
    args = parse_args()
    db_path = Path(args.db).expanduser().resolve()
    ensure_database_exists(db_path)

    with sqlite3.connect(str(db_path)) as conn:
        ensure_tables(conn)
        import_quarter_demo(conn, args.username, clear_first=not args.keep_existing)
        conn.commit()

    print(f"已写入最近三个月演示数据: {db_path}")
    print(f"用户名: {args.username}")


if __name__ == "__main__":
    main()
