# AGENTS.md — 英语学习打卡系统 工作手册

毕业设计项目：基于微信小程序的英语学习打卡系统设计与实现
桂林电子科技大学 · 网络工程专业 · 导师：徐凯

---

## 技术栈

| 层 | 技术 | 说明 |
|---|---|---|
| 后端框架 | Django 4.2+ / DRF | 当前运行 Django 6.0.6 |
| 数据库 | MySQL 8.0 | utf8mb4，数据库名 `english_checkin` |
| 认证 | SimpleJWT | access 2h / refresh 7d，自定义认证后端 |
| 后台管理 | Django Admin + simpleui | 美化主题，`simpleui` 在 INSTALLED_APPS 首位 |
| API 文档 | drf-yasg | `/swagger/` 和 `/redoc/` |
| 前端 | 微信小程序 | 9 个页面，4 个 tab |
| 部署 | Nginx 反向代理 | HTTPS + 限流 + 安全头 |

## 目录结构

```
D:\毕业设计\
├── backend/                  # Django 后端
│   ├── apps/
│   │   ├── users/            # 用户模块（User, LoginLog, Admin）
│   │   ├── words/            # 单词模块（Word, WordProgress）
│   │   ├── checkin/          # 打卡模块（Checkin）
│   │   └── leaderboard/      # 排行榜（无模型，聚合查询）
│   ├── config/               # Django 配置（settings, urls, wsgi）
│   ├── middleware/            # 自定义中间件（IP 溯源）
│   ├── utils/                # 工具（JWT 认证后端, 异常处理, api_response）
│   └── manage.py
├── front/                    # 微信小程序
│   ├── pages/                # 9 个页面
│   └── utils/api.js          # 所有 API 请求封装
├── database/                 # SQL 建表 + 种子数据 + ER 图
├── docs/                     # 毕业论文
├── ppt/                      # 答辩 PPT
└── .venv/                    # Python 虚拟环境
```

## 虚拟环境

**必须使用 venv 运行项目。** venv 位于 `backend\.venv\`，Python 路径：

```
D:\毕业设计\backend\.venv\Scripts\python.exe
```

启动服务：

```bash
cd D:/毕业设计/backend
.venv/Scripts/python.exe manage.py runserver 0.0.0.0:8000
```

安装新包时必须加版本约束，否则 pip 可能自动拉取不兼容的最新版（如 Django 6.0）：

```bash
.venv/Scripts/pip.exe install "django>=4.2,<5.0" "djangorestframework>=3.14,<3.16"
```

## 数据库

| 配置项 | 值 |
|---|---|
| Host | 127.0.0.1:3306 |
| 用户 | root |
| 密码 | 144312 |
| 数据库 | english_checkin |
| 字符集 | utf8mb4 |

**导入 SQL**：从 MySQL CLI 内用 `source` 命令，路径用正斜杠。含中文默认值的 SQL 可能报 1067，需在文件开头加 `SET SESSION sql_mode = 'NO_ENGINE_SUBSTITUTION';`

**Django Admin 登录**：`admin` / `admin123`

## 数据模型关系

```
User (tb_user)
 ├── 1:N → Checkin (tb_checkin)        unique(user, checkin_date)
 ├── 1:N → LoginLog (tb_login_log)
 └── 1:N → WordProgress (tb_word_progress)   unique(user, word)

Word (tb_word)
 └── 1:N → WordProgress (tb_word_progress)

Admin (tb_admin) — 独立表，bcrypt 密码
tb_system_config — 仅 SQL 层，无 Django 模型
```

## API 设计规范

### 响应格式

所有接口统一返回 `{code, message, data}`。成功 `code=200`。

```python
from utils.exceptions import api_response
return api_response(data={...})                           # 成功
return api_response(message='参数错误', code=400)          # 业务错误
```

### 认证

- 微信小程序：POST `/api/auth/wechat-login/` 换取 JWT，后续请求带 `Authorization: Bearer <token>`
- 管理员：POST `/api/auth/admin-login/` 获取身份信息

### 自定义 JWT 认证后端

`utils/authentication.py` — 因为 User 模型未继承 `AbstractUser`，需要自定义认证后端从 token 的 `user_id` 查找 `tb_user` 记录。User 模型必须提供 `is_authenticated` 和 `is_anonymous` 属性以通过 DRF `IsAuthenticated` 权限检查。

## Django ORM 踩坑记录

**DateTimeField__date 在 MySQL+USE_TZ=True 下不可用**

Django 将 `DateTimeField__date` 翻译为 `DATE(CONVERT_TZ(...))`，在 MySQL 中可能返回空。**必须用以下方式替代：**

```python
from django.utils import timezone

# 错误写法
Model.objects.filter(created_at__date=today)

# 正确写法
day_start = timezone.make_aware(datetime.combine(today, time.min))
day_end = timezone.make_aware(datetime.combine(today, time.max))
Model.objects.filter(created_at__gte=day_start, created_at__lt=day_end)
```

**日期聚合**：用 `RawSQL('DATE(field)', ())` 代替 `DateTimeField__date`。

## 排行榜逻辑

日榜/周榜/月榜基于 `WordProgress` 统计（`is_learned=1, learned_at__gte=时间窗口起点`），总榜用 `User.total_words`。

## 微信小程序

- 单词发音：优先用后台 `audio_url`，兜底用有道 TTS（`dict.youdao.com/dictvoice?audio=WORD&type=0` 美音 / `type=1` 英音），通过 `wx.createInnerAudioContext()` 播放
- API 封装在 `front/utils/api.js`，所有请求自动带 token，401 自动跳转登录页

## Windows 环境注意事项

- Python 命令用 `python`，不是 `python3`
- Git Bash 下中文路径文件操作（如 Pillow `open()`）可能失败，先存到临时目录再 cp
- `taskkill /PID` 在 Git Bash 下可能因编码乱码失败，改用 `powershell -Command "Stop-Process -Id PID -Force"`

## 依赖项

```
Django==4.2.30          # 当前实际运行 6.0.6
djangorestframework     # 需跟随 Django 版本，6.0 需 3.17+
django-cors-headers
django-filter
djangorestframework-simplejwt
drf-yasg
django-simpleui
mysqlclient
bcrypt
PyJWT
```
