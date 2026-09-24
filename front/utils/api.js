// ──────────────────────────────────────────────
// API 请求工具
// utils/api.js
// ──────────────────────────────────────────────

const app = getApp();

const request = (url, method = 'GET', data = {}, needAuth = true) => {
  return new Promise((resolve, reject) => {
    const token = app.getToken();
    const header = {
      'Content-Type': 'application/json',
    };
    if (needAuth && token) {
      header['Authorization'] = `Bearer ${token}`;
    }

    wx.request({
      url: `${app.globalData.baseUrl}${url}`,
      method,
      data,
      header,
      timeout: 10000,  // 10秒超时，避免默认60秒长时间等待
      success(res) {
        if (res.statusCode === 200) {
          resolve(res.data);
        } else if (res.statusCode === 401) {
          // token过期，跳转登录
          app.clearLoginInfo();
          wx.reLaunch({ url: '/pages/login/login' });
          reject(new Error('登录已过期'));
        } else {
          reject(res.data);
        }
      },
      fail(err) {
        // 超时或网络不通时给出友好提示
        if (err.errMsg && err.errMsg.includes('timeout')) {
          wx.showToast({ title: '请求超时，请检查后端服务是否启动', icon: 'none', duration: 2500 });
        } else {
          wx.showToast({ title: '网络请求失败', icon: 'none' });
        }
        reject(err);
      },
    });
  });
};

// ──── 用户相关 ────
const userApi = {
  wechatLogin: (code, nickname, avatarUrl) =>
    request('/auth/wechat-login/', 'POST', { code, nickname, avatar_url: avatarUrl }, false),

  getProfile: () => request('/users/profile/', 'GET'),

  updateProfile: (data) => request('/users/update_profile/', 'PUT', data),

  getStats: () => request('/users/stats/', 'GET'),

  getLoginLogs: () => request('/users/login_logs/', 'GET'),
};

// ──── 单词相关 ────
const wordApi = {
  getWords: (params = {}) => request(`/words/?${buildQuery(params)}`),

  getWordDetail: (id) => request(`/words/${id}/`),

  getDailyWords: (count = 10) => request(`/words/daily_words/?count=${count}`),

  getRandomWord: () => request('/words/random_word/'),

  getCategories: () => request('/words/categories/'),

  markLearned: (wordId, isMastered = false) =>
    request('/words/mark_learned/', 'POST', { word_id: wordId, is_mastered: isMastered }),

  getMyProgress: () => request('/words/my_progress/'),

  searchWords: (keyword) => request(`/words/search/?q=${keyword}`),

  unmarkLearned: (wordId) =>
    request('/words/unmark_learned/', 'POST', { word_id: wordId }),
};

// ──── 打卡相关 ────
const checkinApi = {
  doCheckin: (wordCount = 0, studyDuration = 0, note = '') =>
    request('/checkin/do_checkin/', 'POST', {
      word_count: wordCount,
      study_duration: studyDuration,
      note,
    }),

  todayStatus: () => request('/checkin/today_status/'),

  getCalendar: (year, month) => request(`/checkin/calendar/?year=${year}&month=${month}`),

  getRecords: () => request('/checkin/records/'),

  getStreak: () => request('/checkin/streak/'),
};

// ──── 排行榜相关 ────
const leaderboardApi = {
  getDaily: () => request('/leaderboard/daily/'),
  getWeekly: () => request('/leaderboard/weekly/'),
  getMonthly: () => request('/leaderboard/monthly/'),
  getAlltime: () => request('/leaderboard/alltime/'),
  getStats: () => request('/leaderboard/stats/'),
};

// ──── 数据统计相关 ────
const statsApi = {
  /** 最近 30 天学习数据（热力图 + 每日学习趋势） */
  getAnalytics: () => request('/stats/analytics/'),
};

