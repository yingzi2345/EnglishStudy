"""
用户模块 — 序列化器
"""
from rest_framework import serializers
from .models import User, LoginLog, Admin


class UserSerializer(serializers.ModelSerializer):
    """用户信息序列化器"""

    class Meta:
        model = User
        fields = [
            'id', 'openid', 'nickname', 'avatar_url', 'gender',
            'status', 'total_words', 'total_days', 'max_continuous',
            'last_login_ip', 'created_at', 'last_login_at',
        ]
        read_only_fields = ['id', 'openid', 'created_at', 'last_login_at']


class UserProfileSerializer(serializers.ModelSerializer):
    """用户个人资料（不含敏感字段）"""
    continuous_days = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = [
            'id', 'nickname', 'avatar_url', 'gender',
            'total_words', 'total_days', 'max_continuous',
            'continuous_days', 'last_login_at',
        ]

    def get_continuous_days(self, obj):
        """获取当前连续打卡天数"""
        from apps.checkin.models import Checkin
        from datetime import date, timedelta
        today = date.today()
        continuous = 0
        for i in range(365):
            check_date = today - timedelta(days=i)
            exists = Checkin.objects.filter(user=obj, checkin_date=check_date).exists()
            if exists:
                continuous += 1
            else:
                break
        return continuous


class LoginLogSerializer(serializers.ModelSerializer):
    """登录日志序列化器"""
    nickname = serializers.CharField(source='user.nickname', read_only=True)

    class Meta:
        model = LoginLog
        fields = [
            'id', 'user', 'nickname', 'ip_address', 'device_info',
            'location', 'login_time', 'is_abnormal', 'abnormal_reason',
        ]


class AdminLoginSerializer(serializers.Serializer):
    """管理员登录请求"""
    username = serializers.CharField(max_length=64)
    password = serializers.CharField(max_length=128)


class WechatLoginSerializer(serializers.Serializer):
    """微信小程序登录请求"""
    code = serializers.CharField(max_length=256)
    nickname = serializers.CharField(max_length=64, required=False, allow_blank=True)
    avatar_url = serializers.CharField(max_length=512, required=False, allow_blank=True)
