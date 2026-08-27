// 单词列表页 — pages/words/words.js
const { wordApi } = require('../../utils/api');

Page({
  data: {
    keyword: '',
    categories: [],
    activeCategory: '',
    words: [],
    page: 1,
    hasMore: true,
  },

  onLoad() {
    this.loadCategories();
    this.loadWords();
  },

  onShow() {
    // 从详情页返回时刷新列表，确保学习状态更新
    if (this.data.words.length > 0) {
      this.loadWords(true);
    }
  },

  // 加载分类
  async loadCategories() {
    try {
      const res = await wordApi.getCategories();
      if (res.code === 200) {
        this.setData({ categories: ['全部', ...res.data] });
      }
    } catch (err) {
      console.log('获取分类失败:', err);
    }
  },

  // 加载单词列表
  async loadWords(reset = false) {
    const page = reset ? 1 : this.data.page;
    try {
      const params = { page, page_size: 20 };
      if (this.data.activeCategory && this.data.activeCategory !== '全部') {
        params.category = this.data.activeCategory;
      }
      if (this.data.keyword) {
        params.search = this.data.keyword;
      }

      const res = await wordApi.getWords(params);
      if (res.code === 200) {
        const newWords = res.data.results || res.data;
        this.setData({
          words: reset ? newWords : [...this.data.words, ...newWords],
          page: page + 1,
          hasMore: newWords.length >= 20,
        });
      }
    } catch (err) {
      console.log('获取单词失败:', err);
    }
  },

  // 搜索
  onSearchInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onSearch() {
    this.loadWords(true);
  },

  // 分类切换
  onCategoryTap(e) {
    const cat = e.currentTarget.dataset.cat;
    this.setData({ activeCategory: cat });
    this.loadWords(true);
  },

  // 加载更多
  loadMore() {
    if (this.data.hasMore) {
      this.loadWords();
    }
  },

  // 跳转详情
  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/word-detail/word-detail?id=${id}` });
  },
});
