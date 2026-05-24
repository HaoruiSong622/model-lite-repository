package com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.TaskStatus;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
public class UploadTask {

    private static final Set<TaskStatus> TERMINAL_STATUSES = Set.of(
            TaskStatus.COMPLETED, TaskStatus.FAILED, TaskStatus.CANCELLED
    );

    private UUID taskId;
    private UUID modelId;
    private UUID versionId;
    private SourcePath sourcePath;
    private String targetPath;
    private Integer progress;
    private TaskStatus status;
    private String errorMessage;
    private String createUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long version;

    private UploadTask() {
    }

    public static UploadTask createUploadTask(UUID taskId, UUID modelId, UUID versionId,
                                               SourcePath sourcePath, String targetPath,
                                               String createUser) {
        if (sourcePath == null) {
            throw new ModelLiteException(ErrorCode.UPLOAD_SOURCE_PATH_INVALID,
                    "源路径不能为空");
        }

        UploadTask task = new UploadTask();
        task.taskId = taskId;
        task.modelId = modelId;
        task.versionId = versionId;
        task.sourcePath = sourcePath;
        task.targetPath = targetPath;
        task.progress = 0;
        task.status = TaskStatus.PENDING;
        task.errorMessage = null;
        task.createUser = createUser;
        task.createTime = LocalDateTime.now();
        task.updateTime = LocalDateTime.now();
        task.version = 0L;
        return task;
    }

    public void start() {
        ensureNotTerminal("start");
        ensureStatusIn(Set.of(TaskStatus.PENDING, TaskStatus.PAUSED), "start");
        this.status = TaskStatus.RUNNING;
        this.updateTime = LocalDateTime.now();
    }

    public void pause() {
        ensureNotTerminal("pause");
        ensureStatus(TaskStatus.RUNNING, "pause");
        this.status = TaskStatus.PAUSED;
        this.updateTime = LocalDateTime.now();
    }

    public void resume() {
        ensureNotTerminal("resume");
        ensureStatus(TaskStatus.PAUSED, "resume");
        this.status = TaskStatus.PENDING;
        this.updateTime = LocalDateTime.now();
    }

    public void cancel() {
        ensureNotTerminal("cancel");
        ensureStatusIn(Set.of(TaskStatus.PENDING, TaskStatus.RUNNING, TaskStatus.PAUSED), "cancel");
        this.status = TaskStatus.CANCELLED;
        this.updateTime = LocalDateTime.now();
    }

    public void complete() {
        ensureNotTerminal("complete");
        ensureStatus(TaskStatus.RUNNING, "complete");
        this.status = TaskStatus.COMPLETED;
        this.progress = 100;
        this.updateTime = LocalDateTime.now();
    }

    public void fail(String message) {
        ensureNotTerminal("fail");
        ensureStatusIn(Set.of(TaskStatus.PENDING, TaskStatus.RUNNING), "fail");
        this.status = TaskStatus.FAILED;
        this.errorMessage = message;
        this.updateTime = LocalDateTime.now();
    }

    public void updateProgress(Integer percent) {
        ensureNotTerminal("updateProgress");
        ensureStatus(TaskStatus.RUNNING, "updateProgress");
        if (percent < 0 || percent > 100) {
            throw new ModelLiteException(ErrorCode.UPLOAD_TASK_INVALID_PROGRESS,
                    "进度必须在0-100之间，当前值: " + percent);
        }
        if (percent >= this.progress) {
            this.progress = percent;
            this.updateTime = LocalDateTime.now();
        }
    }

    public boolean isTerminal() {
        return TERMINAL_STATUSES.contains(this.status);
    }

    private void ensureNotTerminal(String action) {
        if (isTerminal()) {
            throw new ModelLiteException(ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED,
                    "任务已处于终态，无法执行操作: " + action + "，当前状态: " + this.status.getDbValue());
        }
    }

    private void ensureStatus(TaskStatus expected, String action) {
        if (this.status != expected) {
            throw new ModelLiteException(ErrorCode.UPLOAD_TASK_STATUS_CONFLICT,
                    "操作 " + action + " 需要状态 " + expected.getDbValue() +
                            "，当前状态: " + this.status.getDbValue());
        }
    }

    private void ensureStatusIn(Set<TaskStatus> expected, String action) {
        if (!expected.contains(this.status)) {
            throw new ModelLiteException(ErrorCode.UPLOAD_TASK_STATUS_CONFLICT,
                    "操作 " + action + " 需要状态 " + expected +
                            "，当前状态: " + this.status.getDbValue());
        }
    }
}
