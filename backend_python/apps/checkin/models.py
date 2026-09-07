"""
打卡模块 — 模型定义
tb_checkin
"""
from django.db import models
from apps.users.models import User


class Checkin(models.Model):
    """每日打卡记录"""
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='checkins', verbose_name='用户')
    checkin_date = models.DateField(verbose_name='打卡日期')
    checkin_time = models.DateTimeField(auto_now_add=True, verbose_name='打卡时间')
    word_count = models.IntegerField(default=0, verbose_name='当日学习单词数')
    study_duration = models.IntegerField(default=0, verbose_name='当日学习时长(分钟)')
    continuous_days = models.IntegerField(default=0, verbose_name='连续打卡天数')
    note = models.CharField(max_length=256, blank=True, default='', verbose_name='打卡备注')

    class Meta:
        db_table = 'tb_checkin'
        verbose_name = '打卡记录'
        verbose_name_plural = verbose_name
        unique_together = [('user', 'checkin_date')]
        ordering = ['-checkin_date']

    def __str__(self):
        return f'{self.user.nickname} @ {self.checkin_date} (连续{self.continuous_days}天)'
