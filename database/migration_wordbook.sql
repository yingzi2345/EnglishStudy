-- =====================================================
-- 词书库 + 自定义词本 数据库迁移
-- 设计：官方词书与用户自定义词本统一为 tb_word_book（book_type 区分），
--       自定义单词写入 tb_word（source='custom', owner_id=用户），
--       学习进度仍走 tb_word_progress，学习链路完全统一，天然支持多词书并行。
-- 日期：2026-09-22
-- =====================================================

SET SESSION sql_mode = 'NO_ENGINE_SUBSTITUTION';

-- ─────────────────────────────────────────────
-- 1. tb_word 增加来源与归属字段，并给 word 加唯一索引（用于 INSERT IGNORE 去重）
-- ─────────────────────────────────────────────
ALTER TABLE tb_word
    ADD COLUMN source   VARCHAR(10) DEFAULT 'system' COMMENT '来源: system系统词, custom用户自定义词' AFTER category,
    ADD COLUMN owner_id BIGINT DEFAULT NULL COMMENT '自定义词归属用户ID(系统词为NULL)' AFTER source;

-- 现有普通索引 idx_word 改为唯一索引（已确认 100 词无重复）
ALTER TABLE tb_word DROP INDEX idx_word;
ALTER TABLE tb_word ADD UNIQUE KEY uk_word (word);
ALTER TABLE tb_word ADD INDEX idx_source_owner (source, owner_id);

-- ─────────────────────────────────────────────
-- 2. 词书表（官方词书 + 用户自定义词本）
-- ─────────────────────────────────────────────
DROP TABLE IF EXISTS tb_word_book;
CREATE TABLE tb_word_book (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '词书ID',
    code        VARCHAR(32) DEFAULT NULL COMMENT '官方词书编码(自定义词本为NULL)',
    name        VARCHAR(64) NOT NULL COMMENT '词书名称',
    description VARCHAR(255) DEFAULT '' COMMENT '词书简介',
    icon        VARCHAR(16) DEFAULT '📘' COMMENT '图标(emoji)',
    book_group  VARCHAR(16) DEFAULT '通用' COMMENT '分组: 中小学/大学/应试/通用/自定义',
    book_type   VARCHAR(10) DEFAULT 'system' COMMENT '类型: system官方, custom自定义',
    owner_id    BIGINT DEFAULT NULL COMMENT '自定义词本归属用户ID(官方为NULL)',
    sort_order  INT DEFAULT 0 COMMENT '排序',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    INDEX idx_type_owner (book_type, owner_id),
    INDEX idx_group_sort (book_group, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='词书表';

-- ─────────────────────────────────────────────
-- 3. 词书-单词 关联表
-- ─────────────────────────────────────────────
DROP TABLE IF EXISTS tb_word_book_item;
CREATE TABLE tb_word_book_item (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    book_id BIGINT NOT NULL COMMENT '词书ID',
    word_id BIGINT NOT NULL COMMENT '单词ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_book_word (book_id, word_id),
    INDEX idx_word (word_id),
    CONSTRAINT fk_item_book FOREIGN KEY (book_id) REFERENCES tb_word_book(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_word FOREIGN KEY (word_id) REFERENCES tb_word(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='词书单词关联表';

-- ─────────────────────────────────────────────
-- 4. 用户-词书 关系表（加入的词书 + 当前学习词书）
-- ─────────────────────────────────────────────
DROP TABLE IF EXISTS tb_user_book;
CREATE TABLE tb_user_book (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    book_id     BIGINT NOT NULL COMMENT '词书ID',
    is_current  TINYINT DEFAULT 0 COMMENT '是否当前学习词书: 0否, 1是',
    added_at    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY uk_user_book (user_id, book_id),
    INDEX idx_user_current (user_id, is_current),
    CONSTRAINT fk_ub_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_ub_book FOREIGN KEY (book_id) REFERENCES tb_word_book(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户词书关系表';
