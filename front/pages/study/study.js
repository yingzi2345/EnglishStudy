// 卡片式学习页 — pages/study/study.js
const { studyApi, userApi } = require('../../utils/api');
const app = getApp();

Page({
  data: {
    tasks: [],
    currentIndex: 0,
    currentTask: null,
    isFlipped: false,
    loading: true,
    finished: false,
    // 学习模式：en2zh 看英文回忆中文（默认），zh2en 看中文回忆英文
    studyMode: 'en2zh',
    // 统计
    easyCount: 0,
    vagueCount: 0,
    hardCount: 0,
    skipCount: 0,
    reviewCount: 0,
    newCount: 0,
    total: 0,
  },

  // 音频实例放页面实例上，不进 data（避免 setData 序列化开销）
  audioCtx: null,

  onLoad() {
    // 读取学习模式偏好（本地缓存优先，其次全局用户信息）
    const cachedMode = wx.getStorageSync('study_mode');
    const profileMode = app.globalData.userInfo && app.globalData.userInfo.study_mode;
    const studyMode = cachedMode || profileMode || 'en2zh';
    this.setData({ studyMode });
    this.loadTasks();
  },

  onUnload() {
    if (this.audioCtx) {
      this.audioCtx.destroy();
      this.audioCtx = null;
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
          easyCount: 0,
          vagueCount: 0,
          hardCount: 0,
          skipCount: 0,
        });
        // 看英忆中模式：第一张卡片正面就是英文，自动播放发音
        if (this.data.studyMode === 'en2zh') {
          setTimeout(() => this.playAudio(), 400);
        }
      } else {
        this.setData({ loading: false, finished: true, total: 0 });
        wx.showToast({ title: '今日学习任务已完成', icon: 'none' });
      }
    } catch (err) {
      console.log('加载学习任务失败:', err);
      this.setData({ loading: false });
    }
  },

  // 切换学习模式
  async toggleMode() {
    const mode = this.data.studyMode === 'en2zh' ? 'zh2en' : 'en2zh';
    this.setData({ studyMode: mode });
    wx.setStorageSync('study_mode', mode);
    // 同步到后端用户偏好
    try {
      await userApi.updateProfile({ study_mode: mode });
    } catch (err) {
      console.log('学习模式同步失败:', err);
    }
    wx.showToast({
      title: mode === 'en2zh' ? '看英文回忆中文' : '看中文回忆英文',
      icon: 'none',
      duration: 1200,
    });
    // 切到看英忆中时，当前卡片正面是英文，播放发音
    if (mode === 'en2zh' && !this.data.isFlipped) {
      setTimeout(() => this.playAudio(), 300);
    }
  },

  // 点击卡片翻面
  flipCard() {
    const flipped = !this.data.isFlipped;
    this.setData({ isFlipped: flipped });
    // 看中忆英模式：翻面后显示英文，自动播放发音
    if (flipped && this.data.studyMode === 'zh2en') {
      setTimeout(() => this.playAudio(), 300);
    }
  },

  // 发音（复用同一个音频实例，不每次重建）
  playAudio() {
    const task = this.data.currentTask;
    if (!task) return;
    const url = task.audio_url || `https://dict.youdao.com/dictvoice?audio=${encodeURIComponent(task.word)}&type=0`;
    if (!this.audioCtx) {
      this.audioCtx = wx.createInnerAudioContext();
    }
    this.audioCtx.stop();
    this.audioCtx.src = url;
    this.audioCtx.play();
  },

  // 收藏 / 取消收藏当前单词
  async toggleFavorite() {
    const task = this.data.currentTask;
    if (!task) return;
    // 乐观更新
    const fav = !task.is_favorite;
    this.updateCurrentTask({ is_favorite: fav });
    try {
      await studyApi.toggleFavorite(task.word_id);
      wx.showToast({ title: fav ? '已加入生词本' : '已取消收藏', icon: 'none', duration: 1000 });
    } catch (err) {
      // 失败回滚
      this.updateCurrentTask({ is_favorite: !fav });
      wx.showToast({ title: '操作失败', icon: 'none' });
    }
  },

  // 更新当前卡片字段（同时同步 tasks 数组）
  updateCurrentTask(patch) {
    const task = Object.assign({}, this.data.currentTask, patch);
    const tasks = this.data.tasks.slice();
    tasks[this.data.currentIndex] = task;
    this.setData({ currentTask: task, tasks });
  },

  // 三档评分
  markEasy() { return this.submitResult('easy'); },
  markVague() { return this.submitResult('vague'); },
  markHard() { return this.submitResult('hard'); },

  async submitResult(grade) {
    const task = this.data.currentTask;
    if (!task) return;

    try {
      await studyApi.submitRecord(task.word_id, grade);
    } catch (err) {
      console.log('提交学习结果失败:', err);
    }

    const patch = {
      easyCount: this.data.easyCount + (grade === 'easy' ? 1 : 0),
      vagueCount: this.data.vagueCount + (grade === 'vague' ? 1 : 0),
      hardCount: this.data.hardCount + (grade === 'hard' ? 1 : 0),
    };
    this.goNext(patch);
  },

  // 跳过：不提交结果，直接下一张
  skipWord() {
    this.goNext({ skipCount: this.data.skipCount + 1 });
  },

  // 进入下一张
  goNext(extraPatch) {
    const nextIndex = this.data.currentIndex + 1;
    if (nextIndex >= this.data.tasks.length) {
      this.setData(Object.assign({ finished: true }, extraPatch));
    } else {
      this.setData(Object.assign({
        currentIndex: nextIndex,
        currentTask: this.data.tasks[nextIndex],
        isFlipped: false,
      }, extraPatch));
      // 看英忆中模式自动播放下一张发音
      if (this.data.studyMode === 'en2zh') {
        setTimeout(() => this.playAudio(), 400);
      }
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
