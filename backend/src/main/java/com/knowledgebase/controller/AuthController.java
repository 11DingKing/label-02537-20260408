package com.knowledgebase.controller;

import com.knowledgebase.common.OperationLog;
import com.knowledgebase.common.Result;
import com.knowledgebase.dto.LoginRequest;
import com.knowledgebase.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @OperationLog(module = "认证", operation = "用户登录")
    public Result<?> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req.getUsername(), req.getPassword()));
    }

    @GetMapping("/info")
    public Result<?> info(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(authService.getUserInfo(userId));
    }
}
