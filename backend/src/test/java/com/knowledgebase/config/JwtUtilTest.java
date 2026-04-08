package com.knowledgebase.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil 单元测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-2024-must-be-at-least-256-bits-long-for-hs256");
        props.setExpiration(86400000L);
        jwtUtil = new JwtUtil(props);
    }

    @Test
    @DisplayName("生成Token并解析 - 正常流程")
    void generateAndParseToken() {
        String token = jwtUtil.generateToken(1L, "admin");
        assertNotNull(token);
        assertFalse(token.isEmpty());

        assertTrue(jwtUtil.isValid(token));
        assertEquals(1L, jwtUtil.getUserId(token));
        assertEquals("admin", jwtUtil.getUsername(token));
    }

    @Test
    @DisplayName("无效Token验证 - 应返回false")
    void invalidToken() {
        assertFalse(jwtUtil.isValid("invalid.token.here"));
        assertFalse(jwtUtil.isValid(""));
        assertFalse(jwtUtil.isValid(null));
    }

    @Test
    @DisplayName("过期Token验证")
    void expiredToken() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-2024-must-be-at-least-256-bits-long-for-hs256");
        props.setExpiration(0L); // 立即过期
        JwtUtil expiredJwtUtil = new JwtUtil(props);

        String token = expiredJwtUtil.generateToken(1L, "admin");
        // Token 刚生成可能还没过期（毫秒级），但 expiration=0 意味着 issuedAt == expiration
        // 实际上 0ms 后就过期了
        assertFalse(expiredJwtUtil.isValid(token));
    }

    @Test
    @DisplayName("不同用户生成不同Token")
    void differentUsersGetDifferentTokens() {
        String token1 = jwtUtil.generateToken(1L, "user1");
        String token2 = jwtUtil.generateToken(2L, "user2");
        assertNotEquals(token1, token2);

        assertEquals(1L, jwtUtil.getUserId(token1));
        assertEquals(2L, jwtUtil.getUserId(token2));
        assertEquals("user1", jwtUtil.getUsername(token1));
        assertEquals("user2", jwtUtil.getUsername(token2));
    }

    @Test
    @DisplayName("Token格式正确-三段式JWT")
    void tokenFormat() {
        String token = jwtUtil.generateToken(1L, "admin");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    @DisplayName("解析无效Token抛出异常")
    void parseInvalidToken() {
        assertThrows(Exception.class, () -> jwtUtil.parseToken("invalid.token"));
    }

    @Test
    @DisplayName("同一用户多次生成Token都有效")
    void multipleTokensValid() {
        String token1 = jwtUtil.generateToken(1L, "admin");
        String token2 = jwtUtil.generateToken(1L, "admin");
        assertTrue(jwtUtil.isValid(token1));
        assertTrue(jwtUtil.isValid(token2));
        // 两个token不同（因为时间戳不同）
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Token包含正确的username claim")
    void tokenContainsUsername() {
        String token = jwtUtil.generateToken(42L, "testuser");
        assertEquals("testuser", jwtUtil.getUsername(token));
        assertEquals(42L, jwtUtil.getUserId(token));
    }
}
