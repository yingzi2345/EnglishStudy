const { wordBookApi } = require('../../utils/api.js');

const GROUP_ORDER = ['中小学', '大学', '应试', '通用', '自定义'];

Page({
  data: {
    groups: [], // [{name, books:[]}]
    showCreate: false,
    newBookName: '',
    newBookDesc: '',
    loading: true,
  },

  onLoad() {
    this.loadBooks();
  },

  onShow() {
    // 从详情页返回时刷新
    if (!this.data.loading) {
      this.loadBooks();
    }
  },

  async loadBooks() {
    this.setData({ loading: true });
    try {
      const res = await wordBookApi.getBooks();
      if (res.code === 200) {
        const books = res.data || [];
        const groupMap = {};
        books.forEach((b) => {
          const g = b.book_group || '其他';
          if (!groupMap[g]) groupMap[g] = [];
          groupMap[g].push(b);
        });
        const groups = GROUP_ORDER.filter((g) => groupMap[g])
          .map((g) => ({ name: g, books: groupMap[g] }));
        // 追加未在预设顺序里的分组
        Object.keys(groupMap).forEach((g) => {
          if (!GROUP_ORDER.includes(g)) {
            groups.push({ name: g, books: groupMap[g] });
          }
        });
        this.setData({ groups });
      }
    } catch (e) {
      console.error('加载词书失败', e);
    } finally {
      this.setData({ loading: false });
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/word-book-detail/word-book-detail?id=${id}` });
  },

  showCreateModal() {
    this.setData({ showCreate: true, newBookName: '', newBookDesc: '' });
  },

  hideCreateModal() {
    this.setData({ showCreate: false });
  },

  onNameInput(e) {
    this.setData({ newBookName: e.detail.value });
  },

  onDescInput(e) {
    this.setData({ newBookDesc: e.detail.value });
  },

  async confirmCreate() {
    const name = this.data.newBookName.trim();
    if (!name) {
      wx.showToast({ title: '请输入词本名称', icon: 'none' });
      return;
    }
    try {
      const res = await wordBookApi.createCustomBook(name, this.data.newBookDesc.trim(), '📒');
      if (res.code === 200) {
        wx.showToast({ title: '创建成功', icon: 'success' });
        this.setData({ showCreate: false });
        this.loadBooks();
      } else {
        wx.showToast({ title: res.message || '创建失败', icon: 'none' });
      }
    } catch (e) {
      wx.showToast({ title: '创建失败', icon: 'none' });
    }
  },
});
