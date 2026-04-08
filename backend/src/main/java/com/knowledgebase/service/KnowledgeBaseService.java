package com.knowledgebase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledgebase.common.BusinessException;
import com.knowledgebase.dto.KbDTO;
import com.knowledgebase.entity.KbDocument;
import com.knowledgebase.entity.KbKnowledgeBase;
import com.knowledgebase.entity.KbKnowledgeChunk;
import com.knowledgebase.mapper.KbDocumentMapper;
import com.knowledgebase.mapper.KbKnowledgeBaseMapper;
import com.knowledgebase.mapper.KbKnowledgeChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KbKnowledgeBaseMapper kbMapper;
    private final KbDocumentMapper docMapper;
    private final KbKnowledgeChunkMapper chunkMapper;

    public Page<KbKnowledgeBase> page(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<KbKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(KbKnowledgeBase::getName, keyword);
        }
        wrapper.orderByDesc(KbKnowledgeBase::getCreatedAt);
        return kbMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public KbKnowledgeBase getById(Long id) {
        KbKnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null) {
            log.warn("知识库不存在: id={}", id);
            throw new BusinessException("知识库不存在");
        }
        return kb;
    }

    public void create(KbDTO dto, Long userId) {
        KbKnowledgeBase kb = new KbKnowledgeBase();
        kb.setName(dto.getName());
        kb.setDescription(dto.getDescription());
        kb.setUserId(userId);
        kb.setDocCount(0);
        kb.setChunkCount(0);
        kb.setStatus(1);
        kbMapper.insert(kb);
        log.info("创建知识库: name={}, userId={}", dto.getName(), userId);
    }

    public void update(Long id, KbDTO dto) {
        KbKnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null) {
            log.warn("更新知识库失败-不存在: id={}", id);
            throw new BusinessException("知识库不存在");
        }
        kb.setName(dto.getName());
        kb.setDescription(dto.getDescription());
        kbMapper.updateById(kb);
        log.info("更新知识库: id={}, name={}", id, dto.getName());
    }

    @Transactional
    public void delete(Long id) {
        KbKnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null) {
            log.warn("删除知识库失败-不存在: id={}", id);
            throw new BusinessException("知识库不存在");
        }
        // 删除关联的知识块
        long chunkDeleted = chunkMapper.delete(new LambdaQueryWrapper<KbKnowledgeChunk>().eq(KbKnowledgeChunk::getKbId, id));
        // 删除关联的文档
        long docDeleted = docMapper.delete(new LambdaQueryWrapper<KbDocument>().eq(KbDocument::getKbId, id));
        kbMapper.deleteById(id);
        log.info("删除知识库: id={}, name={}, 级联删除文档={}, 知识块={}", id, kb.getName(), docDeleted, chunkDeleted);
    }

    public void refreshCount(Long kbId) {
        Long docCount = docMapper.selectCount(
                new LambdaQueryWrapper<KbDocument>().eq(KbDocument::getKbId, kbId));
        Long chunkCount = chunkMapper.selectCount(
                new LambdaQueryWrapper<KbKnowledgeChunk>().eq(KbKnowledgeChunk::getKbId, kbId));
        KbKnowledgeBase kb = new KbKnowledgeBase();
        kb.setId(kbId);
        kb.setDocCount(docCount.intValue());
        kb.setChunkCount(chunkCount.intValue());
        kbMapper.updateById(kb);
        log.debug("刷新知识库计数: kbId={}, docCount={}, chunkCount={}", kbId, docCount, chunkCount);
    }
}
