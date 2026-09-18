-- 学习系统扩展：为 tb_word_progress 添加复习/错词字段
-- 艾宾浩斯间隔重复 + 错词本

ALTER TABLE tb_word_progress
    ADD COLUMN next_review_at  DATETIME DEFAULT NULL COMMENT '下次复习时间（UTC）' AFTER review_count,
    ADD COLUMN interval_level  INT      DEFAULT 0    COMMENT '艾宾浩斯间隔等级: 0=1天,1=2天,2=4天,3=7天,4=15天,5=30天(已掌握)' AFTER next_review_at,
    ADD COLUMN wrong_count     INT      DEFAULT 0    COMMENT '答错次数' AFTER interval_level,
    ADD COLUMN is_wrong        TINYINT  DEFAULT 0    COMMENT '是否在错词本: 0否,1是' AFTER wrong_count,
    ADD COLUMN last_study_at   DATETIME DEFAULT NULL COMMENT '上次学习时间（UTC）' AFTER is_wrong;

-- 索引：加速今日待复习查询
CREATE INDEX idx_user_next_review ON tb_word_progress (user_id, next_review_at);
CREATE INDEX idx_user_wrong ON tb_word_progress (user_id, is_wrong);
