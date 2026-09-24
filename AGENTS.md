# AGENTS.md — 英语学习打卡系统 工作手册

毕业设计项目：基于微信小程序的英语学习打卡系统设计与实现
桂林电子科技大学 · 网络工程专业 · 导师：徐凯

> **后端已迁移至 Java/Spring Boot：当前主后端为 `backend-java/`。`backend/` 是旧 Django 实现，仅保留作论文对照，不要再修改。** 详细说明见 `backend-java/README.md`。

## 技术栈（当前主后端）

| 层 | 技术 | 说明 |
|---|---|---|
| 后端框架 | Spring Boot 4.1.1（JDK 21） | 主后端 `backend-java/` |
| ORM | MyBatis-Plus 3.5.17 | `mybatis-plus-spring-boot4-starter` + `mybatis-plus-jsqlparser`（分页） |
| 数据库 | MySQL（`english_checkin`，utf8mb4） | 表结构与 Django 版完全一致 |
| 认证 | JWT（jjwt 0.13.0，HS256） | access 2h / refresh 7d，`Bearer <token>` |
| 工具 | Lombok、spring-security-crypto | 管理员密码 BCrypt（`$2b$` 兼容 Django 侧哈希） |
| 前端 | 微信小程序（9 页 4 tab） | 接口零改动兼容 |

## 目录结构

```
D:\yingzi\Documents\项目\毕业设计\
├── backend-java/               # ★ 当前主后端（Spring Boot）
│   ├── pom.xml
│   ├── src/main/java/com/guet/englishcheckin/
│   │   ├── common/             # ApiResponse 统一响应 / BusinessException / 全局异常
│   │   ├── config/             # MybatisPlusConfig / WebConfig(CORS+拦截器) / JwtProperties / JacksonConfig
│   │   ├── security/           # JwtUtil / JwtAuthInterceptor / TrailingSlashFilter / IpUtil
│   │   ├── entity/ mapper/ dto/ vo/ service/ controller/
│   │   └── resources/application.yml   # 数据源 / JWT secret / Jackson(SNAKE_CASE) 配置
│   ├── smoke_test.ps1          # 接口冒烟测试（28 个接口）
│   └── detail_test.py          # 字段命名/编码/时区兼容性验证（需 venv Python + requests）
├── backend/                    # 旧 Django 后端（论文对照，勿改）
├── front/                      # 微信小程序
├── database/                   # SQL 建表 + 种子数据 + ER 图
├── docs/ ppt/                  # 毕业论文 / 答辩 PPT
└── .venv/                      # 已删除（原 Django 虚拟环境，不再使用）
```

## 构建与启动（主后端）

```bash
cd D:\yingzi\Documents\项目\毕业设计\backend-java
mvn package -DskipTests
java -jar target\english-checkin-backend-1.0.0.jar    # 监听 8000，与小程序 baseUrl 一致
```

- 改代码后快速检查：`mvn -q compile`
- 改接口后跑 `smoke_test.ps1`（PowerShell 原生）或 `detail_test.py`（标准库，`D:\python\python3.12.4\python.exe`，无需第三方包）验证；测试会创建临时 mock 用户，验证后须清理（参照 `cleanup_test_data.sql`）
- 旧 Django 版启动（如需）：`backend\.venv\Scripts\python.exe manage.py runserver 0.0.0.0:8000`（两版端口冲突，勿同时运行）

## 数据库

| 配置项 | 值 |
|---|---|
| Host | 127.0.0.1:3306 |
| 用户 | root |
| 密码 | YfQGEP9Pk8HMHoFNm8q7 |
| 数据库 | english_checkin |
| 字符集 | utf8mb4 |

**导入 SQL**：从 MySQL CLI 内用 `source` 命令，路径用正斜杠。含中文默认值的 SQL 可能报 1067，需在文件开头加 `SET SESSION sql_mode = 'NO_ENGINE_SUBSTITUTION';`

**管理员登录**：`admin` / `admin123`（数据库密码与 JWT secret 见 `backend-java/src/main/resources/application.yml`）

## 数据模型关系

```
User (tb_user)
 ├── 1:N → Checkin (tb_checkin)        unique(user, checkin_date)
 ├── 1:N → LoginLog (tb_login_log)
 └── 1:N → WordProgress (tb_word_progress)   unique(user, word)

Word (tb_word)
 └── 1:N → WordProgress (tb_word_progress)

Admin (tb_admin) — 独立表，bcrypt 密码（实体映射：entity/ 包，Mapper 继承 BaseMapper）
tb_system_config — 仅 SQL 层，无实体类
```

## 词书库与自定义词本

**统一模型**：官方词书与用户自定义词本共用 `tb_word_book`（`book_type` 区分 system/custom），自定义单词写入 `tb_word`（`source='custom'`, `owner_id=用户`），学习进度仍走 `tb_word_progress`，学习链路天然统一、支持多词书并行。

```
WordBook (tb_word_book)          — system官方(code唯一) / custom自定义(owner_id, code=NULL)
 ├── 1:N → WordBookItem (tb_word_book_item)   unique(book_id, word_id)
 └── 1:N → UserBook (tb_user_book)            unique(user_id, book_id), is_current标记当前学习词书

Word (tb_word) 加 source/owner_id 字段 + uk_word 唯一索引（INSERT IGNORE 去重）
```

