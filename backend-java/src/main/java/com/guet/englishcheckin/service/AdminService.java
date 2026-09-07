package com.guet.englishcheckin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guet.englishcheckin.common.BusinessException;
import com.guet.englishcheckin.entity.Admin;
import com.guet.englishcheckin.entity.Checkin;
import com.guet.englishcheckin.entity.LoginLog;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.mapper.AdminMapper;
import com.guet.englishcheckin.mapper.CheckinMapper;
import com.guet.englishcheckin.mapper.LoginLogMapper;
import com.guet.englishcheckin.mapper.UserMapper;
import com.guet.englishcheckin.util.TimeUtil;
import com.guet.englishcheckin.vo.LoginLogVO;
import com.guet.englishcheckin.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员模块服务：后台登录、看板统计、用户管理、全量登录日志
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminMapper adminMapper;
    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final CheckinMapper checkinMapper;
    private final UserService userService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 管理员登录（BCrypt 校验，与 Django bcrypt 兼容；$2b$ 前缀支持）
     */
    public Map<String, Object> login(String username, String password, String ip) {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username)
                .eq(Admin::getStatus, 1));
        if (admin == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        admin.setLastLoginAt(TimeUtil.nowUtc());
        admin.setLastLoginIp(ip == null || ip.length() <= 64 ? ip : ip.substring(0, 64));
        adminMapper.updateById(admin);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", admin.getId());
        data.put("username", admin.getUsername());
        data.put("role", admin.getRole());
        return data;
    }

    /**
     * 管理后台首页数据（对应 Django AdminViewSet.dashboard）
     */
    public Map<String, Object> dashboard() {
        LocalDate today = TimeUtil.todayShanghai();
        long totalUsers = userMapper.selectCount(null);
        long todayCheckins = checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getCheckinDate, today));
        long totalWordsLearned = userMapper.selectSumTotalWords() == null ? 0L : userMapper.selectSumTotalWords();
        long abnormalLogins = loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getIsAbnormal, 1));
        // 今日活跃用户数 = 今日有打卡记录的去重用户数
        long todayActiveUsers = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                        .eq(Checkin::getCheckinDate, today)
                        .select(Checkin::getUserId))
                .stream().map(Checkin::getUserId).distinct().count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total_users", totalUsers);
        data.put("today_checkins", todayCheckins);
        data.put("total_words_learned", totalWordsLearned);
        data.put("abnormal_logins", abnormalLogins);
        data.put("today_active_users", todayActiveUsers);
        return data;
    }

    /**
     * 用户列表（管理端）
     */
    public List<UserVO> userList() {
        return userService.listUsers();
    }

    /**
     * 全量登录日志（最近 100 条，含异常标记）
     */
    public List<LoginLogVO> loginLogList() {
        List<LoginLog> logs = loginLogMapper.selectList(new LambdaQueryWrapper<LoginLog>()
                .orderByDesc(LoginLog::getLoginTime)
                .last("LIMIT 100"));
        Map<Long, String> nicknameMap = userService.loadNicknames(logs);
        List<LoginLogVO> result = logs.stream()
                .map(l -> toLoginLogVO(l, nicknameMap))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * 禁用/启用用户（对应 Django toggle_user_status）
     */
    public String toggleUserStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus(user.getStatus() != null && user.getStatus() == 1 ? 0 : 1);
        userMapper.updateById(user);
        return "用户状态已更新为" + (user.getStatus() == 1 ? "正常" : "禁用");
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
}
