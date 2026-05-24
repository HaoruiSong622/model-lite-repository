package com.huawei.modellite.repository.weighttask.domain.repository;

import com.huawei.modellite.repository.common.enums.TaskStatus;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.UploadTask;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UploadTask aggregate persistence.
 * Defines domain-level persistence operations without implementation details.
 */
public interface UploadTaskRepository {

    /**
     * Save a new upload task.
     *
     * @param task the upload task to save
     */
    void save(UploadTask task);

    /**
     * Find an upload task by its ID.
     *
     * @param taskId the task ID
     * @return Optional containing the task if found
     */
    Optional<UploadTask> findById(UUID taskId);

    /**
     * Find all upload tasks for a given model, ordered by creation time descending.
     *
     * @param modelId the model ID
     * @return list of upload tasks
     */
    List<UploadTask> findByModelId(UUID modelId);

    /**
     * Find all upload tasks for a given version.
     *
     * @param versionId the version ID
     * @return list of upload tasks
     */
    List<UploadTask> findByVersionId(UUID versionId);

    /**
     * Find the active upload task for a given version.
     * Active means status is one of: Pending, Running, Paused.
     *
     * @param versionId the version ID
     * @return Optional containing the active task if exists
     */
    Optional<UploadTask> findActiveByVersionId(UUID versionId);

    /**
     * Find all upload tasks with a given status.
     * Used by TaskReconciler for scanning tasks.
     *
     * @param status the task status
     * @return list of upload tasks
     */
    List<UploadTask> findByStatus(TaskStatus status);

    /**
     * Find all upload tasks with any of the given statuses.
     *
     * @param statuses the list of task statuses
     * @return list of upload tasks
     */
    List<UploadTask> findByStatusIn(List<TaskStatus> statuses);

    /**
     * Update an upload task's status/progress with optimistic locking.
     * The version field is incremented on successful update.
     *
     * @param task the upload task with updated fields
     */
    void update(UploadTask task);

    /**
     * Update only the progress field of an upload task.
     *
     * @param task the upload task with updated progress
     */
    void updateProgress(UploadTask task);

    /**
     * Find terminal tasks (Completed/Failed/Cancelled) older than the specified age.
     * @param maxAgeMs maximum age in milliseconds
     * @return list of terminal tasks older than maxAgeMs
     */
    List<UploadTask> findTerminalTasksOlderThan(long maxAgeMs);

    /**
     * Find tasks with given statuses that are older than the specified age.
     * @param statuses list of task statuses to filter
     * @param maxAgeMs maximum age in milliseconds
     * @return list of tasks matching criteria
     */
    List<UploadTask> findByStatusInOlderThan(List<TaskStatus> statuses, long maxAgeMs);

    /**
     * Delete an upload task by its ID (hard delete).
     *
     * @param taskId the task ID
     */
    void deleteById(UUID taskId);
}
