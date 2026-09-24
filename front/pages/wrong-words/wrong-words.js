// 单词本页（错词本 / 收藏本）— pages/wrong-words/wrong-words.js
const { studyApi } = require('../../utils/api');

Page({
  data: {
    activeTab: 'wrong',   // wrong=错词本, favorite=收藏本
    list: [],
    loading: true,
    page: 1,
    pageSize: 50,
    total: 0,
  },

  onLoad(options) {
    // 支持从入口直接指定 tab：?tab=favorite
    if (options.tab === 'favorite') {
      this.setData({ activeTab: 'favorite' });
    }
    this.loadList();
  },

  onShow() {
    // 从详情页返回时刷新
    if (!this.data.loading) {
      this.loadList();
    }
  },

  // 切换 tab
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (tab === this.data.activeTab) return;
    this.setData({ activeTab: tab, list: [], loading: true, page: 1, total: 0 });
    this.loadList();
  },

  loadList() {
    if (this.data.activeTab === 'favorite') {
      this.loadFavorites();
    } else {
      this.loadWrongWords();
    }
  },

  async loadWrongWords() {
    try {
      const res = await studyApi.getWrongWords(this.data.page, this.data.pageSize);
      if (res.code === 200) {
        this.setData({
          list: res.data.results || [],
          total: res.data.count || 0,
          loading: false,
        });
      } else {
        this.setData({ loading: false });
      }
    } catch (err) {
      console.log('加载错词本失败:', err);
      this.setData({ loading: false });
    }
  },

  async loadFavorites() {
    try {
      const res = await studyApi.getFavorites(this.data.page, this.data.pageSize);
      if (res.code === 200) {
        this.setData({
          list: res.data.results || [],
          total: res.data.count || 0,
          loading: false,
        });
      } else {
        this.setData({ loading: false });
      }
    } catch (err) {
      console.log('加载收藏本失败:', err);
      this.setData({ loading: false });
    }
  },

  // 移除（统一入口，按当前 tab 分流到错词移除 / 取消收藏）
  removeItem(e) {
    if (this.data.activeTab === 'favorite') {
      this.unfavorite(e);
    } else {
      this.removeWord(e);
    }
  },

  // 移除错词
  async removeWord(e) {
    const wordId = e.currentTarget.dataset.id;
    const word = e.currentTarget.dataset.word;
    const res = await wx.showModal({
      title: '移除错词',
      content: `确定将「${word}」从错词本移除吗？`,
      confirmText: '移除',
      confirmColor: '#C9A9A6',
    });
    if (!res.confirm) return;

    try {
      await studyApi.removeWrongWord(wordId);
      wx.showToast({ title: '已移除', icon: 'success' });
      this.loadList();
    } catch (err) {
      console.log('移除失败:', err);
    }
  },

  // 取消收藏
  async unfavorite(e) {
    const wordId = e.currentTarget.dataset.id;
    const word = e.currentTarget.dataset.word;
    const res = await wx.showModal({
      title: '取消收藏',
      content: `确定将「${word}」从生词本移除吗？`,
      confirmText: '移除',
      confirmColor: '#C9A9A6',
    });
    if (!res.confirm) return;

    try {
      await studyApi.toggleFavorite(wordId);
      wx.showToast({ title: '已移除', icon: 'success' });
      this.loadList();
    } catch (err) {
      console.log('取消收藏失败:', err);
    }
  },

  // 跳转单词详情
  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/word-detail/word-detail?id=${id}` });
  },

  // 去学习（返回首页引导学习）
  goStudy() {
    wx.switchTab({ url: '/pages/index/index' });
  },
});
