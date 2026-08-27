-- =====================================================
-- 英语学习打卡系统 MySQL 数据库初始化脚本
-- 基于微信小程序的英语学习打卡系统设计与实现
-- 版本: 1.0
-- 数据库: MySQL 8.0+
-- =====================================================

-- 临时放宽 SQL 模式，避免严格模式下的兼容性问题
SET SESSION sql_mode = 'NO_ENGINE_SUBSTITUTION';

-- 创建数据库
CREATE DATABASE IF NOT EXISTS english_checkin
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE english_checkin;

-- =====================================================
-- 1. 用户表 (tb_user)
-- =====================================================
DROP TABLE IF EXISTS tb_user;
CREATE TABLE tb_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    openid        VARCHAR(128)  NOT NULL UNIQUE COMMENT '微信openid',
    nickname      VARCHAR(64)   DEFAULT '微信用户' COMMENT '用户昵称',
    avatar_url    VARCHAR(512)  DEFAULT '' COMMENT '头像URL',
    gender        TINYINT       DEFAULT 0 COMMENT '性别: 0未知, 1男, 2女',
    status        TINYINT       DEFAULT 1 COMMENT '账号状态: 1正常, 0禁用',
    total_words   INT           DEFAULT 0 COMMENT '累计学习单词数',
    total_days    INT           DEFAULT 0 COMMENT '累计打卡天数',
    max_continuous INT          DEFAULT 0 COMMENT '最长连续打卡天数',
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    last_login_at DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '最近登录时间',
    last_login_ip VARCHAR(64)   DEFAULT '' COMMENT '最近登录IP',
    updated_at    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_openid (openid),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';


