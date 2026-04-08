package com.knowledgebase.service;

import com.knowledgebase.entity.KbDocument;
import com.knowledgebase.entity.KbKnowledgeChunk;
import com.knowledgebase.mapper.KbDocumentMapper;
import com.knowledgebase.mapper.KbKnowledgeChunkMapper;
import com.knowledgebase.service.parser.DocumentParser;
import com.knowledgebase.service.parser.DocumentParserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档解析执行器
 * 使用策略模式，通过 DocumentParserFactory 自动路由到对应的解析器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParseExecutor {

    private final KbDocumentMapper docMapper;
    private final KbKnowledgeChunkMapper chunkMapper;
    private final DocumentParserFactory parserFactory;
    private final KnowledgeBaseService kbService;

    @Async
    public void asyncParse(KbDocument doc) {
        log.info("开始异步解析文档: docId={}, type={}", doc.getId(), doc.getFileType());
        doc.setParseStatus(1);
        docMapper.updateById(doc);

        try {
            // 通过工厂获取对应的解析器（策略模式）
            DocumentParser parser = parserFactory.getParser(doc.getFileType());
            List<KbKnowledgeChunk> chunks = parser.parse(doc.getFilePath(), doc.getId(), doc.getKbId());

            for (KbKnowledgeChunk chunk : chunks) {
                chunkMapper.insert(chunk);
            }

            doc.setParseStatus(2);
            doc.setChunkCount(chunks.size());
            doc.setParseMessage("解析成功，共" + chunks.size() + "个知识块");
            docMapper.updateById(doc);

            kbService.refreshCount(doc.getKbId());
            log.info("文档解析成功: docId={}, chunks={}", doc.getId(), chunks.size());
        } catch (Exception e) {
            log.error("文档解析失败: docId={}", doc.getId(), e);
            doc.setParseStatus(3);
            doc.setParseMessage("解析失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
            docMapper.updateById(doc);
        }
    }

    public void syncParse(KbDocument doc) {
        asyncParse(doc);
    }
}