- **取词逻辑**：`StudyService.getTodayTasks` 新词从用户 `is_current=1` 词书的 items 中取无 progress 的词；用户未选任何词书时回退到全部 `source='system'` 词。复习词仍从用户全部到期 progress 取（跨书复习）。
- **可见性**：自定义词仅属主可见（`WordService.applyVisibility` 统一过滤 `source='system' OR owner_id=userId`）；自定义词本仅 owner 可操作。
- **词书数据**：9本官方词书（中考/高考/四级/六级/考研英一/考研英二/雅思/托福/日常），精选高频词约 600 词（`database/seed_book_*.sql`），跨书重复词靠 `uk_word` 去重。
- **接口**：`/api/word-books/`（列表/详情/加入/设为当前）、`/api/word-books/custom/`（创建）、`/{id}/words/`（手动加词）、`/{id}/import/`（文本粘贴批量导入，支持 `word,释义` / `word - 释义` / `word：释义` / `word`）。
- **迁移 SQL**：`database/migration_wordbook.sql`（建表+字段+索引），`database/seed_wordbook_meta.sql`（9本词书元数据）。

## API 设计规范

### 响应格式

所有接口统一返回 `{code, message, data}`，成功 `code=200`。**业务错误返回 HTTP 200 + body.code=4xx**（前端只判断 body.code）；**认证失败返回 HTTP 401**（前端据此清 token 跳登录页）。字段命名一律下划线风格（`is_learned` / `avatar_url`…，Jackson 全局 SNAKE_CASE 自动转换），**不要改前端字段名**。

### 认证

- 微信登录：POST `/api/auth/wechat-login/` → `{token:{access,refresh}, user}`；微信接口失败时生成 `mock_openid_{code前16位}` 兜底（开发模式）
- 管理员登录：POST `/api/auth/admin-login/`（BCrypt 校验，返回 `{id, username, role}`）
- 受保护接口：请求头 `Authorization: Bearer <access>`；`JwtAuthInterceptor` 解析 token 的 `user_id` 查 `tb_user` 且 `status=1`，用户对象放入 request attribute `"currentUser"`
- 公开路径：`/api/auth/wechat-login`、`/api/auth/admin-login`、`/api/admin-users/login`（在 `config/WebConfig.java` 维护）
- 尾斜杠：前端所有 URL 以 `/` 结尾，`TrailingSlashFilter` 自动去尾斜杠再路由，Controller 的 `@RequestMapping` 一律写无尾斜杠路径

### 新增/修改接口要点

- 时间：库内统一存 UTC（`TimeUtil.nowUtc()`），响应自动带 `Z`；`checkin_date` 用东八区日期（`TimeUtil.todayShanghai()`）
- 排行榜时间窗口：东八区 0 点换算为 UTC 时刻（`TimeUtil.shanghaiMidnightToUtc`），聚合 SQL 在 `mapper/LeaderboardStatsMapper.java`
- 分页：MyBatis-Plus `Page` + 分页插件；Controller 参数 `@RequestParam(name="page_size", defaultValue="20")`（前端传 `page_size`）

## 踩坑记录（Java 版）

- **Jackson 3 与 Jackson 2 类名不同**：Boot 4 默认 Jackson 3（`tools.jackson.*`）。`JsonSerializer`→`ValueSerializer`、`SerializerProvider`→`SerializationContext`、`Module`→`JacksonModule`；序列化方法不声明受检 `IOException`
- **分页插件缺失**：`PaginationInnerInterceptor` 自 MP 3.5.9 拆到 `mybatis-plus-jsqlparser` 模块，pom 必须显式引入，starter 不带
- **jjwt 序列化器**：用 `jjwt-gson`（`jjwt-jackson` 依赖 Jackson 2，与 Boot 4 的 Jackson 3 冲突）
- **selectCount 不支持 groupBy**：分组/去重统计用自定义 `@Select` SQL（见 LeaderboardStatsMapper）或 `selectList` + stream
- **PowerShell 转义**：`$` 在双引号内会被变量展开，含 `$2b$` 的 bcrypt 哈希写库用 SQL 文件 + `cmd /c "mysql ... < file.sql"` 执行；PowerShell 不支持 `<` 重定向
- **超长字段截断**：MySQL 严格模式下 VARCHAR 超长直接 500（如微信开发者工具 UA 超 256 字符）。入库前须截断——参照 `UserService.truncate()`（device_info/nickname/avatar_url 等）
- 改 `application.yml` 后需重新 `mvn package`（配置打进 jar）

## 排行榜逻辑

日榜/周榜/月榜基于 `tb_word_progress` 统计（`is_learned=1 AND learned_at >= 窗口起点`，`days_count` 用 `COUNT(DISTINCT DATE(learned_at))` 避免日期函数时区问题），总榜用 `tb_user.total_words` 排序。

## 微信小程序

- 单词发音：优先用后台 `audio_url`，兜底用有道 TTS（`dict.youdao.com/dictvoice?audio=WORD&type=0` 美音 / `type=1` 英音），通过 `wx.createInnerAudioContext()` 播放
- API 封装在 `front/utils/api.js`，所有请求自动带 token，401 自动跳转登录页；baseUrl `http://127.0.0.1:8000/api`

## Windows 环境注意事项

- Java 命令：`java -version` / `mvn`（本机 JDK 21 + Maven 3.9.16）
- 中文路径：Python 脚本处理中文路径文件（如 Pillow）可能失败，先存临时目录再拷贝
- 杀进程：`taskkill /PID` 在 Git Bash 下可能乱码失败，用 `powershell -Command "Stop-Process -Id PID -Force"`

## 依赖（backend-java/pom.xml）

```
spring-boot-starter-parent 4.1.1（parent，管理版本）
spring-boot-starter-web / spring-boot-starter-validation
mybatis-plus-spring-boot4-starter 3.5.17 + mybatis-plus-jsqlparser 3.5.17
mysql-connector-j（版本由 Boot BOM 管理）
jjwt-api / jjwt-impl / jjwt-gson 0.13.0
spring-security-crypto（仅 BCrypt）
lombok（optional）
```
