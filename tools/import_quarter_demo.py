#!/usr/bin/env python3
import argparse
import sqlite3
from dataclasses import dataclass
from datetime import date, datetime, timedelta
from pathlib import Path


STATUS_NOT_CHECKED = "\u672a\u7b7e\u5230"
STATUS_NORMAL = "\u6b63\u5e38"
STATUS_LATE = "\u8fdf\u5230"
STATUS_EARLY_LEAVE = "\u65e9\u9000"
STATUS_LATE_EARLY = "\u8fdf\u5230\u65e9\u9000"
STATUS_MISSING_CARD = "\u7f3a\u5361"
STATUS_LEAVE = "\u8bf7\u5047"

APPEAL_NONE = "\u672a\u7533\u8bc9"

REQUEST_TYPE_LEAVE = "\u8bf7\u5047"
REQUEST_TYPE_MAKE_UP = "\u8865\u7b7e"
REQUEST_TYPE_CANCEL_LEAVE = "\u9500\u5047"
REQUEST_STATUS_PENDING = "\u5f85\u5904\u7406"
REQUEST_STATUS_APPROVED = "\u5df2\u901a\u8fc7"
REQUEST_STATUS_REJECTED = "\u5df2\u9a73\u56de"


@dataclass
class DemoAttendance:
    check_in_time: str
    check_out_time: str
    status: str
    remark: str
    course_name: str
    course_type: str
    weekday_label: str


ANDROID_TO_PY = {1: 6, 2: 0, 3: 1, 4: 2, 5: 3, 6: 4, 7: 5}
PY_TO_LABEL = {
    0: "\u5468\u4e00",
    1: "\u5468\u4e8c",
    2: "\u5468\u4e09",
    3: "\u5468\u56db",
    4: "\u5468\u4e94",
    5: "\u5468\u516d",
    6: "\u5468\u65e5",
}


def parse_args():
    parser = argparse.ArgumentParser(
        description="Import recent three months of demo attendance data into CampusAttendance SQLite database."
    )
    parser.add_argument("--db", required=True, help="SQLite database file path")
    parser.add_argument("--username", required=True, help="Target username")
    parser.add_argument(
        "--keep-existing",
        action="store_true",
        help="Keep existing attendance and request rows for this user",
    )
    return parser.parse_args()


def ensure_database_exists(db_path: Path):
    if not db_path.exists():
        raise FileNotFoundError(f"Database file not found: {db_path}")


def ensure_tables(conn: sqlite3.Connection):
    tables = {row[0] for row in conn.execute("SELECT name FROM sqlite_master WHERE type='table'")}
    required = {"attendance", "leave_request", "course_schedule"}
    missing = required - tables
    if missing:
        raise RuntimeError(f"Missing tables: {', '.join(sorted(missing))}")


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


def append_remark(old_remark: str, extra: str) -> str:
    if not old_remark:
        return extra
    if extra in old_remark:
        return old_remark
    return old_remark + "\uff1b" + extra


def load_schedule_by_weekday(conn: sqlite3.Connection):
    rows = conn.execute(
        """
        SELECT course_name, weekday, start_time, end_time,
               check_in_start, check_in_deadline, check_out_start, check_out_deadline,
               classroom, is_enabled
        FROM course_schedule
        WHERE is_enabled=1
        ORDER BY weekday, start_time
        """
    ).fetchall()
    schedule_map = {}
    for row in rows:
        py_weekday = ANDROID_TO_PY[row[1]]
        schedule_map[py_weekday] = row
    return schedule_map


def build_demo_attendance(day: date, sample_index: int, course_name: str) -> DemoAttendance:
    course_type = "\u5b9e\u9a8c\u8bfe" if "\u5b9e\u9a8c" in course_name else "\u8bfe\u7a0b\u8003\u52e4"
    label = PY_TO_LABEL[day.weekday()]

    slot = sample_index % 9
    if slot == 1:
        return DemoAttendance("08:16:00", "17:20:00", STATUS_LATE, "\u8d85\u8fc7\u7b7e\u5230\u622a\u6b62\u65f6\u95f4", course_name, course_type, label)
    if slot == 2:
        return DemoAttendance("07:58:00", "16:36:00", STATUS_EARLY_LEAVE, "\u65e9\u4e8e\u7b7e\u9000\u5f00\u59cb\u65f6\u95f4", course_name, course_type, label)
    if slot == 3:
        return DemoAttendance("08:18:00", "16:38:00", STATUS_LATE_EARLY, "\u8fdf\u5230\u4e14\u65e9\u9000", course_name, course_type, label)
    if slot == 4:
        return DemoAttendance("08:05:00", "", STATUS_MISSING_CARD, "\u8d85\u8fc7\u7b7e\u9000\u622a\u6b62\u65f6\u95f4\u672a\u7b7e\u9000", course_name, course_type, label)
    if slot == 5:
        return DemoAttendance("", "", STATUS_LEAVE, "\u6f14\u793a\u8bf7\u5047\u8bb0\u5f55", course_name, "\u8bf7\u5047", label)
    if slot == 6:
        return DemoAttendance("08:10:00", "17:05:00", STATUS_LATE, "\u8f7b\u5fae\u8fdf\u5230", course_name, course_type, label)
    if slot == 7:
        return DemoAttendance("07:52:00", "17:18:00", STATUS_NORMAL, "\u8bfe\u5802\u6b63\u5e38", course_name, course_type, label)
    if slot == 8:
        return DemoAttendance("08:01:00", "17:08:00", STATUS_NORMAL, "\u5230\u8bfe\u6b63\u5e38", course_name, course_type, label)
    return DemoAttendance("07:56:00", "17:15:00", STATUS_NORMAL, "\u8bfe\u5802\u6b63\u5e38", course_name, course_type, label)


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
            username,
            fmt_day(day),
            username,
            fmt_day(day),
            record.check_in_time,
            record.check_out_time,
            record.status,
            record.remark,
            APPEAL_NONE,
            record.course_name,
            record.course_type,
            record.weekday_label,
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
            append_remark(row[0] or "", "\u8865\u7b7e\u7533\u8bf7\u5df2\u901a\u8fc7"),
            username,
            target_date,
        ),
    )


