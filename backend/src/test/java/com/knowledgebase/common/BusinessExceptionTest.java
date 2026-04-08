package com.knowledgebase.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BusinessException 测试")
class BusinessExceptionTest {

    @Test
    @DisplayName("默认code为500")
    void defaultCode() {
        BusinessException ex = new BusinessException("业务错误");
        assertEquals(500, ex.getCode());
        assertEquals("业务错误", ex.getMessage());
    }

    @Test
    @DisplayName("自定义code")
    void customCode() {
        BusinessException ex = new BusinessException(403, "禁止访问");
        assertEquals(403, ex.getCode());
        assertEquals("禁止访问", ex.getMessage());
    }
}
