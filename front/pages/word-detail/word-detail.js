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
        const word = res.data;
        // 预处理：逗号分隔字符串转数组（wxml 不支持 split 方法调用）
        word.synonymsList = word.synonyms ? word.synonyms.split(',').filter(s => s.trim()) : [];
        word.antonymsList = word.antonyms ? word.antonyms.split(',').filter(s => s.trim()) : [];
        word.formsList = word.word_forms ? word.word_forms.split(',').filter(s => s.trim()) : [];
        this.setData({ word });
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
  // 使用页面级单例，连续点击时先打断上一次播放，避免多个实例叠加播放
  playAudio() {
    const word = this.data.word;
    if (!word) return;

    if (!this.audioCtx) {
      this.audioCtx = wx.createInnerAudioContext();
      this.audioCtx.onError((err) => {
        console.error('发音播放失败:', err);
        wx.showToast({ title: '发音播放失败', icon: 'none' });
      });
    }

    // 先停止当前正在播放的音频，再播放新的
    this.audioCtx.stop();

    if (word.audio_url) {
      this.audioCtx.src = word.audio_url;
      this.audioCtx.play();
    } else {
      // 有道 TTS：type=0 美音，type=1 英音
      const type = this.data.accent === 'uk' ? 1 : 0;
      const ttsUrl = `https://dict.youdao.com/dictvoice?audio=${encodeURIComponent(word.word)}&type=${type}`;
      this.audioCtx.src = ttsUrl;
      this.audioCtx.play();
    }
  },

  // 页面卸载时销毁播放实例，避免资源泄漏
  onUnload() {
    if (this.audioCtx) {
      this.audioCtx.destroy();
      this.audioCtx = null;
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
