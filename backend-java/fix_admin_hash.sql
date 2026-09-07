-- 修复 tb_admin 种子密码哈希：原哈希与 admin123 不匹配（系统隐藏 bug）
-- 新哈希由 Python bcrypt.hashpw('admin123', gensalt(rounds=12)) 生成并已验证
USE english_checkin;
UPDATE tb_admin
SET password_hash = '$2b$12$.Gajd7cFKsmqVkHNTCbS9ObK9Nkrnkgzbi5Ui5.pfNX5HcJJ7hLa2'
WHERE username = 'admin';
SELECT username, LEFT(password_hash, 15) AS hash_prefix, role FROM tb_admin;
