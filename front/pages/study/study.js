// 卡片式学习页 — pages/study/study.js
const { studyApi, wordApi } = require('../../utils/api');

Page({
  data: {
    tasks: [],
    currentIndex: 0,
    currentTask: null,
    isFlipped: false,
    loading: true,
    finished: false,
    // 统计
    knownCount: 0,
    unknownCount: 0,
    reviewCount: 0,
    newCount: 0,
    total: 0,
    // 音频
    audioContext: null,
  },

  onLoad() {
    this.loadTasks();
  },

  onUnload() {
    if (this.data.audioContext) {
      this.data.audioContext.destroy();
    }
  },

  async loadTasks() {
    try {
      const res = await studyApi.getTodayTasks(10, 10);
      if (res.code === 200 && res.data.tasks.length > 0) {
        const tasks = res.data.tasks;
        this.setData({
          tasks,
          currentTask: tasks[0],
          currentIndex: 0,
          total: tasks.length,
          reviewCount: res.data.review_count,
          newCount: res.data.new_count,
          loading: false,
          finished: false,
          knownCount: 0,
          unknownCount: 0,
        });
        // 第一张卡片自动播放发音
        setTimeout(() => this.playAudio(), 400);
      } else {
        this.setData({ loading: false, finished: true, total: 0 });
        wx.showToast({ title: '今日学习任务已完成', icon: 'none' });
      }
    } catch (err) {
      console.log('加载学习任务失败:', err);
      this.setData({ loading: false });
    }
  },

  // 点击卡片翻面
  flipCard() {
    this.setData({ isFlipped: !this.data.isFlipped });
  },

  // 发音
  playAudio() {
    const task = this.data.currentTask;
    if (!task) return;
    const url = task.audio_url || `https://dict.youdao.com/dictvoice?audio=${encodeURIComponent(task.word)}&type=0`;
    if (this.data.audioContext) {
      this.data.audioContext.destroy();
    }
    const ctx = wx.createInnerAudioContext();
    ctx.src = url;
    ctx.play();
    this.setData({ audioContext: ctx });
  },

  // 认识
  async markKnown() {
    await this.submitResult(true);
  },

  // 不认识
  async markUnknown() {
    await this.submitResult(false);
  },

  async submitResult(known) {
    const task = this.data.currentTask;
    if (!task) return;

    try {
      await studyApi.submitRecord(task.word_id, known);
    } catch (err) {
      console.log('提交学习结果失败:', err);
    }

    const knownCount = this.data.knownCount + (known ? 1 : 0);
    const unknownCount = this.data.unknownCount + (known ? 0 : 1);
    const nextIndex = this.data.currentIndex + 1;

    if (nextIndex >= this.data.tasks.length) {
      this.setData({
        finished: true,
        knownCount,
        unknownCount,
      });
    } else {
      this.setData({
        currentIndex: nextIndex,
        currentTask: this.data.tasks[nextIndex],
        isFlipped: false,
        knownCount,
        unknownCount,
      });
      // 切换到新卡片自动播放发音
      setTimeout(() => this.playAudio(), 400);
    }
  },

  // 去测验
  goQuiz() {
    wx.navigateTo({ url: '/pages/quiz/quiz' });
  },

  // 再学一组
  studyAgain() {
    this.setData({ loading: true });
    this.loadTasks();
  },

  // 返回首页
  goBack() {
    wx.switchTab({ url: '/pages/index/index' });
  },
});
