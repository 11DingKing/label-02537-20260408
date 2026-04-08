package com.knowledgebase.controller;

import com.knowledgebase.BaseTest;
import com.knowledgebase.entity.KbKnowledgeBase;
import com.knowledgebase.mapper.KbKnowledgeBaseMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("知识库管理接口测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KnowledgeBaseControllerTest extends BaseTest {

    @Autowired
    private KbKnowledgeBaseMapper kbMapper;

    private String token;

    @BeforeEach
    void setUp() {
        kbMapper.delete(null);
        token = getAdminToken(1L, "admin");
    }

    @Test
    @Order(1)
    @DisplayName("创建知识库")
    void testCreate() throws Exception {
        mockMvc.perform(post("/api/kb")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试知识库\",\"description\":\"用于测试\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(2)
    @DisplayName("创建知识库-名称为空校验")
    void testCreateValidation() throws Exception {
        mockMvc.perform(post("/api/kb")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(3)
    @DisplayName("分页查询知识库")
    void testPage() throws Exception {
        insertKb("知识库A", "描述A");
        insertKb("知识库B", "描述B");

        mockMvc.perform(get("/api/kb")
                        .header("Authorization", token)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    @Order(4)
    @DisplayName("关键词搜索知识库")
    void testSearch() throws Exception {
        insertKb("Java基础", "Java相关");
        insertKb("Python入门", "Python相关");

        mockMvc.perform(get("/api/kb")
                        .header("Authorization", token)
                        .param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @Order(5)
    @DisplayName("获取知识库详情")
    void testDetail() throws Exception {
        KbKnowledgeBase kb = insertKb("详情测试", "测试描述");

        mockMvc.perform(get("/api/kb/" + kb.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("详情测试"));
    }

    @Test
    @Order(6)
    @DisplayName("获取不存在的知识库")
    void testDetailNotFound() throws Exception {
        mockMvc.perform(get("/api/kb/99999")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("知识库不存在"));
    }

    @Test
    @Order(7)
    @DisplayName("修改知识库")
    void testUpdate() throws Exception {
        KbKnowledgeBase kb = insertKb("原名称", "原描述");

        mockMvc.perform(put("/api/kb/" + kb.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新名称\",\"description\":\"新描述\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/kb/" + kb.getId())
                        .header("Authorization", token))
                .andExpect(jsonPath("$.data.name").value("新名称"));
    }

    @Test
    @Order(8)
    @DisplayName("删除知识库")
    void testDelete() throws Exception {
        KbKnowledgeBase kb = insertKb("待删除", "");

        mockMvc.perform(delete("/api/kb/" + kb.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/kb/" + kb.getId())
                        .header("Authorization", token))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @Order(9)
    @DisplayName("删除不存在的知识库")
    void testDeleteNotFound() throws Exception {
        mockMvc.perform(delete("/api/kb/99999")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("知识库不存在"));
    }

    @Test
    @Order(10)
    @DisplayName("修改不存在的知识库")
    void testUpdateNotFound() throws Exception {
        mockMvc.perform(put("/api/kb/99999")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"xxx\",\"description\":\"xxx\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("知识库不存在"));
    }

    @Test
    @Order(11)
    @DisplayName("未登录访问知识库-返回401")
    void testUnauthorized() throws Exception {
        mockMvc.perform(get("/api/kb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @Order(12)
    @DisplayName("创建知识库-描述超长校验")
    void testCreateDescriptionTooLong() throws Exception {
        String longDesc = "a".repeat(501);
        mockMvc.perform(post("/api/kb")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试\",\"description\":\"" + longDesc + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    private KbKnowledgeBase insertKb(String name, String desc) {
        KbKnowledgeBase kb = new KbKnowledgeBase();
        kb.setName(name);
        kb.setDescription(desc);
        kb.setUserId(1L);
        kb.setDocCount(0);
        kb.setChunkCount(0);
        kb.setStatus(1);
        kbMapper.insert(kb);
        return kb;
    }
}
