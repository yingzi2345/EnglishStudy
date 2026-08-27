# 英语学习打卡系统 — 数据库ER图设计

## 实体关系图（ER图描述）

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│     tb_user     │       │   tb_checkin    │       │    tb_word      │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│ PK id (BIGINT)  │──1:N──│ FK user_id      │       │ PK id (BIGINT)  │
│ openid (VARCHAR)│       │ PK id (BIGINT)  │       │ word (VARCHAR)  │
│ nickname        │       │ checkin_date    │       │ phonetic        │
│ avatar_url      │       │ checkin_time    │       │ meaning         │
│ created_at      │       │ continuous_days │       │ example_en      │
│ last_login_at   │       │ created_at      │       │ example_zh      │
│ last_login_ip   │       └─────────────────┘       │ audio_url       │
│ status          │                                  │ level (INT)     │
└─────────────────┘                                  │ created_at      │
        │                                            └─────────────────┘
        │                                                    │
        │ 1:N                                                │ N:M
        ▼                                                    ▼
┌─────────────────┐                               ┌─────────────────┐
│  tb_login_log   │                               │ tb_word_progress│
├─────────────────┤                               ├─────────────────┤
│ PK id (BIGINT)  │                               │ PK id (BIGINT)  │
│ FK user_id      │                               │ FK user_id      │
│ ip_address      │                               │ FK word_id      │
│ device_info     │                               │ is_learned      │
│ login_time      │                               │ learned_at      │
│ is_abnormal     │                               │ review_count    │
│ location        │                               └─────────────────┘
└─────────────────┘

┌──────────────────────┐        ┌──────────────────────┐
│  tb_admin            │        │  tb_system_config    │
├──────────────────────┤        ├──────────────────────┤
│ PK id (BIGINT)       │        │ PK id (BIGINT)       │
│ username (VARCHAR)   │        │ config_key (VARCHAR) │
│ password_hash        │        │ config_value (TEXT)  │
│ role (VARCHAR)       │        │ description          │
│ created_at           │        │ updated_at           │
│ last_login_at        │        └──────────────────────┘
│ last_login_ip        │
└──────────────────────┘
```

## 实体关系说明

1. **tb_user : tb_checkin** = 1 : N  — 一个用户可有多条打卡记录
2. **tb_user : tb_login_log** = 1 : N  — 一个用户可有多条登录日志
3. **tb_user : tb_word_progress** = 1 : N  — 一个用户可有多条单词学习进度
4. **tb_word : tb_word_progress** = 1 : N  — 一个单词可被多个用户学习
5. **tb_user : tb_checkin** 通过 user_id 外键关联
6. **tb_checkin.checkin_date** 与 **tb_user.id** 联合唯一，保证每天只能打卡一次
