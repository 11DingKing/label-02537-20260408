package com.knowledgebase.aspect;

import cn.hutool.crypto.digest.BCrypt;
import com.knowledgebase.BaseTest;
import com.knowledgebase.entity.SysOperationLog;
import com.knowledgebase.entity.SysUser;
import com.knowledgebase.mapper.SysOperationLogMapper;
import com.knowledgebase.mapper.SysUserMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("OperationLogAspect 操作日志切面测试")
class OperationLogAspectTest extends BaseTest {

    @Autowired
    private SysOperationLogMapper logMapper;
    @Autowired
    private SysUserMapper userMapper;

    private String token;

    @BeforeEach
    void setUp() {
        logMapper.delete(null);
        userMapper.delete(null);

        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("admin123"));
        admin.setNickname("管理员");
        admin.setStatus(1);
        userMapper.insert(admin);
        token = getAdminToken(admin.getId(), "admin");
    }

    @Test
    @DisplayName("登录操作记录日志")
    void testLoginLogged() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        List<SysOperationLog> logs = logMapper.selectList(null);
        assertFalse(logs.isEmpty());
        SysOperationLog log = logs.get(0);
        assertEquals("认证", log.getModule());
        assertEquals("用户登录", log.getOperation());
        assertEquals(1, log.getStatus());
        assertNotNull(log.getDuration());
        assertTrue(log.getDuration() >= 0);
    }

    @Test
    @DisplayName("创建用户操作记录日志-包含用户信息")
    void testCreateUserLogged() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"pass123\",\"nickname\":\"新用户\"}"))
                .andExpect(status().isOk());

        List<SysOperationLog> logs = logMapper.selectList(null);
        assertTrue(logs.stream().anyMatch(l ->
                "用户管理".equals(l.getModule()) && "新增用户".equals(l.getOperation())));
    }

    @Test
    @DisplayName("操作失败也记录日志-status为0")
    void testFailedOperationLogged() throws Exception {
        // 创建重复用户名触发异常
        mockMvc.perform(post("/api/users")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        List<SysOperationLog> logs = logMapper.selectList(null);
        assertTrue(logs.stream().anyMatch(l ->
                l.getStatus() == 0 && l.getErrorMsg() != null));
    }

    @Test
    @DisplayName("日志记录包含请求参数")
    void testLogContainsParams() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk());

        List<SysOperationLog> logs = logMapper.selectList(null);
        assertFalse(logs.isEmpty());
        String params = logs.get(0).getParams();
        assertNotNull(params);
        assertTrue(params.contains("admin"));
    }

    @Test
    @DisplayName("日志记录包含方法信息")
    void testLogContainsMethod() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk());

        List<SysOperationLog> logs = logMapper.selectList(null);
        assertFalse(logs.isEmpty());
        assertNotNull(logs.get(0).getMethod());
        assertTrue(logs.get(0).getMethod().contains("AuthController"));
    }

    @Test
    @DisplayName("日志记录包含IP地址")
    void testLogContainsIp() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk());

        List<SysOperationLog> logs = logMapper.selectList(null);
        assertFalse(logs.isEmpty());
        assertNotNull(logs.get(0).getIp());
    }

    @Test
    @DisplayName("日志记录耗时大于等于0")
    void testLogDurationNonNegative() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk());

        List<SysOperationLog> logs = logMapper.selectList(null);
        assertFalse(logs.isEmpty());
        assertTrue(logs.get(0).getDuration() >= 0);
    }

    @Test
    @DisplayName("删除知识库操作记录日志")
    void testDeleteKbLogged() throws Exception {
        // 先创建知识库
        mockMvc.perform(post("/api/kb")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"待删除\",\"description\":\"测试\"}"))
                .andExpect(status().isOk());

        int logCountBefore = logMapper.selectList(null).size();

        // 删除不存在的知识库也会记录日志
        mockMvc.perform(delete("/api/kb/99999")
                        .header("Authorization", token))
                .andExpect(status().isOk());

        int logCountAfter = logMapper.selectList(null).size();
        assertTrue(logCountAfter > logCountBefore);
    }

    @Test
    @DisplayName("带Token的操作记录用户信息")
    void testLogContainsUserInfo() throws Exception {
        mockMvc.perform(post("/api/kb")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试知识库\",\"description\":\"测试\"}"))
                .andExpect(status().isOk());

        List<SysOperationLog> logs = logMapper.selectList(null);
        boolean hasUserInfo = logs.stream().anyMatch(l ->
                l.getUsername() != null && l.getUserId() != null);
        assertTrue(hasUserInfo);
    }
}
