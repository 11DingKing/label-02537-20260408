package com.knowledgebase.service.parser;

import com.knowledgebase.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文档解析器工厂
 * 自动发现所有 DocumentParser 实现，根据文件扩展名路由到对应解析器
 */
@Slf4j
@Component
public class DocumentParserFactory {

    private final Map<String, DocumentParser> parserMap = new HashMap<>();

    /**
     * 通过构造函数注入所有 DocumentParser 实现
     * Spring 会自动收集所有实现了 DocumentParser 接口的 Bean
     */
    public DocumentParserFactory(List<DocumentParser> parsers) {
        for (DocumentParser parser : parsers) {
            for (String ext : parser.getSupportedExtensions()) {
                parserMap.put(ext.toLowerCase(), parser);
                log.info("注册文档解析器: {} -> {}", ext, parser.getClass().getSimpleName());
            }
        }
    }

    /**
     * 根据文件扩展名获取解析器
     */
    public DocumentParser getParser(String fileExtension) {
        DocumentParser parser = parserMap.get(fileExtension.toLowerCase());
        if (parser == null) {
            throw new BusinessException("不支持的文件类型: " + fileExtension + 
                    "，支持的类型: " + getSupportedExtensions());
        }
        return parser;
    }

    /**
     * 获取所有支持的文件扩展名
     */
    public Set<String> getSupportedExtensions() {
        return parserMap.keySet();
    }

    /**
     * 检查是否支持指定的文件类型
     */
    public boolean isSupported(String fileExtension) {
        return parserMap.containsKey(fileExtension.toLowerCase());
    }
}
