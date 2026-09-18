// 学习进度页 — pages/my-progress/my-progress.js
const { wordApi, statsApi } = require('../../utils/api');

// 热力图等级：0未打卡，1-4按当日单词数由浅到深
function heatLevel(day) {
  if (!day.checked) return 0;
  const wc = day.word_count || 0;
  if (wc <= 0) return 1;
  if (wc < 10) return 2;
  if (wc < 20) return 3;
  return 4;
}

function formatMD(dateStr) {
  const parts = dateStr.split('-');
  return `${parseInt(parts[1], 10)}/${parseInt(parts[2], 10)}`;
}

Page({
  data: {
    progress: null,
    loading: true,
    // 热力图
    heatmap: [],        // 原始 30 天数据 [{date, checked, word_count,...}]
    heatmapGrid: [],    // 渲染网格 [{date, level, isToday, label}]
    heatmapRange: '',   // 日期范围文案
    heatTotal: 0,       // 30 天内打卡天数
    heatTotalWords: 0,  // 30 天内学习单词总数
    // 学习趋势
    trend: [],          // 原始数据 [{date, words}]
    trendBars: [],      // 渲染柱子 [{date, words, height, label}]
    trendMax: 0,
    trendHalf: 0,
    trendLabels: [],
  },

  onLoad() {
    this.loadProgress();
    // 热力图+趋势图计算量大，延迟到首帧渲染后再算，避免跳转卡顿
    setTimeout(() => this.loadAnalytics(), 50);
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

  // 加载 30 天热力图 + 学习趋势
  async loadAnalytics() {
    try {
      const res = await statsApi.getAnalytics();
      if (res.code !== 200 || !res.data) return;

      const heatmap = res.data.heatmap || [];
      const trend = res.data.trend || [];

      // 热力图网格
      const todayStr = heatmap.length ? heatmap[heatmap.length - 1].date : '';
      const heatmapGrid = heatmap.map((d) => ({
        date: d.date,
        level: heatLevel(d),
        isToday: d.date === todayStr,
        label: d.date.split('-')[2],
      }));

      // 统计
      const checkedDays = heatmap.filter((d) => d.checked).length;
      const totalWords = heatmap.reduce((s, d) => s + (d.word_count || 0), 0);
      const heatmapRange = heatmap.length >= 2
        ? `${formatMD(heatmap[0].date)} - ${formatMD(heatmap[heatmap.length - 1].date)}`
        : '';

      // 趋势柱子
      const maxWords = Math.max(1, ...trend.map((t) => t.words || 0));
      const trendBars = trend.map((t) => ({
        date: t.date,
        words: t.words || 0,
        height: Math.max(4, Math.round(((t.words || 0) / maxWords) * 100)),
      }));

      // 横轴刻度（首/中/尾），WXML 不支持 slice 调用，提前格式化
      const trendLabels = trendBars.length
        ? [
            trendBars[0].date.slice(5),
            trendBars[Math.floor((trendBars.length - 1) / 2)].date.slice(5),
            trendBars[trendBars.length - 1].date.slice(5),
          ]
        : [];

      this.setData({
        heatmap,
        heatmapGrid,
        heatmapRange,
        heatTotal: checkedDays,
        heatTotalWords: totalWords,
        trend,
        trendBars,
        trendMax: maxWords,
        trendHalf: Math.max(1, Math.ceil(maxWords / 2)),
        trendLabels,
      });
    } catch (err) {
      console.log('获取学习数据失败:', err);
    }
  },

  // 点击热力图格子，查看当日详情
  onHeatTap(e) {
    const { date, level } = e.currentTarget.dataset;
    const day = this.data.heatmap.find((d) => d.date === date);
    if (!day) return;
    if (level === 0) {
      wx.showToast({ title: `${date} 未打卡`, icon: 'none' });
      return;
    }
    wx.showToast({
      title: `${date} 打卡${day.word_count}词 ${day.study_duration}分钟`,
      icon: 'none',
      duration: 2000,
    });
  },

  // 点击趋势柱子，查看当日单词数
  onTrendTap(e) {
    const { date, words } = e.currentTarget.dataset;
    wx.showToast({ title: `${date} 学习${words}词`, icon: 'none', duration: 1800 });
  },
});
