"""
排行榜模块 — 视图
排行榜数据统计
"""
from datetime import date, timedelta, datetime
from django.db.models import Count, Sum, Q
from django.db.models.expressions import RawSQL
from django.utils import timezone
from rest_framework import viewsets, permissions
from rest_framework.decorators import action
from apps.users.models import User
from apps.checkin.models import Checkin
from apps.words.models import WordProgress
from .serializers import LeaderboardUserSerializer, StudyStatsSerializer
from utils.exceptions import api_response


class LeaderboardViewSet(viewsets.GenericViewSet):
    """
    排行榜视图集
    日榜、周榜、月榜、总榜
    """
    permission_classes = [permissions.IsAuthenticated]

    @action(detail=False, methods=['get'])
    def daily(self, request):
        """今日排行榜 (按学习单词数)"""
        return self._get_rank('daily')

    @action(detail=False, methods=['get'])
    def weekly(self, request):
        """本周排行榜"""
        return self._get_rank('weekly')

    @action(detail=False, methods=['get'])
    def monthly(self, request):
        """本月排行榜"""
        return self._get_rank('monthly')

    @action(detail=False, methods=['get'])
    def alltime(self, request):
        """总排行榜"""
        return self._get_rank('alltime')

    def _get_rank(self, period):
        """通用排行榜计算
        - 日榜/周榜/月榜：基于 WordProgress 实际学习数据，按时间段统计
        - 总榜：基于 User.total_words 累计数据
        注意：避免使用 learned_at__date 查询（MySQL + USE_TZ=True 兼容性问题），
        改用 timezone-aware datetime + __gte/__lt 比较。
        """
        today = date.today()

        if period == 'daily':
            start_dt = timezone.make_aware(datetime.combine(today, datetime.min.time()))
            end_dt = timezone.make_aware(datetime.combine(today + timedelta(days=1), datetime.min.time()))
            period_qs = WordProgress.objects.filter(
                is_learned=1, learned_at__gte=start_dt, learned_at__lt=end_dt
            )
        elif period == 'weekly':
            start = today - timedelta(days=today.weekday())
            start_dt = timezone.make_aware(datetime.combine(start, datetime.min.time()))
            period_qs = WordProgress.objects.filter(
                is_learned=1, learned_at__gte=start_dt
            )
        elif period == 'monthly':
            start = today.replace(day=1)
            start_dt = timezone.make_aware(datetime.combine(start, datetime.min.time()))
            period_qs = WordProgress.objects.filter(
                is_learned=1, learned_at__gte=start_dt
            )
        else:
            start_dt = None
            period_qs = None

        if period == 'alltime':
            # 总榜保持原有逻辑：按累计单词数排序
            users = User.objects.filter(status=1, total_words__gt=0).order_by('-total_words')[:50]
            result = []
            for i, user in enumerate(users):
                result.append({
                    'rank': i + 1,
                    'user_id': user.id,
                    'nickname': user.nickname,
                    'avatar_url': user.avatar_url,
                    'days_count': user.total_days,
                    'words_count': user.total_words,
                    'max_continuous': user.max_continuous,
                })
        else:
            # 日/周/月榜：按时间段内实际学习单词数聚合
            # days_count 使用 RawSQL 直接取 MySQL DATE() 避免 ORM 时区转换问题
            user_stats = period_qs.values(
                'user_id', 'user__nickname', 'user__avatar_url',
                'user__total_days', 'user__max_continuous',
            ).annotate(
                words_count=Count('id'),
                days_count=Count(RawSQL('DATE(learned_at)', ()), distinct=True),
            ).order_by('-words_count')[:50]

            result = []
            for i, stat in enumerate(user_stats):
                result.append({
                    'rank': i + 1,
                    'user_id': stat['user_id'],
                    'nickname': stat['user__nickname'],
                    'avatar_url': stat['user__avatar_url'],
                    'days_count': stat['days_count'],
                    'words_count': stat['words_count'],
                    'max_continuous': stat['user__max_continuous'],
                })

        # 查找当前用户排名
        my_rank = None
        for r in result:
            if r['user_id'] == self.request.user.id:
                my_rank = r
                break

        # 如果当前用户不在前 50，单独查询
        if my_rank is None and period != 'alltime' and period_qs is not None:
            my_stats = period_qs.filter(
                user_id=self.request.user.id
            ).aggregate(
                words_count=Count('id'),
                days_count=Count(RawSQL('DATE(learned_at)', ()), distinct=True),
            )
            if my_stats['words_count'] > 0:
                user = self.request.user
                my_rank = {
                    'rank': None,  # 不在前 50，排名未知
                    'user_id': user.id,
                    'nickname': user.nickname,
                    'avatar_url': user.avatar_url,
                    'days_count': my_stats['days_count'],
                    'words_count': my_stats['words_count'],
                    'max_continuous': user.max_continuous,
                }

        return api_response(data={
            'period': period,
            'list': result[:50],
            'my_rank': my_rank,
        })

    @action(detail=False, methods=['get'])
    def stats(self, request):
        """首页统计数据"""
        today = date.today()
        today_start = timezone.make_aware(datetime.combine(today, datetime.min.time()))
        tomorrow_start = timezone.make_aware(datetime.combine(today + timedelta(days=1), datetime.min.time()))
        week_start = today - timedelta(days=today.weekday())
        week_start_dt = timezone.make_aware(datetime.combine(week_start, datetime.min.time()))
        month_start = today.replace(day=1)
        month_start_dt = timezone.make_aware(datetime.combine(month_start, datetime.min.time()))

        total_users = User.objects.filter(status=1).count()
        today_checkins = Checkin.objects.filter(checkin_date=today).count()
        today_words = WordProgress.objects.filter(
            is_learned=1, learned_at__gte=today_start, learned_at__lt=tomorrow_start
        ).count()
        weekly_words = WordProgress.objects.filter(
            is_learned=1, learned_at__gte=week_start_dt
        ).count()
        monthly_words = WordProgress.objects.filter(
            is_learned=1, learned_at__gte=month_start_dt
        ).count()

        return api_response(data={
            'total_users': total_users,
            'today_checkin_count': today_checkins,
            'today_words': today_words,
            'weekly_words': weekly_words,
            'monthly_words': monthly_words,
        })
