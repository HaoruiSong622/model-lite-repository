package com.huawei.modellite.repository.common.exception;

public class ModelLiteException extends RuntimeException {

    private final String code;

    public ModelLiteException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
