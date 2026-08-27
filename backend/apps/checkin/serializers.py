"""
打卡模块 — 序列化器
"""
from rest_framework import serializers
from .models import Checkin


class CheckinSerializer(serializers.ModelSerializer):
    """打卡记录序列化器"""
    nickname = serializers.CharField(source='user.nickname', read_only=True)

    class Meta:
        model = Checkin
        fields = [
            'id', 'user', 'nickname', 'checkin_date', 'checkin_time',
            'word_count', 'study_duration', 'continuous_days', 'note',
        ]
        read_only_fields = ['id', 'user', 'checkin_date', 'checkin_time', 'continuous_days']


class CheckinRequestSerializer(serializers.Serializer):
    """打卡请求"""
    word_count = serializers.IntegerField(default=0)
    study_duration = serializers.IntegerField(default=0)
    note = serializers.CharField(max_length=256, required=False, allow_blank=True)
