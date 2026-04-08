package com.knowledgebase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledgebase.common.BusinessException;
import com.knowledgebase.entity.KbDocument;
import com.knowledgebase.entity.KbKnowledgeChunk;
import com.knowledgebase.mapper.KbDocumentMapper;
import com.knowledgebase.mapper.KbKnowledgeChunkMapper;
import com.knowledgebase.service.parser.DocumentParserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

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
}
