package com.huawei.modellite.repository.common.exception;

import com.huawei.modellite.repository.common.dto.response.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ModelLiteException.class)
    public ResponseEntity<BaseResponse<Void>> handleModelLiteException(ModelLiteException ex) {
        String code = ex.getCode();
        HttpStatus status = mapCodeToStatus(code);
        BaseResponse<Void> response = BaseResponse.error(code, ex.getMessage());
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception ex) {
        BaseResponse<Void> response = BaseResponse.error("0105001", "内部服务错误");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpStatus mapCodeToStatus(String code) {
        if (code.endsWith("01") || code.endsWith("06") || code.endsWith("10") ||
            code.endsWith("13") || code.endsWith("15") || code.endsWith("19") ||
            code.endsWith("22") || code.endsWith("26")) {
            return HttpStatus.NOT_FOUND;
        }
        if (code.endsWith("02") || code.endsWith("08") ||
            code.endsWith("16") || code.endsWith("18") || code.endsWith("21")) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }
}