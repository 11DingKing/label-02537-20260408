package com.knowledgebase.controller;

import com.knowledgebase.BaseTest;
import com.knowledgebase.entity.SysOperationLog;
import com.knowledgebase.mapper.SysOperationLogMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("操作日志接口测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LogControllerTest extends BaseTest {

    @Autowired
    private SysOperationLogMapper logMapper;

    private String token;

    @BeforeEach
    void setUp() {
        logMapper.delete(null);
        token = getAdminToken(1L, "admin");

        insertLog("admin", "用户管理", "新增用户", 1);
        insertLog("admin", "知识库", "创建知识库", 1);
        insertLog("admin", "文档管理", "上传文档", 0);
    }

    @Test
    @Order(1)
    @DisplayName("分页查询操作日志")
    void testPageLogs() throws Exception {
        mockMvc.perform(get("/api/logs")
                        .header("Authorization", token)
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    @Order(2)
    @DisplayName("按关键词搜索日志")
    void testSearchLogs() throws Exception {
        mockMvc.perform(get("/api/logs")
                        .header("Authorization", token)
                        .param("keyword", "知识库"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @Order(3)
    @DisplayName("按操作人搜索日志")
    void testSearchByUsername() throws Exception {
        mockMvc.perform(get("/api/logs")
                        .header("Authorization", token)
                        .param("keyword", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    @Order(4)
    @DisplayName("按操作类型搜索日志")
    void testSearchByOperation() throws Exception {
        mockMvc.perform(get("/api/logs")
                        .header("Authorization", token)
                        .param("keyword", "上传"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @Order(5)
    @DisplayName("无匹配关键词返回空")
    void testSearchNoMatch() throws Exception {
        mockMvc.perform(get("/api/logs")
                        .header("Authorization", token)
                        .param("keyword", "ZZZZNOTEXIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @Order(6)
    @DisplayName("分页参数正确")
    void testPagination() throws Exception {
        mockMvc.perform(get("/api/logs")
                        .header("Authorization", token)
                        .param("pageNum", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(2));
    }

    @Test
    @Order(7)
    @DisplayName("日志记录包含完整字段")
    void testLogFields() throws Exception {
        mockMvc.perform(get("/api/logs")
                        .header("Authorization", token)
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].username").isNotEmpty())
                .andExpect(jsonPath("$.data.records[0].module").isNotEmpty())
                .andExpect(jsonPath("$.data.records[0].operation").isNotEmpty())
                .andExpect(jsonPath("$.data.records[0].ip").isNotEmpty())
                .andExpect(jsonPath("$.data.records[0].status").isNumber())
                .andExpect(jsonPath("$.data.records[0].duration").isNumber());
    }

    @Test
    @Order(8)
    @DisplayName("未登录访问日志-返回401")
    void testUnauthorized() throws Exception {
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    private void insertLog(String username, String module, String operation, int status) {
        SysOperationLog log = new SysOperationLog();
        log.setUserId(1L);
        log.setUsername(username);
        log.setModule(module);
        log.setOperation(operation);
        log.setMethod("TestMethod");
        log.setIp("127.0.0.1");
        log.setStatus(status);
        log.setDuration(100L);
        logMapper.insert(log);
    }
}
