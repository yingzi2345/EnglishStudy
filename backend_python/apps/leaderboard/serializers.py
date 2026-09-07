"""
排行榜模块 — 序列化器
"""
from rest_framework import serializers


class LeaderboardUserSerializer(serializers.Serializer):
    """排行榜用户信息"""
    user_id = serializers.IntegerField()
    nickname = serializers.CharField()
    avatar_url = serializers.CharField()
    total_days = serializers.IntegerField()
    total_words = serializers.IntegerField()
    max_continuous = serializers.IntegerField()
    rank = serializers.IntegerField()


class StudyStatsSerializer(serializers.Serializer):
    """学习统计数据"""
    today_words = serializers.IntegerField()
    today_checkin = serializers.BooleanField()
    continuous_days = serializers.IntegerField()
    total_users = serializers.IntegerField()
    today_checkin_count = serializers.IntegerField()
    weekly_words = serializers.IntegerField()
    monthly_words = serializers.IntegerField()
