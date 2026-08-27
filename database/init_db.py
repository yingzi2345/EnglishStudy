"""
MySQL 数据库初始化脚本
用于英语学习打卡系统
"""
import MySQLdb
import os

# 配置
MYSQL_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'passwd': '144312',
    'charset': 'utf8mb4',
}

SQL_FILE = r'D:\毕业设计\database\init.sql'


def main():
    # 先连接 MySQL（不指定数据库）
    conn = MySQLdb.connect(**MYSQL_CONFIG)
    cursor = conn.cursor()
    print('[OK] MySQL 连接成功')

    # 读取 SQL 文件
    with open(SQL_FILE, 'r', encoding='utf-8') as f:
        sql_content = f.read()

    print(f'[OK] 已读取 SQL 文件 ({len(sql_content)} 字符)')

    # 按分号分割语句，逐条执行
    statements = []
    current = []
    for line in sql_content.split('\n'):
        stripped = line.strip()
        # 跳过纯注释行
        if stripped.startswith('--'):
            continue
        current.append(line)
        if stripped.endswith(';'):
            stmt = '\n'.join(current).strip()
            if stmt and stmt != ';':
                statements.append(stmt)
            current = []

    # 处理最后一条（可能没有分号）
    if current:
        stmt = '\n'.join(current).strip()
        if stmt:
            statements.append(stmt)

    success = 0
    fail = 0

    for i, stmt in enumerate(statements):
        try:
            cursor.execute(stmt)
            # 对于 INSERT/SET 等非查询语句，提交事务
            if not stmt.strip().upper().startswith(('SELECT', 'SHOW', 'DESC', 'USE')):
                conn.commit()
            success += 1
        except Exception as e:
            fail += 1
            # 提取语句前 80 字符用于显示
            preview = stmt.strip()[:80].replace('\n', ' ')
            print(f'[ERROR] 第 {i+1} 条失败: {e}')
            print(f'        语句: {preview}...')

    print(f'\n===== 初始化完成 =====')
    print(f'成功: {success} 条')
    print(f'失败: {fail} 条')

    cursor.close()
    conn.close()


if __name__ == '__main__':
    main()
