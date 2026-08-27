// 打卡记录页 — pages/records/records.js
const { checkinApi, userApi, wordApi } = require('../../utils/api');

Page({
  data: {
    totalDays: 0,
    totalWords: 0,
    maxContinuous: 0,
    records: [],
  },

  onLoad() {
    this.loadStats();
    this.loadRecords();
  },

  async loadStats() {
    try {
      const res = await userApi.getStats();
      if (res.code === 200) {
        this.setData({
          totalDays: res.data.total_days || 0,
          totalWords: res.data.total_words || 0,
          maxContinuous: res.data.max_continuous || 0,
        });
      }
    } catch (err) {
      console.log('获取统计失败:', err);
    }
  },

  async loadRecords() {
    try {
      const res = await checkinApi.getRecords();
      if (res.code === 200) {
        this.setData({ records: res.data });
      }
    } catch (err) {
      console.log('获取记录失败:', err);
    }
  },
});
