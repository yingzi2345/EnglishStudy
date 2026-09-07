# -*- coding: utf-8 -*-
"""模拟微信小程序真实请求：超长 User-Agent + 中文昵称（仅用标准库）"""
import json
import urllib.request

BASE = 'http://127.0.0.1:8000/api'


def post(url, payload, headers=None, token=None):
    data = json.dumps(payload).encode('utf-8')
    h = {'Content-Type': 'application/json'}
    if headers:
        h.update(headers)
    if token:
        h['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(BASE + url, data=data, headers=h, method='POST')
    with urllib.request.urlopen(req) as resp:
        return resp.status, json.loads(resp.read().decode('utf-8'))


def get(url, token):
    req = urllib.request.Request(BASE + url, headers={'Authorization': 'Bearer ' + token})
    with urllib.request.urlopen(req) as resp:
        return resp.status, json.loads(resp.read().decode('utf-8'))


ua = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 '
      'MicroMessenger/8.0.47 WeChatDevTools/1.06.2310120 wx_devtools '
      + 'X' * 1500)
status, d = post('/auth/wechat-login/', {'code': 'ua_fix_test', 'nickname': '超长UA测试😀'},
                 headers={'User-Agent': ua})
print('HTTP:', status, '| body.code:', d['code'])
print('msg:', d['message'])
if d.get('data'):
    u = d['data']['user']
    print('user id:', u['id'], '| nickname:', u['nickname'])
    token = d['data']['token']['access']
    s2, logs = get('/users/login_logs/', token)
    if logs['data']:
        dev = logs['data'][0]['device_info'] or ''
        print('device_info 长度:', len(dev), '(上限 256)')
        print('device_info 前缀:', dev[:60])
