"""
单词模块 — 模型定义
tb_word, tb_word_progress
"""
from django.db import models
from apps.users.models import User


class Word(models.Model):
    """英语单词"""
    LEVEL_CHOICES = [(1, '初级'), (2, '中级'), (3, '高级')]

    word = models.CharField(max_length=128, verbose_name='单词')
    phonetic = models.CharField(max_length=128, blank=True, default='', verbose_name='音标')
    meaning = models.CharField(max_length=512, verbose_name='中文释义')
    example_en = models.CharField(max_length=1024, blank=True, default='', verbose_name='英文例句')
    example_zh = models.CharField(max_length=1024, blank=True, default='', verbose_name='例句翻译')
    audio_url = models.CharField(max_length=512, blank=True, default='', verbose_name='发音URL')
    level = models.IntegerField(choices=LEVEL_CHOICES, default=1, verbose_name='难度等级')
    category = models.CharField(max_length=64, default='CET-4', verbose_name='分类标签')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='创建时间')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='更新时间')

    class Meta:
        db_table = 'tb_word'
        verbose_name = '单词'
        verbose_name_plural = verbose_name

    def __str__(self):
        return f'{self.word} — {self.meaning}'


class WordProgress(models.Model):
    """用户单词学习进度"""
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='word_progress', verbose_name='用户')
    word = models.ForeignKey(Word, on_delete=models.CASCADE, verbose_name='单词')
    is_learned = models.SmallIntegerField(default=0, verbose_name='是否已学')
    is_mastered = models.SmallIntegerField(default=0, verbose_name='是否掌握')
    learned_at = models.DateTimeField(null=True, blank=True, verbose_name='学习时间')
    review_count = models.IntegerField(default=0, verbose_name='复习次数')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='创建时间')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='更新时间')

    class Meta:
        db_table = 'tb_word_progress'
        verbose_name = '单词学习进度'
        verbose_name_plural = verbose_name
        unique_together = [('user', 'word')]

    def __str__(self):
        return f'{self.user.nickname} → {self.word.word} [{"已学" if self.is_learned else "未学"}]'
