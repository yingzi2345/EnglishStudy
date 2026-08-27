"""
单词模块 — 序列化器
"""
from rest_framework import serializers
from .models import Word, WordProgress


class WordSerializer(serializers.ModelSerializer):
    """单词序列化器"""
    is_learned = serializers.SerializerMethodField()
    is_mastered = serializers.SerializerMethodField()

    class Meta:
        model = Word
        fields = [
            'id', 'word', 'phonetic', 'meaning',
            'example_en', 'example_zh', 'audio_url',
            'level', 'category', 'is_learned', 'is_mastered',
            'created_at',
        ]

    def get_is_learned(self, obj):
        request = self.context.get('request')
        if request and request.user and hasattr(request.user, 'id'):
            return WordProgress.objects.filter(
                user_id=request.user.id, word=obj, is_learned=1
            ).exists()
        return False

    def get_is_mastered(self, obj):
        request = self.context.get('request')
        if request and request.user and hasattr(request.user, 'id'):
            return WordProgress.objects.filter(
                user_id=request.user.id, word=obj, is_mastered=1
            ).exists()
        return False


class WordProgressSerializer(serializers.ModelSerializer):
    """学习进度序列化器"""
    word_name = serializers.CharField(source='word.word', read_only=True)
    word_meaning = serializers.CharField(source='word.meaning', read_only=True)

    class Meta:
        model = WordProgress
        fields = [
            'id', 'user', 'word', 'word_name', 'word_meaning',
            'is_learned', 'is_mastered', 'learned_at',
            'review_count', 'created_at', 'updated_at',
        ]


class MarkWordSerializer(serializers.Serializer):
    """标记单词学习状态"""
    word_id = serializers.IntegerField()
    is_mastered = serializers.BooleanField(required=False, default=False)
