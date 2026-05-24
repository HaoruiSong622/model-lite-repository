package com.huawei.modellite.repository.weighttask.application.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UploadTaskResponse {
    private UUID taskId;
    private UUID modelId;
    private UUID versionId;
    private Integer versionNumber;
    private String sourceType;
    private String sourcePath;
    private String targetPath;
    private Integer progress;
    private String status;
    private String errorMessage;
    private String createUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
