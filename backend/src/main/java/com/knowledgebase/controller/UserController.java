package com.knowledgebase.controller;

import com.knowledgebase.common.OperationLog;
import com.knowledgebase.common.Result;
import com.knowledgebase.dto.UserDTO;
import com.knowledgebase.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(userService.page(pageNum, pageSize, keyword));
    }

    @PostMapping
    @OperationLog(module = "用户管理", operation = "新增用户")
    public Result<?> create(@Valid @RequestBody UserDTO dto) {
        userService.create(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @OperationLog(module = "用户管理", operation = "修改用户")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody UserDTO dto) {
        userService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "用户管理", operation = "删除用户")
    public Result<?> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @OperationLog(module = "用户管理", operation = "更新用户状态")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.ok();
    }
}
