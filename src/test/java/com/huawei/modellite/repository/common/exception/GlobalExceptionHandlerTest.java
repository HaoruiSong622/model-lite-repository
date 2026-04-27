package com.huawei.modellite.repository.common.exception;

import com.huawei.modellite.repository.common.dto.response.BaseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("should return 404 for NOT_FOUND error codes")
    void should_return404_forNotFoundCodes() {
        ModelLiteException ex = new ModelLiteException("0102001", "模型不存在");

        ResponseEntity<BaseResponse<Void>> response = handler.handleModelLiteException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(102001, response.getBody().getCode());
        assertEquals("模型不存在", response.getBody().getMessage());
    }

    @Test
    @DisplayName("should return 409 for CONFLICT error codes")
    void should_return409_forConflictCodes() {
        ModelLiteException ex = new ModelLiteException("0102002", "模型名称已存在");

        ResponseEntity<BaseResponse<Void>> response = handler.handleModelLiteException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(102002, response.getBody().getCode());
        assertEquals("模型名称已存在", response.getBody().getMessage());
    }

    @Test
    @DisplayName("should return 400 for BAD_REQUEST error codes")
    void should_return400_forBadRequestCodes() {
        ModelLiteException ex = new ModelLiteException("0102003", "模型名称不可修改");

        ResponseEntity<BaseResponse<Void>> response = handler.handleModelLiteException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(102003, response.getBody().getCode());
        assertEquals("模型名称不可修改", response.getBody().getMessage());
    }

    @Test
    @DisplayName("should return 500 for generic Exception")
    void should_return500_forGenericException() {
        Exception ex = new Exception("some internal error");

        ResponseEntity<BaseResponse<Void>> response = handler.handleException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(105001, response.getBody().getCode());
        assertEquals("内部服务错误", response.getBody().getMessage());
    }

    @Test
    @DisplayName("should preserve code and message from ModelLiteException")
    void should_preserveCodeAndMessage() {
        ModelLiteException ex = new ModelLiteException("0102027", "模型类型不属于该分类");

        ResponseEntity<BaseResponse<Void>> response = handler.handleModelLiteException(ex);

        assertEquals(102027, response.getBody().getCode());
        assertEquals("模型类型不属于该分类", response.getBody().getMessage());
    }
}