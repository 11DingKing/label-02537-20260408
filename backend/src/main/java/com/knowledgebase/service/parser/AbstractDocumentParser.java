package com.knowledgebase.service.parser;

import com.knowledgebase.entity.KbKnowledgeChunk;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析器抽象基类
 * 提取公共的分块逻辑，避免代码重复
 */
public abstract class AbstractDocumentParser implements DocumentParser {

    @Value("${app.parser.chunk-size:1000}")
    protected int maxChunkSize;

    /**
     * 分块上下文，用于累积文本并自动分块
     */
    protected class ChunkContext {
        private final Long docId;
        private final Long kbId;
        private final List<KbKnowledgeChunk> chunks = new ArrayList<>();
        private final StringBuilder buffer = new StringBuilder();
        private int chunkIndex = 0;
        private String currentSource = "段落";

        public ChunkContext(Long docId, Long kbId) {
            this.docId = docId;
            this.kbId = kbId;
        }

        public void setSource(String source) {
            this.currentSource = source;
        }

        /**
         * 添加文本内容，自动处理分块
         * 支持超长文本的强制切分
         */
        public void append(String text) {
            if (text == null || text.trim().isEmpty()) return;
            text = text.trim();

            // 如果单段文本超过限制，进行强制切分
            if (text.length() > maxChunkSize) {
                // 先保存当前缓存
                flush();
                // 对超长文本进行切分
                splitAndAdd(text);
                return;
            }

            // 正常分块逻辑
            if (buffer.length() + text.length() + 1 > maxChunkSize && buffer.length() > 0) {
                flush();
            }
            buffer.append(text).append("\n");
        }

        /**
         * 强制切分超长文本
         */
        private void splitAndAdd(String text) {
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + maxChunkSize, text.length());
                // 尝试在标点符号处断开
                if (end < text.length()) {
                    int breakPoint = findBreakPoint(text, start, end);
                    if (breakPoint > start) {
                        end = breakPoint;
                    }
                }
                String segment = text.substring(start, end).trim();
                if (!segment.isEmpty()) {
                    chunks.add(buildChunk(segment));
                }
                start = end;
            }
        }

        /**
         * 查找合适的断点（标点符号）
         */
        private int findBreakPoint(String text, int start, int end) {
            String breakChars = "。！？；\n.!?;";
            for (int i = end - 1; i > start + maxChunkSize / 2; i--) {
                if (breakChars.indexOf(text.charAt(i)) >= 0) {
                    return i + 1;
                }
            }
            return end;
        }

        /**
         * 刷新缓存，生成知识块
         */
        public void flush() {
            if (buffer.length() > 0) {
                chunks.add(buildChunk(buffer.toString().trim()));
                buffer.setLength(0);
            }
        }

        private KbKnowledgeChunk buildChunk(String content) {
            KbKnowledgeChunk chunk = new KbKnowledgeChunk();
            chunk.setDocId(docId);
            chunk.setKbId(kbId);
            chunk.setContent(content);
            chunk.setChunkIndex(chunkIndex++);
            chunk.setSourceInfo(currentSource);
            return chunk;
        }

        public List<KbKnowledgeChunk> getChunks() {
            flush();
            return chunks;
        }
    }
}
