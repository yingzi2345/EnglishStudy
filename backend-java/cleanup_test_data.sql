-- 清理接口冒烟测试产生的测试用户（外键级联删除其打卡/日志/学习进度）
USE english_checkin;
DELETE FROM tb_user WHERE created_at >= '2026-09-07 00:00:00' AND openid LIKE 'mock_openid_%';
SELECT (SELECT COUNT(*) FROM tb_user) AS users,
       (SELECT COUNT(*) FROM tb_checkin) AS checkins,
       (SELECT COUNT(*) FROM tb_login_log) AS login_logs,
       (SELECT COUNT(*) FROM tb_word_progress) AS word_progress,
       (SELECT COUNT(*) FROM tb_admin) AS admins;
