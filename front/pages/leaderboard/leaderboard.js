// 排行榜页 — pages/leaderboard/leaderboard.js
const { leaderboardApi } = require('../../utils/api');

Page({
  data: {
    activePeriod: 'daily',
    rankList: [],
    myRank: null,
  },

  onLoad() {
    this.loadRank();
  },

  switchPeriod(e) {
    const period = e.currentTarget.dataset.period;
    this.setData({ activePeriod: period });
    this.loadRank();
  },

  async loadRank() {
    try {
      let res;
      const period = this.data.activePeriod;
      if (period === 'daily') res = await leaderboardApi.getDaily();
      else if (period === 'weekly') res = await leaderboardApi.getWeekly();
      else if (period === 'monthly') res = await leaderboardApi.getMonthly();
      else res = await leaderboardApi.getAlltime();

      if (res.code === 200) {
        this.setData({
          rankList: res.data.list || [],
          myRank: res.data.my_rank || null,
        });
      }
    } catch (err) {
      console.log('获取排行榜失败:', err);
    }
  },
});
