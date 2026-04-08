package com.knowledgebase.config;

import com.knowledgebase.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            log.debug("请求缺少有效Token: uri={}, ip={}", request.getRequestURI(), request.getRemoteAddr());
            throw new BusinessException(401, "未登录或Token已过期");
        }
        token = token.substring(7);
        if (!jwtUtil.isValid(token)) {
            log.warn("Token验证失败: uri={}, ip={}", request.getRequestURI(), request.getRemoteAddr());
            throw new BusinessException(401, "Token无效或已过期");
        }
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        log.debug("Token验证通过: userId={}, username={}, uri={}", userId, username, request.getRequestURI());
        return true;
    }
}
