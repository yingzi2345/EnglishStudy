# 英语学习打卡系统 — Spring Boot 后端（Java 版）

> 原 Django 后端（`backend/`）的 Java 重构版，**接口路径、响应格式、字段命名与小程序前端完全兼容，前端零改动**。

## 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 框架 | Spring Boot | 4.1.1 |
| ORM | MyBatis-Plus（`mybatis-plus-spring-boot4-starter` + `jsqlparser` 分页） | 3.5.17 |
| 数据库 | MySQL（原库 `english_checkin`，表结构不变） | 8.0+（实测 9.7） |
| 认证 | JWT（jjwt 0.13.0，HS256）access 2h / refresh 7d | 0.13.0 |
| 工具 | Lombok、spring-security-crypto（BCrypt 校验管理员密码） | — |
| 运行环境 | JDK 21 + Maven 3.9+ | — |

## 目录结构

```
backend-java/
├── pom.xml                          # Maven 依赖
├── smoke_test.ps1                   # 接口冒烟测试脚本（28 个接口）
├── detail_test.py                   # 字段命名/编码/时区兼容性详细验证脚本
├── fix_admin_hash.sql               # 管理员密码哈希修复脚本（见"数据修复"）
├── cleanup_test_data.sql            # 清理冒烟测试产生的测试用户
└── src/main/
    ├── java/com/guet/englishcheckin/
    │   ├── EnglishCheckinApplication.java   # 启动类（@MapperScan）
    │   ├── common/                          # ApiResponse 统一响应 / BusinessException / 全局异常处理
    │   ├── config/                          # JwtProperties / MybatisPlusConfig / WebConfig(CORS+拦截器) / JacksonConfig
    │   ├── security/                        # JwtUtil / JwtAuthInterceptor / IpUtil / TrailingSlashFilter
    │   ├── entity/                          # User / Word / Checkin / WordProgress / LoginLog / Admin
    │   ├── mapper/                          # 6 个 BaseMapper + LeaderboardStatsMapper（聚合 SQL）
    │   ├── dto/ vo/                         # 请求 DTO 与响应 VO
    │   ├── service/                         # User / Admin / Word / Checkin / Leaderboard
    │   └── controller/                      # Auth / User / Admin / Word / Checkin / Leaderboard
    └── resources/application.yml            # 数据源 / JWT / MyBatis-Plus / Jackson 配置
```

## 启动方式

```bash
cd D:\yingzi\Documents\项目\毕业设计\backend-java

# 1. 构建
mvn package -DskipTests

# 2. 运行（监听 8000 端口，与小程序 baseUrl 一致）
java -jar target\english-checkin-backend-1.0.0.jar
```

启动成功后访问 `http://127.0.0.1:8000/api/...`，前端 `front/` 无需任何修改即可对接。

## 与原 Django 版的兼容设计

| 兼容点 | 实现方式 |
|---|---|
| 统一响应 `{code, message, data}` | `common/ApiResponse`；业务错误返回 HTTP 200 + body.code（与 Django `api_response` 一致） |
| 认证失败 HTTP 401 | `JwtAuthInterceptor` 拦截器，解析 `Bearer <token>` 的 `user_id` 查 `tb_user` 且 `status=1` |
| 字段下划线命名（`is_learned` / `avatar_url`…） | Jackson 全局 `property-naming-strategy: SNAKE_CASE` |
| 路径尾斜杠（`/users/profile/`） | `TrailingSlashFilter` 规范化后再路由 |
| 分页参数 `page` / `page_size` | MyBatis-Plus 分页插件 + `@RequestParam(name="page_size")` |
| 时间时区 | 库内统一存 UTC（与 Django `USE_TZ=True` 一致），响应带 `Z` 标记；`checkin_date` 用东八区日期 |
| 微信登录 mock 兜底 | 微信接口失败时生成 `mock_openid_{code前16位}`（与 Django 相同） |
| 管理员 BCrypt 校验 | `BCryptPasswordEncoder`，兼容 `$2a/$2b/$2y` 前缀 |

## API 一览（全部与原 Django 路由一致）

- **认证**：`POST /api/auth/wechat-login/`、`POST /api/auth/admin-login/`
- **用户**：`GET/POST /api/users/`、`GET /api/users/profile/`、`PUT /api/users/update_profile/`、`GET /api/users/login_logs/`、`GET /api/users/stats/`
- **管理员**：`POST /api/admin-users/login/`、`GET /api/admin-users/dashboard/`、`GET /api/admin-users/user_list/`、`GET /api/admin-users/login_log_list/`、`POST /api/admin-users/toggle_user_status/`
- **单词**：`GET /api/words/`（分页/分类/搜索）、`GET/PUT/DELETE /api/words/{id}/`、`GET /api/words/categories/`、`GET /api/words/random_word/`、`GET /api/words/daily_words/`、`POST /api/words/mark_learned/`、`POST /api/words/unmark_learned/`、`GET /api/words/my_progress/`、`GET /api/words/search/`
- **打卡**：`GET /api/checkin/`、`POST /api/checkin/do_checkin/`、`GET /api/checkin/today_status/`、`GET /api/checkin/calendar/`、`GET /api/checkin/records/`、`GET /api/checkin/streak/`
- **排行榜**：`GET /api/leaderboard/daily|weekly|monthly|alltime/`、`GET /api/leaderboard/stats/`

## 数据修复（重要）

- **管理员密码哈希**：原种子数据 `tb_admin.password_hash` 与 `admin123` **不匹配**（Django 版同样无法登录，为历史遗留 bug）。已用 `fix_admin_hash.sql` 修复为有效 BCrypt 哈希，现 `admin / admin123` 可正常登录（Java 与 Django 两侧通用）。
- **超长字段 500 修复**：微信开发者工具 User-Agent 超 256 字符导致 `tb_login_log.device_info` 插入报 `Data truncation` → 登录接口 500（Django 版靠放宽 `sql_mode` 静默截断掩盖）。已在 `UserService` 对 `device_info`/`nickname`/`avatar_url`/IP 等入库字段做长度截断（`truncate()`）。
- 冒烟测试产生的临时用户已通过 `cleanup_test_data.sql` 清理，数据库数据量与迁移前一致。

## 测试结果

- 28 个接口冒烟测试全部通过（含 401 拒绝、重复打卡拒绝）
- 24 项兼容性细查全部通过（下划线字段、UTC 时间、分页、排行榜 my_rank、无效 token 401、尾斜杠等）
- 测试脚本：`smoke_test.ps1`（PowerShell 原生）、`detail_test.py`（标准库，`D:\python\python3.12.4\python.exe` 直接运行）、`test_ua_fix.py`（模拟小程序超长 UA）
- 测试会产生临时 mock 用户，验证后须清理（参照 `cleanup_test_data.sql`）

## 备注

- 原 Django 后端保留在 `backend/` 目录未删除，可作论文对比与回退。
- 数据库密码为项目本地配置（root / 144312），如需改配置请编辑 `src/main/resources/application.yml`。
