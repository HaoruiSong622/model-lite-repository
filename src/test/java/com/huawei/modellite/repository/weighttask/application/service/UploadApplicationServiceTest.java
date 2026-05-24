package com.huawei.modellite.repository.weighttask.application.service;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.TaskStatus;
import com.huawei.modellite.repository.common.enums.VersionStatus;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.infrastructure.config.WeightImportConfig;
import com.huawei.modellite.repository.modelweight.application.dto.VersionResponse;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.Model;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.ModelVersion;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.StoragePath;
import com.huawei.modellite.repository.modelweight.domain.repository.ModelRepository;
import com.huawei.modellite.repository.weighttask.application.dto.ArchiveRequest;
import com.huawei.modellite.repository.weighttask.application.dto.UploadTaskCreateRequest;
import com.huawei.modellite.repository.weighttask.application.dto.UploadTaskResponse;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.SourcePath;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.UploadTask;
import com.huawei.modellite.repository.weighttask.domain.dto.UploadJobSpec;
import com.huawei.modellite.repository.weighttask.domain.repository.UploadTaskRepository;
import com.huawei.modellite.repository.weighttask.domain.service.K8sJobService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadApplicationServiceTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private UploadTaskRepository uploadTaskRepository;

    @Mock
    private K8sJobService k8sJobService;

    @Mock
    private WeightImportConfig weightImportConfig;

    private UploadApplicationService service;

    @BeforeEach
    void setUp() {
        service = new UploadApplicationService(modelRepository, uploadTaskRepository,
                k8sJobService, weightImportConfig);
        lenient().when(weightImportConfig.getSuffixWhitelist()).thenReturn(".safetensors,.bin");
        lenient().when(weightImportConfig.getImage()).thenReturn("file-copier:test");
        lenient().when(weightImportConfig.getNamespace()).thenReturn("default");
    }

    @Nested
    @DisplayName("createUploadTask tests")
    class CreateUploadTaskTests {

        @Test
        @DisplayName("should create upload task with NFS source successfully")
        void should_createUploadTask_withNfs_successfully() {
            UUID modelId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "default", null, "author", "series", "1000", 512L);

            UploadTaskCreateRequest request = new UploadTaskCreateRequest();
            request.setSourceType("NFS");
            request.setNfsServer("192.168.1.1");
            request.setNfsPath("/nfs/models");
            request.setWeightType("FP32");

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.of(model));
            doNothing().when(modelRepository).saveVersion(any(UUID.class), any(ModelVersion.class));
            when(uploadTaskRepository.findActiveByVersionId(any())).thenReturn(Optional.empty());
            doNothing().when(uploadTaskRepository).save(any(UploadTask.class));
            doNothing().when(k8sJobService).createUploadJob(any(UploadJobSpec.class));

            UploadTaskResponse response = service.createUploadTask(modelId, request, "testUser");

            assertNotNull(response);
            assertEquals(modelId, response.getModelId());
            assertEquals(2, response.getVersionNumber());
            assertEquals("NFS", response.getSourceType());
            assertEquals("192.168.1.1:/nfs/models", response.getSourcePath());
            assertEquals(0, response.getProgress());
            assertEquals("Pending", response.getStatus());
            assertEquals("testUser", response.getCreateUser());
            verify(modelRepository).saveVersion(any(UUID.class), any(ModelVersion.class));
            verify(uploadTaskRepository).save(any(UploadTask.class));
            verify(k8sJobService).createUploadJob(any(UploadJobSpec.class));
        }

        @Test
        @DisplayName("should throw MODEL_NOT_FOUND when model does not exist")
        void should_throw_whenModelNotFound() {
            UUID modelId = UUID.randomUUID();
            UploadTaskCreateRequest request = new UploadTaskCreateRequest();
            request.setSourceType("NFS");
            request.setNfsServer("192.168.1.1");
            request.setNfsPath("/nfs/models");

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.createUploadTask(modelId, request, "testUser"));

            assertEquals(ErrorCode.MODEL_NOT_FOUND, exception.getCode());
            verify(uploadTaskRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw UPLOAD_TASK_ACTIVE_EXISTS when active task exists for version")
        void should_throw_whenActiveTaskExists() {
            UUID modelId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "default", null, "author", "series", "1000", 512L);

            UploadTaskCreateRequest request = new UploadTaskCreateRequest();
            request.setSourceType("PVC");
            request.setSourcePvcName("test-pvc");
            request.setSourceInternalPath("/models");
            request.setWeightType("FP16");

            UploadTask existingTask = UploadTask.createUploadTask(
                    UUID.randomUUID(), modelId, UUID.randomUUID(),
                    SourcePath.ofPvc("pvc", "/path"), "target", "user");

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.of(model));
            doNothing().when(modelRepository).saveVersion(any(UUID.class), any(ModelVersion.class));
            when(uploadTaskRepository.findActiveByVersionId(any())).thenReturn(Optional.of(existingTask));

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.createUploadTask(modelId, request, "testUser"));

            assertEquals(ErrorCode.UPLOAD_TASK_ACTIVE_EXISTS, exception.getCode());
            verify(uploadTaskRepository, never()).save(any());
            verify(k8sJobService, never()).createUploadJob(any());
        }
    }

    @Nested
    @DisplayName("getUploadTask tests")
    class GetUploadTaskTests {

        @Test
        @DisplayName("should return upload task successfully")
        void should_returnUploadTask_successfully() {
            UUID modelId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            UploadTask task = UploadTask.createUploadTask(
                    taskId, modelId, versionId,
                    SourcePath.ofNfs("192.168.1.1", "/nfs"),
                    modelId + "/" + versionId, "user");

            ModelVersion version = new ModelVersion(versionId, 3, StoragePath.empty(),
                    "FP32", VersionStatus.NO_WEIGHT, false, false, null);

            when(uploadTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(modelRepository.findVersionById(modelId, versionId)).thenReturn(Optional.of(version));

            UploadTaskResponse response = service.getUploadTask(modelId, taskId);

            assertNotNull(response);
            assertEquals(taskId, response.getTaskId());
            assertEquals(modelId, response.getModelId());
            assertEquals(versionId, response.getVersionId());
            assertEquals(3, response.getVersionNumber());
            assertEquals("NFS", response.getSourceType());
            assertEquals("Pending", response.getStatus());
        }

        @Test
        @DisplayName("should throw UPLOAD_TASK_NOT_FOUND when task does not exist")
        void should_throw_whenTaskNotFound() {
            UUID modelId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();

            when(uploadTaskRepository.findById(taskId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.getUploadTask(modelId, taskId));

            assertEquals(ErrorCode.UPLOAD_TASK_NOT_FOUND, exception.getCode());
        }

        @Test
        @DisplayName("should throw UPLOAD_TASK_NOT_FOUND when task belongs to different model")
        void should_throw_whenModelIdMismatch() {
            UUID modelId = UUID.randomUUID();
            UUID otherModelId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();

            UploadTask task = UploadTask.createUploadTask(
                    taskId, otherModelId, UUID.randomUUID(),
                    SourcePath.ofNfs("192.168.1.1", "/nfs"),
                    "target", "user");

            when(uploadTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.getUploadTask(modelId, taskId));

            assertEquals(ErrorCode.UPLOAD_TASK_NOT_FOUND, exception.getCode());
        }
    }

    @Nested
    @DisplayName("listUploadTasks tests")
    class ListUploadTasksTests {

        @Test
        @DisplayName("should return filtered tasks by status")
        void should_returnTasks_withStatusFilter() {
            UUID modelId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            UploadTask task1 = UploadTask.createUploadTask(
                    UUID.randomUUID(), modelId, versionId,
                    SourcePath.ofNfs("192.168.1.1", "/nfs"),
                    "target", "user");
            task1.start();

            UploadTask task2 = UploadTask.createUploadTask(
                    UUID.randomUUID(), modelId, versionId,
                    SourcePath.ofNfs("192.168.1.2", "/nfs2"),
                    "target2", "user2");

            ModelVersion version = new ModelVersion(versionId, 2, StoragePath.empty(),
                    "FP32", VersionStatus.NO_WEIGHT, false, false, null);

            when(uploadTaskRepository.findByModelId(modelId)).thenReturn(List.of(task1, task2));
            when(modelRepository.findVersionById(modelId, versionId))
                    .thenReturn(Optional.of(version));

            List<UploadTaskResponse> responses = service.listUploadTasks(modelId, "Running");

            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals("Running", responses.get(0).getStatus());
        }

        @Test
        @DisplayName("should return all tasks when no status filter")
        void should_returnAllTasks_withoutFilter() {
            UUID modelId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            UploadTask task1 = UploadTask.createUploadTask(
                    UUID.randomUUID(), modelId, versionId,
                    SourcePath.ofNfs("192.168.1.1", "/nfs"),
                    "target", "user");

            UploadTask task2 = UploadTask.createUploadTask(
                    UUID.randomUUID(), modelId, versionId,
                    SourcePath.ofNfs("192.168.1.2", "/nfs2"),
                    "target2", "user2");

            ModelVersion version = new ModelVersion(versionId, 2, StoragePath.empty(),
                    "FP32", VersionStatus.NO_WEIGHT, false, false, null);

            when(uploadTaskRepository.findByModelId(modelId)).thenReturn(List.of(task1, task2));
            when(modelRepository.findVersionById(modelId, versionId))
                    .thenReturn(Optional.of(version));

            List<UploadTaskResponse> responses = service.listUploadTasks(modelId, null);

            assertNotNull(responses);
            assertEquals(2, responses.size());
        }
    }

    @Nested
    @DisplayName("pauseUploadTask tests")
    class PauseUploadTaskTests {

        @Test
        @DisplayName("should pause running task and delete job")
        void should_pauseTask_andDeleteJob() {
            UUID modelId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            UploadTask task = UploadTask.createUploadTask(
                    taskId, modelId, versionId,
                    SourcePath.ofNfs("192.168.1.1", "/nfs"),
                    "target", "user");
            task.start();

            when(uploadTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
            doNothing().when(uploadTaskRepository).update(any(UploadTask.class));
            doNothing().when(k8sJobService).deleteJob(taskId.toString());

            service.pauseUploadTask(modelId, taskId);

            assertEquals(TaskStatus.PAUSED, task.getStatus());
            verify(uploadTaskRepository).update(task);
            verify(k8sJobService).deleteJob(taskId.toString());
        }
    }

    @Nested
    @DisplayName("cancelUploadTask tests")
    class CancelUploadTaskTests {

        @Test
        @DisplayName("should cancel pending task and delete job")
        void should_cancelTask_andDeleteJob() {
            UUID modelId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            UploadTask task = UploadTask.createUploadTask(
                    taskId, modelId, versionId,
                    SourcePath.ofNfs("192.168.1.1", "/nfs"),
                    "target", "user");

            ModelVersion version = new ModelVersion(versionId, 2, StoragePath.empty(),
                    "FP32", VersionStatus.UPLOADING, false, false, null);

            when(uploadTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
            doNothing().when(uploadTaskRepository).update(any(UploadTask.class));
            doNothing().when(k8sJobService).deleteJob(taskId.toString());
            when(modelRepository.findVersionById(modelId, versionId)).thenReturn(Optional.of(version));
            doNothing().when(modelRepository).updateVersion(any(ModelVersion.class));

            service.cancelUploadTask(modelId, taskId);

            assertEquals(TaskStatus.CANCELLED, task.getStatus());
            verify(uploadTaskRepository).update(task);
            verify(k8sJobService).deleteJob(taskId.toString());
        }
    }

    @Nested
    @DisplayName("deleteUploadTask tests")
    class DeleteUploadTaskTests {

        @Test
        @DisplayName("should delete terminal task and job resources")
        void should_deleteTerminalTask_successfully() {
            UUID modelId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            UploadTask task = UploadTask.createUploadTask(
                    taskId, modelId, versionId,
                    SourcePath.ofNfs("192.168.1.1", "/nfs"),
                    "target", "user");
            task.cancel();

            when(uploadTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
            doNothing().when(k8sJobService).deleteJobResources(taskId.toString());
            doNothing().when(uploadTaskRepository).deleteById(taskId);

            service.deleteUploadTask(modelId, taskId);

            verify(k8sJobService).deleteJobResources(taskId.toString());
            verify(uploadTaskRepository).deleteById(taskId);
        }

        @Test
        @DisplayName("should throw when deleting non-terminal task")
        void should_throw_whenDeletingNonTerminalTask() {
            UUID modelId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            UploadTask task = UploadTask.createUploadTask(
                    taskId, modelId, versionId,
                    SourcePath.ofNfs("192.168.1.1", "/nfs"),
                    "target", "user");

            when(uploadTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.deleteUploadTask(modelId, taskId));

            assertEquals(ErrorCode.UPLOAD_TASK_STATUS_CONFLICT, exception.getCode());
            verify(uploadTaskRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("archiveTrainingWeight tests")
    class ArchiveTrainingWeightTests {

        @Test
        @DisplayName("should archive training weight with PVC successfully")
        void should_archive_withPvc_successfully() {
            UUID modelId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "default", null, "author", "series", "1000", 512L);

            ArchiveRequest request = new ArchiveRequest();
            request.setSourceType("PVC");
            request.setPvcName("archive-pvc");
            request.setInternalPath("/weights");
            request.setWeightType("safetensors");

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.of(model));
            doNothing().when(modelRepository).saveVersion(any(UUID.class), any(ModelVersion.class));

            VersionResponse response = service.archiveTrainingWeight(modelId, request);

            assertNotNull(response);
            assertEquals(2, response.getVersionNumber());
            assertEquals(VersionStatus.AVAILABLE.getDbValue(), response.getStatus());
            assertTrue(response.getRegistered());
            assertEquals("PVC", response.getSourceType());
            assertEquals("archive-pvc", response.getPvcName());
            assertEquals("/weights", response.getInternalPath());
            assertEquals("safetensors", response.getWeightType());
            verify(modelRepository).saveVersion(any(UUID.class), any(ModelVersion.class));
        }

        @Test
        @DisplayName("should throw MODEL_NOT_FOUND when model does not exist")
        void should_throw_whenModelNotFound() {
            UUID modelId = UUID.randomUUID();
            ArchiveRequest request = new ArchiveRequest();
            request.setSourceType("NFS");

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.archiveTrainingWeight(modelId, request));

            assertEquals(ErrorCode.MODEL_NOT_FOUND, exception.getCode());
        }
    }

    @Nested
    @DisplayName("TaskReconciler callback tests")
    class CallbackTests {

        @Test
        @DisplayName("onJobRunning should start task and update version to UPLOADING")
        void onJobRunning_should_startTask_andUpdateVersion() {
            UUID modelId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            UploadTask task = UploadTask.createUploadTask(
                    taskId, modelId, versionId,
                    SourcePath.ofNfs("192.168.1.1", "/nfs"),
                    "target", "user");

            ModelVersion version = new ModelVersion(versionId, 2, StoragePath.empty(),
                    "FP32", VersionStatus.NO_WEIGHT, false, false, null);

            when(uploadTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
            doNothing().when(uploadTaskRepository).update(any(UploadTask.class));
            when(modelRepository.findVersionById(modelId, versionId)).thenReturn(Optional.of(version));
            doNothing().when(modelRepository).updateVersion(any(ModelVersion.class));

            service.onJobRunning(taskId.toString());

            assertEquals(TaskStatus.RUNNING, task.getStatus());
            assertEquals(VersionStatus.UPLOADING, version.getStatus());
            verify(uploadTaskRepository).update(task);
            verify(modelRepository).updateVersion(version);
        }

        @Test
        @DisplayName("onJobCompleted should complete task and update version to AVAILABLE")
        void onJobCompleted_should_completeTask_andUpdateVersion() {
            UUID modelId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            UploadTask task = UploadTask.createUploadTask(
                    taskId, modelId, versionId,
                    SourcePath.ofNfs("192.168.1.1", "/nfs"),
                    "target", "user");
            task.start();

            ModelVersion version = new ModelVersion(versionId, 2, StoragePath.empty(),
                    "FP32", VersionStatus.UPLOADING, false, false, null);

            when(uploadTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
            doNothing().when(uploadTaskRepository).update(any(UploadTask.class));
            when(modelRepository.findVersionById(modelId, versionId)).thenReturn(Optional.of(version));
            doNothing().when(modelRepository).updateVersion(any(ModelVersion.class));

            service.onJobCompleted(taskId.toString());

            assertEquals(TaskStatus.COMPLETED, task.getStatus());
            assertEquals(100, task.getProgress());
            assertEquals(VersionStatus.AVAILABLE, version.getStatus());
            verify(uploadTaskRepository).update(task);
            verify(modelRepository).updateVersion(version);
        }

        @Test
        @DisplayName("onJobFailed should fail task and update version to UPLOAD_FAILED")
        void onJobFailed_should_failTask_andUpdateVersion() {
            UUID modelId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            UploadTask task = UploadTask.createUploadTask(
                    taskId, modelId, versionId,
                    SourcePath.ofNfs("192.168.1.1", "/nfs"),
                    "target", "user");
            task.start();

            ModelVersion version = new ModelVersion(versionId, 2, StoragePath.empty(),
                    "FP32", VersionStatus.UPLOADING, false, false, null);

            when(uploadTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
            doNothing().when(uploadTaskRepository).update(any(UploadTask.class));
            when(modelRepository.findVersionById(modelId, versionId)).thenReturn(Optional.of(version));
            doNothing().when(modelRepository).updateVersion(any(ModelVersion.class));

            service.onJobFailed(taskId.toString(), "Connection timeout");

            assertEquals(TaskStatus.FAILED, task.getStatus());
            assertEquals("Connection timeout", task.getErrorMessage());
            assertEquals(VersionStatus.UPLOAD_FAILED, version.getStatus());
            verify(uploadTaskRepository).update(task);
            verify(modelRepository).updateVersion(version);
        }

        @Test
        @DisplayName("updateProgress should update task progress")
        void updateProgress_should_updateTaskProgress() {
            UUID modelId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            UploadTask task = UploadTask.createUploadTask(
                    taskId, modelId, versionId,
                    SourcePath.ofNfs("192.168.1.1", "/nfs"),
                    "target", "user");
            task.start();

            when(uploadTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
            doNothing().when(uploadTaskRepository).updateProgress(any(UploadTask.class));

            service.updateProgress(taskId.toString(), 75);

            assertEquals(75, task.getProgress());
            verify(uploadTaskRepository).updateProgress(task);
        }
    }
}
