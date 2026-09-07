# -*- coding: utf-8 -*-
"""字段命名/编码/时区/分页兼容性验证（仅标准库，无需 requests）

用法: python detail_test.py
"""
import json
import time
import urllib.request
import urllib.error

BASE = 'http://127.0.0.1:8000/api'
ok = fail = 0


def check(name, cond, extra=''):
    global ok, fail
    if cond:
        ok += 1
        print(f'[PASS] {name} {extra}')
    else:
        fail += 1
        print(f'[FAIL] {name} {extra}')


def request(method, url, payload=None, token=None):
    data = None
    headers = {}
    if payload is not None:
        data = json.dumps(payload).encode('utf-8')
        headers['Content-Type'] = 'application/json'
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(BASE + url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        try:
            body = json.loads(e.read().decode('utf-8'))
        except Exception:
            body = {}
        return e.code, body


def get(url, token=None):
    return request('GET', url, token=token)


def post(url, payload, token=None):
    return request('POST', url, payload, token)


# 1. 登录（带时间戳的全新测试用户，保证脚本可重复运行）
TEST_CODE = f'detail_{int(time.time())}'
status, d = post('/auth/wechat-login/', {'code': TEST_CODE, 'nickname': '字段验证用户'})
check('登录 code=200', d['code'] == 200)
token = d['data']['token']['access']
check('token.access 存在', bool(token))
user = d['data']['user']
check('user 字段为下划线命名',
      'is_new_user' in user and 'total_words' in user and 'avatar_url' in user and 'max_continuous' in user,
      str(sorted(user.keys())))
check('token 字段 access/refresh', sorted(d['data']['token'].keys()) == ['access', 'refresh'])

# 2. profile
status, d = get('/users/profile/', token)
check('profile 字段', d['code'] == 200 and 'continuous_days' in d['data'] and 'total_days' in d['data'],
      str(sorted(d['data'].keys())))

# 3. words 分页（验证 page_size 参数生效）
status, d = get('/words/?page=1&page_size=3', token)
wl = d['data']
check('words 分页结构 count/results',
      d['code'] == 200 and 'count' in wl and 'results' in wl and len(wl['results']) == 3,
      f"count={wl.get('count')} results={len(wl.get('results', []))}")
w0 = wl['results'][0]
check('word 字段下划线',
      'example_en' in w0 and 'audio_url' in w0 and 'is_learned' in w0 and 'is_mastered' in w0,
      str(sorted(w0.keys())))

# 4. 打卡
status, d = post('/checkin/do_checkin/', {'word_count': 8, 'study_duration': 20, 'note': '详细验证'}, token)
check('打卡成功', d['code'] == 200, f"msg={d['data']['message']}")
check('打卡响应字段', 'continuous_days' in d['data'] and 'total_days' in d['data'] and 'checkin' in d['data'])
ci = d['data']['checkin']
check('checkin 字段下划线',
      'checkin_date' in ci and 'study_duration' in ci and 'continuous_days' in ci)
check('checkin_time 带 UTC 标记', ci['checkin_time'].endswith('Z'), f"time={ci['checkin_time']}")
check('checkin_date 为日期字符串', len(ci['checkin_date']) == 10, f"date={ci['checkin_date']}")

# 5. 日历
status, d = get('/checkin/calendar/', token)
cd = d['data']
check('日历字段', 'checkin_dates' in cd and 'days' in cd and 'year' in cd, str(sorted(cd.keys())))
check('日历日期格式 yyyy-MM-dd',
      cd['checkin_dates'] == [] or all(len(x) == 10 for x in cd['checkin_dates']), str(cd['checkin_dates']))

# 6. 排行榜（先学 2 个单词，验证日榜 my_rank 出现）
status, d = post('/words/mark_learned/', {'word_id': 2, 'is_mastered': False}, token)
check('mark_learned 成功', d['code'] == 200)
status, d = post('/words/mark_learned/', {'word_id': 3, 'is_mastered': False}, token)
check('mark_learned 2 成功', d['code'] == 200)

status, d = get('/leaderboard/daily/', token)
lb = d['data']
check('日榜结构 period/list/my_rank', 'period' in lb and 'list' in lb and 'my_rank' in lb)
check('榜单项字段下划线',
      lb['list'] == [] or ('user_id' in lb['list'][0] and 'words_count' in lb['list'][0] and 'days_count' in lb['list'][0]),
      str(sorted(lb['list'][0].keys()) if lb['list'] else 'empty'))
uid = get('/users/profile/', token)[1]['data']['id']
check('my_rank 中当前用户', lb['my_rank'] is not None and lb['my_rank']['user_id'] == uid,
      f"my_rank={lb['my_rank']}")

# 7. 首页统计
status, d = get('/leaderboard/stats/', token)
check('stats 字段',
      d['code'] == 200 and 'today_checkin_count' in d['data'] and 'weekly_words' in d['data'],
      str(sorted(d['data'].keys())))

# 8. JWT 无效 token → 401
status, d = get('/users/profile/', 'invalid.token.here')
check('无效 token 返回 401', status == 401 and d['code'] == 401, f"http={status}")

# 9. 管理端 dashboard
status, d = get('/admin-users/dashboard/', token)
check('dashboard 字段',
      d['code'] == 200 and 'today_active_users' in d['data'] and 'abnormal_logins' in d['data'],
      str(sorted(d['data'].keys())))

# 10. 尾斜杠兼容
status, d = get('/users/profile', token)
check('无尾斜杠路径也可访问', status == 200)

print(f'\n===== 结果: 通过 {ok} 项, 失败 {fail} 项 =====')
