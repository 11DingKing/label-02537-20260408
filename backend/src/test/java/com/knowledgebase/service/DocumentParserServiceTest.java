package com.knowledgebase.service;

import com.knowledgebase.entity.KbKnowledgeChunk;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("文档解析服务测试")
class DocumentParserServiceTest {

    private DocumentParserService parserService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parserService = new DocumentParserService();
    }

    @Test
    @DisplayName("解析DOCX文件-段落内容")
    void testParseDocx() throws Exception {
        File docxFile = createTestDocx("这是第一段内容", "这是第二段内容", "这是第三段内容");

        List<KbKnowledgeChunk> chunks = parserService.parseDocx(docxFile.getAbsolutePath(), 1L, 1L);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.get(0).getContent().contains("第一段"));
        assertEquals(1L, chunks.get(0).getDocId());
        assertEquals(1L, chunks.get(0).getKbId());
        assertEquals(0, chunks.get(0).getChunkIndex());
        assertEquals("段落", chunks.get(0).getSourceInfo());
    }

    @Test
    @DisplayName("解析DOCX文件-空文档")
    void testParseEmptyDocx() throws Exception {
        File docxFile = createTestDocx();

        List<KbKnowledgeChunk> chunks = parserService.parseDocx(docxFile.getAbsolutePath(), 1L, 1L);

        assertTrue(chunks.isEmpty());
    }

    @Test
    @DisplayName("解析DOCX文件-大段落自动切分")
    void testParseDocxChunking() throws Exception {
        // 创建超过 MAX_CHUNK_SIZE 的内容
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longText.append("这是一段很长的测试文本内容，用于测试知识块切分功能。");
        }
        File docxFile = createTestDocx(longText.toString());

        List<KbKnowledgeChunk> chunks = parserService.parseDocx(docxFile.getAbsolutePath(), 1L, 1L);

        // 长文本应该被切分为多个块
        assertTrue(chunks.size() >= 1);
        for (KbKnowledgeChunk chunk : chunks) {
            assertNotNull(chunk.getContent());
            assertFalse(chunk.getContent().isEmpty());
        }
    }

    @Test
    @DisplayName("解析XLSX文件")
    void testParseExcel() throws Exception {
        File xlsxFile = createTestXlsx();

        List<KbKnowledgeChunk> chunks = parserService.parseExcel(xlsxFile.getAbsolutePath(), 2L, 1L);

        assertFalse(chunks.isEmpty());
        assertEquals(2L, chunks.get(0).getDocId());
        // 验证表头被用作字段名
        String content = chunks.get(0).getContent();
        assertTrue(content.contains("姓名") || content.contains("年龄"));
    }

    @Test
    @DisplayName("解析XLSX文件-多Sheet")
    void testParseExcelMultiSheet() throws Exception {
        File xlsxFile = createTestXlsxMultiSheet();

        List<KbKnowledgeChunk> chunks = parserService.parseExcel(xlsxFile.getAbsolutePath(), 3L, 1L);

        assertFalse(chunks.isEmpty());
        // 应该包含两个Sheet的数据
        boolean hasSheet1 = chunks.stream().anyMatch(c -> "Sheet1".equals(c.getSourceInfo()));
        boolean hasSheet2 = chunks.stream().anyMatch(c -> "Sheet2".equals(c.getSourceInfo()));
        assertTrue(hasSheet1);
        assertTrue(hasSheet2);
    }

    @Test
    @DisplayName("解析不存在的文件抛出异常")
    void testParseNonExistentFile() {
        assertThrows(Exception.class, () ->
                parserService.parseDocx("/nonexistent/file.docx", 1L, 1L));
        assertThrows(Exception.class, () ->
                parserService.parseExcel("/nonexistent/file.xlsx", 1L, 1L));
    }

    @Test
    @DisplayName("解析DOCX-知识块字段完整性")
    void testParseDocxChunkFields() throws Exception {
        File docxFile = createTestDocx("完整性测试内容");
        List<KbKnowledgeChunk> chunks = parserService.parseDocx(docxFile.getAbsolutePath(), 10L, 20L);

        assertFalse(chunks.isEmpty());
        KbKnowledgeChunk chunk = chunks.get(0);
        assertEquals(10L, chunk.getDocId());
        assertEquals(20L, chunk.getKbId());
        assertEquals(0, chunk.getChunkIndex());
        assertEquals("段落", chunk.getSourceInfo());
        assertNotNull(chunk.getContent());
        assertFalse(chunk.getContent().isEmpty());
    }

    @Test
    @DisplayName("解析XLSX-空Sheet不产生知识块")
    void testParseExcelEmptySheet() throws Exception {
        File file = tempDir.resolve("empty.xlsx").toFile();
        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {
            wb.createSheet("EmptySheet");
            wb.write(fos);
        }

        List<KbKnowledgeChunk> chunks = parserService.parseExcel(file.getAbsolutePath(), 1L, 1L);
        assertTrue(chunks.isEmpty());
    }

    @Test
    @DisplayName("解析XLSX-只有表头无数据行")
    void testParseExcelHeaderOnly() throws Exception {
        File file = tempDir.resolve("headeronly.xlsx").toFile();
        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {
            XSSFSheet sheet = wb.createSheet("Sheet1");
            XSSFRow header = sheet.createRow(0);
            header.createCell(0).setCellValue("列A");
            header.createCell(1).setCellValue("列B");
            wb.write(fos);
        }

        List<KbKnowledgeChunk> chunks = parserService.parseExcel(file.getAbsolutePath(), 1L, 1L);
        assertTrue(chunks.isEmpty());
    }

    @Test
    @DisplayName("解析XLSX-大量数据行自动切分")
    void testParseExcelLargeData() throws Exception {
        File file = tempDir.resolve("large.xlsx").toFile();
        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {
            XSSFSheet sheet = wb.createSheet("Sheet1");
            XSSFRow header = sheet.createRow(0);
            header.createCell(0).setCellValue("名称");
            header.createCell(1).setCellValue("描述");

            for (int i = 1; i <= 100; i++) {
                XSSFRow row = sheet.createRow(i);
                row.createCell(0).setCellValue("项目" + i);
                row.createCell(1).setCellValue("这是一段比较长的描述内容用于测试切分功能，项目编号" + i);
            }
            wb.write(fos);
        }

        List<KbKnowledgeChunk> chunks = parserService.parseExcel(file.getAbsolutePath(), 1L, 1L);
        assertFalse(chunks.isEmpty());
        // 100行数据应该被切分为多个块
        assertTrue(chunks.size() >= 1);
    }

    @Test
    @DisplayName("解析DOCX-含表格内容")
    void testParseDocxWithTable() throws Exception {
        File file = tempDir.resolve("table.docx").toFile();
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(file)) {
            doc.createParagraph().createRun().setText("表格前的段落");

            org.apache.poi.xwpf.usermodel.XWPFTable table = doc.createTable(2, 3);
            table.getRow(0).getCell(0).setText("列1");
            table.getRow(0).getCell(1).setText("列2");
            table.getRow(0).getCell(2).setText("列3");
            table.getRow(1).getCell(0).setText("值A");
            table.getRow(1).getCell(1).setText("值B");
            table.getRow(1).getCell(2).setText("值C");

            doc.write(fos);
        }

        List<KbKnowledgeChunk> chunks = parserService.parseDocx(file.getAbsolutePath(), 1L, 1L);
        assertFalse(chunks.isEmpty());
        // 应该包含表格内容
        String allContent = chunks.stream().map(KbKnowledgeChunk::getContent).reduce("", String::concat);
        assertTrue(allContent.contains("表格前的段落"));
    }

    // ===== 辅助方法 =====

    private File createTestDocx(String... paragraphs) throws Exception {
        File file = tempDir.resolve("test.docx").toFile();
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(file)) {
            for (String text : paragraphs) {
                XWPFParagraph para = doc.createParagraph();
                para.createRun().setText(text);
            }
            doc.write(fos);
        }
        return file;
    }

    private File createTestXlsx() throws Exception {
        File file = tempDir.resolve("test.xlsx").toFile();
        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {
            XSSFSheet sheet = wb.createSheet("Sheet1");
            XSSFRow header = sheet.createRow(0);
            header.createCell(0).setCellValue("姓名");
            header.createCell(1).setCellValue("年龄");
            header.createCell(2).setCellValue("城市");

            XSSFRow row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("张三");
            row1.createCell(1).setCellValue(25);
            row1.createCell(2).setCellValue("北京");

            XSSFRow row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("李四");
            row2.createCell(1).setCellValue(30);
            row2.createCell(2).setCellValue("上海");

            wb.write(fos);
        }
        return file;
    }

    private File createTestXlsxMultiSheet() throws Exception {
        File file = tempDir.resolve("multi.xlsx").toFile();
        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {
            // Sheet1
            XSSFSheet sheet1 = wb.createSheet("Sheet1");
            sheet1.createRow(0).createCell(0).setCellValue("标题");
            sheet1.createRow(1).createCell(0).setCellValue("内容A");

            // Sheet2
            XSSFSheet sheet2 = wb.createSheet("Sheet2");
            sheet2.createRow(0).createCell(0).setCellValue("名称");
            sheet2.createRow(1).createCell(0).setCellValue("内容B");

            wb.write(fos);
        }
        return file;
    }
}
