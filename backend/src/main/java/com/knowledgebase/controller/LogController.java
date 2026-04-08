package com.knowledgebase.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledgebase.common.Result;
import com.knowledgebase.entity.SysOperationLog;
import com.knowledgebase.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    private final SysOperationLogMapper logMapper;

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "20") int pageSize,
                          @RequestParam(required = false) String keyword) {
        log.debug("查询操作日志: pageNum={}, pageSize={}, keyword={}", pageNum, pageSize, keyword);
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysOperationLog::getModule, keyword)
                    .or().like(SysOperationLog::getOperation, keyword)
                    .or().like(SysOperationLog::getUsername, keyword);
        }
        wrapper.orderByDesc(SysOperationLog::getCreatedAt);
        return Result.ok(logMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }
}
