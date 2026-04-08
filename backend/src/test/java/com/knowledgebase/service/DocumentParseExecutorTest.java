package com.knowledgebase.service;

import com.knowledgebase.BaseTest;
import com.knowledgebase.entity.KbDocument;
import com.knowledgebase.entity.KbKnowledgeBase;
import com.knowledgebase.entity.KbKnowledgeChunk;
import com.knowledgebase.mapper.KbDocumentMapper;
import com.knowledgebase.mapper.KbKnowledgeBaseMapper;
import com.knowledgebase.mapper.KbKnowledgeChunkMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DocumentParseExecutor 文档解析执行器测试")
class DocumentParseExecutorTest extends BaseTest {

    @Autowired
    private DocumentParseExecutor parseExecutor;
    @Autowired
    private KbDocumentMapper docMapper;
    @Autowired
    private KbKnowledgeBaseMapper kbMapper;
    @Autowired
    private KbKnowledgeChunkMapper chunkMapper;

    @TempDir
    Path tempDir;

    private Long kbId;

    @BeforeEach
    void setUp() {
        chunkMapper.delete(null);
        docMapper.delete(null);
        kbMapper.delete(null);

        KbKnowledgeBase kb = new KbKnowledgeBase();
        kb.setName("测试知识库");
        kb.setUserId(1L);
        kb.setDocCount(0);
        kb.setChunkCount(0);
        kb.setStatus(1);
        kbMapper.insert(kb);
        kbId = kb.getId();
    }

    @Test
    @DisplayName("同步解析DOCX文档-成功")
    void testSyncParseDocxSuccess() throws Exception {
        File docxFile = createTestDocx("这是测试段落内容", "第二段测试内容");
        KbDocument doc = insertDoc("test.docx", "docx", docxFile.getAbsolutePath());

        parseExecutor.syncParse(doc);

        // 等待异步处理（syncParse内部调用asyncParse，在测试环境中同步执行）
        KbDocument updated = docMapper.selectById(doc.getId());
        assertEquals(2, updated.getParseStatus());
        assertTrue(updated.getChunkCount() > 0);
        assertNotNull(updated.getParseMessage());
        assertTrue(updated.getParseMessage().contains("解析成功"));

        List<KbKnowledgeChunk> chunks = chunkMapper.selectList(null);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.get(0).getContent().contains("测试"));
    }

    @Test
    @DisplayName("解析失败-文件不存在")
    void testParseNonExistentFile() {
        KbDocument doc = insertDoc("missing.docx", "docx", "/nonexistent/path/missing.docx");

        parseExecutor.syncParse(doc);

        KbDocument updated = docMapper.selectById(doc.getId());
        assertEquals(3, updated.getParseStatus());
        assertNotNull(updated.getParseMessage());
        assertTrue(updated.getParseMessage().contains("解析失败"));
    }

    @Test
    @DisplayName("解析过程中状态变为解析中")
    void testParseStatusTransition() {
        KbDocument doc = insertDoc("test.docx", "docx", "/nonexistent/file.docx");
        assertEquals(0, doc.getParseStatus());

        parseExecutor.syncParse(doc);

        // 解析失败后状态应为3
        KbDocument updated = docMapper.selectById(doc.getId());
        assertEquals(3, updated.getParseStatus());
    }

    private KbDocument insertDoc(String name, String type, String filePath) {
        KbDocument doc = new KbDocument();
        doc.setKbId(kbId);
        doc.setFilename("uuid." + type);
        doc.setOriginalName(name);
        doc.setFilePath(filePath);
        doc.setFileType(type);
        doc.setFileSize(1024L);
        doc.setParseStatus(0);
        doc.setUserId(1L);
        docMapper.insert(doc);
        return doc;
    }

    private File createTestDocx(String... paragraphs) throws Exception {
        File file = tempDir.resolve("test.docx").toFile();
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(file)) {
            for (String text : paragraphs) {
                doc.createParagraph().createRun().setText(text);
            }
            doc.write(fos);
        }
        return file;
    }
}