def apply_approved_leave(conn: sqlite3.Connection, username: str, target_date: str, reason: str):
    conn.execute(
        """
        UPDATE attendance
        SET status=?, remark=?, course_type=?
        WHERE username=? AND date=?
        """,
        (
            STATUS_LEAVE,
            reason,
            "\u8bf7\u5047",
            username,
            target_date,
        ),
    )


def clear_user_data(conn: sqlite3.Connection, username: str):
    conn.execute("DELETE FROM attendance WHERE username=?", (username,))
    conn.execute("DELETE FROM leave_request WHERE username=?", (username,))


def import_quarter_demo(conn: sqlite3.Connection, username: str, clear_first: bool):
    if clear_first:
        clear_user_data(conn, username)

    schedule_map = load_schedule_by_weekday(conn)
    start = first_day_months_ago(2)
    end = date.today()
    current = start
    sample_index = 0
    written = []

    while current <= end:
        course_row = schedule_map.get(current.weekday())
        if course_row is not None:
            course_name = course_row[0]
            record = build_demo_attendance(current, sample_index, course_name)
            upsert_attendance(conn, username, current, record)
            written.append((current, course_name))
            sample_index += 1
        current += timedelta(days=1)

    if len(written) < 10:
        return

    request_samples = [written[8], written[18], written[26], written[34], written[44]]
    request_specs = [
        (REQUEST_TYPE_LEAVE, request_samples[0][0], request_samples[0][1], "\u6f14\u793a\u8bf7\u5047\uff1a\u6821\u5916\u7ade\u8d5b", REQUEST_STATUS_APPROVED, "\u6559\u5e08\u5ba1\u6279\uff1a\u5df2\u901a\u8fc7"),
        (REQUEST_TYPE_MAKE_UP, request_samples[1][0], request_samples[1][1], "\u6f14\u793a\u8865\u7b7e\uff1a\u6d3b\u52a8\u7ed3\u675f\u540e\u8865\u7b7e", REQUEST_STATUS_PENDING, ""),
        (REQUEST_TYPE_CANCEL_LEAVE, request_samples[2][0], request_samples[2][1], "\u6f14\u793a\u9500\u5047\uff1a\u63d0\u524d\u8fd4\u6821", REQUEST_STATUS_REJECTED, "\u6559\u5e08\u5ba1\u6279\uff1a\u6682\u4e0d\u901a\u8fc7"),
        (REQUEST_TYPE_LEAVE, request_samples[3][0], request_samples[3][1], "\u6f14\u793a\u8bf7\u5047\uff1a\u8eab\u4f53\u4e0d\u9002", REQUEST_STATUS_PENDING, ""),
        (REQUEST_TYPE_MAKE_UP, request_samples[4][0], request_samples[4][1], "\u6f14\u793a\u8865\u7b7e\uff1a\u7f51\u7edc\u5f02\u5e38\u6f0f\u6253\u5361", REQUEST_STATUS_APPROVED, "\u6559\u5e08\u5ba1\u6279\uff1a\u5df2\u901a\u8fc7"),
    ]

    for request_type, target_day, course_name, reason, status, result_remark in request_specs:
        target_date = fmt_day(target_day)
        insert_request(conn, username, request_type, target_date, course_name, reason, status, result_remark)
        if request_type == REQUEST_TYPE_LEAVE and status == REQUEST_STATUS_APPROVED:
            apply_approved_leave(conn, username, target_date, reason)
        if request_type == REQUEST_TYPE_MAKE_UP and status == REQUEST_STATUS_APPROVED:
            apply_approved_make_up(conn, username, target_date)


def main():
    args = parse_args()
    db_path = Path(args.db).expanduser().resolve()
    ensure_database_exists(db_path)

    with sqlite3.connect(str(db_path)) as conn:
        ensure_tables(conn)
        import_quarter_demo(conn, args.username, clear_first=not args.keep_existing)
        conn.commit()

    print(f"Imported quarter demo attendance into: {db_path}")
    print(f"Username: {args.username}")


if __name__ == "__main__":
    main()
