package com.huawei.modellite.repository.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelLiteExceptionTest {

    @Test
    @DisplayName("should store code and message")
    void should_storeCodeAndMessage() {
        ModelLiteException exception = new ModelLiteException("0102001", "模型不存在");

        assertEquals("0102001", exception.getCode());
        assertEquals("模型不存在", exception.getMessage());
    }

    @Test
    @DisplayName("should be a RuntimeException")
    void should_beRuntimeException() {
        ModelLiteException exception = new ModelLiteException("0102001", "test");

        assertInstanceOf(RuntimeException.class, exception);
    }
}
