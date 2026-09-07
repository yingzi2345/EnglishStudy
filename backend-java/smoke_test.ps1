# 英语学习打卡系统 Spring Boot 后端 — 接口冒烟测试
$ErrorActionPreference = "Continue"
$base = "http://127.0.0.1:8000/api"
$pass = 0; $fail = 0

function Test-Api($name, $method, $url, $body, $token) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $params = @{ Uri = $url; Method = $method; Headers = $headers }
    if ($body) { $params["ContentType"] = "application/json"; $params["Body"] = ($body | ConvertTo-Json -Compress) }
    try {
        $resp = Invoke-RestMethod @params
        $code = $resp.code
        if ($code -eq 200) {
            Write-Host "[PASS] $name  (code=$code)" -ForegroundColor Green
            return $resp.data
        } else {
            Write-Host "[FAIL] $name  (code=$code, msg=$($resp.message))" -ForegroundColor Red
            $script:fail++
            return $null
        }
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        $resp = $null
        try { $resp = $_.ErrorDetails.Message | ConvertFrom-Json } catch {}
        if ($status -eq 401 -and $resp.code -eq 401) {
            Write-Host "[PASS] $name  (期望 401 拒绝: $($resp.message))" -ForegroundColor Green
            return $null
        }
        Write-Host "[FAIL] $name  (HTTP $status, $($_.Exception.Message))" -ForegroundColor Red
        $script:fail++
        return $null
    }
    $script:pass++
}

Write-Host "===== 1. 认证接口 =====" -ForegroundColor Cyan
# 1.1 无 token 访问受保护接口 → 应 401
Test-Api "无token访问 /users/profile/" "GET" "$base/users/profile/" $null $null

# 1.2 微信登录（mock 模式）
$loginData = Test-Api "微信登录 /auth/wechat-login/" "POST" "$base/auth/wechat-login/" @{ code = "smoke_test_code_001"; nickname = "冒烟测试用户" } $null
$token = $null
if ($loginData) {
    $token = $loginData.token.access
    $script:pass++
    Write-Host "[PASS] 微信登录返回 token.access (前20字符: $($token.Substring(0,20))...)" -ForegroundColor Green
    Write-Host "      新用户标记: is_new_user=$($loginData.user.is_new_user), 连续天数=$($loginData.user.continuous_days)"
}

# 1.3 管理员登录
$adminData = Test-Api "管理员登录 /auth/admin-login/" "POST" "$base/auth/admin-login/" @{ username = "admin"; password = "admin123" } $null
if ($adminData) {
    Write-Host "      管理员身份: id=$($adminData.id) role=$($adminData.role)"
}

Write-Host "`n===== 2. 用户模块 =====" -ForegroundColor Cyan
Test-Api "用户资料 /users/profile/" "GET" "$base/users/profile/" $null $token | Out-Null
Test-Api "更新资料 /users/update_profile/" "PUT" "$base/users/update_profile/" @{ nickname = "冒烟测试用户"; gender = 1 } $token | Out-Null
Test-Api "登录日志 /users/login_logs/" "GET" "$base/users/login_logs/" $null $token | Out-Null
Test-Api "学习统计 /users/stats/" "GET" "$base/users/stats/" $null $token | Out-Null

Write-Host "`n===== 3. 单词模块 =====" -ForegroundColor Cyan
$wordList = Test-Api "单词列表 /words/" "GET" "$base/words/?page=1&page_size=5" $null $token
if ($wordList) { Write-Host "      分页结果: count=$($wordList.count), results=$($wordList.results.Count) 条" }
Test-Api "分类 /words/categories/" "GET" "$base/words/categories/" $null $token | Out-Null
Test-Api "随机单词 /words/random_word/" "GET" "$base/words/random_word/" $null $token | Out-Null
$daily = Test-Api "每日推荐 /words/daily_words/?count=5" "GET" "$base/words/daily_words/?count=5" $null $token
if ($daily) { Write-Host "      推荐数量: $($daily.Count) 个, 首个单词: $($daily[0].word)" }
Test-Api "我的进度 /words/my_progress/" "GET" "$base/words/my_progress/" $null $token | Out-Null
Test-Api "搜索 /words/search/?q=abandon" "GET" "$base/words/search/?q=abandon" $null $token | Out-Null
$wordDetail = Test-Api "单词详情 /words/1/" "GET" "$base/words/1/" $null $token
if ($wordDetail) { Write-Host "      单词详情: $($wordDetail.word) - $($wordDetail.meaning)" }

# 标记已学/撤销（用详情里的单词，避免影响其他）
if ($wordDetail) {
    Test-Api "标记已学 /words/mark_learned/" "POST" "$base/words/mark_learned/" @{ word_id = 1; is_mastered = $true } $token | Out-Null
    Test-Api "撤销学习 /words/unmark_learned/" "POST" "$base/words/unmark_learned/" @{ word_id = 1 } $token | Out-Null
}

Write-Host "`n===== 4. 打卡模块 =====" -ForegroundColor Cyan
Test-Api "今日状态 /checkin/today_status/" "GET" "$base/checkin/today_status/" $null $token | Out-Null
$checkinRes = Test-Api "执行打卡 /checkin/do_checkin/" "POST" "$base/checkin/do_checkin/" @{ word_count = 10; study_duration = 30; note = "冒烟测试打卡" } $token
if ($checkinRes) { Write-Host "      $($checkinRes.message)" }
Test-Api "打卡日历 /checkin/calendar/" "GET" "$base/checkin/calendar/?year=2026&month=9" $null $token | Out-Null
Test-Api "打卡记录 /checkin/records/" "GET" "$base/checkin/records/" $null $token | Out-Null
Test-Api "连续打卡 /checkin/streak/" "GET" "$base/checkin/streak/" $null $token | Out-Null
# 重复打卡应被拒绝
Test-Api "重复打卡(应拒绝) /checkin/do_checkin/" "POST" "$base/checkin/do_checkin/" @{ word_count = 1 } $token | Out-Null

Write-Host "`n===== 5. 排行榜模块 =====" -ForegroundColor Cyan
Test-Api "日榜 /leaderboard/daily/" "GET" "$base/leaderboard/daily/" $null $token | Out-Null
Test-Api "周榜 /leaderboard/weekly/" "GET" "$base/leaderboard/weekly/" $null $token | Out-Null
Test-Api "月榜 /leaderboard/monthly/" "GET" "$base/leaderboard/monthly/" $null $token | Out-Null
Test-Api "总榜 /leaderboard/alltime/" "GET" "$base/leaderboard/alltime/" $null $token | Out-Null
Test-Api "首页统计 /leaderboard/stats/" "GET" "$base/leaderboard/stats/" $null $token | Out-Null

Write-Host "`n===== 6. 管理端接口（需登录用户 token，与原 Django 行为一致） =====" -ForegroundColor Cyan
Test-Api "看板 /admin-users/dashboard/" "GET" "$base/admin-users/dashboard/" $null $token | Out-Null
Test-Api "用户列表 /admin-users/user_list/" "GET" "$base/admin-users/user_list/" $null $token | Out-Null
Test-Api "登录日志 /admin-users/login_log_list/" "GET" "$base/admin-users/login_log_list/" $null $token | Out-Null

Write-Host "`n===== 测试完成 =====" -ForegroundColor Cyan
Write-Host "通过: $pass 项, 失败: $fail 项"
