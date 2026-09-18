// 词汇量测试页 — pages/vocab-test/vocab-test.js
const { vocabTestApi } = require('../../utils/api');

Page({
  data: {
    phase: 'intro', // intro / quiz / result
    questions: [],
    currentIndex: 0,
    answers: [],
    selectedOption: '',
    result: null,
  },

  onLoad() {
    this.loadQuestions();
  },

  async loadQuestions() {
    try {
      wx.showLoading({ title: '加载题目...' });
      const res = await vocabTestApi.getQuestions(30);
      wx.hideLoading();
      if (res.code === 200 && res.data && res.data.length > 0) {
        this.setData({ questions: res.data, phase: 'intro' });
      } else {
        wx.showToast({ title: '题目加载失败', icon: 'none' });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '网络错误', icon: 'none' });
    }
  },

  startTest() {
    this.setData({ phase: 'quiz', currentIndex: 0, answers: [], selectedOption: '' });
  },

  selectOption(e) {
    const option = e.currentTarget.dataset.option;
    this.setData({ selectedOption: option });
    // 延迟自动下一题（缩短到200ms，更跟手）
    setTimeout(() => this.nextQuestion(), 200);
  },

  nextQuestion() {
    const { questions, currentIndex, selectedOption, answers } = this.data;
    const q = questions[currentIndex];

    const newAnswers = [...answers, {
      word_id: q.word_id,
      user_answer: selectedOption,
      question_type: 'meaning',
    }];

    if (currentIndex >= questions.length - 1) {
      this.submitTest(newAnswers);
    } else {
      this.setData({
        currentIndex: currentIndex + 1,
        selectedOption: '',
        answers: newAnswers,
      });
    }
  },

  async submitTest(answers) {
    try {
      wx.showLoading({ title: '正在评估...' });
      const res = await vocabTestApi.submit(answers);
      wx.hideLoading();
      if (res.code === 200) {
        this.setData({ phase: 'result', result: res.data });
      } else {
        wx.showToast({ title: '提交失败', icon: 'none' });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '网络错误', icon: 'none' });
    }
  },

  restart() {
    this.setData({ phase: 'intro', answers: [], selectedOption: '', result: null });
    this.loadQuestions();
  },

  goBack() {
    wx.navigateBack();
  },
});
