package com.knowledgebase.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledgebase.common.BusinessException;
import com.knowledgebase.config.JwtUtil;
import com.knowledgebase.entity.SysUser;
import com.knowledgebase.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(String username, String password) {
        log.debug("用户登录尝试: username={}", username);
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            log.warn("登录失败-用户不存在: username={}", username);
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            log.warn("登录失败-账号被禁用: username={}, userId={}", username, user.getId());
            throw new BusinessException(403, "账号已被禁用");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            log.warn("登录失败-密码错误: username={}, userId={}", username, user.getId());
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        log.info("用户登录成功: username={}, userId={}", username, user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    public SysUser getUserInfo(Long userId) {
        log.debug("获取用户信息: userId={}", userId);
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("获取用户信息失败-用户不存在: userId={}", userId);
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }
}
