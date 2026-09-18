// 个人中心页 — pages/profile/profile.js
const { userApi, achievementApi } = require('../../utils/api');
const app = getApp();

Page({
  data: {
    userInfo: {},
    showLoginLogs: false,
    loginLogs: [],
    showGoalPicker: false,
    goalOptions: [10, 20, 30, 50],
    achievements: [],
    unlockedCount: 0,
  },

  onShow() {
    this.loadUserInfo();
    this.loadAchievements();
  },

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

  // 加载徽章
  async loadAchievements() {
    try {
      const res = await achievementApi.getUserAchievements();
      if (res.code === 200) {
        const list = res.data || [];
        const unlocked = list.filter(a => a.unlocked).length;
        this.setData({ achievements: list, unlockedCount: unlocked });
      }
    } catch (err) {
      console.log('获取徽章失败:', err);
    }
  },

  // 跳转打卡记录
  goToRecords() {
    wx.navigateTo({ url: '/pages/records/records' });
  },

  // 跳转单词本
  goToWords() {
    wx.navigateTo({ url: '/pages/words/words' });
  },

  // 跳转学习进度
  goToMyProgress() {
    wx.navigateTo({ url: '/pages/my-progress/my-progress' });
  },

  // 登录日志 — 弹窗展示
  async goToLoginLogs() {
    try {
      wx.showLoading({ title: '加载中...' });
      const res = await userApi.getLoginLogs();
      wx.hideLoading();
      if (res.code === 200) {
        this.setData({ loginLogs: res.data, showLoginLogs: true });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '获取日志失败', icon: 'none' });
    }
  },

  // 关闭登录日志弹窗
  closeLoginLogs() {
    this.setData({ showLoginLogs: false });
  },

  // 空函数，防止弹窗内容点击穿透
  noop() {},

  // 打开每日目标选择
  openGoalPicker() {
    this.setData({ showGoalPicker: true });
  },

  // 关闭每日目标选择
  closeGoalPicker() {
    this.setData({ showGoalPicker: false });
  },

  // 选择每日目标
  async selectGoal(e) {
    const goal = e.currentTarget.dataset.goal;
    try {
      wx.showLoading({ title: '保存中...' });
      const res = await userApi.updateProfile({ daily_goal: goal });
      wx.hideLoading();
      if (res.code === 200) {
        this.setData({ userInfo: res.data, showGoalPicker: false });
        wx.showToast({ title: '已更新', icon: 'success' });
      } else {
        wx.showToast({ title: res.message || '更新失败', icon: 'none' });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '更新失败', icon: 'none' });
    }
  },

  // 退出登录
  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          app.clearLoginInfo();
          wx.reLaunch({ url: '/pages/login/login' });
        }
      },
    });
  },
});
