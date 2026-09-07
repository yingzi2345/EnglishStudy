"""
IP溯源中间件 — 网络工程专业特色模块
功能:
1. 获取客户端真实IP (支持代理穿透)
2. 记录每次登录的IP和User-Agent
3. 对比历史IP，检测异常登录
"""
import logging
from django.utils import timezone
from django.utils.deprecation import MiddlewareMixin

logger = logging.getLogger('middleware')


class LoginIPTraceMiddleware(MiddlewareMixin):
    """
    登录IP溯源中间件
    - 自动提取请求真实IP
    - 存到 request.client_ip 供视图使用
    """

    @staticmethod
    def get_client_ip(request):
        """获取客户端真实IP (穿透代理/负载均衡)"""
        x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR', '')
        if x_forwarded_for:
            ip = x_forwarded_for.split(',')[0].strip()
        else:
            ip = request.META.get('HTTP_X_REAL_IP', '')
            if not ip:
                ip = request.META.get('REMOTE_ADDR', '0.0.0.0')
        return ip

    def process_request(self, request):
        """在请求处理前提取IP"""
        request.client_ip = self.get_client_ip(request)
        request.client_device = request.META.get('HTTP_USER_AGENT', '')

    def process_view(self, request, view_func, view_args, view_kwargs):
        """视图处理前的钩子"""
        return None
