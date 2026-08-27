// 学习进度页 — pages/my-progress/my-progress.js
const { wordApi } = require('../../utils/api');

Page({
  data: {
    progress: null,
    loading: true,
  },

  onShow() {
    this.loadProgress();
  },

  async loadProgress() {
    this.setData({ loading: true });
    try {
      const res = await wordApi.getMyProgress();
      if (res.code === 200) {
        this.setData({ progress: res.data, loading: false });
      }
    } catch (err) {
      console.log('获取学习进度失败:', err);
      this.setData({ loading: false });
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },
});
