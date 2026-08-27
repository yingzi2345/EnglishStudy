"""
打卡模块 — Django Admin 注册
"""
from django.contrib import admin
from .models import Checkin


@admin.register(Checkin)
class CheckinAdmin(admin.ModelAdmin):
    list_display = ['id', 'user', 'checkin_date', 'continuous_days', 'word_count', 'study_duration', 'checkin_time']
    list_filter = ['checkin_date']
    search_fields = ['user__nickname']
    readonly_fields = ['checkin_time']
    ordering = ['-checkin_date']
    list_per_page = 30
    date_hierarchy = 'checkin_date'
