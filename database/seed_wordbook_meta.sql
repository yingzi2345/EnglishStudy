-- =====================================================
-- 词书元数据（9 本官方词书）
-- =====================================================
SET SESSION sql_mode = 'NO_ENGINE_SUBSTITUTION';

INSERT INTO tb_word_book (code, name, description, icon, book_group, book_type, owner_id, sort_order) VALUES
('ZHONGKAO', '中考词汇', '初中英语中考核心高频词，夯实基础', '📗', '中小学', 'system', NULL, 1),
('GAOKAO',   '高考词汇', '高考英语核心词汇，覆盖全国卷考纲', '📘', '中小学', 'system', NULL, 2),
('CET4',     '四级词汇', '大学英语四级(CET-4)高频核心词', '📙', '大学',   'system', NULL, 3),
('CET6',     '六级词汇', '大学英语六级(CET-6)高频核心词', '📕', '大学',   'system', NULL, 4),
('KAOYAN1',  '考研英语一', '考研英语(一)大纲核心词汇，难度较高', '📓', '大学', 'system', NULL, 5),
('KAOYAN2',  '考研英语二', '考研英语(二)大纲核心词汇，侧重应用', '📔', '大学', 'system', NULL, 6),
('IELTS',    '雅思词汇', 'IELTS 雅思考试听说读写高频词', '🌍', '应试',   'system', NULL, 7),
('TOEFL',    '托福词汇', 'TOEFL 托福考试学术场景核心词', '🎓', '应试',   'system', NULL, 8),
('DAILY',    '日常高频词', '生活交际常用高频词，口语写作两用', '💬', '通用',   'system', NULL, 9);

-- 现有 100 词按 category 关联进对应词书
INSERT IGNORE INTO tb_word_book_item (book_id, word_id)
SELECT b.id, w.id FROM tb_word w JOIN tb_word_book b
  ON b.code = 'CET4' AND w.category = 'CET-4';

INSERT IGNORE INTO tb_word_book_item (book_id, word_id)
SELECT b.id, w.id FROM tb_word w JOIN tb_word_book b
  ON b.code = 'CET6' AND w.category = 'CET-6';
