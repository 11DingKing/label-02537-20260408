package com.knowledgebase.controller;

import com.knowledgebase.common.Result;
import com.knowledgebase.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public Result<?> search(@RequestParam String keyword,
                            @RequestParam(required = false) Long kbId,
                            @RequestParam(defaultValue = "20") int limit) {
        log.debug("知识检索请求: keyword={}, kbId={}, limit={}", keyword, kbId, limit);
        return Result.ok(searchService.search(keyword, kbId, limit));
    }
}
