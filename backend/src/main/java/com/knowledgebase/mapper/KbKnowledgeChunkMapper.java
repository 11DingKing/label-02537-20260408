package com.knowledgebase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledgebase.entity.KbKnowledgeChunk;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KbKnowledgeChunkMapper extends BaseMapper<KbKnowledgeChunk> {

    /**
     * 全文检索知识块
     * 使用 XML 配置避免 SQL 注入风险
     */
    List<KbKnowledgeChunk> fullTextSearch(@Param("keyword") String keyword, 
                                          @Param("kbIds") List<Long> kbIds, 
                                          @Param("limit") int limit);
}
