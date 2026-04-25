package com.huawei.modellite.repository.common.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseResponseTest {

    @Test
    @DisplayName("should create success response with code 0")
    void should_createSuccessResponse() {
        BaseResponse<String> response = BaseResponse.success("test-data");

        assertEquals(0, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals("test-data", response.getData());
        assertNotNull(response.getTimestamp());
        assertNotNull(response.getRequestId());
    }

    @Test
    @DisplayName("should create error response with given code and message")
    void should_createErrorResponse() {
        BaseResponse<Object> response = BaseResponse.error("0102001", "模型不存在");

        assertEquals(102001, response.getCode());
        assertEquals("模型不存在", response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
        assertNotNull(response.getRequestId());
    }

    @Test
    @DisplayName("should create success response with null data")
    void should_createSuccessResponseWithNullData() {
        BaseResponse<Object> response = BaseResponse.success(null);

        assertEquals(0, response.getCode());
        assertNull(response.getData());
    }
}
