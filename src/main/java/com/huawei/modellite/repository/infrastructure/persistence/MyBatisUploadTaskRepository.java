package com.huawei.modellite.repository.infrastructure.persistence;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.TaskStatus;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.infrastructure.persistence.mapper.UploadTaskMapper;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.UploadTask;
import com.huawei.modellite.repository.weighttask.domain.repository.UploadTaskRepository;

import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MyBatisUploadTaskRepository implements UploadTaskRepository {

    private final UploadTaskMapper uploadTaskMapper;

    public MyBatisUploadTaskRepository(UploadTaskMapper uploadTaskMapper) {
        this.uploadTaskMapper = uploadTaskMapper;
    }

    @Override
    public void save(UploadTask task) {
        uploadTaskMapper.insert(task);
    }

    @Override
    public Optional<UploadTask> findById(UUID taskId) {
        return Optional.ofNullable(uploadTaskMapper.selectById(taskId));
    }

    @Override
    public List<UploadTask> findByModelId(UUID modelId) {
        return uploadTaskMapper.selectByModelId(modelId);
    }

    @Override
    public List<UploadTask> findByVersionId(UUID versionId) {
        return uploadTaskMapper.selectByVersionId(versionId);
    }

    @Override
    public Optional<UploadTask> findActiveByVersionId(UUID versionId) {
        return Optional.ofNullable(uploadTaskMapper.selectActiveByVersionId(versionId));
    }

    @Override
    public List<UploadTask> findByStatus(TaskStatus status) {
        return uploadTaskMapper.selectByStatus(status);
    }

    @Override
    public List<UploadTask> findByStatusIn(List<TaskStatus> statuses) {
        return uploadTaskMapper.selectByStatusIn(statuses);
    }

    @Override
    public List<UploadTask> findTerminalTasksOlderThan(long maxAgeMs) {
        LocalDateTime cutoffTime = LocalDateTime.now().minus(Duration.ofMillis(maxAgeMs));
        return uploadTaskMapper.selectTerminalTasksOlderThan(cutoffTime);
    }

    @Override
    public List<UploadTask> findByStatusInOlderThan(List<TaskStatus> statuses, long maxAgeMs) {
        LocalDateTime cutoffTime = LocalDateTime.now().minus(Duration.ofMillis(maxAgeMs));
        return uploadTaskMapper.selectByStatusInOlderThan(statuses, cutoffTime);
    }

    @Override
    public void update(UploadTask task) {
        int affectedRows = uploadTaskMapper.update(task);
        if (affectedRows == 0) {
            throw new ModelLiteException(ErrorCode.UPLOAD_TASK_STATUS_CONFLICT, "任务状态已被其他操作更新");
        }
    }

    @Override
    public void updateProgress(UploadTask task) {
        int affectedRows = uploadTaskMapper.updateProgress(task);
        if (affectedRows == 0) {
            throw new ModelLiteException(ErrorCode.UPLOAD_TASK_STATUS_CONFLICT, "任务进度已被其他操作更新");
        }
    }

    @Override
    public void deleteById(UUID taskId) {
        uploadTaskMapper.deleteById(taskId);
    }
}
