package com.knowledgebase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledgebase.BaseTest;
import com.knowledgebase.common.BusinessException;
import com.knowledgebase.entity.KbDocument;
import com.knowledgebase.entity.KbKnowledgeBase;
import com.knowledgebase.entity.KbKnowledgeChunk;
import com.knowledgebase.mapper.KbDocumentMapper;
import com.knowledgebase.mapper.KbKnowledgeBaseMapper;
import com.knowledgebase.mapper.KbKnowledgeChunkMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DocumentService 文档服务测试")
class DocumentServiceTest extends BaseTest {

    @Autowired
    private DocumentService documentService;
    @Autowired
    private KbDocumentMapper docMapper;
    @Autowired
    private KbKnowledgeBaseMapper kbMapper;
    @Autowired
    private KbKnowledgeChunkMapper chunkMapper;

    private Long kbId;

    @BeforeEach
    void setUp() {
        chunkMapper.delete(null);
        docMapper.delete(null);
        kbMapper.delete(null);

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

    @Test
    @DisplayName("分页查询-无过滤")
    void testPage() {
        insertDoc("doc1.docx", "docx");
        insertDoc("doc2.xlsx", "xlsx");
        Page<KbDocument> page = documentService.page(1, 10, null, null);
        assertEquals(2, page.getTotal());
    }

    @Test
    @DisplayName("分页查询-按知识库过滤")
    void testPageByKbId() {
        insertDoc("doc1.docx", "docx");
        Page<KbDocument> page = documentService.page(1, 10, kbId, null);
        assertEquals(1, page.getTotal());

        Page<KbDocument> emptyPage = documentService.page(1, 10, 99999L, null);
        assertEquals(0, emptyPage.getTotal());
    }

    @Test
    @DisplayName("分页查询-关键词搜索")
    void testPageWithKeyword() {
        insertDoc("Java基础.docx", "docx");
        insertDoc("Python入门.xlsx", "xlsx");
        Page<KbDocument> page = documentService.page(1, 10, null, "Java");
        assertEquals(1, page.getTotal());
    }

    @Test
    @DisplayName("上传文档-不支持的文件类型")
    void testUploadUnsupportedType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());
        assertThrows(BusinessException.class,
                () -> documentService.upload(file, kbId, 1L));
    }

    @Test
    @DisplayName("上传文档-文件名为空")
    void testUploadEmptyFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "", "application/octet-stream", "content".getBytes());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> documentService.upload(file, kbId, 1L));
        assertEquals("文件名不能为空", ex.getMessage());
    }

    @Test
    @DisplayName("上传文档-无扩展名")
    void testUploadNoExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "testfile", "application/octet-stream", "content".getBytes());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> documentService.upload(file, kbId, 1L));
        assertEquals("文件缺少扩展名", ex.getMessage());
    }

    @Test
    @DisplayName("上传文档-知识库不存在")
    void testUploadToNonExistentKb() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.docx", "application/octet-stream", "content".getBytes());
        assertThrows(BusinessException.class,
                () -> documentService.upload(file, 99999L, 1L));
    }

    @Test
    @DisplayName("删除文档-成功")
    void testDelete() {
        KbDocument doc = insertDoc("test.docx", "docx");

        // 添加关联知识块
        KbKnowledgeChunk chunk = new KbKnowledgeChunk();
        chunk.setDocId(doc.getId());
        chunk.setKbId(kbId);
        chunk.setContent("测试内容");
        chunk.setChunkIndex(0);
        chunkMapper.insert(chunk);

        documentService.delete(doc.getId());

        Page<KbDocument> page = documentService.page(1, 10, null, null);
        assertEquals(0, page.getTotal());
        assertEquals(0, chunkMapper.selectCount(null).intValue());
    }

    @Test
    @DisplayName("删除文档-不存在")
    void testDeleteNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> documentService.delete(99999L));
        assertEquals("文档不存在", ex.getMessage());
    }

    @Test
    @DisplayName("重新解析-不存在的文档")
    void testReparseNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> documentService.reparse(99999L));
        assertEquals("文档不存在", ex.getMessage());
    }

    @Test
    @DisplayName("获取知识块列表")
    void testGetChunks() {
        KbDocument doc = insertDoc("test.docx", "docx");

        KbKnowledgeChunk chunk1 = new KbKnowledgeChunk();
        chunk1.setDocId(doc.getId());
        chunk1.setKbId(kbId);
        chunk1.setContent("内容1");
        chunk1.setChunkIndex(0);
        chunkMapper.insert(chunk1);

        KbKnowledgeChunk chunk2 = new KbKnowledgeChunk();
        chunk2.setDocId(doc.getId());
        chunk2.setKbId(kbId);
        chunk2.setContent("内容2");
        chunk2.setChunkIndex(1);
        chunkMapper.insert(chunk2);

        List<KbKnowledgeChunk> chunks = documentService.getChunks(doc.getId());
        assertEquals(2, chunks.size());
        assertEquals(0, chunks.get(0).getChunkIndex());
        assertEquals(1, chunks.get(1).getChunkIndex());
    }

    @Test
    @DisplayName("获取知识块-无数据")
    void testGetChunksEmpty() {
        List<KbKnowledgeChunk> chunks = documentService.getChunks(99999L);
        assertTrue(chunks.isEmpty());
    }

    @Test
    @DisplayName("分页查询-分页参数正确")
    void testPagePagination() {
        for (int i = 0; i < 15; i++) {
            insertDoc("doc" + i + ".docx", "docx");
        }
        Page<KbDocument> page1 = documentService.page(1, 10, null, null);
        assertEquals(15, page1.getTotal());
        assertEquals(10, page1.getRecords().size());

        Page<KbDocument> page2 = documentService.page(2, 10, null, null);
        assertEquals(5, page2.getRecords().size());
    }

    @Test
    @DisplayName("分页查询-按时间倒序")
    void testPageOrderByCreatedAt() {
        insertDoc("first.docx", "docx");
        insertDoc("second.xlsx", "xlsx");
        Page<KbDocument> page = documentService.page(1, 10, null, null);
        assertEquals(2, page.getTotal());
        // 最新的在前面
        assertTrue(page.getRecords().get(0).getId() > page.getRecords().get(1).getId());
    }

    @Test
    @DisplayName("删除文档-同时删除关联知识块")
    void testDeleteCascadeChunks() {
        KbDocument doc = insertDoc("cascade.docx", "docx");
        for (int i = 0; i < 5; i++) {
            KbKnowledgeChunk chunk = new KbKnowledgeChunk();
            chunk.setDocId(doc.getId());
            chunk.setKbId(kbId);
            chunk.setContent("内容" + i);
            chunk.setChunkIndex(i);
            chunkMapper.insert(chunk);
        }
        assertEquals(5, chunkMapper.selectCount(null).intValue());

        documentService.delete(doc.getId());

        assertEquals(0, chunkMapper.selectCount(null).intValue());
    }

    @Test
    @DisplayName("获取知识块-按chunkIndex排序")
    void testGetChunksOrdered() {
        KbDocument doc = insertDoc("ordered.docx", "docx");
        // 故意乱序插入
        for (int i = 4; i >= 0; i--) {
            KbKnowledgeChunk chunk = new KbKnowledgeChunk();
            chunk.setDocId(doc.getId());
            chunk.setKbId(kbId);
            chunk.setContent("内容" + i);
            chunk.setChunkIndex(i);
            chunkMapper.insert(chunk);
        }

        List<KbKnowledgeChunk> chunks = documentService.getChunks(doc.getId());
        assertEquals(5, chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).getChunkIndex());
        }
    }
}
