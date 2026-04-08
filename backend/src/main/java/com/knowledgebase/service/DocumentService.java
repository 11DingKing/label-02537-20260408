package com.knowledgebase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledgebase.common.BusinessException;
import com.knowledgebase.dto.DocumentPreviewDTO;
import com.knowledgebase.dto.ExcelSheetDTO;
import com.knowledgebase.entity.KbDocument;
import com.knowledgebase.entity.KbKnowledgeChunk;
import com.knowledgebase.mapper.KbDocumentMapper;
import com.knowledgebase.mapper.KbKnowledgeChunkMapper;
import com.knowledgebase.service.parser.DocumentParserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final KbDocumentMapper docMapper;
    private final KbKnowledgeChunkMapper chunkMapper;
    private final DocumentParseExecutor parseExecutor;
    private final KnowledgeBaseService kbService;
    private final DocumentParserFactory parserFactory;

    @Value("${app.upload.path}")
    private String uploadPath;

    public Page<KbDocument> page(int pageNum, int pageSize, Long kbId, String keyword) {
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        if (kbId != null) wrapper.eq(KbDocument::getKbId, kbId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(KbDocument::getOriginalName, keyword);
        }
        wrapper.orderByDesc(KbDocument::getCreatedAt);
        return docMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Transactional
    public KbDocument upload(MultipartFile file, Long kbId, Long userId) throws IOException {
        // 校验知识库存在
        kbService.getById(kbId);

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }

        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex < 0) {
            throw new BusinessException("文件缺少扩展名");
        }
        String ext = originalName.substring(dotIndex + 1).toLowerCase();
        if (!parserFactory.isSupported(ext)) {
            throw new BusinessException("不支持的文件类型，仅支持: " + parserFactory.getSupportedExtensions());
        }

        // 保存文件
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String dirPath = uploadPath + "/" + kbId;
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("创建上传目录失败: {}", dirPath);
            throw new BusinessException("创建上传目录失败");
        }
        String filePath = dirPath + "/" + filename;
        file.transferTo(new File(filePath));
        log.info("文件保存成功: path={}, size={}", filePath, file.getSize());

        // 保存记录
        KbDocument doc = new KbDocument();
        doc.setKbId(kbId);
        doc.setFilename(filename);
        doc.setOriginalName(originalName);
        doc.setFilePath(filePath);
        doc.setFileType(ext);
        doc.setFileSize(file.getSize());
        doc.setParseStatus(0);
        doc.setUserId(userId);
        docMapper.insert(doc);

        log.info("文档上传成功: name={}, kbId={}, userId={}, docId={}", originalName, kbId, userId, doc.getId());

        // 异步解析（通过独立 Bean 确保 @Async 代理生效）
        parseExecutor.asyncParse(doc);
        return doc;
    }

    public void reparse(Long docId) {
        KbDocument doc = docMapper.selectById(docId);
        if (doc == null) throw new BusinessException("文档不存在");
        log.info("重新解析文档: docId={}, name={}", docId, doc.getOriginalName());
        // 清除旧的知识块
        chunkMapper.delete(new LambdaQueryWrapper<KbKnowledgeChunk>().eq(KbKnowledgeChunk::getDocId, docId));
        doc.setParseStatus(0);
        doc.setChunkCount(0);
        docMapper.updateById(doc);
        parseExecutor.asyncParse(doc);
    }

    @Transactional
    public void delete(Long id) {
        KbDocument doc = docMapper.selectById(id);
        if (doc == null) throw new BusinessException("文档不存在");
        chunkMapper.delete(new LambdaQueryWrapper<KbKnowledgeChunk>().eq(KbKnowledgeChunk::getDocId, id));
        docMapper.deleteById(id);
        // 删除物理文件
        File physicalFile = new File(doc.getFilePath());
        if (physicalFile.exists() && !physicalFile.delete()) {
            log.warn("删除物理文件失败: {}", doc.getFilePath());
        }
        kbService.refreshCount(doc.getKbId());
        log.info("删除文档: id={}, name={}", id, doc.getOriginalName());
    }

    public List<KbKnowledgeChunk> getChunks(Long docId) {
        return chunkMapper.selectList(
                new LambdaQueryWrapper<KbKnowledgeChunk>()
                        .eq(KbKnowledgeChunk::getDocId, docId)
                        .orderByAsc(KbKnowledgeChunk::getChunkIndex));
    }

    public DocumentPreviewDTO getPreview(Long docId) {
        KbDocument doc = docMapper.selectById(docId);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }

        DocumentPreviewDTO preview = new DocumentPreviewDTO();
        String fileType = doc.getFileType();

        try {
            if ("doc".equals(fileType) || "docx".equals(fileType)) {
                String html = convertWordToHtml(doc.getFilePath(), fileType);
                preview.setHtml(html);
            } else if ("xls".equals(fileType) || "xlsx".equals(fileType)) {
                List<ExcelSheetDTO> sheets = convertExcelToSheets(doc.getFilePath(), fileType);
                preview.setSheets(sheets);
            } else {
                throw new BusinessException("不支持的文档格式");
            }
        } catch (Exception e) {
            log.error("获取文档预览失败: docId={}, error={}", docId, e.getMessage(), e);
            throw new BusinessException("获取文档预览失败: " + e.getMessage());
        }

        return preview;
    }

    private String convertWordToHtml(String filePath, String fileType) throws Exception {
        if ("doc".equals(fileType)) {
            return convertDocToHtml(filePath);
        } else {
            return convertDocxToHtml(filePath);
        }
    }

    private String convertDocToHtml(String filePath) throws Exception {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"word-content\">");

        try (InputStream is = new FileInputStream(filePath);
             HWPFDocument doc = new HWPFDocument(is)) {
            
            Range range = doc.getRange();
            
            // 遍历段落和表格
            int numParagraphs = range.numParagraphs();
            for (int i = 0; i < numParagraphs; i++) {
                Paragraph para = range.getParagraph(i);
                
                // 检查是否是表格
                if (para.isInTable()) {
                    // 找到表格起始位置
                    Table table = range.getTable(para);
                    if (table != null) {
                        html.append(convertTableToHtml(table));
                        // 跳过表格中的所有段落
                        i += table.numParagraphs() - 1;
                        continue;
                    }
                }
                
                String text = para.text().trim();
                if (!text.isEmpty()) {
                    // 根据样式判断标题级别
                    int styleIndex = para.getStyleIndex();
                    String tag = "p";
                    
                    // 简单的标题检测
                    if (styleIndex >= 1 && styleIndex <= 9) {
                        tag = "h" + styleIndex;
                    }
                    
                    html.append("<").append(tag).append(">")
                        .append(escapeHtml(text))
                        .append("</").append(tag).append(">");
                }
            }
        }

        html.append("</div>");
        return html.toString();
    }

    private String convertDocxToHtml(String filePath) throws Exception {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"word-content\">");

        try (InputStream is = new FileInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(is)) {
            
            // 按文档原始顺序遍历所有元素
            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph para) {
                    String text = para.getText().trim();
                    if (!text.isEmpty()) {
                        // 根据样式判断标题级别
                        String style = para.getStyle();
                        String tag = "p";
                        
                        if (style != null && style.matches("Heading\\d")) {
                            int level = Integer.parseInt(style.substring(7));
                            tag = "h" + level;
                        }
                        
                        html.append("<").append(tag).append(">")
                            .append(escapeHtml(text))
                            .append("</").append(tag).append(">");
                    }
                } else if (element instanceof XWPFTable table) {
                    html.append(convertTableToHtml(table));
                }
            }
        }

        html.append("</div>");
        return html.toString();
    }

    private String convertTableToHtml(Table table) {
        StringBuilder html = new StringBuilder();
        html.append("<table>");
        
        int numRows = table.numRows();
        for (int r = 0; r < numRows; r++) {
            TableRow row = table.getRow(r);
            html.append("<tr>");
            
            int numCells = row.numCells();
            for (int c = 0; c < numCells; c++) {
                TableCell cell = row.getCell(c);
                String text = cell.text().trim();
                html.append("<td>").append(escapeHtml(text)).append("</td>");
            }
            
            html.append("</tr>");
        }
        
        html.append("</table>");
        return html.toString();
    }

    private String convertTableToHtml(XWPFTable table) {
        StringBuilder html = new StringBuilder();
        html.append("<table>");
        
        List<XWPFTableRow> rows = table.getRows();
        for (XWPFTableRow row : rows) {
            html.append("<tr>");
            
            List<XWPFTableCell> cells = row.getTableCells();
            for (XWPFTableCell cell : cells) {
                String text = cell.getText().trim();
                html.append("<td>").append(escapeHtml(text)).append("</td>");
            }
            
            html.append("</tr>");
        }
        
        html.append("</table>");
        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private List<ExcelSheetDTO> convertExcelToSheets(String filePath, String fileType) throws Exception {
        List<ExcelSheetDTO> sheets = new ArrayList<>();
        boolean isXlsx = "xlsx".equals(fileType);

        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = isXlsx ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {
            
            DataFormatter formatter = new DataFormatter();

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                ExcelSheetDTO sheetDTO = new ExcelSheetDTO();
                sheetDTO.setName(sheet.getSheetName());

                List<String> headers = new ArrayList<>();
                List<Map<Integer, String>> rows = new ArrayList<>();

                // 获取表头（第一行）
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                        Cell cell = headerRow.getCell(c);
                        headers.add(cell != null ? formatter.formatCellValue(cell) : "");
                    }
                }
                sheetDTO.setHeaders(headers);

                // 获取数据行
                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    Map<Integer, String> rowData = new LinkedHashMap<>();
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        String value = cell != null ? formatter.formatCellValue(cell) : "";
                        rowData.put(c, value);
                    }
                    rows.add(rowData);
                }
                sheetDTO.setRows(rows);

                sheets.add(sheetDTO);
            }
        }

        return sheets;
    }
}
