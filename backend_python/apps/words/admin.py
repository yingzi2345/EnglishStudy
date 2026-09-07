"""
单词模块 — Django Admin 注册
"""
from django.contrib import admin
from .models import Word, WordProgress


@admin.register(Word)
class WordAdmin(admin.ModelAdmin):
    list_display = ['id', 'word', 'meaning', 'level', 'category', 'created_at']
    list_filter = ['level', 'category']
    search_fields = ['word', 'meaning']
    readonly_fields = ['created_at', 'updated_at']
    sortable_by = ('id',)
    ordering = ['level', 'word']
    list_per_page = 50


@admin.register(WordProgress)
class WordProgressAdmin(admin.ModelAdmin):
    list_display = ['id', 'user', 'word', 'is_learned', 'is_mastered', 'review_count', 'learned_at']
    list_filter = ['is_learned', 'is_mastered', 'learned_at']
    search_fields = ['user__nickname', 'word__word']
    readonly_fields = ['created_at', 'updated_at']
    ordering = ['-learned_at']
    list_per_page = 50
    date_hierarchy = 'learned_at'
