package com.knowledgebase.controller;

import com.knowledgebase.common.OperationLog;
import com.knowledgebase.common.Result;
import com.knowledgebase.dto.KbDTO;
import com.knowledgebase.service.KnowledgeBaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(kbService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(kbService.getById(id));
    }

    @PostMapping
    @OperationLog(module = "知识库", operation = "创建知识库")
    public Result<?> create(@Valid @RequestBody KbDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        kbService.create(dto, userId);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @OperationLog(module = "知识库", operation = "修改知识库")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody KbDTO dto) {
        kbService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "知识库", operation = "删除知识库")
    public Result<?> delete(@PathVariable Long id) {
        kbService.delete(id);
        return Result.ok();
    }
}
