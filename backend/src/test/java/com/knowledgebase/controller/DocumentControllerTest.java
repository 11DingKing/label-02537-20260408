package com.knowledgebase.controller;

import com.knowledgebase.BaseTest;
import com.knowledgebase.entity.KbDocument;
import com.knowledgebase.entity.KbKnowledgeBase;
import com.knowledgebase.entity.KbKnowledgeChunk;
import com.knowledgebase.mapper.KbDocumentMapper;
import com.knowledgebase.mapper.KbKnowledgeBaseMapper;
import com.knowledgebase.mapper.KbKnowledgeChunkMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("文档管理接口测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocumentControllerTest extends BaseTest {

    @Autowired
    private KbDocumentMapper docMapper;
    @Autowired
    private KbKnowledgeBaseMapper kbMapper;
    @Autowired
    private KbKnowledgeChunkMapper chunkMapper;

    private String token;
    private Long kbId;

    @BeforeEach
    void setUp() {
        chunkMapper.delete(null);
        docMapper.delete(null);
        kbMapper.delete(null);
        token = getAdminToken(1L, "admin");

        KbKnowledgeBase kb = new KbKnowledgeBase();
        kb.setName("测试知识库");
        kb.setDescription("测试用");
        kb.setUserId(1L);
        kb.setDocCount(0);
        kb.setChunkCount(0);
        kb.setStatus(1);
        kbMapper.insert(kb);
        kbId = kb.getId();
    }

    @Test
    @Order(1)
    @DisplayName("分页查询文档列表")
    void testPageDocuments() throws Exception {
        insertDoc("test.docx", "docx");

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", token)
                        .param("kbId", kbId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @Order(2)
    @DisplayName("按关键词搜索文档")
    void testSearchDocuments() throws Exception {
        insertDoc("java基础.docx", "docx");
        insertDoc("python入门.xlsx", "xlsx");

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", token)
                        .param("keyword", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @Order(3)
    @DisplayName("上传文档-不支持的文件类型")
    void testUploadUnsupportedType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("kbId", kbId.toString())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @Order(4)
    @DisplayName("上传文档-知识库不存在")
    void testUploadToNonExistentKb() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.docx", "application/octet-stream", "content".getBytes());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("kbId", "99999")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("知识库不存在"));
    }

    @Test
    @Order(5)
    @DisplayName("查看文档知识块")
    void testGetChunks() throws Exception {
        KbDocument doc = insertDoc("test.docx", "docx");

        KbKnowledgeChunk chunk = new KbKnowledgeChunk();
        chunk.setDocId(doc.getId());
        chunk.setKbId(kbId);
        chunk.setContent("这是测试知识内容");
        chunk.setChunkIndex(0);
        chunk.setSourceInfo("段落");
        chunkMapper.insert(chunk);

        mockMvc.perform(get("/api/documents/" + doc.getId() + "/chunks")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].content").value("这是测试知识内容"));
    }

    @Test
    @Order(6)
    @DisplayName("删除文档")
    void testDeleteDocument() throws Exception {
        KbDocument doc = insertDoc("todelete.docx", "docx");

        mockMvc.perform(delete("/api/documents/" + doc.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", token)
                        .param("kbId", kbId.toString()))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @Order(7)
    @DisplayName("删除不存在的文档")
    void testDeleteNonExistent() throws Exception {
        mockMvc.perform(delete("/api/documents/99999")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("文档不存在"));
    }

    @Test
    @Order(8)
    @DisplayName("重新解析不存在的文档")
    void testReparseNonExistent() throws Exception {
        mockMvc.perform(post("/api/documents/99999/reparse")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("文档不存在"));
    }

    @Test
    @Order(9)
    @DisplayName("未登录访问文档列表-返回401")
    void testUnauthorized() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @Order(10)
    @DisplayName("查看空文档的知识块-返回空数组")
    void testGetChunksEmpty() throws Exception {
        KbDocument doc = insertDoc("empty.docx", "docx");

        mockMvc.perform(get("/api/documents/" + doc.getId() + "/chunks")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @Order(11)
    @DisplayName("分页查询-默认参数")
    void testPageDefaultParams() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @Order(12)
    @DisplayName("上传文档-文件名为空")
    void testUploadEmptyFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "", "application/octet-stream", "content".getBytes());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("kbId", kbId.toString())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    private KbDocument insertDoc(String name, String type) {
        KbDocument doc = new KbDocument();
        doc.setKbId(kbId);
        doc.setFilename("uuid." + type);
        doc.setOriginalName(name);
        doc.setFilePath("/tmp/test/" + name);
        doc.setFileType(type);
        doc.setFileSize(1024L);
        doc.setParseStatus(2);
        doc.setUserId(1L);
        docMapper.insert(doc);
        return doc;
    }
}
