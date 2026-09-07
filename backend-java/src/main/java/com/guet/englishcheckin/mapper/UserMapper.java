package com.guet.englishcheckin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guet.englishcheckin.entity.User;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {

    /** 累计学习单词总数（SUM(total_words)） */
    @Select("SELECT COALESCE(SUM(total_words), 0) FROM tb_user")
    Long selectSumTotalWords();
}
