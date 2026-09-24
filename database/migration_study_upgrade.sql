-- 学习流程升级：收藏单词 + 3档难度
-- 1. tb_word_progress 增加收藏标记
ALTER TABLE tb_word_progress
    ADD COLUMN is_favorite TINYINT DEFAULT 0 COMMENT '是否收藏: 0否,1是' AFTER is_wrong;

CREATE INDEX idx_user_favorite ON tb_word_progress (user_id, is_favorite);

-- 2. tb_user 增加学习模式偏好：en2zh=看英文回忆中文(默认), zh2en=看中文回忆英文
ALTER TABLE tb_user
    ADD COLUMN study_mode VARCHAR(10) DEFAULT 'en2zh' COMMENT '学习模式: en2zh看英忆中, zh2en看中忆英' AFTER daily_goal;
