package com.knowledgebase.service.parser;

import com.knowledgebase.common.DocumentParseException;
import com.knowledgebase.common.DocumentParseException.ErrorType;
import com.knowledgebase.entity.KbKnowledgeChunk;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Word 文档解析器
 * 支持 .doc (Word 97-2003) 和 .docx (Word 2007+) 格式
 */
@Slf4j
@Component
public class WordDocumentParser extends AbstractDocumentParser {

    @Override
    public Set<String> getSupportedExtensions() {
        return Set.of("doc", "docx");
    }

    @Override
    public List<KbKnowledgeChunk> parse(String filePath, Long docId, Long kbId) throws DocumentParseException {
        if (filePath.endsWith(".doc")) {
            return parseDoc(filePath, docId, kbId);
        } else {
            return parseDocx(filePath, docId, kbId);
        }
    }

    /**
     * 解析 .doc 文件 (Word 97-2003)
     */
    private List<KbKnowledgeChunk> parseDoc(String filePath, Long docId, Long kbId) throws DocumentParseException {
        log.info("开始解析DOC文件: path={}, docId={}", filePath, docId);
        ChunkContext ctx = new ChunkContext(docId, kbId);

        try (InputStream is = new FileInputStream(filePath);
             HWPFDocument doc = new HWPFDocument(is);
             WordExtractor extractor = new WordExtractor(doc)) {

            for (String para : extractor.getParagraphText()) {
                ctx.append(para);
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

        List<KbKnowledgeChunk> chunks = ctx.getChunks();
        if (chunks.isEmpty()) {
            throw new DocumentParseException(ErrorType.EMPTY_CONTENT, filePath);
        }
        log.info("DOC解析完成: docId={}, 共{}个知识块", docId, chunks.size());
        return chunks;
    }

    /**
     * 解析 .docx 文件 (Word 2007+)
     * 保持段落和表格的原始顺序
     */
    private List<KbKnowledgeChunk> parseDocx(String filePath, Long docId, Long kbId) throws DocumentParseException {
        log.info("开始解析DOCX文件: path={}, docId={}", filePath, docId);
        ChunkContext ctx = new ChunkContext(docId, kbId);

        try (InputStream is = new FileInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(is)) {

            // 按文档原始顺序遍历所有元素
            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph para) {
                    ctx.append(para.getText());
                } else if (element instanceof XWPFTable table) {
                    ctx.setSource("表格");
                    for (XWPFTableRow row : table.getRows()) {
                        List<String> cells = new ArrayList<>();
                        for (XWPFTableCell cell : row.getTableCells()) {
                            cells.add(cell.getText().trim());
                        }
                        ctx.append(String.join(" | ", cells));
                    }
                    ctx.setSource("段落");
                }
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

        List<KbKnowledgeChunk> chunks = ctx.getChunks();
        if (chunks.isEmpty()) {
            throw new DocumentParseException(ErrorType.EMPTY_CONTENT, filePath);
        }
        log.info("DOCX解析完成: docId={}, 共{}个知识块", docId, chunks.size());
        return chunks;
    }
}
