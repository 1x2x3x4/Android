#!/usr/bin/env python3
import argparse
import sqlite3
from pathlib import Path


def parse_args():
    parser = argparse.ArgumentParser(
        description="向 CampusAttendance SQLite 数据库导入计算机科学课程表。"
    )
    parser.add_argument("--db", required=True, help="SQLite 数据库文件路径")
    parser.add_argument(
        "--clear-existing",
        action="store_true",
        help="导入前清空 course_schedule 表；默认追加写入",
    )
    return parser.parse_args()


def ensure_database_exists(db_path: Path):
    if not db_path.exists():
        raise FileNotFoundError(f"数据库文件不存在: {db_path}")


def ensure_table_exists(conn: sqlite3.Connection):
    row = conn.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name='course_schedule'"
    ).fetchone()
    if not row:
        raise RuntimeError("数据库缺少 course_schedule 表")


def import_courses(conn: sqlite3.Connection, clear_existing: bool):
    if clear_existing:
        conn.execute("DELETE FROM course_schedule")

    # weekday 使用 Android Calendar 常量：
    # SUNDAY=1, MONDAY=2, TUESDAY=3, WEDNESDAY=4, THURSDAY=5, FRIDAY=6, SATURDAY=7
    courses = [
        ("数据结构", "张老师", 2, "08:00", "09:40", "07:40", "08:10", "09:35", "10:00", "信息楼 A201", 1),
        ("计算机网络", "李老师", 3, "10:10", "11:50", "09:50", "10:20", "11:45", "12:10", "信息楼 B305", 1),
        ("数据库系统", "王老师", 4, "14:00", "15:40", "13:40", "14:10", "15:35", "16:00", "信息楼 C404", 1),
        ("操作系统", "陈老师", 5, "08:00", "09:40", "07:40", "08:10", "09:35", "10:00", "信息楼 A305", 1),
        ("人工智能导论", "刘老师", 6, "10:10", "11:50", "09:50", "10:20", "11:45", "12:10", "信息楼 D202", 1),
        ("程序设计实验", "赵老师", 7, "14:00", "16:30", "13:40", "14:10", "16:20", "16:50", "实验楼 302", 1),
        # 周日中午课程用于当前截图场景，导入后首页可在 12:51 自动匹配
        ("计算机科学导论", "周老师", 1, "12:30", "14:00", "12:10", "12:55", "13:50", "14:20", "综合楼 101", 1),
    ]

    conn.executemany(
        """
        INSERT INTO course_schedule
        (course_name, teacher_name, weekday, start_time, end_time,
         check_in_start, check_in_deadline, check_out_start, check_out_deadline,
         classroom, is_enabled)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        courses,
    )


def main():
    args = parse_args()
    db_path = Path(args.db).expanduser().resolve()
    ensure_database_exists(db_path)

    with sqlite3.connect(str(db_path)) as conn:
        ensure_table_exists(conn)
        import_courses(conn, clear_existing=args.clear_existing)
        conn.commit()

    print(f"已导入计算机科学课程表: {db_path}")
    print("课程数量: 7")


if __name__ == "__main__":
    main()
