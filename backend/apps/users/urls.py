"""
用户模块 — URL 配置
"""
from django.urls import path
from .views import UserViewSet, AdminViewSet

# 手动注册action路由
urlpatterns = [
    path('wechat-login/', UserViewSet.as_view({'post': 'wechat_login'}), name='wechat-login'),
    path('admin-login/', AdminViewSet.as_view({'post': 'login'}), name='admin-login'),
]
