package com.huawei.modellite.repository.infrastructure.persistence.mapper;

import com.huawei.modellite.repository.common.enums.TaskStatus;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.UploadTask;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface UploadTaskMapper {

    void insert(UploadTask task);

    UploadTask selectById(UUID taskId);

    List<UploadTask> selectByModelId(UUID modelId);

    List<UploadTask> selectByVersionId(UUID versionId);

    UploadTask selectActiveByVersionId(UUID versionId);

    List<UploadTask> selectByStatus(TaskStatus status);

    List<UploadTask> selectByStatusIn(@Param("statuses") List<TaskStatus> statuses);

    int update(UploadTask task);

    int updateProgress(UploadTask task);

    int deleteById(UUID taskId);
}
