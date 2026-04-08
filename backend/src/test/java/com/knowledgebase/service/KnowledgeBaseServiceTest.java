package com.knowledgebase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledgebase.BaseTest;
import com.knowledgebase.common.BusinessException;
import com.knowledgebase.dto.KbDTO;
import com.knowledgebase.entity.KbDocument;
import com.knowledgebase.entity.KbKnowledgeBase;
import com.knowledgebase.entity.KbKnowledgeChunk;
import com.knowledgebase.mapper.KbDocumentMapper;
import com.knowledgebase.mapper.KbKnowledgeBaseMapper;
import com.knowledgebase.mapper.KbKnowledgeChunkMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KnowledgeBaseService 知识库服务测试")
class KnowledgeBaseServiceTest extends BaseTest {

    @Autowired
    private KnowledgeBaseService kbService;
    @Autowired
    private KbKnowledgeBaseMapper kbMapper;
    @Autowired
    private KbDocumentMapper docMapper;
    @Autowired
    private KbKnowledgeChunkMapper chunkMapper;

    @BeforeEach
    void setUp() {
        chunkMapper.delete(null);
        docMapper.delete(null);
        kbMapper.delete(null);
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

    @Test
    @DisplayName("分页查询-无关键词")
    void testPage() {
        insertKb("知识库A", "描述A");
        insertKb("知识库B", "描述B");
        Page<KbKnowledgeBase> page = kbService.page(1, 10, null);
        assertEquals(2, page.getTotal());
    }

    @Test
    @DisplayName("分页查询-关键词搜索")
    void testPageWithKeyword() {
        insertKb("Java知识库", "Java相关");
        insertKb("Python知识库", "Python相关");
        Page<KbKnowledgeBase> page = kbService.page(1, 10, "Java");
        assertEquals(1, page.getTotal());
    }

    @Test
    @DisplayName("获取详情-成功")
    void testGetById() {
        KbKnowledgeBase kb = insertKb("测试", "描述");
        KbKnowledgeBase result = kbService.getById(kb.getId());
        assertEquals("测试", result.getName());
    }

    @Test
    @DisplayName("获取详情-不存在")
    void testGetByIdNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> kbService.getById(99999L));
        assertEquals("知识库不存在", ex.getMessage());
    }

    @Test
    @DisplayName("创建知识库")
    void testCreate() {
        KbDTO dto = new KbDTO();
        dto.setName("新知识库");
        dto.setDescription("新描述");
        kbService.create(dto, 1L);

        Page<KbKnowledgeBase> page = kbService.page(1, 10, null);
        assertEquals(1, page.getTotal());
        assertEquals("新知识库", page.getRecords().get(0).getName());
    }

    @Test
    @DisplayName("更新知识库")
    void testUpdate() {
        KbKnowledgeBase kb = insertKb("原名称", "原描述");
        KbDTO dto = new KbDTO();
        dto.setName("新名称");
        dto.setDescription("新描述");
        kbService.update(kb.getId(), dto);

        KbKnowledgeBase updated = kbService.getById(kb.getId());
        assertEquals("新名称", updated.getName());
        assertEquals("新描述", updated.getDescription());
    }

    @Test
    @DisplayName("更新知识库-不存在")
    void testUpdateNotFound() {
        KbDTO dto = new KbDTO();
        dto.setName("xxx");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> kbService.update(99999L, dto));
        assertEquals("知识库不存在", ex.getMessage());
    }

    @Test
    @DisplayName("删除知识库-级联删除文档和知识块")
    void testDeleteCascade() {
        KbKnowledgeBase kb = insertKb("待删除", "");

        // 插入关联文档
        KbDocument doc = new KbDocument();
        doc.setKbId(kb.getId());
        doc.setFilename("test.docx");
        doc.setOriginalName("test.docx");
        doc.setFilePath("/tmp/test.docx");
        doc.setFileType("docx");
        doc.setFileSize(1024L);
        doc.setParseStatus(2);
        doc.setUserId(1L);
        docMapper.insert(doc);

        // 插入关联知识块
        KbKnowledgeChunk chunk = new KbKnowledgeChunk();
        chunk.setDocId(doc.getId());
        chunk.setKbId(kb.getId());
        chunk.setContent("测试内容");
        chunk.setChunkIndex(0);
        chunk.setSourceInfo("段落");
        chunkMapper.insert(chunk);

        kbService.delete(kb.getId());

        // 验证级联删除
        assertThrows(BusinessException.class, () -> kbService.getById(kb.getId()));
        assertEquals(0, docMapper.selectCount(null).intValue());
        assertEquals(0, chunkMapper.selectCount(null).intValue());
    }

    @Test
    @DisplayName("删除知识库-不存在")
    void testDeleteNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> kbService.delete(99999L));
        assertEquals("知识库不存在", ex.getMessage());
    }

    @Test
    @DisplayName("刷新知识库计数")
    void testRefreshCount() {
        KbKnowledgeBase kb = insertKb("测试", "");

        KbDocument doc = new KbDocument();
        doc.setKbId(kb.getId());
        doc.setFilename("test.docx");
        doc.setOriginalName("test.docx");
        doc.setFilePath("/tmp/test.docx");
        doc.setFileType("docx");
        doc.setFileSize(1024L);
        doc.setParseStatus(2);
        doc.setUserId(1L);
        docMapper.insert(doc);

        KbKnowledgeChunk chunk1 = new KbKnowledgeChunk();
        chunk1.setDocId(doc.getId());
        chunk1.setKbId(kb.getId());
        chunk1.setContent("内容1");
        chunk1.setChunkIndex(0);
        chunkMapper.insert(chunk1);

        KbKnowledgeChunk chunk2 = new KbKnowledgeChunk();
        chunk2.setDocId(doc.getId());
        chunk2.setKbId(kb.getId());
        chunk2.setContent("内容2");
        chunk2.setChunkIndex(1);
        chunkMapper.insert(chunk2);

        kbService.refreshCount(kb.getId());

        KbKnowledgeBase updated = kbMapper.selectById(kb.getId());
        assertEquals(1, updated.getDocCount());
        assertEquals(2, updated.getChunkCount());
    }

    @Test
    @DisplayName("分页查询-分页参数正确")
    void testPagePagination() {
        for (int i = 0; i < 15; i++) {
            insertKb("知识库" + i, "描述" + i);
        }
        Page<KbKnowledgeBase> page1 = kbService.page(1, 10, null);
        assertEquals(15, page1.getTotal());
        assertEquals(10, page1.getRecords().size());

        Page<KbKnowledgeBase> page2 = kbService.page(2, 10, null);
        assertEquals(5, page2.getRecords().size());
    }

    @Test
    @DisplayName("创建知识库-初始计数为0")
    void testCreateInitialCounts() {
        KbDTO dto = new KbDTO();
        dto.setName("新知识库");
        dto.setDescription("测试");
        kbService.create(dto, 1L);

        KbKnowledgeBase kb = kbMapper.selectList(null).get(0);
        assertEquals(0, kb.getDocCount());
        assertEquals(0, kb.getChunkCount());
        assertEquals(1, kb.getStatus());
    }

    @Test
    @DisplayName("刷新计数-无文档时为0")
    void testRefreshCountEmpty() {
        KbKnowledgeBase kb = insertKb("空知识库", "");
        kbService.refreshCount(kb.getId());

        KbKnowledgeBase updated = kbMapper.selectById(kb.getId());
        assertEquals(0, updated.getDocCount());
        assertEquals(0, updated.getChunkCount());
    }
}
