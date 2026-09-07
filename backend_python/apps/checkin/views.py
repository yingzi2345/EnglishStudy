"""
打卡模块 — 视图
每日打卡、打卡记录查询、连续打卡计算
"""
from datetime import date, timedelta
from rest_framework import viewsets, permissions
from rest_framework.decorators import action
from django.utils import timezone
from .models import Checkin
from apps.users.models import User
from .serializers import CheckinSerializer, CheckinRequestSerializer
from utils.exceptions import api_response


class CheckinViewSet(viewsets.ModelViewSet):
    """
    打卡视图集
    每日打卡、打卡记录、打卡日历
    """
    queryset = Checkin.objects.all()
    serializer_class = CheckinSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return Checkin.objects.filter(user_id=self.request.user.id)

    @action(detail=False, methods=['post'])
    def do_checkin(self, request):
        """执行每日打卡"""
        serializer = CheckinRequestSerializer(data=request.data)
        if not serializer.is_valid():
            return api_response(message='参数错误', code=400)

        user = User.objects.get(id=request.user.id)
        today = date.today()

        # 检查今日是否已打卡
        if Checkin.objects.filter(user=user, checkin_date=today).exists():
            return api_response(message='今日已打卡，请勿重复操作', code=400)

        # 计算连续打卡天数
        continuous_days = 1
        yesterday = today - timedelta(days=1)
        try:
            yesterday_checkin = Checkin.objects.get(user=user, checkin_date=yesterday)
            continuous_days = yesterday_checkin.continuous_days + 1
        except Checkin.DoesNotExist:
            continuous_days = 1

        checkin = Checkin.objects.create(
            user=user,
            checkin_date=today,
            checkin_time=timezone.now(),
            word_count=serializer.validated_data.get('word_count', 0),
            study_duration=serializer.validated_data.get('study_duration', 0),
            continuous_days=continuous_days,
            note=serializer.validated_data.get('note', ''),
        )

        # 更新用户统计数据
        user.total_days = Checkin.objects.filter(user=user).count()
        if continuous_days > user.max_continuous:
            user.max_continuous = continuous_days
        user.save()

        return api_response(data={
            'checkin': CheckinSerializer(checkin).data,
            'continuous_days': continuous_days,
            'total_days': user.total_days,
            'message': f'打卡成功！已连续打卡{continuous_days}天',
        })

    @action(detail=False, methods=['get'])
    def today_status(self, request):
        """查询今日打卡状态"""
        today = date.today()
        checked = Checkin.objects.filter(
            user_id=request.user.id, checkin_date=today
        ).first()

        return api_response(data={
            'is_checked': checked is not None,
            'today_record': CheckinSerializer(checked).data if checked else None,
        })

    @action(detail=False, methods=['get'])
    def calendar(self, request):
        """获取打卡日历数据(指定月份)"""
        year = int(request.query_params.get('year', date.today().year))
        month = int(request.query_params.get('month', date.today().month))

        from calendar import monthrange
        _, days = monthrange(year, month)

        checkin_dates = Checkin.objects.filter(
            user_id=request.user.id,
            checkin_date__year=year,
            checkin_date__month=month,
        ).values_list('checkin_date', flat=True)

        return api_response(data={
            'year': year,
            'month': month,
            'days': days,
            'checkin_dates': [str(d) for d in checkin_dates],
        })

    @action(detail=False, methods=['get'])
    def records(self, request):
        """获取打卡记录列表"""
        records = Checkin.objects.filter(
            user_id=request.user.id
        ).order_by('-checkin_date')[:90]
        return api_response(data=CheckinSerializer(records, many=True).data)

    @action(detail=False, methods=['get'])
    def streak(self, request):
        """获取连续打卡信息"""
        today = date.today()
        continuous = 0
        for i in range(365):
            check_date = today - timedelta(days=i)
            exists = Checkin.objects.filter(
                user_id=request.user.id, checkin_date=check_date
            ).exists()
            if exists:
                continuous += 1
            else:
                break

        user = User.objects.get(id=request.user.id)
        is_today_checked = Checkin.objects.filter(
            user=user, checkin_date=today
        ).exists()

        return api_response(data={
            'continuous_days': continuous,
            'max_continuous': user.max_continuous,
            'total_days': user.total_days,
            'is_today_checked': is_today_checked,
        })
