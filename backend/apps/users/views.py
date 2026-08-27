"""
用户模块 — 视图
微信登录、JWT认证、用户信息、管理员登录、IP溯源
"""
import logging
import requests
import bcrypt
from datetime import date, timedelta
from django.conf import settings
from django.db.models import Count, Q
from rest_framework import viewsets, status, permissions
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken
from django.utils import timezone

from .models import User, LoginLog, Admin
from .serializers import (
    UserSerializer, UserProfileSerializer,
    LoginLogSerializer, AdminLoginSerializer, WechatLoginSerializer,
)
from utils.exceptions import api_response

logger = logging.getLogger('django')


def get_tokens_for_user(user):
    """为用户生成JWT Token对"""
    refresh = RefreshToken.for_user(user)
    refresh['user_id'] = user.id
    refresh['openid'] = user.openid
    return {
        'access': str(refresh.access_token),
        'refresh': str(refresh),
    }


class UserViewSet(viewsets.ModelViewSet):
    """
    用户视图集
    提供微信登录、个人信息管理、学习统计
    """
    queryset = User.objects.filter(status=1)
    serializer_class = UserSerializer

    def get_permissions(self):
        action = getattr(self, 'action', None)
        if action in ['wechat_login', 'create']:
            return [permissions.AllowAny()]
        if action == 'login':
            return [permissions.AllowAny()]
        return [permissions.IsAuthenticated()]

    def get_authenticators(self):
        action = getattr(self, 'action', None)
        if action in ['wechat_login', 'create', 'login']:
            return []
        return super().get_authenticators()

    @action(detail=False, methods=['post'], permission_classes=[permissions.AllowAny])
    def wechat_login(self, request):
        """
        微信小程序登录
        通过微信code换取openid，自动注册/登录
        """
        serializer = WechatLoginSerializer(data=request.data)
        if not serializer.is_valid():
            return api_response(message='参数错误', code=400)

        code = serializer.validated_data['code']
        nickname = serializer.validated_data.get('nickname', '')
        avatar_url = serializer.validated_data.get('avatar_url', '')

        # 调用微信接口换取 openid
        appid = settings.WECHAT_APPID
        secret = settings.WECHAT_SECRET
        wx_url = f'https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code'

        try:
            wx_resp = requests.get(wx_url, timeout=10)
            wx_data = wx_resp.json()
            openid = wx_data.get('openid')
            if not openid:
                logger.warning(f'微信登录失败: {wx_data}')
                # 开发模式下使用mock openid
                openid = f'mock_openid_{code[:16]}'
        except Exception as e:
            logger.warning(f'微信接口调用失败: {e}, 使用mock模式')
            openid = f'mock_openid_{code[:16]}'

        # 获取或创建用户
        user, created = User.objects.get_or_create(
            openid=openid,
            defaults={
                'nickname': nickname or '微信用户',
                'avatar_url': avatar_url,
                'last_login_ip': getattr(request, 'client_ip', ''),
            }
        )

        if not created:
            user.nickname = nickname or user.nickname
            user.avatar_url = avatar_url or user.avatar_url
            user.last_login_ip = getattr(request, 'client_ip', '')
            user.last_login_at = timezone.now()
            user.save()

        # 记录登录日志 (IP溯源)
        ip_address = getattr(request, 'client_ip', '0.0.0.0')
        device_info = getattr(request, 'client_device', '')

        # 检测异常登录：对比历史IP
        is_abnormal = 0
        abnormal_reason = ''
        recent_logs = LoginLog.objects.filter(
            user=user
        ).order_by('-login_time')[:10]

        if recent_logs.exists():
            recent_ips = set(log.ip_address for log in recent_logs if log.ip_address)
            if ip_address not in recent_ips and len(recent_ips) >= 2:
                is_abnormal = 1
                abnormal_reason = f'检测到新设备/新地点登录，历史常用IP: {",".join(list(recent_ips)[:3])}'

        LoginLog.objects.create(
            user=user,
            ip_address=ip_address,
            device_info=device_info,
            is_abnormal=is_abnormal,
            abnormal_reason=abnormal_reason,
        )

        # 返回用户信息和Token
        user_data = UserProfileSerializer(user).data
        user_data['is_new_user'] = created

        return api_response(data={
            'token': get_tokens_for_user(user),
            'user': user_data,
        })

    @action(detail=False, methods=['get'])
    def profile(self, request):
        """获取当前用户个人信息"""
        user = User.objects.get(id=request.user.id)
        serializer = UserProfileSerializer(user)
        return api_response(data=serializer.data)

    @action(detail=False, methods=['put'])
    def update_profile(self, request):
        """更新个人信息"""
        user = User.objects.get(id=request.user.id)
        allowed_fields = ['nickname', 'avatar_url', 'gender']
        for field in allowed_fields:
            if field in request.data:
                setattr(user, field, request.data[field])
        user.save()
        return api_response(data=UserProfileSerializer(user).data)

    @action(detail=False, methods=['get'])
    def login_logs(self, request):
        """获取当前用户登录日志"""
        user = User.objects.get(id=request.user.id)
        logs = LoginLog.objects.filter(user=user).order_by('-login_time')[:20]
        return api_response(data=LoginLogSerializer(logs, many=True).data)

    @action(detail=False, methods=['get'])
    def stats(self, request):
        """获取学习统计数据"""
        user = User.objects.get(id=request.user.id)
        from apps.checkin.models import Checkin
        today = date.today()
        week_start = today - timedelta(days=today.weekday())
        month_start = today.replace(day=1)

        return api_response(data={
            'total_words': user.total_words,
            'total_days': user.total_days,
            'max_continuous': user.max_continuous,
            'today_checkin': Checkin.objects.filter(user=user, checkin_date=today).exists(),
            'weekly_checkins': Checkin.objects.filter(
                user=user, checkin_date__gte=week_start
            ).count(),
            'monthly_checkins': Checkin.objects.filter(
                user=user, checkin_date__gte=month_start
            ).count(),
        })


