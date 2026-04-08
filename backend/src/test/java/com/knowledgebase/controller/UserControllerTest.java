package com.knowledgebase.controller;

import cn.hutool.crypto.digest.BCrypt;
import com.knowledgebase.BaseTest;
import com.knowledgebase.entity.SysUser;
import com.knowledgebase.mapper.SysUserMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("用户管理接口测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerTest extends BaseTest {

    @Autowired
    private SysUserMapper userMapper;

    private String token;
    private Long adminId;

    @BeforeEach
    void setUp() {
        userMapper.delete(null);
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("admin123"));
        admin.setNickname("管理员");
        admin.setStatus(1);
        userMapper.insert(admin);
        adminId = admin.getId();
        token = getAdminToken(adminId, "admin");
    }

    @Test
    @Order(1)
    @DisplayName("分页查询用户列表")
    void testPageUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", token)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @Order(2)
    @DisplayName("关键词搜索用户")
    void testSearchUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", token)
                        .param("keyword", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", token)
                        .param("keyword", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @Order(3)
    @DisplayName("新增用户")
    void testCreateUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"test123\",\"nickname\":\"测试用户\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", token))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    @Order(4)
    @DisplayName("新增用户-用户名重复")
    void testCreateDuplicateUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    @Order(5)
    @DisplayName("新增用户-参数校验失败")
    void testCreateUserValidation() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(6)
    @DisplayName("修改用户")
    void testUpdateUser() throws Exception {
        mockMvc.perform(put("/api/users/" + adminId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"nickname\":\"超级管理员\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(7)
    @DisplayName("修改用户-用户不存在")
    void testUpdateNonExistentUser() throws Exception {
        mockMvc.perform(put("/api/users/99999")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"xxx\",\"nickname\":\"xxx\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    @Order(8)
    @DisplayName("更新用户状态")
    void testUpdateStatus() throws Exception {
        SysUser user = new SysUser();
        user.setUsername("user2");
        user.setPassword(BCrypt.hashpw("123456"));
        user.setNickname("用户2");
        user.setStatus(1);
        userMapper.insert(user);

        mockMvc.perform(put("/api/users/" + user.getId() + "/status")
                        .header("Authorization", token)
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(9)
    @DisplayName("删除用户")
    void testDeleteUser() throws Exception {
        SysUser user = new SysUser();
        user.setUsername("todelete");
        user.setPassword(BCrypt.hashpw("123456"));
        user.setNickname("待删除");
        user.setStatus(1);
        userMapper.insert(user);

        mockMvc.perform(delete("/api/users/" + user.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(10)
    @DisplayName("不能删除ID为1的超级管理员")
    void testCannotDeleteSuperAdmin() throws Exception {
        mockMvc.perform(delete("/api/users/1")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("不能删除超级管理员"));
    }

    @Test
    @Order(11)
    @DisplayName("不能禁用ID为1的超级管理员")
    void testCannotDisableSuperAdmin() throws Exception {
        mockMvc.perform(put("/api/users/1/status")
                        .header("Authorization", token)
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("不能禁用超级管理员"));
    }

    @Test
    @Order(12)
    @DisplayName("未登录访问用户列表-返回401")
    void testUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @Order(13)
    @DisplayName("新增用户-用户名过短")
    void testCreateUsernameTooShort() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ab\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(14)
    @DisplayName("分页查询-自定义分页参数")
    void testPageCustomParams() throws Exception {
        for (int i = 0; i < 5; i++) {
            SysUser user = new SysUser();
            user.setUsername("batch" + i);
            user.setPassword(BCrypt.hashpw("123456"));
            user.setNickname("批量用户" + i);
            user.setStatus(1);
            userMapper.insert(user);
        }

        mockMvc.perform(get("/api/users")
                        .header("Authorization", token)
                        .param("pageNum", "1")
                        .param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(6))
                .andExpect(jsonPath("$.data.records.length()").value(3));
    }
}
