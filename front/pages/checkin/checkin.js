// 打卡页 — pages/checkin/checkin.js
const { checkinApi, userApi } = require('../../utils/api');

// 日历火焰等级：基于截至当日的连续打卡天数
function fireLevel(continuousDays) {
  if (!continuousDays || continuousDays <= 0) return 0;
  if (continuousDays === 1) return 1;
  if (continuousDays <= 3) return 2;
  if (continuousDays <= 6) return 3;
  return 4; // 连续 7 天及以上
}

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
    // 打卡庆祝动画
    celebrate: false,
    // 用户信息（海报用）
    userInfo: null,
    // 打卡海报
    showPoster: false,
    posterImage: '',
    posterGenerating: false,
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
    this.loadUserInfo();
  },

  // 加载用户信息（头像/昵称，海报用）
  async loadUserInfo() {
    try {
      const res = await userApi.getProfile();
      if (res.code === 200) {
        this.setData({ userInfo: res.data });
      }
    } catch (err) {
      console.log('获取用户信息失败:', err);
    }
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
          celebrate: true,
          streakData: {
            ...this.data.streakData,
            continuous_days: res.data.continuous_days,
            total_days: res.data.total_days,
          },
        });
        // 庆祝动画播放后复位
        setTimeout(() => this.setData({ celebrate: false }), 2200);
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
      this.buildCalendarGrid({ days: 30, checkin_dates: [], checkin_details: [] });
    }
  },

  // 构建日历网格（含火焰等级）
  buildCalendarGrid(data) {
    const { year, month, days, checkin_dates, checkin_details } = data;
    const firstDay = new Date(year, month - 1, 1).getDay();
    const today = new Date();
    const grid = [];

    // 打卡详情 map: date -> {continuous_days}
    const detailMap = {};
    (checkin_details || []).forEach((d) => {
      detailMap[d.date] = d;
    });

    // 填充空白
    for (let i = 0; i < firstDay; i++) {
      grid.push({ day: '', isEmpty: true });
    }

    // 填充日期
    for (let d = 1; d <= days; d++) {
      const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      const checked = (checkin_dates || []).includes(dateStr);
      const detail = detailMap[dateStr] || {};
      grid.push({
        day: d,
        isChecked: checked,
        isToday: today.getFullYear() === year &&
                 today.getMonth() + 1 === month &&
                 today.getDate() === d,
        fireLevel: checked ? fireLevel(detail.continuous_days) : 0,
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

  // ────────── 打卡海报 ──────────

  // 打开海报弹层并开始绘制
  showPoster() {
    this.setData({ showPoster: true, posterImage: '', posterGenerating: true });
    wx.nextTick(() => this.drawPoster());
  },

  closePoster() {
    this.setData({ showPoster: false });
  },

  noop() {},

  // 下载头像为本地路径（canvas 只认本地路径）
  loadAvatar(url) {
    return new Promise((resolve) => {
      if (!url) {
        resolve(null);
        return;
      }
      wx.getImageInfo({
        src: url,
        success: (r) => resolve(r.path),
        fail: () => resolve(null),
      });
    });
  },

  // 绘制圆角矩形
  roundRect(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    ctx.closePath();
  },

  // 绘制打卡海报（canvas 2d），失败时延迟重试一次
  async drawPoster(retried = false) {
    const query = wx.createSelectorQuery().in(this);
    query.select('#posterCanvas').fields({ node: true, size: true }).exec(async (res) => {
      if (!res || !res[0] || !res[0].node) {
        if (!retried) {
          setTimeout(() => this.drawPoster(true), 200);
        } else {
          this.setData({ posterGenerating: false });
          wx.showToast({ title: '海报生成失败', icon: 'none' });
        }
        return;
      }
      const canvas = res[0].node;
      const ctx = canvas.getContext('2d');
      const dpr = (wx.getSystemInfoSync().pixelRatio) || 2;
      const W = 300;
      const H = 520;
      canvas.width = W * dpr;
      canvas.height = H * dpr;
      ctx.scale(dpr, dpr);

      const user = this.data.userInfo || {};
      const nickname = user.nickname || '微信用户';
      const continuousDays = this.data.streakData.continuous_days || 0;
      const totalDays = this.data.streakData.total_days || 0;
      const maxContinuous = this.data.streakData.max_continuous || 0;
      const todayWords = this.data.todayWords || 0;

      // 头像本地路径
      const avatarPath = await this.loadAvatar(user.avatar_url);

      // ── 背景渐变 ──
      const bg = ctx.createLinearGradient(0, 0, W, H);
      bg.addColorStop(0, '#4A90D9');
      bg.addColorStop(1, '#24598F');
      ctx.fillStyle = bg;
      ctx.fillRect(0, 0, W, H);

      // ── 装饰圆 ──
      ctx.fillStyle = 'rgba(255,255,255,0.08)';
      ctx.beginPath();
      ctx.arc(258, 62, 70, 0, Math.PI * 2);
      ctx.fill();
      ctx.beginPath();
      ctx.arc(36, 470, 56, 0, Math.PI * 2);
      ctx.fill();

      // ── 顶部标题 ──
      ctx.fillStyle = '#fff';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.font = 'bold 22px sans-serif';
      ctx.fillText('英语学习打卡', W / 2, 46);
      ctx.font = '12px sans-serif';
      ctx.fillStyle = 'rgba(255,255,255,0.8)';
      ctx.fillText(this.data.currentDate, W / 2, 72);

      // ── 白色内容卡 ──
      ctx.fillStyle = '#ffffff';
      this.roundRect(ctx, 24, 102, W - 48, 248, 16);
      ctx.fill();

      // 头像（圆形裁切）
      const avatarX = W / 2;
      const avatarY = 148;
      const avatarR = 30;
      ctx.save();
      ctx.beginPath();
      ctx.arc(avatarX, avatarY, avatarR, 0, Math.PI * 2);
      ctx.clip();
      if (avatarPath) {
        try {
          ctx.drawImage(avatarPath, avatarX - avatarR, avatarY - avatarR, avatarR * 2, avatarR * 2);
        } catch (e) {
          ctx.fillStyle = '#d6eaff';
          ctx.fillRect(avatarX - avatarR, avatarY - avatarR, avatarR * 2, avatarR * 2);
        }
      } else {
        ctx.fillStyle = '#d6eaff';
        ctx.fillRect(avatarX - avatarR, avatarY - avatarR, avatarR * 2, avatarR * 2);
        ctx.fillStyle = '#4A90D9';
        ctx.font = 'bold 26px sans-serif';
        ctx.fillText(nickname.charAt(0), avatarX, avatarY);
      }
      ctx.restore();
      // 头像描边
      ctx.strokeStyle = '#fff';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(avatarX, avatarY, avatarR + 1.5, 0, Math.PI * 2);
      ctx.stroke();

      // 昵称
      ctx.fillStyle = '#333';
      ctx.font = 'bold 15px sans-serif';
      ctx.fillText(nickname, W / 2, 196);

      // 连续打卡主数字
      ctx.fillStyle = '#999';
      ctx.font = '12px sans-serif';
      ctx.fillText('已连续打卡', W / 2, 224);
      ctx.fillStyle = '#4A90D9';
      ctx.font = 'bold 44px sans-serif';
      ctx.fillText(`${continuousDays} 天`, W / 2, 272);

      // 分割线
      ctx.strokeStyle = '#f0f0f0';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(48, 296);
      ctx.lineTo(W - 48, 296);
      ctx.stroke();

      // 三项统计
      ctx.fillStyle = '#666';
      ctx.font = '12px sans-serif';
      const stats = [
        { label: '累计打卡', value: `${totalDays}天` },
        { label: '最长连续', value: `${maxContinuous}天` },
        { label: '今日学习', value: `${todayWords}词` },
      ];
      stats.forEach((s, i) => {
        const x = 40 + i * 74 + 37;
        ctx.fillText(s.value, x, 322);
        ctx.fillStyle = '#999';
        ctx.font = '10px sans-serif';
        ctx.fillText(s.label, x, 340);
        ctx.fillStyle = '#666';
        ctx.font = '12px sans-serif';
      });

      // ── 底部文案 ──
      ctx.fillStyle = '#fff';
      ctx.font = 'bold 16px sans-serif';
      ctx.fillText('坚持学习，每天进步！', W / 2, 452);
      ctx.fillStyle = 'rgba(255,255,255,0.7)';
      ctx.font = '11px sans-serif';
      ctx.fillText('长按保存，分享你的学习成果', W / 2, 478);

      // ── 导出图片 ──
      setTimeout(() => {
        wx.canvasToTempFilePath({
          canvas,
          width: W,
          height: H,
          destWidth: W * dpr,
          destHeight: H * dpr,
          success: (r) => {
            this.setData({ posterImage: r.tempFilePath, posterGenerating: false });
          },
          fail: () => {
            this.setData({ posterGenerating: false });
            wx.showToast({ title: '海报生成失败', icon: 'none' });
          },
        });
      }, 150);
    });
  },

  // 保存海报到相册
  savePoster() {
    if (!this.data.posterImage) return;
    wx.saveImageToPhotosAlbum({
      filePath: this.data.posterImage,
      success: () => wx.showToast({ title: '已保存到相册', icon: 'success' }),
      fail: (err) => {
        if (err.errMsg && err.errMsg.indexOf('auth') > -1) {
          wx.showModal({
            title: '需要相册权限',
            content: '保存海报需要访问你的相册，请在设置中开启权限',
            confirmText: '去设置',
            success: (r) => {
              if (r.confirm) wx.openSetting();
            },
          });
        } else {
          wx.showToast({ title: '保存失败', icon: 'none' });
        }
      },
    });
  },

  // 分享图片到朋友圈（基础库 2.14.3+）
  shareToTimeline() {
    if (!this.data.posterImage) return;
    if (wx.showShareImageMenu) {
      wx.showShareImageMenu({
        path: this.data.posterImage,
        fail: () => wx.showToast({ title: '当前版本不支持，请保存后分享', icon: 'none' }),
      });
    } else {
      wx.showToast({ title: '当前版本不支持，请保存后分享', icon: 'none' });
    }
  },

  // 转发给好友
  onShareAppMessage() {
    return {
      title: this.data.todayChecked
        ? `我已连续打卡 ${this.data.streakData.continuous_days || 0} 天，一起来学英语！`
        : '英语学习打卡，每天进步一点点！',
      path: '/pages/index/index',
    };
  },
});
