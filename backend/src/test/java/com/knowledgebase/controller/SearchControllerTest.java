package com.knowledgebase.controller;

import com.knowledgebase.BaseTest;
import com.knowledgebase.entity.KbKnowledgeBase;
import com.knowledgebase.entity.KbKnowledgeChunk;
import com.knowledgebase.mapper.KbKnowledgeBaseMapper;
import com.knowledgebase.mapper.KbKnowledgeChunkMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("知识检索接口测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SearchControllerTest extends BaseTest {

    @Autowired
    private KbKnowledgeBaseMapper kbMapper;
    @Autowired
    private KbKnowledgeChunkMapper chunkMapper;

    private String token;
    private Long kbId;

    @BeforeEach
    void setUp() {
        chunkMapper.delete(null);
        kbMapper.delete(null);
        token = getAdminToken(1L, "admin");

        KbKnowledgeBase kb = new KbKnowledgeBase();
        kb.setName("测试知识库");
        kb.setUserId(1L);
        kb.setDocCount(0);
        kb.setChunkCount(0);
        kb.setStatus(1);
        kbMapper.insert(kb);
        kbId = kb.getId();

        insertChunk("Java是一种面向对象的编程语言", 0, "段落");
        insertChunk("Spring Boot是Java的微服务框架", 1, "段落");
        insertChunk("Python是一种解释型语言", 2, "段落");
    }

    @Test
    @Order(1)
    @DisplayName("关键词搜索-LIKE降级查询")
    void testSearch() throws Exception {
        mockMvc.perform(get("/api/search")
                        .header("Authorization", token)
                        .param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("指定知识库搜索")
    void testSearchInKb() throws Exception {
        mockMvc.perform(get("/api/search")
                        .header("Authorization", token)
                        .param("keyword", "Python")
                        .param("kbId", kbId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("空关键词返回空结果")
    void testSearchEmpty() throws Exception {
        mockMvc.perform(get("/api/search")
                        .header("Authorization", token)
                        .param("keyword", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("搜索不存在的内容")
    void testSearchNoResult() throws Exception {
        mockMvc.perform(get("/api/search")
                        .header("Authorization", token)
                        .param("keyword", "ZZZZNOTEXIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("搜索-自定义limit参数")
    void testSearchWithLimit() throws Exception {
        mockMvc.perform(get("/api/search")
                        .header("Authorization", token)
                        .param("keyword", "Java")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(6)
    @DisplayName("搜索-未登录返回401")
    void testSearchUnauthorized() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @Order(7)
    @DisplayName("搜索结果包含完整字段")
    void testSearchResultFields() throws Exception {
        mockMvc.perform(get("/api/search")
                        .header("Authorization", token)
                        .param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].chunkId").isNumber())
                .andExpect(jsonPath("$.data[0].content").isString())
                .andExpect(jsonPath("$.data[0].chunkIndex").isNumber())
                .andExpect(jsonPath("$.data[0].knowledgeBaseId").isNumber())
                .andExpect(jsonPath("$.data[0].knowledgeBaseName").value("测试知识库"));
    }

    private void insertChunk(String content, int index, String source) {
        KbKnowledgeChunk chunk = new KbKnowledgeChunk();
        chunk.setDocId(1L);
        chunk.setKbId(kbId);
        chunk.setContent(content);
        chunk.setChunkIndex(index);
        chunk.setSourceInfo(source);
        chunkMapper.insert(chunk);
    }
}
