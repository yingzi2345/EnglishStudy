// 登录页 — pages/login/login.js
const { userApi } = require('../../utils/api');
const app = getApp();

Page({
  data: {
    loading: false,
  },

  onLoad() {
    // 已登录则跳转首页
    if (app.checkLogin()) {
      wx.switchTab({ url: '/pages/index/index' });
    }
  },

  // 微信一键登录
  handleWechatLogin() {
    if (this.data.loading) return;
    this.setData({ loading: true });

    wx.login({
      success: async (loginRes) => {
        try {
          // 获取用户信息
          let nickname = '';
          let avatarUrl = '';
          try {
            const userInfoRes = await wx.getUserProfile({ desc: '用于完善个人资料' });
            nickname = userInfoRes.userInfo.nickName;
            avatarUrl = userInfoRes.userInfo.avatarUrl;
          } catch (e) {
            console.log('用户拒绝授权个人信息');
          }

          // 调用后端登录接口
          const res = await userApi.wechatLogin(loginRes.code, nickname, avatarUrl);

          if (res.code === 200) {
            const { token, user } = res.data;
            app.setLoginInfo(token.access, user);

            wx.showToast({ title: '登录成功', icon: 'success' });
            setTimeout(() => {
              wx.switchTab({ url: '/pages/index/index' });
            }, 800);
          } else {
            wx.showToast({ title: res.message || '登录失败', icon: 'none' });
          }
        } catch (err) {
          // 开发模式：模拟登录
          const mockRes = await userApi.wechatLogin('mock_code_123', '测试用户', '');
          if (mockRes.code === 200) {
            app.setLoginInfo(mockRes.data.token.access, mockRes.data.user);
            wx.showToast({ title: '登录成功(开发模式)', icon: 'success' });
            setTimeout(() => {
              wx.switchTab({ url: '/pages/index/index' });
            }, 800);
          } else {
            wx.showToast({ title: '登录失败，请重试', icon: 'none' });
          }
        } finally {
          this.setData({ loading: false });
        }
      },
      fail: () => {
        this.setData({ loading: false });
        wx.showToast({ title: '微信登录失败', icon: 'none' });
      },
    });
  },
});
