package com.knowledgebase.config;

import com.knowledgebase.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("GlobalExceptionHandler 全局异常处理测试")
class GlobalExceptionHandlerTest extends BaseTest {

    private String token;

    @BeforeEach
    void setUp() {
        token = getAdminToken(1L, "admin");
    }

    @Test
    @DisplayName("BusinessException-返回对应code和message")
    void testBusinessException() throws Exception {
        // 访问不存在的知识库触发BusinessException
        mockMvc.perform(get("/api/kb/99999")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("知识库不存在"));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException-参数校验失败返回400")
    void testValidationException() throws Exception {
        mockMvc.perform(post("/api/kb")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("未登录请求-返回401")
    void testUnauthorized() throws Exception {
        mockMvc.perform(get("/api/kb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("无效Token-返回401")
    void testInvalidToken() throws Exception {
        mockMvc.perform(get("/api/kb")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("缺少Bearer前缀-返回401")
    void testMissingBearerPrefix() throws Exception {
        mockMvc.perform(get("/api/kb")
                        .header("Authorization", "some-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("用户名校验-长度不足")
    void testUsernameTooShort() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ab\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("知识库名称校验-超长")
    void testKbNameTooLong() throws Exception {
        String longName = "a".repeat(101);
        mockMvc.perform(post("/api/kb")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + longName + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("请求体为空-返回400")
    void testEmptyRequestBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("删除不存在的资源-返回业务异常")
    void testDeleteNonExistent() throws Exception {
        mockMvc.perform(delete("/api/documents/99999")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
