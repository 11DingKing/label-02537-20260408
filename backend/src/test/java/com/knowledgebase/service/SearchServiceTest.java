package com.knowledgebase.service;

import com.knowledgebase.BaseTest;
import com.knowledgebase.entity.KbDocument;
import com.knowledgebase.entity.KbKnowledgeBase;
import com.knowledgebase.entity.KbKnowledgeChunk;
import com.knowledgebase.mapper.KbDocumentMapper;
import com.knowledgebase.mapper.KbKnowledgeBaseMapper;
import com.knowledgebase.mapper.KbKnowledgeChunkMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SearchService 搜索服务测试")
class SearchServiceTest extends BaseTest {

    @Autowired
    private SearchService searchService;
    @Autowired
    private KbKnowledgeBaseMapper kbMapper;
    @Autowired
    private KbDocumentMapper docMapper;
    @Autowired
    private KbKnowledgeChunkMapper chunkMapper;

    private Long kbId;
    private Long docId;

    @BeforeEach
    void setUp() {
        chunkMapper.delete(null);
        docMapper.delete(null);
        kbMapper.delete(null);

        KbKnowledgeBase kb = new KbKnowledgeBase();
        kb.setName("测试知识库");
        kb.setUserId(1L);
        kb.setDocCount(1);
        kb.setChunkCount(3);
        kb.setStatus(1);
        kbMapper.insert(kb);
        kbId = kb.getId();

        KbDocument doc = new KbDocument();
        doc.setKbId(kbId);
        doc.setFilename("test.docx");
        doc.setOriginalName("Java基础.docx");
        doc.setFilePath("/tmp/test.docx");
        doc.setFileType("docx");
        doc.setFileSize(1024L);
        doc.setParseStatus(2);
        doc.setUserId(1L);
        docMapper.insert(doc);
        docId = doc.getId();

        insertChunk("Java是一种面向对象的编程语言", 0);
        insertChunk("Spring Boot是Java的微服务框架", 1);
        insertChunk("Python是一种解释型语言", 2);
    }

    private void insertChunk(String content, int index) {
        KbKnowledgeChunk chunk = new KbKnowledgeChunk();
        chunk.setDocId(docId);
        chunk.setKbId(kbId);
        chunk.setContent(content);
        chunk.setChunkIndex(index);
        chunk.setSourceInfo("段落");
        chunkMapper.insert(chunk);
    }

    @Test
    @DisplayName("搜索-LIKE降级查询")
    void testSearch() {
        List<SearchService.SearchResult> results = searchService.search("Java", null, 20);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(r -> r.getContent().contains("Java")));
    }

    @Test
    @DisplayName("搜索-指定知识库")
    void testSearchInKb() {
        List<SearchService.SearchResult> results = searchService.search("Python", kbId, 20);
        assertFalse(results.isEmpty());
        assertEquals(kbId, results.get(0).getKnowledgeBaseId());
    }

    @Test
    @DisplayName("搜索-空关键词返回空")
    void testSearchEmptyKeyword() {
        List<SearchService.SearchResult> results = searchService.search("", null, 20);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("搜索-null关键词返回空")
    void testSearchNullKeyword() {
        List<SearchService.SearchResult> results = searchService.search(null, null, 20);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("搜索-无匹配结果")
    void testSearchNoResult() {
        List<SearchService.SearchResult> results = searchService.search("ZZZZNOTEXIST", null, 20);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("搜索-limit参数边界")
    void testSearchLimitBoundary() {
        // limit <= 0 应该被修正为20
        List<SearchService.SearchResult> results = searchService.search("Java", null, 0);
        assertNotNull(results);

        // limit > 100 应该被修正为20
        results = searchService.search("Java", null, 200);
        assertNotNull(results);
    }

    @Test
    @DisplayName("搜索结果包含文档和知识库信息")
    void testSearchResultContainsDocInfo() {
        List<SearchService.SearchResult> results = searchService.search("Java", null, 20);
        assertFalse(results.isEmpty());
        SearchService.SearchResult first = results.get(0);
        assertNotNull(first.getDocumentId());
        assertNotNull(first.getKnowledgeBaseId());
        assertEquals("Java基础.docx", first.getDocumentName());
        assertEquals("测试知识库", first.getKnowledgeBaseName());
    }

    @Test
    @DisplayName("搜索-不存在的知识库ID")
    void testSearchNonExistentKb() {
        List<SearchService.SearchResult> results = searchService.search("Java", 99999L, 20);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("搜索-limit为1只返回一条")
    void testSearchLimitOne() {
        List<SearchService.SearchResult> results = searchService.search("Java", null, 1);
        assertTrue(results.size() <= 1);
    }

    @Test
    @DisplayName("搜索-纯空格关键词返回空")
    void testSearchWhitespaceKeyword() {
        List<SearchService.SearchResult> results = searchService.search("   ", null, 20);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("搜索结果包含chunkIndex")
    void testSearchResultChunkIndex() {
        List<SearchService.SearchResult> results = searchService.search("Java", null, 20);
        assertFalse(results.isEmpty());
        for (SearchService.SearchResult r : results) {
            assertNotNull(r.getChunkIndex());
            assertNotNull(r.getChunkId());
        }
    }

    @Test
    @DisplayName("搜索-中文关键词")
    void testSearchChineseKeyword() {
        List<SearchService.SearchResult> results = searchService.search("面向对象", null, 20);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getContent().contains("面向对象"));
    }

    @Test
    @DisplayName("搜索-多个知识库")
    void testSearchMultipleKbs() {
        // 创建第二个知识库
        KbKnowledgeBase kb2 = new KbKnowledgeBase();
        kb2.setName("第二知识库");
        kb2.setUserId(1L);
        kb2.setDocCount(0);
        kb2.setChunkCount(0);
        kb2.setStatus(1);
        kbMapper.insert(kb2);

        KbKnowledgeChunk chunk = new KbKnowledgeChunk();
        chunk.setDocId(docId);
        chunk.setKbId(kb2.getId());
        chunk.setContent("Java虚拟机JVM原理");
        chunk.setChunkIndex(0);
        chunk.setSourceInfo("段落");
        chunkMapper.insert(chunk);

        // 不指定kbId搜索应该搜索所有知识库
        List<SearchService.SearchResult> results = searchService.search("Java", null, 20);
        assertTrue(results.size() >= 2);
    }
}
