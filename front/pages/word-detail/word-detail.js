// 单词详情页 — pages/word-detail/word-detail.js
const { wordApi } = require('../../utils/api');

Page({
  data: {
    word: null,
    accent: 'us',  // 'us' 美音 | 'uk' 英音
  },

  onLoad(options) {
    const wordId = options.id;
    if (wordId) {
      this.loadWordDetail(wordId);
    }
  },

  async loadWordDetail(id) {
    try {
      const res = await wordApi.getWordDetail(id);
      if (res.code === 200) {
        this.setData({ word: res.data });
      } else {
        wx.showToast({ title: res.message || '获取单词失败', icon: 'none' });
        this.setData({ loadError: true });
      }
    } catch (err) {
      wx.showToast({ title: '获取单词失败', icon: 'none' });
      this.setData({ loadError: true });
    }
  },

  // 标记已学
  async markLearned() {
    if (!this.data.word) return;
    try {
      const res = await wordApi.markLearned(this.data.word.id, false);
      if (res.code === 200) {
        wx.showToast({ title: '已标记为已学', icon: 'success' });
        this.setData({ 'word.is_learned': true });
      }
    } catch (err) {
      wx.showToast({ title: '操作失败', icon: 'none' });
    }
  },

  // 标记已掌握
  async markMastered() {
    if (!this.data.word) return;
    try {
      const res = await wordApi.markLearned(this.data.word.id, true);
      if (res.code === 200) {
        wx.showToast({ title: '已标记为已掌握', icon: 'success' });
        this.setData({
          'word.is_learned': true,
          'word.is_mastered': true,
        });
      }
    } catch (err) {
      wx.showToast({ title: '操作失败', icon: 'none' });
    }
  },

  // 撤销学习状态
  async unmarkLearned() {
    if (!this.data.word) return;
    wx.showModal({
      title: '确认撤销',
      content: '确定要清除该单词的学习状态吗？',
      success: async (modalRes) => {
        if (!modalRes.confirm) return;
        try {
          const res = await wordApi.unmarkLearned(this.data.word.id);
          if (res.code === 200) {
            wx.showToast({ title: '已撤销学习状态', icon: 'success' });
            this.setData({
              'word.is_learned': false,
              'word.is_mastered': false,
            });
          }
        } catch (err) {
          wx.showToast({ title: '操作失败', icon: 'none' });
        }
      },
    });
  },

  // 播放发音：优先使用后台音频地址，兜底使用有道 TTS 在线发音
  playAudio() {
    const word = this.data.word;
    if (!word) return;

    const innerAudioContext = wx.createInnerAudioContext();

    innerAudioContext.onError((err) => {
      console.error('发音播放失败:', err);
      wx.showToast({ title: '发音播放失败', icon: 'none' });
    });

    if (word.audio_url) {
      innerAudioContext.src = word.audio_url;
      innerAudioContext.play();
    } else {
      // 有道 TTS：type=0 美音，type=1 英音
      const type = this.data.accent === 'uk' ? 1 : 0;
      const ttsUrl = `https://dict.youdao.com/dictvoice?audio=${encodeURIComponent(word.word)}&type=${type}`;
      innerAudioContext.src = ttsUrl;
      innerAudioContext.play();
    }
  },

  // 切换英音/美音
  toggleAccent(e) {
    const accent = e.currentTarget.dataset.accent;
    if (accent && accent !== this.data.accent) {
      this.setData({ accent });
    }
  },
});
