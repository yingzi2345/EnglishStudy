// 首页 — pages/index/index.js
const { wordApi, userApi, checkinApi, studyApi } = require('../../utils/api');

Page({
  data: {
    userInfo: {},
    dailyWords: [],
    continuousDays: 0,
    todayChecked: false,
    studyStats: { due_review: 0, wrong_count: 0, mastered_count: 0 },
  },

  onLoad() {
    this.loadUserInfo();
    this.loadDailyWords();
    this.checkTodayStatus();
    this.loadStudyStats();
  },

  onShow() {
    this.loadUserInfo();
    this.loadDailyWords();
    this.checkTodayStatus();
    this.loadStudyStats();
  },

  // 获取用户信息
  async loadUserInfo() {
    try {
      const res = await userApi.getProfile();
      if (res.code === 200) {
        this.setData({ userInfo: res.data });
      }
    } catch (err) {
      console.log('获取用户信息失败:', err);
    }
  },

  // 获取每日推荐单词
  async loadDailyWords() {
    try {
      const res = await wordApi.getDailyWords(5);
      if (res.code === 200) {
        this.setData({ dailyWords: res.data });
      }
    } catch (err) {
      console.log('获取推荐单词失败:', err);
    }
  },

  // 查询今日打卡状态
  async checkTodayStatus() {
    try {
      const res = await checkinApi.todayStatus();
      if (res.code === 200) {
        this.setData({
          todayChecked: res.data.is_checked,
          continuousDays: res.data.today_record?.continuous_days || 0,
        });
      }
    } catch (err) {
      console.log('查询打卡状态失败:', err);
    }
  },

  // 加载学习统计（待复习/错词/已掌握）
  async loadStudyStats() {
    try {
      const res = await studyApi.getStudyStats();
      if (res.code === 200) {
        this.setData({ studyStats: res.data });
      }
    } catch (err) {
      console.log('获取学习统计失败:', err);
    }
  },

  // ──── 页面跳转 ────
  goToWords() {
    wx.navigateTo({ url: '/pages/words/words' });
  },

  goToCheckin() {
    wx.switchTab({ url: '/pages/checkin/checkin' });
  },

  goToLeaderboard() {
    wx.switchTab({ url: '/pages/leaderboard/leaderboard' });
  },

  goToRecords() {
    wx.navigateTo({ url: '/pages/records/records' });
  },

  goToWordDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/word-detail/word-detail?id=${id}` });
  },

  goToStudy() {
    wx.navigateTo({ url: '/pages/study/study' });
  },

  goToQuiz() {
    wx.navigateTo({ url: '/pages/quiz/quiz' });
  },

  goToWrongWords() {
    wx.navigateTo({ url: '/pages/wrong-words/wrong-words' });
  },
});