// ──── 学习系统相关 ────
const studyApi = {
  /** 今日学习任务（待复习+新词） */
  getTodayTasks: (reviewCount = 10, newCount = 10) =>
    request(`/study/today/?review_count=${reviewCount}&new_count=${newCount}`),

  /** 提交学习结果（grade: easy认识/vague模糊/hard不认识） */
  submitRecord: (wordId, grade) =>
    request('/study/record/', 'POST', { word_id: wordId, grade }),

  /** 获取测验题目 */
  getQuiz: (count = 10) => request(`/study/quiz/?count=${count}`),

  /** 提交测验答案 */
  submitQuiz: (answers) => request('/study/quiz/submit/', 'POST', { answers }),

  /** 错词本列表 */
  getWrongWords: (page = 1, pageSize = 20) =>
    request(`/study/wrong-words/?page=${page}&page_size=${pageSize}`),

  /** 从错词本移除 */
  removeWrongWord: (wordId) =>
    request('/study/wrong-words/remove/', 'POST', { word_id: wordId }),

  /** 收藏 / 取消收藏单词 */
  toggleFavorite: (wordId) =>
    request('/study/favorite/', 'POST', { word_id: wordId }),

  /** 收藏单词列表 */
  getFavorites: (page = 1, pageSize = 20) =>
    request(`/study/favorites?page=${page}&page_size=${pageSize}`),

  /** 今日学习统计 */
  getStudyStats: () => request('/study/stats/'),
};

// ──── 词汇量测试 ────
const vocabTestApi = {
  /** 获取测试题目 */
  getQuestions: (count = 30) => request(`/vocab-test/questions?count=${count}`),
  /** 提交答案 */
  submit: (answers) => request('/vocab-test/submit', 'POST', { answers }),
};

// ──── 成就徽章 ────
const achievementApi = {
  /** 获取用户徽章列表（含未解锁） */
  getUserAchievements: () => request('/achievements/'),
};

// ──── 词书库 + 自定义词本 ────
const wordBookApi = {
  /** 词书库列表（官方 + 本人自定义） */
  getBooks: () => request('/word-books/'),

  /** 我的词书（已加入） */
  getMyBooks: () => request('/word-books/mine/'),

  /** 词书详情 + 单词分页 */
  getBookDetail: (id, page = 1, pageSize = 20) =>
    request(`/word-books/${id}/?page=${page}&page_size=${pageSize}`),

  /** 加入词书 */
  joinBook: (id) => request(`/word-books/${id}/join/`, 'POST'),

  /** 设为当前学习词书 */
  selectBook: (id) => request(`/word-books/${id}/select/`, 'POST'),

  /** 创建自定义词本 */
  createCustomBook: (name, description, icon) =>
    request('/word-books/custom/', 'POST', { name, description, icon }),

  /** 手动添加单词到自定义词本 */
  addWord: (id, word, phonetic, meaning, exampleEn, exampleZh) =>
    request(`/word-books/${id}/words/`, 'POST', { word, phonetic, meaning, example_en: exampleEn, example_zh: exampleZh }),

  /** 文本粘贴批量导入 */
  importWords: (id, text) => request(`/word-books/${id}/import/`, 'POST', { text }),

  /** 删除自定义词本 */
  deleteBook: (id) => request(`/word-books/${id}/`, 'DELETE'),

  /** 从自定义词本移除单词 */
  removeWord: (id, wordId) => request(`/word-books/${id}/words/${wordId}/`, 'DELETE'),
};

// ──── 工具函数 ────
const buildQuery = (params) => {
  return Object.keys(params)
    .filter((k) => params[k] !== undefined && params[k] !== '')
    .map((k) => `${k}=${encodeURIComponent(params[k])}`)
    .join('&');
};

module.exports = {
  userApi,
  wordApi,
  checkinApi,
  leaderboardApi,
  statsApi,
  studyApi,
  achievementApi,
  vocabTestApi,
  wordBookApi,
};
