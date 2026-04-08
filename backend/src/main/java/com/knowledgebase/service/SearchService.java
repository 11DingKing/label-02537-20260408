package com.knowledgebase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledgebase.entity.KbDocument;
import com.knowledgebase.entity.KbKnowledgeBase;
import com.knowledgebase.entity.KbKnowledgeChunk;
import com.knowledgebase.mapper.KbDocumentMapper;
import com.knowledgebase.mapper.KbKnowledgeBaseMapper;
import com.knowledgebase.mapper.KbKnowledgeChunkMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final KbKnowledgeChunkMapper chunkMapper;
    private final KbDocumentMapper docMapper;
    private final KbKnowledgeBaseMapper kbMapper;

    @Data
    public static class SearchResult {
        private Long chunkId;
        private String content;
        private String sourceInfo;
        private Integer chunkIndex;
        private String documentName;
        private Long documentId;
        private String knowledgeBaseName;
        private Long knowledgeBaseId;
    }

    public List<SearchResult> search(String keyword, Long kbId, int limit) {
        if (!StringUtils.hasText(keyword)) return Collections.emptyList();
        if (limit <= 0 || limit > 100) limit = 20;
        long startTime = System.currentTimeMillis();
        log.info("知识检索开始: keyword={}, kbId={}, limit={}", keyword, kbId, limit);

        // 确定搜索范围
        List<Long> kbIds;
        if (kbId != null) {
            kbIds = List.of(kbId);
        } else {
            kbIds = kbMapper.selectList(new LambdaQueryWrapper<KbKnowledgeBase>()
                            .select(KbKnowledgeBase::getId))
                    .stream().map(KbKnowledgeBase::getId).collect(Collectors.toList());
        }
        if (kbIds.isEmpty()) {
            log.info("知识检索完成: 无可搜索的知识库, 耗时={}ms", System.currentTimeMillis() - startTime);
            return Collections.emptyList();
        }

        // 全文检索
        List<KbKnowledgeChunk> chunks;
        try {
            chunks = chunkMapper.fullTextSearch(keyword, kbIds, limit);
            log.debug("全文检索命中: count={}", chunks.size());
        } catch (Exception e) {
            log.warn("全文检索失败，降级为LIKE查询: {}", e.getMessage());
            chunks = chunkMapper.selectList(new LambdaQueryWrapper<KbKnowledgeChunk>()
                    .in(KbKnowledgeChunk::getKbId, kbIds)
                    .like(KbKnowledgeChunk::getContent, keyword)
                    .orderByDesc(KbKnowledgeChunk::getId)
                    .last("LIMIT " + limit));
            log.debug("LIKE降级查询命中: count={}", chunks.size());
        }

        // 组装结果
        Map<Long, KbDocument> docCache = new HashMap<>();
        Map<Long, KbKnowledgeBase> kbCache = new HashMap<>();

        List<SearchResult> results = chunks.stream().map(chunk -> {
            SearchResult sr = new SearchResult();
            sr.setChunkId(chunk.getId());
            sr.setContent(chunk.getContent());
            sr.setSourceInfo(chunk.getSourceInfo());
            sr.setChunkIndex(chunk.getChunkIndex());
            sr.setDocumentId(chunk.getDocId());
            sr.setKnowledgeBaseId(chunk.getKbId());

            KbDocument doc = docCache.computeIfAbsent(chunk.getDocId(), docMapper::selectById);
            if (doc != null) sr.setDocumentName(doc.getOriginalName());

            KbKnowledgeBase kb = kbCache.computeIfAbsent(chunk.getKbId(), kbMapper::selectById);
            if (kb != null) sr.setKnowledgeBaseName(kb.getName());

            return sr;
        }).collect(Collectors.toList());

        log.info("知识检索完成: keyword={}, resultCount={}, 耗时={}ms", keyword, results.size(), System.currentTimeMillis() - startTime);
        return results;
    }
}
