package com.knowledgebase.service.parser;

import com.knowledgebase.common.DocumentParseException;
import com.knowledgebase.common.DocumentParseException.ErrorType;
import com.knowledgebase.entity.KbKnowledgeChunk;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Excel 文档解析器
 * 支持 .xls (Excel 97-2003) 和 .xlsx (Excel 2007+) 格式
 */
@Slf4j
@Component
public class ExcelDocumentParser extends AbstractDocumentParser {

    @Override
    public Set<String> getSupportedExtensions() {
        return Set.of("xls", "xlsx");
    }

    @Override
    public List<KbKnowledgeChunk> parse(String filePath, Long docId, Long kbId) throws DocumentParseException {
        log.info("开始解析Excel文件: path={}, docId={}", filePath, docId);
        List<KbKnowledgeChunk> allChunks = new ArrayList<>();
        boolean isXlsx = filePath.endsWith(".xlsx");

        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = isXlsx ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {

            DataFormatter formatter = new DataFormatter();

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                ChunkContext ctx = new ChunkContext(docId, kbId);
                ctx.setSource(sheetName);

                // 获取表头
                Row headerRow = sheet.getRow(0);
                List<String> headers = new ArrayList<>();
                if (headerRow != null) {
                    for (Cell cell : headerRow) {
                        headers.add(formatter.formatCellValue(cell));
                    }
                }

                // 解析数据行
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
                    ctx.append(rowContent.toString());
                }

                allChunks.addAll(ctx.getChunks());
            }
        } catch (FileNotFoundException e) {
            throw new DocumentParseException(ErrorType.FILE_NOT_FOUND, filePath, e);
        } catch (EncryptedDocumentException e) {
            throw new DocumentParseException(ErrorType.ENCRYPTED_FILE, filePath, e);
        } catch (IOException e) {
            throw new DocumentParseException(ErrorType.FILE_READ_ERROR, filePath, e);
        } catch (Exception e) {
            throw new DocumentParseException(ErrorType.PARSE_ERROR, e.getMessage(), e);
        }

        if (allChunks.isEmpty()) {
            throw new DocumentParseException(ErrorType.EMPTY_CONTENT, filePath);
        }
        log.info("Excel解析完成: docId={}, 共{}个知识块", docId, allChunks.size());
        return allChunks;
    }
}
