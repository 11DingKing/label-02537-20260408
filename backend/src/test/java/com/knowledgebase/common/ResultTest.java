package com.knowledgebase.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Result 统一响应测试")
class ResultTest {

    @Test
    @DisplayName("成功响应 - 带数据")
    void ok_withData() {
        Result<String> result = Result.ok("hello");
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("hello", result.getData());
    }

    @Test
    @DisplayName("成功响应 - 无数据")
    void ok_noData() {
        Result<?> result = Result.ok();
        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("失败响应 - 默认500")
    void fail_default() {
        Result<?> result = Result.fail("出错了");
        assertEquals(500, result.getCode());
        assertEquals("出错了", result.getMessage());
    }

    @Test
    @DisplayName("失败响应 - 自定义code")
    void fail_customCode() {
        Result<?> result = Result.fail(401, "未授权");
        assertEquals(401, result.getCode());
        assertEquals("未授权", result.getMessage());
    }
}