-- =====================================================
-- 2. 单词表 (tb_word)
-- =====================================================
DROP TABLE IF EXISTS tb_word;
CREATE TABLE tb_word (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '单词ID',
    word          VARCHAR(128)  NOT NULL COMMENT '单词原文',
    phonetic      VARCHAR(128)  DEFAULT '' COMMENT '音标',
    meaning       VARCHAR(512)  NOT NULL COMMENT '中文释义',
    example_en    VARCHAR(1024) DEFAULT '' COMMENT '英文例句',
    example_zh    VARCHAR(1024) DEFAULT '' COMMENT '例句翻译',
    audio_url     VARCHAR(512)  DEFAULT '' COMMENT '发音音频URL',
    level         INT           DEFAULT 1 COMMENT '难度等级: 1初级, 2中级, 3高级',
    category      VARCHAR(64)   DEFAULT 'CET-4' COMMENT '分类标签',
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_word (word),
    INDEX idx_level (level),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单词表';


-- =====================================================
-- 3. 打卡记录表 (tb_checkin)
-- =====================================================
DROP TABLE IF EXISTS tb_checkin;
CREATE TABLE tb_checkin (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '打卡记录ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    checkin_date    DATE         NOT NULL COMMENT '打卡日期',
    checkin_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
    word_count      INT          DEFAULT 0 COMMENT '当日学习单词数',
    study_duration  INT          DEFAULT 0 COMMENT '当日学习时长(分钟)',
    continuous_days INT          DEFAULT 0 COMMENT '截至当日连续打卡天数',
    note            VARCHAR(256) DEFAULT '' COMMENT '打卡备注',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_date (user_id, checkin_date),
    INDEX idx_user_id (user_id),
    INDEX idx_checkin_date (checkin_date),
    CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡记录表';


-- =====================================================
-- 4. 单词学习进度表 (tb_word_progress)
-- =====================================================
DROP TABLE IF EXISTS tb_word_progress;
CREATE TABLE tb_word_progress (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '进度ID',
    user_id       BIGINT   NOT NULL COMMENT '用户ID',
    word_id       BIGINT   NOT NULL COMMENT '单词ID',
    is_learned    TINYINT  DEFAULT 0 COMMENT '是否已学: 0未学, 1已学',
    is_mastered   TINYINT  DEFAULT 0 COMMENT '是否掌握: 0未掌握, 1已掌握',
    learned_at    DATETIME DEFAULT NULL COMMENT '学习时间',
    review_count  INT      DEFAULT 0 COMMENT '复习次数',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_word (user_id, word_id),
    INDEX idx_user_id (user_id),
    INDEX idx_word_id (word_id),
    CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_word FOREIGN KEY (word_id) REFERENCES tb_word(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单词学习进度表';


-- =====================================================
-- 5. 登录日志表 (tb_login_log) — 网络工程专业特色
-- =====================================================
DROP TABLE IF EXISTS tb_login_log;
CREATE TABLE tb_login_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    ip_address    VARCHAR(64)  DEFAULT '' COMMENT '登录IP地址',
    device_info   VARCHAR(256) DEFAULT '' COMMENT '设备信息',
    location      VARCHAR(128) DEFAULT '' COMMENT 'IP归属地',
    login_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    is_abnormal   TINYINT      DEFAULT 0 COMMENT '是否异常登录: 0正常, 1异常',
    abnormal_reason VARCHAR(256) DEFAULT '' COMMENT '异常原因',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time),
    INDEX idx_is_abnormal (is_abnormal),
    CONSTRAINT fk_loginlog_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表(IP溯源)';


-- =====================================================
-- 6. 管理员表 (tb_admin)
-- =====================================================
DROP TABLE IF EXISTS tb_admin;
CREATE TABLE tb_admin (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '管理员ID',
    username        VARCHAR(64)  NOT NULL UNIQUE COMMENT '管理员用户名',
    password_hash   VARCHAR(256) NOT NULL COMMENT '密码哈希(BCrypt)',
    role            VARCHAR(32)  DEFAULT 'admin' COMMENT '角色: admin/superadmin',
    nickname        VARCHAR(64)  DEFAULT '' COMMENT '昵称',
    status          TINYINT      DEFAULT 1 COMMENT '状态: 1正常, 0禁用',
    last_login_at   DATETIME     DEFAULT NULL COMMENT '最近登录时间',
    last_login_ip   VARCHAR(64)  DEFAULT '' COMMENT '最近登录IP',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';


-- =====================================================
-- 7. 系统配置表 (tb_system_config)
-- =====================================================
DROP TABLE IF EXISTS tb_system_config;
CREATE TABLE tb_system_config (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    config_key    VARCHAR(128) NOT NULL UNIQUE COMMENT '配置键',
    config_value  TEXT         DEFAULT '' COMMENT '配置值',
    description   VARCHAR(256) DEFAULT '' COMMENT '配置说明',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';


-- =====================================================
-- 初始数据插入
-- =====================================================

-- 插入默认管理员 (密码: admin123, BCrypt加密)
INSERT INTO tb_admin (username, password_hash, role, nickname) VALUES
('admin', '$2b$12$LJ3m4ys3u0YqG6sODBFV.O7VJqEYPp5H5KqGQHkVUOY5LpLqjqDWO', 'superadmin', '系统管理员');

-- 插入示例单词数据 (CET-4词汇)
INSERT INTO tb_word (word, phonetic, meaning, example_en, example_zh, level, category) VALUES
('abandon', '/əˈbændən/', 'v. 放弃；抛弃', 'He decided to abandon his plan.', '他决定放弃他的计划。', 1, 'CET-4'),
('ability', '/əˈbɪləti/', 'n. 能力；才能', 'She has the ability to learn quickly.', '她有快速学习的能力。', 1, 'CET-4'),
('absent', '/ˈæbsənt/', 'adj. 缺席的；不在的', 'He was absent from school yesterday.', '他昨天没来上学。', 1, 'CET-4'),
('absorb', '/əbˈzɔːrb/', 'v. 吸收；吸引', 'Plants absorb water from the soil.', '植物从土壤中吸收水分。', 1, 'CET-4'),
('abstract', '/ˈæbstrækt/', 'adj. 抽象的；n. 摘要', 'The concept is too abstract to understand.', '这个概念太抽象了，难以理解。', 1, 'CET-4'),
('abundant', '/əˈbʌndənt/', 'adj. 丰富的；充裕的', 'The region has abundant natural resources.', '该地区拥有丰富的自然资源。', 1, 'CET-4'),
('academy', '/əˈkædəmi/', 'n. 学院；研究院', 'He graduated from a military academy.', '他毕业于一所军事学院。', 1, 'CET-4'),
('accelerate', '/əkˈseləreɪt/', 'v. 加速；促进', 'The car accelerated to overtake the truck.', '汽车加速超过了卡车。', 1, 'CET-4'),
('access', '/ˈækses/', 'n. 进入；通道；v. 访问', 'You need a password to access the system.', '你需要密码才能访问系统。', 1, 'CET-4'),
('accommodate', '/əˈkɒmədeɪt/', 'v. 容纳；提供住宿', 'The hotel can accommodate 500 guests.', '这家酒店可以容纳500位客人。', 2, 'CET-4'),
('accompany', '/əˈkʌmpəni/', 'v. 陪伴；伴随', 'She accompanied her friend to the hospital.', '她陪朋友去了医院。', 2, 'CET-4'),
('accomplish', '/əˈkʌmplɪʃ/', 'v. 完成；实现', 'We have accomplished our mission.', '我们已经完成了任务。', 2, 'CET-4'),
('accurate', '/ˈækjərət/', 'adj. 精确的；准确的', 'The data must be accurate and reliable.', '数据必须准确可靠。', 1, 'CET-4'),
('achieve', '/əˈtʃiːv/', 'v. 达到；实现', 'She worked hard to achieve her goals.', '她努力奋斗以实现她的目标。', 1, 'CET-4'),
('acknowledge', '/əkˈnɒlɪdʒ/', 'v. 承认；确认', 'He acknowledged his mistake publicly.', '他公开承认了自己的错误。', 2, 'CET-4'),
('acquire', '/əˈkwaɪər/', 'v. 获得；习得', 'It takes time to acquire a new skill.', '掌握一项新技能需要时间。', 2, 'CET-4'),
('adapt', '/əˈdæpt/', 'v. 适应；改编', 'We must adapt to the changing environment.', '我们必须适应不断变化的环境。', 1, 'CET-4'),
('adequate', '/ˈædɪkwət/', 'adj. 足够的；适当的', 'The supply is not adequate for the demand.', '供应不足以满足需求。', 2, 'CET-4'),
('adjust', '/əˈdʒʌst/', 'v. 调整；适应', 'Please adjust the volume to a comfortable level.', '请将音量调整到舒适的水平。', 1, 'CET-4'),
('administration', '/ədˌmɪnɪˈstreɪʃn/', 'n. 管理；行政', 'He works in business administration.', '他从事工商管理工作。', 2, 'CET-4'),
('admire', '/ədˈmaɪər/', 'v. 钦佩；赞赏', 'I really admire her courage and determination.', '我非常钦佩她的勇气和决心。', 1, 'CET-4'),
('adopt', '/əˈdɒpt/', 'v. 采用；收养', 'The company decided to adopt a new strategy.', '公司决定采用新策略。', 1, 'CET-4'),
('advance', '/ədˈvɑːns/', 'v. 前进；进展；n. 进步', 'Technology continues to advance rapidly.', '科技继续快速发展。', 1, 'CET-4'),
('adventure', '/ədˈventʃər/', 'n. 冒险；奇遇', 'The journey was a great adventure.', '这次旅程是一次伟大的冒险。', 1, 'CET-4'),
('affair', '/əˈfeər/', 'n. 事务；事件', 'This is a matter of national affairs.', '这是国家大事。', 2, 'CET-4'),
('aggressive', '/əˈɡresɪv/', 'adj. 侵略的；好斗的', 'The company adopted an aggressive marketing strategy.', '公司采取了激进的营销策略。', 2, 'CET-4'),
('allocate', '/ˈæləkeɪt/', 'v. 分配；拨出', 'The government allocated funds for education.', '政府为教育拨款。', 2, 'CET-4'),
('alternative', '/ɔːlˈtɜːrnətɪv/', 'n. 替代品；adj. 替代的', 'We need to find an alternative solution.', '我们需要找到替代方案。', 1, 'CET-4'),
('amaze', '/əˈmeɪz/', 'v. 使惊奇；使惊愕', 'Her performance amazed the audience.', '她的表演让观众惊叹不已。', 1, 'CET-4'),
('analyze', '/ˈænəlaɪz/', 'v. 分析；解析', 'We need to analyze the problem carefully.', '我们需要仔细分析这个问题。', 1, 'CET-4'),
('anniversary', '/ˌænɪˈvɜːrsəri/', 'n. 周年纪念日', 'Today is our wedding anniversary.', '今天是我们的结婚纪念日。', 2, 'CET-4'),
('anticipate', '/ænˈtɪsɪpeɪt/', 'v. 预期；期望', 'We anticipate a successful outcome.', '我们预期会有一个成功的结果。', 2, 'CET-4'),
('anxiety', '/æŋˈzaɪəti/', 'n. 焦虑；忧虑', 'The exam caused her great anxiety.', '考试让她非常焦虑。', 2, 'CET-4'),
('apparent', '/əˈpærənt/', 'adj. 明显的；表面上的', 'The reason for his anger was apparent.', '他生气的原因很明显。', 1, 'CET-4'),
('appetite', '/ˈæpɪtaɪt/', 'n. 食欲；胃口', 'The exercise gave me a good appetite.', '运动让我胃口大开。', 2, 'CET-4'),
('appreciate', '/əˈpriːʃieɪt/', 'v. 欣赏；感激', 'I really appreciate your help.', '我真的很感激你的帮助。', 1, 'CET-4'),
('approach', '/əˈprəʊtʃ/', 'v. 接近；n. 方法', 'We need a new approach to solve this problem.', '我们需要新方法来解决这个问题。', 1, 'CET-4'),
('appropriate', '/əˈprəʊpriət/', 'adj. 适当的；合适的', 'Please wear appropriate clothing for the interview.', '请穿合适的衣服参加面试。', 1, 'CET-4'),
('approve', '/əˈpruːv/', 'v. 批准；赞同', 'The committee approved the proposal.', '委员会批准了这项提议。', 1, 'CET-4'),
('arise', '/əˈraɪz/', 'v. 出现；产生', 'New problems may arise during the project.', '项目过程中可能会出现新问题。', 2, 'CET-4'),
('artificial', '/ˌɑːrtɪˈfɪʃl/', 'adj. 人工的；人造的', 'Artificial intelligence is changing our lives.', '人工智能正在改变我们的生活。', 1, 'CET-4'),
('aspect', '/ˈæspekt/', 'n. 方面；层面', 'We should consider every aspect of the issue.', '我们应该考虑问题的每个方面。', 1, 'CET-4'),
('assemble', '/əˈsembl/', 'v. 组装；集合', 'The students assembled in the hall.', '学生们在大厅集合。', 2, 'CET-4'),
('assess', '/əˈses/', 'v. 评估；评定', 'We need to assess the risks involved.', '我们需要评估所涉及的风险。', 2, 'CET-4'),
('assign', '/əˈsaɪn/', 'v. 分配；指派', 'The teacher assigned homework to the students.', '老师给学生布置了作业。', 1, 'CET-4'),
('assist', '/əˈsɪst/', 'v. 帮助；协助', 'She assisted the doctor during the surgery.', '她在手术中协助医生。', 1, 'CET-4'),
('associate', '/əˈsəʊʃieɪt/', 'v. 联系；关联', 'People often associate success with happiness.', '人们常常把成功和幸福联系在一起。', 1, 'CET-4'),
('assume', '/əˈsjuːm/', 'v. 假设；承担', 'Let us assume the theory is correct.', '让我们假设这个理论是正确的。', 1, 'CET-4'),
('atmosphere', '/ˈætməsfɪər/', 'n. 气氛；大气层', 'The restaurant has a romantic atmosphere.', '这家餐厅有一种浪漫的氛围。', 1, 'CET-4'),
('attach', '/əˈtætʃ/', 'v. 附上；连接', 'Please attach your resume to the email.', '请将简历附在邮件中。', 1, 'CET-4');

-- 插入CET-6词汇
INSERT INTO tb_word (word, phonetic, meaning, example_en, example_zh, level, category) VALUES
('abolish', '/əˈbɒlɪʃ/', 'v. 废除；取消', 'The government plans to abolish the outdated law.', '政府计划废除这项过时的法律。', 2, 'CET-6'),
('absurd', '/əbˈsɜːrd/', 'adj. 荒谬的；荒唐的', 'It is absurd to believe such a ridiculous story.', '相信如此荒唐的故事是荒谬的。', 2, 'CET-6'),
('abuse', '/əˈbjuːz/', 'v./n. 滥用；虐待', 'We should not abuse our power and authority.', '我们不应该滥用权力。', 2, 'CET-6'),
('acquaint', '/əˈkweɪnt/', 'v. 使熟悉；使了解', 'You should acquaint yourself with the new rules.', '你应该熟悉一下新规则。', 2, 'CET-6'),
('adhere', '/ədˈhɪər/', 'v. 坚持；遵守；黏附', 'We must adhere to the principles of fairness.', '我们必须坚持公平原则。', 2, 'CET-6'),
('adjacent', '/əˈdʒeɪsnt/', 'adj. 邻近的；毗连的', 'The hotel is adjacent to the railway station.', '酒店毗邻火车站。', 2, 'CET-6'),
('administer', '/ədˈmɪnɪstər/', 'v. 管理；执行', 'He was appointed to administer the department.', '他被任命管理这个部门。', 2, 'CET-6'),
('adolescent', '/ˌædəˈlesnt/', 'n. 青少年；adj. 青春期的', 'Adolescents often face peer pressure at school.', '青少年在学校经常面临同伴压力。', 2, 'CET-6'),
('agony', '/ˈæɡəni/', 'n. 极大的痛苦', 'She went through the agony of losing her family.', '她经历了失去家人的巨大痛苦。', 2, 'CET-6'),
('alliance', '/əˈlaɪəns/', 'n. 联盟；联合', 'The two companies formed a strategic alliance.', '两家公司结成了战略联盟。', 2, 'CET-6'),
('ambassador', '/æmˈbæsədər/', 'n. 大使；代表', 'He was appointed as the ambassador to France.', '他被任命为驻法国大使。', 2, 'CET-6'),
('amend', '/əˈmend/', 'v. 修改；修订', 'The constitution was amended to protect human rights.', '宪法被修订以保护人权。', 2, 'CET-6'),
('ample', '/ˈæmpl/', 'adj. 充足的；宽敞的', 'There is ample evidence to support this theory.', '有充足的证据支持这一理论。', 2, 'CET-6'),
('analogy', '/əˈnælədʒi/', 'n. 类比；比喻', 'The teacher used an analogy to explain the concept.', '老师用了一个类比来解释这个概念。', 2, 'CET-6'),
('anonymous', '/əˈnɒnɪməs/', 'adj. 匿名的；无名的', 'The donation was made by an anonymous benefactor.', '这笔捐款来自一位匿名捐助者。', 2, 'CET-6'),
('apparatus', '/ˌæpəˈreɪtəs/', 'n. 器械；设备；机构', 'The laboratory is equipped with modern apparatus.', '实验室配备了现代化设备。', 2, 'CET-6'),
('applaud', '/əˈplɔːd/', 'v. 鼓掌；称赞', 'The audience applauded the performers enthusiastically.', '观众热情地为表演者鼓掌。', 2, 'CET-6'),
('apt', '/æpt/', 'adj. 恰当的；易于…的', 'His remarks were apt and well-timed.', '他的评论恰当而及时。', 2, 'CET-6'),
('arrogant', '/ˈærəɡənt/', 'adj. 傲慢的；自大的', 'His arrogant attitude made him unpopular among colleagues.', '他傲慢的态度让他在同事中不受欢迎。', 2, 'CET-6'),
('articulate', '/ɑːrˈtɪkjuleɪt/', 'v. 清晰表达；adj. 善于表达的', 'She is able to articulate her ideas clearly.', '她能够清晰地表达自己的想法。', 3, 'CET-6'),
('ascend', '/əˈsend/', 'v. 上升；攀登', 'The climbers slowly ascended the steep mountain.', '登山者缓缓攀登陡峭的山峰。', 2, 'CET-6'),
('assault', '/əˈsɔːlt/', 'n./v. 攻击；袭击', 'The troops launched an assault on the enemy base.', '部队对敌军基地发起了攻击。', 2, 'CET-6'),
('assert', '/əˈsɜːrt/', 'v. 断言；主张；维护', 'He continued to assert his innocence in court.', '他在法庭上继续坚称自己无罪。', 2, 'CET-6'),
('assimilate', '/əˈsɪmɪleɪt/', 'v. 吸收；同化', 'New immigrants take time to assimilate into society.', '新移民需要时间融入社会。', 3, 'CET-6'),
('attorney', '/əˈtɜːrni/', 'n. 律师；代理人', 'The defendant was represented by a skilled attorney.', '被告由一位经验丰富的律师代理。', 2, 'CET-6'),
('authentic', '/ɔːˈθentɪk/', 'adj. 真实的；可靠的', 'We need to verify whether the document is authentic.', '我们需要核实这份文件是否真实。', 2, 'CET-6'),
('authorize', '/ˈɔːθəraɪz/', 'v. 授权；批准', 'Only the manager can authorize this transaction.', '只有经理才能批准这笔交易。', 2, 'CET-6'),
('avail', '/əˈveɪl/', 'v. 利用；n. 效用', 'All his efforts were of no avail in the end.', '他所有的努力最终都白费了。', 3, 'CET-6'),
('bankrupt', '/ˈbæŋkrʌpt/', 'adj. 破产的；v. 使破产', 'The company went bankrupt due to poor management.', '公司因管理不善而破产了。', 2, 'CET-6'),
('beforehand', '/bɪˈfɔːrhænd/', 'adv. 预先；提前', 'You should let me know beforehand if you cannot come.', '如果你不能来，应该提前告诉我。', 2, 'CET-6'),
('bewilder', '/bɪˈwɪldər/', 'v. 使困惑；使不知所措', 'The complex instructions bewildered the new employees.', '复杂的说明让新员工不知所措。', 2, 'CET-6'),
('blunder', '/ˈblʌndər/', 'n. 大错；v. 犯大错', 'He made a blunder by deleting the important file.', '他犯了大错，删除了重要文件。', 2, 'CET-6'),
('boycott', '/ˈbɔɪkɒt/', 'v./n. 联合抵制', 'Many consumers decided to boycott the company products.', '许多消费者决定抵制该公司的产品。', 2, 'CET-6'),
('breach', '/briːtʃ/', 'n. 违反；破坏；v. 打破', 'This action is a breach of our agreement.', '这一行为违反了我们之间的协议。', 3, 'CET-6'),
('brisk', '/brɪsk/', 'adj. 轻快的；兴隆的', 'They went for a brisk walk in the morning park.', '他们早晨在公园里快步散步。', 2, 'CET-6'),
('capsule', '/ˈkæpsjuːl/', 'n. 胶囊；太空舱', 'The doctor prescribed some capsules for the infection.', '医生开了一些胶囊治疗感染。', 2, 'CET-6'),
('casualty', '/ˈkæʒuəlti/', 'n. 伤亡人员；受害者', 'The earthquake caused thousands of casualties.', '地震造成了成千上万的伤亡。', 2, 'CET-6'),
('certify', '/ˈsɜːrtɪfaɪ/', 'v. 证明；认证', 'The accountant needs to certify the financial statements.', '会计师需要审核财务报表。', 2, 'CET-6'),
('chronic', '/ˈkrɒnɪk/', 'adj. 慢性的；长期的', 'He suffers from chronic back pain for many years.', '他患有慢性背痛多年。', 2, 'CET-6'),
('circulation', '/ˌsɜːrkjəˈleɪʃn/', 'n. 循环；流通；发行量', 'Good blood circulation is vital for your health.', '良好的血液循环对健康至关重要。', 2, 'CET-6'),
('cling', '/klɪŋ/', 'v. 紧抓；坚持；依附', 'The child clung tightly to his mother hand.', '孩子紧紧抓住妈妈的手。', 2, 'CET-6'),
('coincide', '/ˌkəʊɪnˈsaɪd/', 'v. 同时发生；相符', 'His views coincide with mine on this matter.', '在这个问题上，他的观点与我一致。', 3, 'CET-6'),
('collaborate', '/kəˈlæbəreɪt/', 'v. 合作；协作', 'Scientists from different countries collaborate on the project.', '来自不同国家的科学家在该项目上合作。', 2, 'CET-6'),
('commence', '/kəˈmens/', 'v. 开始；着手', 'The ceremony will commence at 10 o\'clock sharp.', '仪式将于十点整开始。', 2, 'CET-6'),
('commend', '/kəˈmend/', 'v. 赞扬；推荐', 'The teacher commended him for his excellent performance.', '老师赞扬了他出色的表现。', 2, 'CET-6'),
('commodity', '/kəˈmɒdəti/', 'n. 商品；日用品', 'Oil is one of the most traded commodities in the world.', '石油是世界上交易量最大的商品之一。', 2, 'CET-6'),
('commonplace', '/ˈkɒmənpleɪs/', 'adj. 平凡的；普通的', 'Smartphones have become commonplace in modern life.', '智能手机在现代生活中已经变得很普遍。', 2, 'CET-6'),
('compatible', '/kəmˈpætəbl/', 'adj. 兼容的；合得来的', 'This software is not compatible with the old system.', '这个软件与旧系统不兼容。', 2, 'CET-6'),
('compensate', '/ˈkɒmpenseɪt/', 'v. 补偿；弥补', 'The company compensated the workers for their overtime.', '公司为员工加班支付了补偿。', 2, 'CET-6'),
('competence', '/ˈkɒmpɪtəns/', 'n. 能力；胜任', 'He demonstrated great competence in solving complex problems.', '他在解决复杂问题方面表现出很强能力。', 2, 'CET-6');

-- 插入系统配置
INSERT INTO tb_system_config (config_key, config_value, description) VALUES
('checkin_reward_words', '10', '每次打卡奖励学习单词数'),
('max_daily_words', '50', '每日最大学习单词数'),
('login_abnormal_threshold', '3', '异常登录阈值(次数)'),
('app_version', '1.0.0', '系统版本号');
