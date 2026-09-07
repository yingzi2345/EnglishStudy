"""
自定义 JWT 认证后端
适配自定义 User 模型（tb_user），从 JWT Token 中解析 user_id 并查找用户
"""
from django.contrib.auth.models import AnonymousUser
from rest_framework import authentication, exceptions
from rest_framework_simplejwt.tokens import AccessToken
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError
from apps.users.models import User


class CustomJWTAuthentication(authentication.BaseAuthentication):
    """
    自定义 JWT 认证类
    从 Token 中提取 user_id，在 tb_user 表中查找匹配用户
    """

    def authenticate(self, request):
        auth_header = authentication.get_authorization_header(request).decode('utf-8', errors='ignore')

        if not auth_header or not auth_header.startswith('Bearer '):
            return None

        token_str = auth_header.split('Bearer ')[-1].strip()
        if not token_str:
            return None

        try:
            access_token = AccessToken(token_str)
            user_id = access_token.get('user_id')
            if not user_id:
                raise InvalidToken('Token 中缺少 user_id')

            user = User.objects.filter(id=user_id, status=1).first()
            if not user:
                raise InvalidToken('用户不存在或已被禁用')

        except (InvalidToken, TokenError) as e:
            raise exceptions.AuthenticationFailed(str(e))

        return (user, access_token)

    def authenticate_header(self, request):
        return 'Bearer realm="api"'
