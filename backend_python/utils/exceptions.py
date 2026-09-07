"""
工具模块 — 自定义异常处理
"""
import logging
from rest_framework.views import exception_handler
from rest_framework.response import Response
from rest_framework import status

logger = logging.getLogger('django')


def custom_exception_handler(exc, context):
    """自定义DRF异常处理器，统一返回格式"""
    response = exception_handler(exc, context)

    if response is not None:
        custom_response = {
            'code': response.status_code,
            'message': str(exc.detail) if hasattr(exc, 'detail') else '服务器内部错误',
            'data': None,
        }
        response.data = custom_response
    else:
        logger.error(f'Unhandled exception: {exc}', exc_info=True)
        return Response({
            'code': 500,
            'message': '服务器内部错误',
            'data': None,
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

    return response


def api_response(data=None, message='success', code=200):
    """统一成功响应格式"""
    return Response({
        'code': code,
        'message': message,
        'data': data,
    }, status=code if code < 400 else status.HTTP_200_OK)
