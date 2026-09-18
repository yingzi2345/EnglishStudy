-- 成就徽章系统
CREATE TABLE IF NOT EXISTS tb_achievement (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64)  NOT NULL COMMENT '徽章名称',
    description     VARCHAR(255) NOT NULL COMMENT '徽章描述',
    icon            VARCHAR(32)  DEFAULT '🏅' COMMENT '徽章图标（emoji）',
    condition_type  VARCHAR(32)  NOT NULL COMMENT '条件类型: continuous_days/total_words/total_days',
    condition_value INT          NOT NULL COMMENT '条件阈值',
    sort_order      INT          DEFAULT 0 COMMENT '排序',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成就徽章定义';

CREATE TABLE IF NOT EXISTS tb_user_achievement (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    achievement_id BIGINT NOT NULL,
    unlocked_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_achievement (user_id, achievement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户已解锁徽章';

-- 预置徽章
INSERT INTO tb_achievement (name, description, icon, condition_type, condition_value, sort_order) VALUES
('初出茅庐', '累计学习 10 个单词', '🌱', 'total_words', 10, 1),
('小有所成', '累计学习 50 个单词', '📗', 'total_words', 50, 2),
('学霸之路', '累计学习 200 个单词', '📚', 'total_words', 200, 3),
('词汇大师', '累计学习 500 个单词', '🎓', 'total_words', 500, 4),
('坚持三天', '连续打卡 3 天', '🔥', 'continuous_days', 3, 5),
('一周达人', '连续打卡 7 天', '⚡', 'continuous_days', 7, 6),
('月度之星', '连续打卡 30 天', '🌟', 'continuous_days', 30, 7),
('百日筑基', '累计打卡 10 天', '📅', 'total_days', 10, 8),
('持之以恒', '累计打卡 50 天', '🏆', 'total_days', 50, 9);
