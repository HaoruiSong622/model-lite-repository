package com.huawei.modellite.repository.weighttask.application.service;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.SourceType;
import com.huawei.modellite.repository.common.enums.TaskStatus;
import com.huawei.modellite.repository.common.enums.VersionStatus;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.infrastructure.config.WeightImportConfig;
import com.huawei.modellite.repository.modelweight.application.dto.TrainingMetadataDto;
import com.huawei.modellite.repository.modelweight.application.dto.VersionResponse;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.Model;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.ModelVersion;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.StoragePath;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.TrainingMetadata;
import com.huawei.modellite.repository.modelweight.domain.repository.ModelRepository;
import com.huawei.modellite.repository.weighttask.application.dto.ArchiveRequest;
import com.huawei.modellite.repository.weighttask.application.dto.UploadTaskCreateRequest;
import com.huawei.modellite.repository.weighttask.application.dto.UploadTaskResponse;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.SourcePath;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.UploadTask;
import com.huawei.modellite.repository.weighttask.domain.dto.UploadJobSpec;
import com.huawei.modellite.repository.weighttask.domain.repository.UploadTaskRepository;
import com.huawei.modellite.repository.weighttask.domain.service.K8sJobService;
import com.huawei.modellite.repository.weighttask.domain.service.TaskEventCallback;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UploadApplicationService implements TaskEventCallback {

    private final ModelRepository modelRepository;
    private final UploadTaskRepository uploadTaskRepository;
    private final K8sJobService k8sJobService;
    private final WeightImportConfig weightImportConfig;

    public UploadApplicationService(ModelRepository modelRepository,
                                    UploadTaskRepository uploadTaskRepository,
                                    K8sJobService k8sJobService,
                                    WeightImportConfig weightImportConfig) {
        this.modelRepository = modelRepository;
        this.uploadTaskRepository = uploadTaskRepository;
        this.k8sJobService = k8sJobService;
        this.weightImportConfig = weightImportConfig;
    }

    public UploadTaskResponse createUploadTask(UUID modelId, UploadTaskCreateRequest request, String createUser) {
        Model model = modelRepository.findByIdWithVersions(modelId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.MODEL_NOT_FOUND,
                        "模型不存在: " + modelId));

        TrainingMetadata trainingMetadata = buildTrainingMetadata(request.getTrainingMetadata());

        ModelVersion version = model.createVersion(
                StoragePath.empty(),
                request.getWeightType(),
                VersionStatus.NO_WEIGHT,
                false,
                trainingMetadata
        );
        modelRepository.saveVersion(modelId, version);

        Optional<UploadTask> activeTask = uploadTaskRepository.findActiveByVersionId(version.getVersionId());
        if (activeTask.isPresent()) {
            throw new ModelLiteException(ErrorCode.UPLOAD_TASK_ACTIVE_EXISTS,
                    "该版本已存在活跃的上传任务: " + version.getVersionId());
        }

        SourcePath sourcePath = buildSourcePath(request);
        String targetPath = modelId + "/" + version.getVersionId();

        UploadTask task = UploadTask.createUploadTask(
                UUID.randomUUID(),
                modelId,
                version.getVersionId(),
                sourcePath,
                targetPath,
                createUser
        );

        uploadTaskRepository.save(task);
        createK8sJob(task, version);

        return toUploadTaskResponse(task, version.getVersionNumber());
    }

    public UploadTaskResponse getUploadTask(UUID modelId, UUID taskId) {
        UploadTask task = uploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.UPLOAD_TASK_NOT_FOUND,
                        "上传任务不存在: " + taskId));

        if (!task.getModelId().equals(modelId)) {
            throw new ModelLiteException(ErrorCode.UPLOAD_TASK_NOT_FOUND,
                    "上传任务不存在: " + taskId);
        }

        Integer versionNumber = getVersionNumber(modelId, task.getVersionId());
        return toUploadTaskResponse(task, versionNumber);
    }

    public List<UploadTaskResponse> listUploadTasks(UUID modelId, String statusFilter) {
        List<UploadTask> tasks = uploadTaskRepository.findByModelId(modelId);

        if (statusFilter != null && !statusFilter.isEmpty()) {
            TaskStatus filterStatus = TaskStatus.fromDbValue(statusFilter);
            tasks = tasks.stream()
                    .filter(t -> t.getStatus() == filterStatus)
                    .collect(Collectors.toList());
        }

        return tasks.stream()
                .map(t -> toUploadTaskResponse(t, getVersionNumber(modelId, t.getVersionId())))
                .collect(Collectors.toList());
    }

    public void pauseUploadTask(UUID modelId, UUID taskId) {
        UploadTask task = findAndVerifyTask(modelId, taskId);
        task.pause();
        uploadTaskRepository.update(task);
        k8sJobService.deleteJob(taskId.toString());
    }

    public void resumeUploadTask(UUID modelId, UUID taskId) {
        UploadTask task = findAndVerifyTask(modelId, taskId);
        task.resume();
        uploadTaskRepository.update(task);

        ModelVersion version = modelRepository.findVersionById(modelId, task.getVersionId())
                .orElseThrow(() -> new ModelLiteException(ErrorCode.VERSION_NOT_FOUND,
                        "版本不存在: " + task.getVersionId()));
        createK8sJob(task, version);
    }

    public void cancelUploadTask(UUID modelId, UUID taskId) {
        UploadTask task = findAndVerifyTask(modelId, taskId);
        task.cancel();
        uploadTaskRepository.update(task);
        k8sJobService.deleteJob(taskId.toString());

        ModelVersion version = modelRepository.findVersionById(modelId, task.getVersionId())
                .orElseThrow(() -> new ModelLiteException(ErrorCode.VERSION_NOT_FOUND,
                        "版本不存在: " + task.getVersionId()));
        if (version.getStatus() == VersionStatus.UPLOADING) {
            version.updateStatus(VersionStatus.UPLOAD_FAILED);
            modelRepository.updateVersion(version);
        }
    }

    public void deleteUploadTask(UUID modelId, UUID taskId) {
        UploadTask task = findAndVerifyTask(modelId, taskId);

        if (!task.isTerminal()) {
            throw new ModelLiteException(ErrorCode.UPLOAD_TASK_STATUS_CONFLICT,
                    "只能删除处于终态的任务，当前状态: " + task.getStatus().getDbValue());
        }

        k8sJobService.deleteJobResources(taskId.toString());
        uploadTaskRepository.deleteById(taskId);
    }

    public VersionResponse archiveTrainingWeight(UUID modelId, ArchiveRequest request) {
        Model model = modelRepository.findByIdWithVersions(modelId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.MODEL_NOT_FOUND,
                        "模型不存在: " + modelId));

        ModelVersion version = model.createVersion(
                StoragePath.empty(),
                request.getWeightType(),
                VersionStatus.NO_WEIGHT,
                false,
                null
        );

        StoragePath storagePath = buildStoragePathFromArchive(request);
        TrainingMetadata trainingMetadata = buildTrainingMetadata(request.getTrainingMetadata());
        version.register(storagePath, request.getWeightType(), trainingMetadata);

        modelRepository.saveVersion(modelId, version);

        return toVersionResponse(version, modelId);
    }

    public void onJobRunning(String taskId) {
        UUID id = UUID.fromString(taskId);
        UploadTask task = uploadTaskRepository.findById(id)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.UPLOAD_TASK_NOT_FOUND,
                        "上传任务不存在: " + taskId));
        task.start();
        uploadTaskRepository.update(task);

        ModelVersion version = modelRepository.findVersionById(task.getModelId(), task.getVersionId())
                .orElseThrow(() -> new ModelLiteException(ErrorCode.VERSION_NOT_FOUND,
                        "版本不存在: " + task.getVersionId()));
        if (version.getStatus() != VersionStatus.UPLOADING) {
            version.updateStatus(VersionStatus.UPLOADING);
            modelRepository.updateVersion(version);
        }
    }

    public void onJobCompleted(String taskId) {
        UUID id = UUID.fromString(taskId);
        UploadTask task = uploadTaskRepository.findById(id)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.UPLOAD_TASK_NOT_FOUND,
                        "上传任务不存在: " + taskId));
        task.complete();
        uploadTaskRepository.update(task);

        ModelVersion version = modelRepository.findVersionById(task.getModelId(), task.getVersionId())
                .orElseThrow(() -> new ModelLiteException(ErrorCode.VERSION_NOT_FOUND,
                        "版本不存在: " + task.getVersionId()));
        version.updateStatus(VersionStatus.AVAILABLE);
        modelRepository.updateVersion(version);
    }

    public void onJobFailed(String taskId, String errorMessage) {
        UUID id = UUID.fromString(taskId);
        UploadTask task = uploadTaskRepository.findById(id)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.UPLOAD_TASK_NOT_FOUND,
                        "上传任务不存在: " + taskId));
        task.fail(errorMessage);
        uploadTaskRepository.update(task);

        ModelVersion version = modelRepository.findVersionById(task.getModelId(), task.getVersionId())
                .orElseThrow(() -> new ModelLiteException(ErrorCode.VERSION_NOT_FOUND,
                        "版本不存在: " + task.getVersionId()));
        version.updateStatus(VersionStatus.UPLOAD_FAILED);
        modelRepository.updateVersion(version);
    }

    public void updateProgress(String taskId, Integer percent) {
        UUID id = UUID.fromString(taskId);
        UploadTask task = uploadTaskRepository.findById(id)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.UPLOAD_TASK_NOT_FOUND,
                        "上传任务不存在: " + taskId));
        task.updateProgress(percent);
        uploadTaskRepository.updateProgress(task);
    }

    private UploadTask findAndVerifyTask(UUID modelId, UUID taskId) {
        UploadTask task = uploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.UPLOAD_TASK_NOT_FOUND,
                        "上传任务不存在: " + taskId));
        if (!task.getModelId().equals(modelId)) {
            throw new ModelLiteException(ErrorCode.UPLOAD_TASK_NOT_FOUND,
                    "上传任务不存在: " + taskId);
        }
        return task;
    }

    private SourcePath buildSourcePath(UploadTaskCreateRequest request) {
        String sourceType = request.getSourceType();
        if ("NFS".equalsIgnoreCase(sourceType)) {
            return SourcePath.ofNfs(request.getNfsServer(), request.getNfsPath());
        } else if ("CIFS".equalsIgnoreCase(sourceType)) {
            return SourcePath.ofCifs(request.getCifsServer(), request.getCifsShare(),
                    request.getCifsUsername(), request.getCifsPassword());
        } else if ("PVC".equalsIgnoreCase(sourceType)) {
            return SourcePath.ofPvc(request.getSourcePvcName(), request.getSourceInternalPath());
        } else {
            throw new ModelLiteException(ErrorCode.UPLOAD_SOURCE_PATH_INVALID,
                    "不支持的源类型: " + sourceType);
        }
    }

    private StoragePath buildStoragePathFromArchive(ArchiveRequest request) {
        String sourceType = request.getSourceType();
        if ("PVC".equalsIgnoreCase(sourceType)) {
            return StoragePath.ofPvc(request.getPvcName(), request.getInternalPath());
        } else if ("NFS".equalsIgnoreCase(sourceType)) {
            return StoragePath.ofNfs(request.getNfsServer(), request.getNfsPath());
        } else {
            throw new ModelLiteException(ErrorCode.REGISTER_SOURCE_TYPE_REQUIRED,
                    "归档必须指定有效的存储来源类型（PVC或NFS）");
        }
    }

    private TrainingMetadata buildTrainingMetadata(TrainingMetadataDto dto) {
        if (dto == null) {
            return null;
        }
        return new TrainingMetadata(
                dto.getTrainFrame(),
                dto.getTrainType(),
                dto.getTrainStrategy(),
                dto.getTrainTime(),
                dto.getFinalLoss(),
                dto.getSourceVersion()
        );
    }

    private void createK8sJob(UploadTask task, ModelVersion version) {
        String taskIdStr = task.getTaskId().toString();
        String modelIdStr = task.getModelId().toString();
        String versionIdStr = task.getVersionId().toString();
        String targetPvcName = "model-weights";
        String targetSubPath = task.getTargetPath();
        List<String> allowedSuffixes = Arrays.asList(
                weightImportConfig.getSuffixWhitelist().split(","));

        UploadJobSpec spec;
        SourcePath sourcePath = task.getSourcePath();
        SourceType sourceType = sourcePath.getSourceType();

        if (sourceType == SourceType.NFS) {
            spec = UploadJobSpec.ofNfs(
                    taskIdStr, modelIdStr, versionIdStr,
                    sourcePath.getPath(),
                    targetPvcName, targetSubPath,
                    weightImportConfig.getImage(),
                    weightImportConfig.getNamespace(),
                    allowedSuffixes
            );
        } else if (sourceType == SourceType.CIFS) {
            spec = UploadJobSpec.ofCifs(
                    taskIdStr, modelIdStr, versionIdStr,
                    sourcePath.getPath(),
                    targetPvcName, targetSubPath,
                    sourcePath.getCredentials().getUsername(),
                    sourcePath.getCredentials().getPassword(),
                    weightImportConfig.getImage(),
                    weightImportConfig.getNamespace(),
                    allowedSuffixes
            );
        } else if (sourceType == SourceType.PVC) {
            spec = UploadJobSpec.ofPvc(
                    taskIdStr, modelIdStr, versionIdStr,
                    sourcePath.getPath(),
                    targetPvcName, targetSubPath,
                    weightImportConfig.getImage(),
                    weightImportConfig.getNamespace(),
                    allowedSuffixes
            );
        } else {
            throw new ModelLiteException(ErrorCode.UPLOAD_TASK_JOB_SUBMIT_FAILED,
                    "不支持的源类型: " + sourceType);
        }

        k8sJobService.createUploadJob(spec);
    }

    private Integer getVersionNumber(UUID modelId, UUID versionId) {
        return modelRepository.findVersionById(modelId, versionId)
                .map(ModelVersion::getVersionNumber)
                .orElse(null);
    }

    private UploadTaskResponse toUploadTaskResponse(UploadTask task, Integer versionNumber) {
        UploadTaskResponse response = new UploadTaskResponse();
        response.setTaskId(task.getTaskId());
        response.setModelId(task.getModelId());
        response.setVersionId(task.getVersionId());
        response.setVersionNumber(versionNumber);
        if (task.getSourcePath() != null) {
            response.setSourceType(task.getSourcePath().getSourceType().getDbValue());
            response.setSourcePath(task.getSourcePath().getPath());
        }
        response.setTargetPath(task.getTargetPath());
        response.setProgress(task.getProgress());
        response.setStatus(task.getStatus().getDbValue());
        response.setErrorMessage(task.getErrorMessage());
        response.setCreateUser(task.getCreateUser());
        response.setCreateTime(task.getCreateTime());
        response.setUpdateTime(task.getUpdateTime());
        return response;
    }

    private VersionResponse toVersionResponse(ModelVersion version, UUID modelId) {
        VersionResponse response = new VersionResponse();
        response.setId(version.getVersionId());
        response.setModelId(modelId);
        response.setVersionNumber(version.getVersionNumber());
        response.setStatus(version.getStatus() != null ? version.getStatus().getDbValue() : null);
        response.setRegistered(version.isRegistered());
        response.setLocked(version.isLocked());
        if (version.getStoragePath() != null) {
            StoragePath sp = version.getStoragePath();
            response.setSourceType(sp.getSourceType() != null ? sp.getSourceType().getDbValue() : null);
            response.setPvcName(sp.getPvcName());
            response.setInternalPath(sp.getInternalPath());
            response.setNfsServer(sp.getNfsServer());
            response.setNfsPath(sp.getNfsPath());
        }
        response.setWeightType(version.getWeightType());
        if (version.getTrainingMetadata() != null) {
            response.setTrainingMetadata(toTrainingMetadataDto(version.getTrainingMetadata()));
        }
        return response;
    }

    private TrainingMetadataDto toTrainingMetadataDto(TrainingMetadata metadata) {
        TrainingMetadataDto dto = new TrainingMetadataDto();
        dto.setTrainFrame(metadata.getTrainFrame());
        dto.setTrainType(metadata.getTrainType());
        dto.setTrainStrategy(metadata.getTrainStrategy());
        dto.setTrainTime(metadata.getTrainTime());
        dto.setFinalLoss(metadata.getFinalLoss());
        dto.setSourceVersion(metadata.getSourceVersion());
        return dto;
    }
}
