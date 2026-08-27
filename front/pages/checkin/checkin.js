// 打卡页 — pages/checkin/checkin.js
const { checkinApi } = require('../../utils/api');

Page({
  data: {
    todayChecked: false,
    currentDate: '',
    streakData: {},
    todayWords: 0,
    todayMinutes: 0,
    progressPercent: 0,
    weekdays: ['日', '一', '二', '三', '四', '五', '六'],
    calendarYear: 2024,
    calendarMonth: 1,
    calendarGrid: [],
  },

  onLoad() {
    const now = new Date();
    this.setData({
      currentDate: `${now.getFullYear()}-${now.getMonth() + 1}-${now.getDate()}`,
      calendarYear: now.getFullYear(),
      calendarMonth: now.getMonth() + 1,
    });
  },

  onShow() {
    this.loadStatus();
    this.loadStreak();
    this.loadCalendar();
  },

  // 加载今日状态
  async loadStatus() {
    try {
      const res = await checkinApi.todayStatus();
      if (res.code === 200) {
        const record = res.data.today_record;
        this.setData({
          todayChecked: res.data.is_checked,
          todayWords: record ? record.word_count : 0,
          todayMinutes: record ? record.study_duration : 0,
          progressPercent: Math.min(100, (record ? record.word_count / 50 * 100 : 0).toFixed(0)),
        });
      }
    } catch (err) {
      console.log('获取打卡状态失败:', err);
    }
  },

  // 加载连续打卡信息
  async loadStreak() {
    try {
      const res = await checkinApi.getStreak();
      if (res.code === 200) {
        this.setData({ streakData: res.data });
      }
    } catch (err) {
      console.log('获取连续信息失败:', err);
    }
  },

  // 执行打卡
  async doCheckin() {
    if (this.data.todayChecked) {
      wx.showToast({ title: '今日已打卡', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '打卡中...' });
    try {
      const res = await checkinApi.doCheckin(
        this.data.todayWords,
        this.data.todayMinutes,
        '坚持学习，每天进步！'
      );
      wx.hideLoading();

      if (res.code === 200) {
        wx.showToast({ title: res.data.message, icon: 'success' });
        this.setData({
          todayChecked: true,
          streakData: {
            ...this.data.streakData,
            continuous_days: res.data.continuous_days,
            total_days: res.data.total_days,
          },
        });
        this.loadCalendar();
      } else {
        wx.showToast({ title: res.message, icon: 'none' });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '打卡失败', icon: 'none' });
    }
  },

  // 加载日历
  async loadCalendar() {
    try {
      const res = await checkinApi.getCalendar(
        this.data.calendarYear,
        this.data.calendarMonth
      );
      if (res.code === 200) {
        this.buildCalendarGrid(res.data);
      }
    } catch (err) {
      console.log('获取日历失败:', err);
      this.buildCalendarGrid({ days: 30, checkin_dates: [] });
    }
  },

  // 构建日历网格
  buildCalendarGrid(data) {
    const { year, month, days, checkin_dates } = data;
    const firstDay = new Date(year, month - 1, 1).getDay();
    const today = new Date();
    const grid = [];

    // 填充空白
    for (let i = 0; i < firstDay; i++) {
      grid.push({ day: '', isEmpty: true });
    }

    // 填充日期
    for (let d = 1; d <= days; d++) {
      const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      grid.push({
        day: d,
        isChecked: checkin_dates.includes(dateStr),
        isToday: today.getFullYear() === year &&
                 today.getMonth() + 1 === month &&
                 today.getDate() === d,
        isEmpty: false,
      });
    }

    this.setData({ calendarGrid: grid });
  },

  prevMonth() {
    let { calendarYear, calendarMonth } = this.data;
    if (calendarMonth === 1) {
      calendarYear -= 1;
      calendarMonth = 12;
    } else {
      calendarMonth -= 1;
    }
    this.setData({ calendarYear, calendarMonth });
    this.loadCalendar();
  },

  nextMonth() {
    let { calendarYear, calendarMonth } = this.data;
    if (calendarMonth === 12) {
      calendarYear += 1;
      calendarMonth = 1;
    } else {
      calendarMonth += 1;
    }
    this.setData({ calendarYear, calendarMonth });
    this.loadCalendar();
  },
});
