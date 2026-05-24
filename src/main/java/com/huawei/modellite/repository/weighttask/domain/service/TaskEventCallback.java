package com.huawei.modellite.repository.weighttask.domain.service;

/**
 * Callback interface for task lifecycle events.
 * Implementations receive notifications when a task's job state changes.
 */
public interface TaskEventCallback {

    /**
     * Called when the task's job transitions to running state.
     *
     * @param taskId the task ID
     */
    void onJobRunning(String taskId);

    /**
     * Called when the task's job completes successfully.
     *
     * @param taskId the task ID
     */
    void onJobCompleted(String taskId);

    /**
     * Called when the task's job fails.
     *
     * @param taskId       the task ID
     * @param errorMessage description of the failure
     */
    void onJobFailed(String taskId, String errorMessage);

    /**
     * Called to update the task's progress percentage.
     *
     * @param taskId  the task ID
     * @param percent the progress percentage (0-100)
     */
    void updateProgress(String taskId, Integer percent);
}