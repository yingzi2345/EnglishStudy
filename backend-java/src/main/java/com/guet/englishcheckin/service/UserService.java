package com.guet.englishcheckin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guet.englishcheckin.common.BusinessException;
import com.guet.englishcheckin.dto.UpdateProfileRequest;
import com.guet.englishcheckin.dto.WechatLoginRequest;
import com.guet.englishcheckin.entity.Checkin;
import com.guet.englishcheckin.entity.LoginLog;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.mapper.CheckinMapper;
import com.guet.englishcheckin.mapper.LoginLogMapper;
import com.guet.englishcheckin.mapper.UserMapper;
import com.guet.englishcheckin.security.JwtUtil;
import com.guet.englishcheckin.util.TimeUtil;
import com.guet.englishcheckin.vo.LoginLogVO;
import com.guet.englishcheckin.vo.TokenVO;
import com.guet.englishcheckin.vo.UserProfileVO;
import com.guet.englishcheckin.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户模块服务：微信登录、个人资料、登录日志（IP 溯源）、学习统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final CheckinMapper checkinMapper;
    private final JwtUtil jwtUtil;

    private static final String WECHAT_APPID = "your_wechat_appid";
    private static final String WECHAT_SECRET = "your_wechat_secret";

    /**
     * 微信小程序登录（对应 Django UserViewSet.wechat_login）
     * 通过 code 换取 openid，自动注册/登录；微信接口失败时使用 mock openid（开发模式）
     */
    public Map<String, Object> wechatLogin(WechatLoginRequest req, String ip, String deviceInfo) {
        String openid = exchangeOpenid(req.getCode());

        // 获取或创建用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
        boolean created = false;
        LocalDateTime now = TimeUtil.nowUtc();
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(truncate(StringUtils.hasText(req.getNickname()) ? req.getNickname() : "微信用户", 64));
            user.setAvatarUrl(truncate(req.getAvatarUrl(), 512));
            user.setGender(0);
            user.setStatus(1);
            user.setTotalWords(0);
            user.setTotalDays(0);
            user.setMaxContinuous(0);
            user.setLastLoginIp(truncate(ip, 64));
            user.setCreatedAt(now);
            user.setLastLoginAt(now);
            user.setUpdatedAt(now);
            userMapper.insert(user);
            created = true;
        } else {
            if (StringUtils.hasText(req.getNickname())) {
                user.setNickname(truncate(req.getNickname(), 64));
            }
            if (StringUtils.hasText(req.getAvatarUrl())) {
                user.setAvatarUrl(truncate(req.getAvatarUrl(), 512));
            }
            user.setLastLoginIp(truncate(ip, 64));
            user.setLastLoginAt(now);
            userMapper.updateById(user);
        }

        // 记录登录日志（IP 溯源 + 异常登录检测）
        LoginLog loginLog = buildLoginLog(user, ip, deviceInfo);
        loginLogMapper.insert(loginLog);

        // 返回 token + 用户信息
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", new TokenVO(
                jwtUtil.generateAccessToken(user.getId(), user.getOpenid()),
                jwtUtil.generateRefreshToken(user.getId(), user.getOpenid())));
        data.put("user", buildProfileMap(user, created));
        return data;
    }

    /**
     * 调用微信 jscode2session 换取 openid；失败/无 openid 时返回 mock_openid（与 Django 一致）
     */
    private String exchangeOpenid(String code) {
        try {
            String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + WECHAT_APPID
                    + "&secret=" + WECHAT_SECRET + "&js_code=" + code + "&grant_type=authorization_code";
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            // 简易解析 openid 字段
            String openid = extractJsonString(body, "openid");
            if (openid != null && !openid.isBlank()) {
                return openid;
            }
            log.warn("微信登录未返回 openid: {}", body);
        } catch (Exception e) {
            log.warn("微信接口调用失败: {}, 使用 mock 模式", e.getMessage());
        }
        return "mock_openid_" + code.substring(0, Math.min(16, code.length()));
    }

    private String extractJsonString(String json, String key) {
        if (json == null) {
            return null;
        }
        String pattern = "\"" + key + "\"\\s*:\\s*\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            int start = m.end();
            int end = json.indexOf('"', start);
            if (end > start) {
                return json.substring(start, end);
            }
        }
        return null;
    }

    /**
     * 构建登录日志并检测异常登录（对比历史最近 10 条 IP）
     * 注意：微信小程序开发工具 User-Agent 可超 256 字符，入库前必须截断，
     * 否则 MySQL 严格模式抛 Data truncation 导致 500（Django 版靠放宽 sql_mode 掩盖了该问题）。
     */
    private LoginLog buildLoginLog(User user, String ip, String deviceInfo) {
        LoginLog log = new LoginLog();
        log.setUserId(user.getId());
        log.setIpAddress(truncate(ip, 64));
        log.setDeviceInfo(truncate(deviceInfo, 256));
        log.setLocation("");
        log.setLoginTime(TimeUtil.nowUtc());
        log.setCreatedAt(TimeUtil.nowUtc());

        List<LoginLog> recent = loginLogMapper.selectList(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getUserId, user.getId())
                .orderByDesc(LoginLog::getLoginTime)
                .last("LIMIT 10"));
        Set<String> recentIps = recent.stream()
                .map(LoginLog::getIpAddress)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toSet());
        if (!recentIps.isEmpty() && recentIps.size() >= 2 && !recentIps.contains(ip)) {
            log.setIsAbnormal(1);
            String joined = recentIps.stream().limit(3).collect(Collectors.joining(","));
            log.setAbnormalReason("检测到新设备/新地点登录，历史常用IP: " + joined);
        } else {
            log.setIsAbnormal(0);
            log.setAbnormalReason("");
        }
        return log;
    }

    /** 字符串长度截断，避免超长字段触发 MySQL 严格模式报错 */
    private String truncate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    /**
     * 获取当前用户个人资料（含实时连续打卡天数）
     */
    public UserProfileVO getProfile(Long userId) {
        User user = getUserOrThrow(userId);
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setGender(user.getGender());
        vo.setTotalWords(user.getTotalWords());
        vo.setTotalDays(user.getTotalDays());
        vo.setMaxContinuous(user.getMaxContinuous());
        vo.setDailyGoal(user.getDailyGoal() == null ? 20 : user.getDailyGoal());
        vo.setContinuousDays(calcContinuousDays(userId, TimeUtil.todayShanghai()));
        vo.setLastLoginAt(user.getLastLoginAt());
        return vo;
    }

    /**
     * 更新个人信息（仅 nickname / avatar_url / gender）
     */
    public UserProfileVO updateProfile(Long userId, UpdateProfileRequest req) {
        User user = getUserOrThrow(userId);
        if (req.getNickname() != null) {
            user.setNickname(req.getNickname());
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl());
        }
        if (req.getGender() != null) {
            user.setGender(req.getGender());
        }
        if (req.getDailyGoal() != null && req.getDailyGoal() > 0) {
            user.setDailyGoal(req.getDailyGoal());
        }
        user.setUpdatedAt(TimeUtil.nowUtc());
        userMapper.updateById(user);
        return getProfile(userId);
    }

    /**
     * 获取当前用户登录日志（最近 20 条）
     */
    public List<LoginLogVO> getLoginLogs(Long userId) {
        List<LoginLog> logs = loginLogMapper.selectList(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getUserId, userId)
                .orderByDesc(LoginLog::getLoginTime)
                .last("LIMIT 20"));
        Map<Long, String> nicknameMap = loadNicknames(logs);
        return logs.stream().map(l -> toLoginLogVO(l, nicknameMap)).toList();
    }

    /**
     * 批量加载日志对应用户昵称
     */
    public Map<Long, String> loadNicknames(List<LoginLog> logs) {
        Set<Long> userIds = logs.stream().map(LoginLog::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> map = new java.util.HashMap<>();
        for (User u : users) {
            map.put(u.getId(), u.getNickname());
        }
        return map;
    }

    /**
     * 学习统计数据
     */
    public Map<String, Object> getStats(Long userId) {
        User user = getUserOrThrow(userId);
        LocalDate today = TimeUtil.todayShanghai();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        LocalDate monthStart = today.withDayOfMonth(1);

        boolean todayCheckin = checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId).eq(Checkin::getCheckinDate, today)) > 0;
        long weeklyCheckins = checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId).ge(Checkin::getCheckinDate, weekStart));
        long monthlyCheckins = checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId).ge(Checkin::getCheckinDate, monthStart));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total_words", user.getTotalWords());
        data.put("total_days", user.getTotalDays());
        data.put("max_continuous", user.getMaxContinuous());
        data.put("today_checkin", todayCheckin);
        data.put("weekly_checkins", weeklyCheckins);
        data.put("monthly_checkins", monthlyCheckins);
        return data;
    }

    /**
     * 用户列表（管理端）
     */
    public List<UserVO> listUsers() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
        return users.stream().map(this::toUserVO).toList();
    }

    /**
     * 创建用户（管理端 / DRF create 兼容）
     */
    public UserVO createUser(User user) {
        if (user.getOpenid() == null || user.getOpenid().isBlank()) {
            throw new BusinessException(400, "openid 不能为空");
        }
        LocalDateTime now = TimeUtil.nowUtc();
        user.setNickname(user.getNickname() == null ? "微信用户" : user.getNickname());
        user.setAvatarUrl(user.getAvatarUrl() == null ? "" : user.getAvatarUrl());
        user.setGender(user.getGender() == null ? 0 : user.getGender());
        user.setStatus(user.getStatus() == null ? 1 : user.getStatus());
        user.setTotalWords(user.getTotalWords() == null ? 0 : user.getTotalWords());
        user.setTotalDays(user.getTotalDays() == null ? 0 : user.getTotalDays());
        user.setMaxContinuous(user.getMaxContinuous() == null ? 0 : user.getMaxContinuous());
        user.setCreatedAt(now);
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return toUserVO(user);
    }

    /**
     * 连续打卡天数：从今天（东八区）往前数连续存在打卡记录的天数
     */
    public int calcContinuousDays(Long userId, LocalDate today) {
        List<Checkin> checkins = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId));
        Set<LocalDate> dates = new HashSet<>();
        for (Checkin c : checkins) {
            if (c.getCheckinDate() != null) {
                dates.add(c.getCheckinDate());
            }
        }
        int continuous = 0;
        for (int i = 0; i < 365; i++) {
            if (dates.contains(today.minusDays(i))) {
                continuous++;
            } else {
                break;
            }
        }
        return continuous;
    }

    private User getUserOrThrow(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private Map<String, Object> buildProfileMap(User user, boolean isNewUser) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("nickname", user.getNickname());
        m.put("avatar_url", user.getAvatarUrl());
        m.put("gender", user.getGender());
        m.put("total_words", user.getTotalWords());
        m.put("total_days", user.getTotalDays());
        m.put("max_continuous", user.getMaxContinuous());
        m.put("daily_goal", user.getDailyGoal() == null ? 20 : user.getDailyGoal());
        m.put("continuous_days", calcContinuousDays(user.getId(), TimeUtil.todayShanghai()));
        m.put("last_login_at", user.getLastLoginAt());
        m.put("is_new_user", isNewUser);
        return m;
    }

    private LoginLogVO toLoginLogVO(LoginLog log, Map<Long, String> nicknameMap) {
        LoginLogVO vo = new LoginLogVO();
        vo.setId(log.getId());
        vo.setUser(log.getUserId());
        vo.setNickname(nicknameMap == null ? null : nicknameMap.get(log.getUserId()));
        vo.setIpAddress(log.getIpAddress());
        vo.setDeviceInfo(log.getDeviceInfo());
        vo.setLocation(log.getLocation());
        vo.setLoginTime(log.getLoginTime());
        vo.setIsAbnormal(log.getIsAbnormal());
        vo.setAbnormalReason(log.getAbnormalReason());
        return vo;
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setOpenid(user.getOpenid());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setGender(user.getGender());
        vo.setStatus(user.getStatus());
        vo.setTotalWords(user.getTotalWords());
        vo.setTotalDays(user.getTotalDays());
        vo.setMaxContinuous(user.getMaxContinuous());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setLastLoginAt(user.getLastLoginAt());
        return vo;
    }
}
