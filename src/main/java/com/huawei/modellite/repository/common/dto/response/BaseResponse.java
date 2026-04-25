package com.huawei.modellite.repository.common.dto.response;

import java.time.Instant;
import java.util.UUID;

public class BaseResponse<T> {

    private int code;
    private String message;
    private T data;
    private String timestamp;
    private String requestId;

    private BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().toString();
        this.requestId = UUID.randomUUID().toString();
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, "success", data);
    }

    public static <T> BaseResponse<T> error(String code, String message) {
        return new BaseResponse<>(Integer.parseInt(code), message, null);
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public String getTimestamp() { return timestamp; }
    public String getRequestId() { return requestId; }
}
