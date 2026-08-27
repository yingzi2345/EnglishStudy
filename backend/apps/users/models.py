"""
用户模块 — 模型定义
tb_user, tb_login_log, tb_admin
"""
from django.db import models


class User(models.Model):
    """微信小程序用户"""
    openid = models.CharField(max_length=128, unique=True, verbose_name='微信OpenID')
    nickname = models.CharField(max_length=64, default='微信用户', verbose_name='昵称')
    avatar_url = models.CharField(max_length=512, blank=True, default='', verbose_name='头像URL')
    gender = models.SmallIntegerField(default=0, verbose_name='性别(0未知,1男,2女)')
    status = models.SmallIntegerField(default=1, verbose_name='状态(1正常,0禁用)')
    total_words = models.IntegerField(default=0, verbose_name='累计学习单词数')
    total_days = models.IntegerField(default=0, verbose_name='累计打卡天数')
    max_continuous = models.IntegerField(default=0, verbose_name='最长连续打卡天数')
    last_login_ip = models.CharField(max_length=64, blank=True, default='', verbose_name='最近登录IP')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='注册时间')
    last_login_at = models.DateTimeField(auto_now_add=True, verbose_name='最近登录时间')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='更新时间')

    class Meta:
        db_table = 'tb_user'
        verbose_name = '用户'
        verbose_name_plural = verbose_name

    @property
    def is_authenticated(self):
        return True

    @property
    def is_anonymous(self):
        return False

    def __str__(self):
        return f'{self.nickname}({self.openid[:8]}...)'


class LoginLog(models.Model):
    """登录日志 — 网络工程IP溯源特色"""
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='login_logs', verbose_name='用户')
    ip_address = models.CharField(max_length=64, blank=True, default='', verbose_name='IP地址')
    device_info = models.CharField(max_length=256, blank=True, default='', verbose_name='设备信息')
    location = models.CharField(max_length=128, blank=True, default='', verbose_name='IP归属地')
    login_time = models.DateTimeField(auto_now_add=True, verbose_name='登录时间')
    is_abnormal = models.SmallIntegerField(default=0, verbose_name='是否异常(0正常,1异常)')
    abnormal_reason = models.CharField(max_length=256, blank=True, default='', verbose_name='异常原因')

    class Meta:
        db_table = 'tb_login_log'
        verbose_name = '登录日志'
        verbose_name_plural = verbose_name
        ordering = ['-login_time']

    def __str__(self):
        return f'{self.user.nickname} @ {self.ip_address} [{self.login_time}]'


class Admin(models.Model):
    """管理员"""
    username = models.CharField(max_length=64, unique=True, verbose_name='用户名')
    password_hash = models.CharField(max_length=256, verbose_name='密码哈希')
    role = models.CharField(max_length=32, default='admin', verbose_name='角色')
    nickname = models.CharField(max_length=64, blank=True, default='', verbose_name='昵称')
    status = models.SmallIntegerField(default=1, verbose_name='状态(1正常,0禁用)')
    last_login_ip = models.CharField(max_length=64, blank=True, default='', verbose_name='最近登录IP')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='创建时间')
    last_login_at = models.DateTimeField(null=True, blank=True, verbose_name='最近登录时间')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='更新时间')

    class Meta:
        db_table = 'tb_admin'
        verbose_name = '管理员'
        verbose_name_plural = verbose_name

    def __str__(self):
        return f'{self.username}({self.role})'
