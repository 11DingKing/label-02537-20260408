package com.knowledgebase.service;

import com.knowledgebase.entity.KbKnowledgeChunk;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DocumentParserService {

    private static final int MAX_CHUNK_SIZE = 1000;

    /**
     * 解析 .doc 文件 (Word 97-2003 格式)
     */
    public List<KbKnowledgeChunk> parseDoc(String filePath, Long docId, Long kbId) throws Exception {
        log.info("开始解析DOC文件: path={}, docId={}", filePath, docId);
        List<KbKnowledgeChunk> chunks = new ArrayList<>();
        try (InputStream is = new FileInputStream(filePath);
             HWPFDocument doc = new HWPFDocument(is)) {

            WordExtractor extractor = new WordExtractor(doc);
            String[] paragraphs = extractor.getParagraphText();

            StringBuilder buffer = new StringBuilder();
            int chunkIndex = 0;

            for (String para : paragraphs) {
                String text = para.trim();
                if (text.isEmpty()) continue;

                if (buffer.length() + text.length() > MAX_CHUNK_SIZE && buffer.length() > 0) {
                    chunks.add(buildChunk(docId, kbId, buffer.toString(), chunkIndex++, "段落"));
                    buffer.setLength(0);
                }
                buffer.append(text).append("\n");
            }

            if (buffer.length() > 0) {
                chunks.add(buildChunk(docId, kbId, buffer.toString(), chunkIndex, "段落"));
            }
            extractor.close();
        }
        log.info("DOC解析完成: docId={}, 共{}个知识块", docId, chunks.size());
        return chunks;
    }

    /**
     * 解析 .docx 文件 (Word 2007+ 格式)
     */

    public List<KbKnowledgeChunk> parseDocx(String filePath, Long docId, Long kbId) throws Exception {
        log.info("开始解析DOCX文件: path={}, docId={}", filePath, docId);
        List<KbKnowledgeChunk> chunks = new ArrayList<>();
        try (InputStream is = new FileInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(is)) {

            StringBuilder buffer = new StringBuilder();
            int chunkIndex = 0;

            // 按文档原始顺序遍历所有元素（段落和表格交替出现）
            for (var element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph) {
                    XWPFParagraph para = (XWPFParagraph) element;
                    String text = para.getText().trim();
                    if (text.isEmpty()) continue;

                    if (buffer.length() + text.length() > MAX_CHUNK_SIZE && buffer.length() > 0) {
                        chunks.add(buildChunk(docId, kbId, buffer.toString(), chunkIndex++, "段落"));
                        buffer.setLength(0);
                    }
                    buffer.append(text).append("\n");
                } else if (element instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) element;
                    StringBuilder tableContent = new StringBuilder();
                    for (XWPFTableRow row : table.getRows()) {
                        List<String> cells = new ArrayList<>();
                        for (XWPFTableCell cell : row.getTableCells()) {
                            cells.add(cell.getText().trim());
                        }
                        tableContent.append(String.join(" | ", cells)).append("\n");
                    }
                    if (tableContent.length() > 0) {
                        if (buffer.length() + tableContent.length() > MAX_CHUNK_SIZE && buffer.length() > 0) {
                            chunks.add(buildChunk(docId, kbId, buffer.toString(), chunkIndex++, "段落"));
                            buffer.setLength(0);
                        }
                        buffer.append(tableContent);
                    }
                }
            }

            if (buffer.length() > 0) {
                chunks.add(buildChunk(docId, kbId, buffer.toString(), chunkIndex, "段落"));
            }
        }
        log.info("DOCX解析完成: docId={}, 共{}个知识块", docId, chunks.size());
        return chunks;
    }

    public List<KbKnowledgeChunk> parseExcel(String filePath, Long docId, Long kbId) throws Exception {
        log.info("开始解析Excel文件: path={}, docId={}", filePath, docId);
        List<KbKnowledgeChunk> chunks = new ArrayList<>();
        boolean isXlsx = filePath.endsWith(".xlsx");

        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = isXlsx ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {

            DataFormatter formatter = new DataFormatter();
            int chunkIndex = 0;

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                StringBuilder buffer = new StringBuilder();

                // 获取表头
                Row headerRow = sheet.getRow(0);
                List<String> headers = new ArrayList<>();
                if (headerRow != null) {
                    for (Cell cell : headerRow) {
                        headers.add(formatter.formatCellValue(cell));
                    }
                }

                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    StringBuilder rowContent = new StringBuilder();
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        String value = cell != null ? formatter.formatCellValue(cell) : "";
                        if (!value.isEmpty()) {
                            String header = c < headers.size() ? headers.get(c) : "列" + (c + 1);
                            rowContent.append(header).append(": ").append(value).append("; ");
                        }
                    }

                    if (rowContent.length() > 0) {
                        if (buffer.length() + rowContent.length() > MAX_CHUNK_SIZE && buffer.length() > 0) {
                            chunks.add(buildChunk(docId, kbId, buffer.toString(), chunkIndex++, sheetName));
                            buffer.setLength(0);
                        }
                        buffer.append(rowContent).append("\n");
                    }
                }

                if (buffer.length() > 0) {
                    chunks.add(buildChunk(docId, kbId, buffer.toString(), chunkIndex++, sheetName));
                }
            }
        }
        log.info("Excel解析完成: docId={}, 共{}个知识块", docId, chunks.size());
        return chunks;
    }

    private KbKnowledgeChunk buildChunk(Long docId, Long kbId, String content, int index, String source) {
        KbKnowledgeChunk chunk = new KbKnowledgeChunk();
        chunk.setDocId(docId);
        chunk.setKbId(kbId);
        chunk.setContent(content.trim());
        chunk.setChunkIndex(index);
        chunk.setSourceInfo(source);
        return chunk;
    }
}
