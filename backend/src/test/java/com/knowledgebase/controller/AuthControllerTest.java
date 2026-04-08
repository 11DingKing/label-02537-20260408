package com.knowledgebase.controller;

import cn.hutool.crypto.digest.BCrypt;
import com.knowledgebase.BaseTest;
import com.knowledgebase.entity.SysUser;
import com.knowledgebase.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("认证接口测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest extends BaseTest {

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
    @Order(1)
    @DisplayName("登录成功")
    void testLoginSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("admin"));
    }

    @Test
    @Order(2)
    @DisplayName("登录失败-密码错误")
    void testLoginWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @Order(3)
    @DisplayName("登录失败-用户不存在")
    void testLoginUserNotFound() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @Order(4)
    @DisplayName("登录失败-参数校验")
    void testLoginValidation() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(5)
    @DisplayName("获取用户信息-需要Token")
    void testGetInfoWithToken() throws Exception {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin"));
        String token = getAdminToken(user.getId(), "admin");

        mockMvc.perform(get("/api/auth/info")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    @Order(6)
    @DisplayName("获取用户信息-无Token返回401")
    void testGetInfoWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @Order(7)
    @DisplayName("登录失败-账号被禁用")
    void testLoginDisabledUser() throws Exception {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin"));
        user.setStatus(0);
        userMapper.updateById(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("账号已被禁用"));
    }

    @Test
    @Order(8)
    @DisplayName("登录成功-返回token和用户信息")
    void testLoginResponseStructure() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.user").isMap())
                .andExpect(jsonPath("$.data.user.id").isNumber())
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.nickname").value("管理员"));
    }

    @Test
    @Order(9)
    @DisplayName("登录失败-缺少密码字段")
    void testLoginMissingPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(10)
    @DisplayName("登录失败-缺少用户名字段")
    void testLoginMissingUsername() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(11)
    @DisplayName("获取用户信息-Token中userId对应的用户被删除")
    void testGetInfoDeletedUser() throws Exception {
        // 使用一个不存在的userId生成token
        String token = getAdminToken(99999L, "ghost");
        mockMvc.perform(get("/api/auth/info")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