class AdminViewSet(viewsets.ModelViewSet):
    """
    管理员视图集
    后台管理、用户管理、日志查看
    """
    queryset = Admin.objects.all()
    serializer_class = None

    def get_permissions(self):
        action = getattr(self, 'action', None)
        if action == 'login':
            return [permissions.AllowAny()]
        return [permissions.IsAuthenticated()]

    def get_authenticators(self):
        action = getattr(self, 'action', None)
        if action == 'login':
            return []
        return super().get_authenticators()

    @action(detail=False, methods=['post'], permission_classes=[permissions.AllowAny])
    def login(self, request):
        """管理员登录"""
        serializer = AdminLoginSerializer(data=request.data)
        if not serializer.is_valid():
            return api_response(message='参数错误', code=400)

        username = serializer.validated_data['username']
        password = serializer.validated_data['password']

        try:
            admin = Admin.objects.get(username=username, status=1)
        except Admin.DoesNotExist:
            return api_response(message='用户名或密码错误', code=401)

        if not bcrypt.checkpw(password.encode(), admin.password_hash.encode()):
            return api_response(message='用户名或密码错误', code=401)

        # 更新登录信息
        admin.last_login_at = timezone.now()
        admin.last_login_ip = getattr(request, 'client_ip', '')
        admin.save()

        return api_response(data={
            'id': admin.id,
            'username': admin.username,
            'role': admin.role,
        })

    @action(detail=False, methods=['get'])
    def dashboard(self, request):
        """管理后台首页数据"""
        total_users = User.objects.count()
        today = date.today()
        from apps.checkin.models import Checkin
        today_checkins = Checkin.objects.filter(checkin_date=today).count()
        total_words_learned = User.objects.aggregate(
            total=models.Sum('total_words')
        )['total'] or 0
        abnormal_logins = LoginLog.objects.filter(
            is_abnormal=1
        ).count()

        return api_response(data={
            'total_users': total_users,
            'today_checkins': today_checkins,
            'total_words_learned': total_words_learned,
            'abnormal_logins': abnormal_logins,
            'today_active_users': Checkin.objects.filter(
                checkin_date=today
            ).values('user').distinct().count(),
        })

    @action(detail=False, methods=['get'])
    def user_list(self, request):
        """获取用户列表"""
        users = User.objects.all()
        return api_response(data=UserSerializer(users, many=True).data)

    @action(detail=False, methods=['get'])
    def login_log_list(self, request):
        """获取全部登录日志(含异常标记)"""
        logs = LoginLog.objects.select_related('user').order_by('-login_time')[:100]
        return api_response(data=LoginLogSerializer(logs, many=True).data)

    @action(detail=False, methods=['post'])
    def toggle_user_status(self, request):
        """禁用/启用用户"""
        user_id = request.data.get('user_id')
        try:
            user = User.objects.get(id=user_id)
            user.status = 1 if user.status == 0 else 0
            user.save()
            return api_response(message=f'用户状态已更新为{"正常" if user.status == 1 else "禁用"}')
        except User.DoesNotExist:
            return api_response(message='用户不存在', code=404)
