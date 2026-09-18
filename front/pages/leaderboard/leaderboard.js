// 排行榜页 — pages/leaderboard/leaderboard.js
const { leaderboardApi } = require('../../utils/api');

Page({
  data: {
    activePeriod: 'daily',
    rankList: [],
    topThree: [],
    restList: [],
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
        const list = res.data.list || [];
        this.setData({
          rankList: list,
          topThree: list.slice(0, 3),
          restList: list.slice(3),
          myRank: res.data.my_rank || null,
        });
      }
    } catch (err) {
      console.log('获取排行榜失败:', err);
    }
  },
});
