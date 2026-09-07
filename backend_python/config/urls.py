"""
URL configuration for English Check-in System.
"""
from django.contrib import admin
from django.urls import path, include

# 自定义 Admin 站点标题
admin.site.site_header = '后台管理'
admin.site.site_title = '后台管理'
admin.site.index_title = '英语学习打卡系统'
from rest_framework.routers import DefaultRouter
from drf_yasg.views import get_schema_view
from drf_yasg import openapi
from rest_framework.permissions import AllowAny

# API 路由
from apps.users.views import UserViewSet, AdminViewSet
from apps.words.views import WordViewSet
from apps.checkin.views import CheckinViewSet
from apps.leaderboard.views import LeaderboardViewSet

router = DefaultRouter()
router.register(r'users', UserViewSet, basename='users')
router.register(r'admin-users', AdminViewSet, basename='admin-users')
router.register(r'words', WordViewSet, basename='words')
router.register(r'checkin', CheckinViewSet, basename='checkin')
router.register(r'leaderboard', LeaderboardViewSet, basename='leaderboard')

# Swagger 文档
schema_view = get_schema_view(
    openapi.Info(
        title="英语学习打卡系统 API",
        default_version='v1.0',
        description="基于微信小程序的英语学习打卡系统 — 桂林电子科技大学毕业设计",
        contact=openapi.Contact(email="support@guet.edu.cn"),
    ),
    public=True,
    permission_classes=[AllowAny],
)

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/', include(router.urls)),
    path('api/auth/', include('apps.users.urls')),
    path('swagger/', schema_view.with_ui('swagger', cache_timeout=0), name='swagger'),
    path('redoc/', schema_view.with_ui('redoc', cache_timeout=0), name='redoc'),
]
