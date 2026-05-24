package com.huawei.modellite.repository.weighttask.domain.dto;

/**
 * Status of a Kubernetes Job for upload tasks.
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    COMPLETE,
    FAILED,
    NOT_FOUND
}
