package com.knowledgebase.service.parser;

import com.knowledgebase.common.DocumentParseException;
import com.knowledgebase.entity.KbKnowledgeChunk;
import java.util.List;
import java.util.Set;

/**
 * 文档解析器接口 - 策略模式
 * 新增文件格式只需实现此接口并添加 @Component 注解
 */
public interface DocumentParser {

    /**
     * 获取支持的文件扩展名
     */
    Set<String> getSupportedExtensions();

    /**
     * 解析文档
     * @throws DocumentParseException 解析失败时抛出具体类型的异常
     */
    List<KbKnowledgeChunk> parse(String filePath, Long docId, Long kbId) throws DocumentParseException;
}
