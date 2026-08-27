"""
单词模块 — 视图
单词CRUD、学习进度管理
"""
import random
from django.db import models as db_models
from django.utils import timezone
from rest_framework import viewsets, permissions
from rest_framework.decorators import action
from .models import Word, WordProgress
from .serializers import WordSerializer, WordProgressSerializer, MarkWordSerializer
from utils.exceptions import api_response


class WordViewSet(viewsets.ModelViewSet):
    """
    单词视图集
    提供单词查询、学习进度管理
    """
    queryset = Word.objects.all()
    serializer_class = WordSerializer

    def get_permissions(self):
        if self.action in ['list', 'retrieve', 'random_word', 'daily_words']:
            return [permissions.IsAuthenticated()]
        return [permissions.IsAuthenticated()]

    def retrieve(self, request, *args, **kwargs):
        """获取单词详情（统一响应格式）"""
        instance = self.get_object()
        serializer = self.get_serializer(instance)
        return api_response(data=serializer.data)

    def list(self, request, *args, **kwargs):
        """获取单词列表（统一响应格式，支持分类筛选和关键词搜索）"""
        queryset = self.filter_queryset(self.get_queryset())

        # 分类筛选
        category = request.query_params.get('category', None)
        if category:
            queryset = queryset.filter(category=category)

        # 关键词搜索
        search = request.query_params.get('search', None)
        if search:
            queryset = queryset.filter(
                db_models.Q(word__icontains=search) | db_models.Q(meaning__icontains=search)
            )

        page = self.paginate_queryset(queryset)
        if page is not None:
            serializer = self.get_serializer(page, many=True)
            return api_response(data={
                'count': self.paginator.page.paginator.count,
                'next': self.paginator.get_next_link(),
                'previous': self.paginator.get_previous_link(),
                'results': serializer.data,
            })
        serializer = self.get_serializer(queryset, many=True)
        return api_response(data=serializer.data)

    @action(detail=False, methods=['get'])
    def categories(self, request):
        """获取所有单词分类"""
        categories = Word.objects.values_list('category', flat=True).distinct()
        return api_response(data=list(categories))

    @action(detail=False, methods=['get'])
    def random_word(self, request):
        """随机获取一个单词"""
        level = request.query_params.get('level', None)
        category = request.query_params.get('category', None)
        qs = Word.objects.all()
        if level:
            qs = qs.filter(level=int(level))
        if category:
            qs = qs.filter(category=category)

        count = qs.count()
        if count == 0:
            return api_response(message='暂无单词数据', code=404)

        random_idx = random.randint(0, count - 1)
        word = qs[random_idx]
        return api_response(data=WordSerializer(word, context={'request': request}).data)

    @action(detail=False, methods=['get'])
    def daily_words(self, request):
        """获取每日推荐单词列表(10个)"""
        count = int(request.query_params.get('count', 10))
        total = Word.objects.count()
        if total == 0:
            return api_response(message='暂无单词数据', code=404)

        # 优先选择用户未学习的单词
        learned_ids = WordProgress.objects.filter(
            user_id=request.user.id, is_learned=1
        ).values_list('word_id', flat=True)

        unlearned = Word.objects.exclude(id__in=learned_ids)
        if unlearned.count() >= count:
            words = random.sample(list(unlearned), count)
        else:
            words = random.sample(list(Word.objects.all()), min(count, total))

        return api_response(data=WordSerializer(words, many=True, context={'request': request}).data)

    @action(detail=False, methods=['post'])
    def mark_learned(self, request):
        """标记单词为已学"""
        serializer = MarkWordSerializer(data=request.data)
        if not serializer.is_valid():
            return api_response(message='参数错误', code=400)

        word_id = serializer.validated_data['word_id']
        is_mastered = serializer.validated_data.get('is_mastered', False)

        try:
            word = Word.objects.get(id=word_id)
        except Word.DoesNotExist:
            return api_response(message='单词不存在', code=404)

        progress, created = WordProgress.objects.get_or_create(
            user_id=request.user.id,
            word=word,
            defaults={'is_learned': 1, 'learned_at': timezone.now()},
        )

        if not created:
            if not progress.is_learned:
                progress.is_learned = 1
                progress.learned_at = timezone.now()
            progress.review_count += 1
            if is_mastered:
                progress.is_mastered = 1
            progress.save()

        # 更新用户累计学习单词数
        user = request.user.__class__.objects.get(id=request.user.id)
        if created:
            user.total_words = WordProgress.objects.filter(
                user=user, is_learned=1
            ).count()
            user.save()

        return api_response(data=WordProgressSerializer(progress).data)

    @action(detail=False, methods=['post'])
    def unmark_learned(self, request):
        """撤销单词学习状态（用于点错时回退）"""
        word_id = request.data.get('word_id')
        if not word_id:
            return api_response(message='参数错误', code=400)

        try:
            progress = WordProgress.objects.get(user_id=request.user.id, word_id=word_id)
        except WordProgress.DoesNotExist:
            return api_response(message='未找到学习记录', code=404)

        progress.is_learned = 0
        progress.is_mastered = 0
        progress.save()

        # 更新用户累计学习单词数
        user = request.user.__class__.objects.get(id=request.user.id)
        user.total_words = WordProgress.objects.filter(
            user=user, is_learned=1
        ).count()
        user.save()

        return api_response(data={'word_id': word_id, 'is_learned': False, 'is_mastered': False})

    @action(detail=False, methods=['get'])
    def my_progress(self, request):
        """获取我的学习进度"""
        progress_list = WordProgress.objects.filter(
            user_id=request.user.id
        ).select_related('word').order_by('-updated_at')

        learned = progress_list.filter(is_learned=1).count()
        mastered = progress_list.filter(is_mastered=1).count()
        total = Word.objects.count()

        return api_response(data={
            'total_words': total,
            'learned_words': learned,
            'mastered_words': mastered,
            'progress_percent': round(learned / total * 100, 1) if total > 0 else 0,
            'recent_progress': WordProgressSerializer(
                progress_list[:20], many=True
            ).data,
        })

    @action(detail=False, methods=['get'])
    def search(self, request):
        """搜索单词"""
        keyword = request.query_params.get('q', '')
        if not keyword:
            return api_response(message='请输入搜索关键词', code=400)
        words = Word.objects.filter(
            db_models.Q(word__icontains=keyword) | db_models.Q(meaning__icontains=keyword)
        )[:20]
        return api_response(data=WordSerializer(words, many=True, context={'request': request}).data)
