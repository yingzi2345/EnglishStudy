// ──────────────────────────────────────────────
// 英语学习打卡系统 — 微信小程序
// app.js
// ──────────────────────────────────────────────
App({
  globalData: {
    userInfo: null,
    token: null,
    baseUrl: 'http://127.0.0.1:8000/api', // 后端API地址
  },

  onLaunch() {
    // 检查本地存储的token
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    if (token) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
    }
  },

  // 检查登录状态
  checkLogin() {
    return !!this.globalData.token;
  },

  // 获取token
  getToken() {
    return this.globalData.token || wx.getStorageSync('token');
  },

  // 设置登录信息
  setLoginInfo(token, userInfo) {
    this.globalData.token = token;
    this.globalData.userInfo = userInfo;
    wx.setStorageSync('token', token);
    wx.setStorageSync('userInfo', userInfo);
  },

  // 清除登录信息
  clearLoginInfo() {
    this.globalData.token = null;
    this.globalData.userInfo = null;
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
  },
});
