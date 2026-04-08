package com.knowledgebase.service;

import cn.hutool.crypto.digest.BCrypt;
import com.knowledgebase.BaseTest;
import com.knowledgebase.common.BusinessException;
import com.knowledgebase.entity.SysUser;
import com.knowledgebase.mapper.SysUserMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthService 认证服务测试")
class AuthServiceTest extends BaseTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private SysUserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper.delete(null);
        SysUser user = new SysUser();
        user.setUsername("admin");
        user.setPassword(BCrypt.hashpw("admin123"));
        user.setNickname("管理员");
        user.setStatus(1);
        userMapper.insert(user);
    }

    @Test
    @DisplayName("登录成功-返回token和用户信息")
    void testLoginSuccess() {
        Map<String, Object> result = authService.login("admin", "admin123");
        assertNotNull(result.get("token"));
        assertNotNull(result.get("user"));
        SysUser user = (SysUser) result.get("user");
        assertEquals("admin", user.getUsername());
    }

    @Test
    @DisplayName("登录失败-用户不存在")
    void testLoginUserNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login("nobody", "123"));
        assertEquals(401, ex.getCode());
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("登录失败-密码错误")
    void testLoginWrongPassword() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login("admin", "wrong"));
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("登录失败-账号被禁用")
    void testLoginDisabledUser() {
        SysUser user = userMapper.selectList(null).get(0);
        user.setStatus(0);
        userMapper.updateById(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login("admin", "admin123"));
        assertEquals(403, ex.getCode());
        assertEquals("账号已被禁用", ex.getMessage());
    }

    @Test
    @DisplayName("获取用户信息-成功")
    void testGetUserInfo() {
        SysUser user = userMapper.selectList(null).get(0);
        SysUser info = authService.getUserInfo(user.getId());
        assertEquals("admin", info.getUsername());
        assertEquals("管理员", info.getNickname());
    }

    @Test
    @DisplayName("获取用户信息-用户不存在")
    void testGetUserInfoNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.getUserInfo(99999L));
        assertEquals(404, ex.getCode());
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    @DisplayName("登录返回的token有效")
    void testLoginTokenIsValid() {
        Map<String, Object> result = authService.login("admin", "admin123");
        String token = (String) result.get("token");
        assertTrue(jwtUtil.isValid(token));
        assertEquals("admin", jwtUtil.getUsername(token));
    }

    @Test
    @DisplayName("登录返回的用户信息不含密码")
    void testLoginUserInfoNoPassword() {
        Map<String, Object> result = authService.login("admin", "admin123");
        SysUser user = (SysUser) result.get("user");
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
        assertEquals("管理员", user.getNickname());
    }

    @Test
    @DisplayName("登录返回的token包含正确的userId")
    void testLoginTokenContainsUserId() {
        Map<String, Object> result = authService.login("admin", "admin123");
        String token = (String) result.get("token");
        SysUser user = (SysUser) result.get("user");
        assertEquals(user.getId(), jwtUtil.getUserId(token));
    }

    @Test
    @DisplayName("获取用户信息-返回正确的字段")
    void testGetUserInfoFields() {
        SysUser user = userMapper.selectList(null).get(0);
        SysUser info = authService.getUserInfo(user.getId());
        assertNotNull(info.getId());
        assertNotNull(info.getUsername());
        assertNotNull(info.getNickname());
        assertNotNull(info.getStatus());
    }
}
