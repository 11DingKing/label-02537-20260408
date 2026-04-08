package com.knowledgebase.controller;

import com.knowledgebase.common.OperationLog;
import com.knowledgebase.common.Result;
import com.knowledgebase.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) Long kbId,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(documentService.page(pageNum, pageSize, kbId, keyword));
    }

    @PostMapping("/upload")
    @OperationLog(module = "文档管理", operation = "上传文档")
    public Result<?> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam Long kbId,
                            HttpServletRequest request) throws Exception {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(documentService.upload(file, kbId, userId));
    }

    @PostMapping("/{id}/reparse")
    @OperationLog(module = "文档管理", operation = "重新解析文档")
    public Result<?> reparse(@PathVariable Long id) {
        documentService.reparse(id);
        return Result.ok();
    }

    @GetMapping("/{id}/chunks")
    public Result<?> chunks(@PathVariable Long id) {
        return Result.ok(documentService.getChunks(id));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "文档管理", operation = "删除文档")
    public Result<?> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.ok();
    }
}
