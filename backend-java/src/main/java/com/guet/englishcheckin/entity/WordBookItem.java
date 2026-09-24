package com.guet.englishcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 词书-单词 关联（tb_word_book_item）
 */
@Data
@TableName("tb_word_book_item")
public class WordBookItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookId;

    private Long wordId;

    private LocalDateTime createdAt;
}
