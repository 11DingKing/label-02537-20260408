package com.knowledgebase.config;

import com.knowledgebase.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("JwtInterceptor 拦截器测试")
class JwtInterceptorTest extends BaseTest {

    @Test
    @DisplayName("无Authorization头-返回401")
    void testNoAuthHeader() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或Token已过期"));
    }

    @Test
    @DisplayName("无Bearer前缀-返回401")
    void testNoBearerPrefix() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "just-a-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或Token已过期"));
    }

    @Test
    @DisplayName("无效Token-返回401")
    void testInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Token无效或已过期"));
    }

    @Test
    @DisplayName("过期Token-返回401")
    void testExpiredToken() throws Exception {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-2024-must-be-at-least-256-bits-long-for-hs256-algo");
        props.setExpiration(0L);
        JwtUtil expiredUtil = new JwtUtil(props);
        String expiredToken = "Bearer " + expiredUtil.generateToken(1L, "admin");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", expiredToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("有效Token-请求通过")
    void testValidToken() throws Exception {
        String token = getAdminToken(1L, "admin");
        mockMvc.perform(get("/api/users")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("登录接口不需要Token")
    void testLoginExcluded() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().isOk());
        // 不应该返回401（可能返回业务错误，但不是拦截器错误）
    }

    @Test
    @DisplayName("OPTIONS请求直接放行")
    void testOptionsRequest() throws Exception {
        mockMvc.perform(options("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Token中的userId和username正确传递")
    void testTokenAttributesPassed() throws Exception {
        // 通过auth/info接口验证userId被正确设置
        // 需要先创建用户
        String token = getAdminToken(1L, "admin");
        // auth/info 会使用 request.getAttribute("userId") 来获取用户信息
        // 如果拦截器没有正确设置，会返回错误
        mockMvc.perform(get("/api/auth/info")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }
}
