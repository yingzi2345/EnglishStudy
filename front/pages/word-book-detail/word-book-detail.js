const { wordBookApi } = require('../../utils/api.js');

Page({
  data: {
    bookId: null,
    book: null,
    words: [],
    page: 1,
    pageSize: 30,
    hasMore: true,
    loading: true,
    isCustom: false,
    // 添加单词弹层
    showAdd: false,
    addWord: '',
    addPhonetic: '',
    addMeaning: '',
    // 导入弹层
    showImport: false,
    importText: '',
  },

  onLoad(options) {
    this.setData({ bookId: options.id });
    this.loadDetail(true);
  },

  async loadDetail(reset) {
    if (reset) {
      this.setData({ page: 1, words: [], hasMore: true });
    }
    if (!this.data.hasMore) return;
    this.setData({ loading: true });
    try {
      const res = await wordBookApi.getBookDetail(this.data.bookId, this.data.page, this.data.pageSize);
      if (res.code === 200) {
        const book = res.data.book;
        if (book) {
          book.progress_percent = book.total_words > 0
            ? Math.round(book.learned_words / book.total_words * 100) : 0;
        }
        const newWords = res.data.results || [];
        const words = reset ? newWords : this.data.words.concat(newWords);
        this.setData({
          book,
          words,
          isCustom: book && book.book_type === 'custom',
          hasMore: newWords.length >= this.data.pageSize,
          page: this.data.page + 1,
        });
        wx.setNavigationBarTitle({ title: book ? book.name : '词书详情' });
      }
    } catch (e) {
      console.error('加载词书详情失败', e);
    } finally {
      this.setData({ loading: false });
    }
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadDetail(false);
    }
  },

  async joinBook() {
    try {
      const res = await wordBookApi.joinBook(this.data.bookId);
      if (res.code === 200) {
        wx.showToast({ title: '已加入', icon: 'success' });
        this.loadDetail(true);
      }
    } catch (e) {}
  },

  async selectBook() {
    try {
      const res = await wordBookApi.selectBook(this.data.bookId);
      if (res.code === 200) {
        wx.showToast({ title: '已设为当前学习词书', icon: 'success' });
        this.loadDetail(true);
      }
    } catch (e) {}
  },

  goWordDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/word-detail/word-detail?id=${id}` });
  },

  // ──── 自定义词本：添加单词 ────
  showAddModal() {
    this.setData({ showAdd: true, addWord: '', addPhonetic: '', addMeaning: '' });
  },
  hideAddModal() {
    this.setData({ showAdd: false });
  },
  onAddInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail.value });
  },
  async confirmAdd() {
    const word = this.data.addWord.trim();
    if (!word) {
      wx.showToast({ title: '请输入单词', icon: 'none' });
      return;
    }
    try {
      const res = await wordBookApi.addWord(
        this.data.bookId, word,
        this.data.addPhonetic.trim(),
        this.data.addMeaning.trim()
      );
      if (res.code === 200) {
        wx.showToast({ title: res.data.added ? '已添加' : '词本中已存在', icon: 'success' });
        this.setData({ showAdd: false });
        this.loadDetail(true);
      } else {
        wx.showToast({ title: res.message || '添加失败', icon: 'none' });
      }
    } catch (e) {
      wx.showToast({ title: '添加失败', icon: 'none' });
    }
  },

  // ──── 自定义词本：粘贴导入 ────
  showImportModal() {
    this.setData({ showImport: true, importText: '' });
  },
  hideImportModal() {
    this.setData({ showImport: false });
  },
  onImportInput(e) {
    this.setData({ importText: e.detail.value });
  },
  async confirmImport() {
    const text = this.data.importText.trim();
    if (!text) {
      wx.showToast({ title: '请粘贴单词内容', icon: 'none' });
      return;
    }
    try {
      const res = await wordBookApi.importWords(this.data.bookId, text);
      if (res.code === 200) {
        const d = res.data;
        wx.showModal({
          title: '导入完成',
          content: `成功 ${d.success} 个，跳过 ${d.skipped} 个${d.errors && d.errors.length ? '\n' + d.errors.slice(0, 3).join('\n') : ''}`,
          showCancel: false,
        });
        this.setData({ showImport: false });
        this.loadDetail(true);
      }
    } catch (e) {
      wx.showToast({ title: '导入失败', icon: 'none' });
    }
  },

  // ──── 自定义词本：删除 ────
  deleteBook() {
    wx.showModal({
      title: '删除词本',
      content: '确定删除这个自定义词本吗？词本内的单词记录不会被删除。',
      confirmColor: '#C49A97',
      success: async (res) => {
        if (res.confirm) {
          try {
            const r = await wordBookApi.deleteBook(this.data.bookId);
            if (r.code === 200) {
              wx.showToast({ title: '已删除', icon: 'success' });
              setTimeout(() => wx.navigateBack(), 800);
            }
          } catch (e) {}
        }
      },
    });
  },

  // ──── 自定义词本：移除单词 ────
  removeWord(e) {
    const wordId = e.currentTarget.dataset.id;
    const wordText = e.currentTarget.dataset.word;
    wx.showModal({
      title: '移除单词',
      content: `确定从词本中移除「${wordText}」吗？`,
      confirmColor: '#C49A97',
      success: async (res) => {
        if (res.confirm) {
          try {
            const r = await wordBookApi.removeWord(this.data.bookId, wordId);
            if (r.code === 200) {
              wx.showToast({ title: '已移除', icon: 'success' });
              this.loadDetail(true);
            }
          } catch (e) {}
        }
      },
    });
  },
});
