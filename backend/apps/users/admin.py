"""
用户模块 — Django Admin 注册
"""
from django.contrib import admin
from .models import User, LoginLog, Admin as AdminUser


@admin.register(User)
class UserAdmin(admin.ModelAdmin):
    list_display = ['id', 'nickname', 'openid', 'status', 'total_words', 'total_days', 'max_continuous', 'last_login_at', 'created_at']
    list_filter = ['status', 'gender', 'created_at']
    search_fields = ['nickname', 'openid']
    readonly_fields = ['openid', 'created_at', 'updated_at', 'last_login_at']
    ordering = ['-created_at']
    list_per_page = 30


@admin.register(LoginLog)
class LoginLogAdmin(admin.ModelAdmin):
    list_display = ['id', 'user', 'ip_address', 'location', 'is_abnormal', 'login_time']
    list_filter = ['is_abnormal', 'login_time']
    search_fields = ['user__nickname', 'ip_address', 'location']
    readonly_fields = ['user', 'ip_address', 'device_info', 'login_time']
    ordering = ['-login_time']
    list_per_page = 30
    date_hierarchy = 'login_time'


@admin.register(AdminUser)
class AdminUserAdmin(admin.ModelAdmin):
    list_display = ['id', 'username', 'nickname', 'role', 'status', 'last_login_at', 'created_at']
    list_filter = ['role', 'status']
    search_fields = ['username', 'nickname']
    readonly_fields = ['last_login_at', 'last_login_ip', 'created_at', 'updated_at']
    ordering = ['-created_at']
