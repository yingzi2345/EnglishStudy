// 测验页 — pages/quiz/quiz.js
const { studyApi } = require('../../utils/api');

Page({
  data: {
    questions: [],
    currentIndex: 0,
    currentQuestion: null,
    loading: true,
    finished: false,
    // 答题记录
    answers: [],
    selectedAnswer: '',
    spellingInput: '',
    // 结果
    result: null,
  },

  // 音频实例放页面实例上，不进 data
  audioCtx: null,

  onLoad() {
    this.loadQuestions();
  },

  onUnload() {
    if (this.audioCtx) {
      this.audioCtx.destroy();
      this.audioCtx = null;
    }
  },

  async loadQuestions() {
    try {
      const res = await studyApi.getQuiz(10);
      if (res.code === 200 && res.data.length > 0) {
        this.setData({
          questions: res.data,
          currentQuestion: res.data[0],
          currentIndex: 0,
          loading: false,
          answers: [],
          selectedAnswer: '',
          spellingInput: '',
        });
        // 听音题自动播放
        if (res.data[0].question_type === 'listening') {
          setTimeout(() => this.playAudio(), 300);
        }
      } else {
        this.setData({ loading: false });
        wx.showToast({ title: '还没有学习过单词', icon: 'none' });
      }
    } catch (err) {
      console.log('加载测验题失败:', err);
      this.setData({ loading: false });
    }
  },

  // 播放发音（复用音频实例）
  playAudio() {
    const q = this.data.currentQuestion;
    if (!q) return;
    const url = q.audio_url || `https://dict.youdao.com/dictvoice?audio=${encodeURIComponent(q.word)}&type=0`;
    if (!this.audioCtx) {
      this.audioCtx = wx.createInnerAudioContext();
    }
    this.audioCtx.stop();
    this.audioCtx.src = url;
    this.audioCtx.play();
  },

  // 选择题选择
  selectOption(e) {
    const answer = e.currentTarget.dataset.answer;
    this.setData({ selectedAnswer: answer });
  },

  // 拼写输入
  onSpellingInput(e) {
    this.setData({ spellingInput: e.detail.value });
  },

  // 下一题
  nextQuestion() {
    const q = this.data.currentQuestion;
    let userAnswer = '';

    if (q.question_type === 'spelling') {
      userAnswer = this.data.spellingInput.trim();
      if (!userAnswer) {
        wx.showToast({ title: '请输入单词', icon: 'none' });
        return;
      }
    } else {
      userAnswer = this.data.selectedAnswer;
      if (!userAnswer) {
        wx.showToast({ title: '请选择答案', icon: 'none' });
        return;
      }
    }

    const answers = [...this.data.answers, {
      word_id: q.word_id,
      question_type: q.question_type,
      user_answer: userAnswer,
    }];

    const nextIndex = this.data.currentIndex + 1;
    if (nextIndex >= this.data.questions.length) {
      this.submitQuiz(answers);
    } else {
      this.setData({
        answers,
        currentIndex: nextIndex,
        currentQuestion: this.data.questions[nextIndex],
        selectedAnswer: '',
        spellingInput: '',
      });
      // 听音题自动播放
      if (this.data.questions[nextIndex].question_type === 'listening') {
        setTimeout(() => this.playAudio(), 300);
      }
    }
  },

  async submitQuiz(answers) {
    wx.showLoading({ title: '判分中...' });
    try {
      const res = await studyApi.submitQuiz(answers);
      if (res.code === 200) {
        this.setData({
          finished: true,
          result: res.data,
          answers,
        });
      }
    } catch (err) {
      console.log('提交测验失败:', err);
    } finally {
      wx.hideLoading();
    }
  },

  // 查看错词本
  goWrongWords() {
    wx.navigateTo({ url: '/pages/wrong-words/wrong-words' });
  },

  // 再测一次
  retryQuiz() {
    this.setData({ loading: true, finished: false, result: null });
    this.loadQuestions();
  },

  goBack() {
    wx.navigateBack();
  },
});
