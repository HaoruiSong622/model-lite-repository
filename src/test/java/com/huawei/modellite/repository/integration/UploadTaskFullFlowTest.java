package com.huawei.modellite.repository.integration;

import com.huawei.modellite.repository.common.enums.TaskStatus;
import com.huawei.modellite.repository.common.enums.VersionStatus;
import com.huawei.modellite.repository.modelweight.domain.repository.ModelRepository;
import com.huawei.modellite.repository.weighttask.application.dto.UploadTaskCreateRequest;
import com.huawei.modellite.repository.weighttask.application.dto.UploadTaskResponse;
import com.huawei.modellite.repository.weighttask.application.service.UploadApplicationService;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.UploadTask;
import com.huawei.modellite.repository.weighttask.domain.repository.UploadTaskRepository;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
        "weight-import.job.image=file-copier:test",
        "weight-import.job.namespace=default",
        "weight-import.job.cpu-request=100m",
        "weight-import.job.memory-request=128Mi",
        "weight-import.job.suffix-whitelist=.safetensors,.bin"
})
@EnableKubernetesMockClient(crud = true)
@Import(K8sMockConfig.class)
class UploadTaskFullFlowTest extends AbstractIntegrationTest {

    @Autowired
    private UploadApplicationService uploadApplicationService;

    @Autowired
    private UploadTaskRepository uploadTaskRepository;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Resource(name = "kubernetesClient")
    private Object k8sClient;

    private UUID categoryId;
    private UUID typeId;
    private UUID modelId;

    @AfterAll
    static void tearDown() {
        K8sMockConfig.destroy();
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM upload_task");
        jdbcTemplate.execute("DELETE FROM model_version");
        jdbcTemplate.execute("DELETE FROM model_tag");
        jdbcTemplate.execute("DELETE FROM model");
        jdbcTemplate.execute("DELETE FROM model_type");
        jdbcTemplate.execute("DELETE FROM category");

        categoryId = UUID.randomUUID();
        typeId = UUID.randomUUID();
        modelId = UUID.randomUUID();

        jdbcTemplate.execute(String.format(
                "INSERT INTO category (id, name) VALUES ('%s', 'TestCategory')", categoryId));
        jdbcTemplate.execute(String.format(
                "INSERT INTO model_type (id, category_id, name) VALUES ('%s', '%s', 'TestType')",
                typeId, categoryId));
        jdbcTemplate.execute(String.format(
                "INSERT INTO model (id, name, category_id, type_id, resource_group, create_user) " +
                        "VALUES ('%s', 'TestModel', '%s', '%s', 'test-rg', 'test-user')",
                modelId, categoryId, typeId));

        jdbcTemplate.execute(String.format(
                "INSERT INTO model_version (id, model_id, version_number, status) " +
                        "VALUES ('%s', '%s', 1, 'NoWeight')",
                UUID.randomUUID(), modelId));
    }

    private UploadTaskCreateRequest buildNfsRequest() {
        UploadTaskCreateRequest request = new UploadTaskCreateRequest();
        request.setSourceType("NFS");
        request.setNfsServer("10.0.1.100");
        request.setNfsPath("/data/models/glm-5/v2");
        request.setWeightType("safetensors");
        return request;
    }

    private UploadTaskCreateRequest buildPvcRequest() {
        UploadTaskCreateRequest request = new UploadTaskCreateRequest();
        request.setSourceType("PVC");
        request.setSourcePvcName("source-pvc");
        request.setSourceInternalPath("/models");
        request.setWeightType("safetensors");
        return request;
    }

    private void assertTaskStatus(UUID taskId, TaskStatus expectedStatus) {
        await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() -> {
            Optional<UploadTask> taskOpt = uploadTaskRepository.findById(taskId);
            assertTrue(taskOpt.isPresent(), "Task should exist");
            assertEquals(expectedStatus, taskOpt.get().getStatus());
        });
    }

    private KubernetesClient kubernetesClient() {
        return (KubernetesClient) k8sClient;
    }

    private void assertVersionStatus(UUID versionId, VersionStatus expectedStatus) {
        await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() -> {
            var versionOpt = modelRepository.findVersionById(modelId, versionId);
            assertTrue(versionOpt.isPresent(), "Version should exist");
            assertEquals(expectedStatus, versionOpt.get().getStatus());
        });
    }

    @Test
    @DisplayName("FF-1: full success flow create validation pass copy complete")
    void ff1_fullSuccessFlow() {
        UploadTaskCreateRequest request = buildNfsRequest();
        UploadTaskResponse response = uploadApplicationService.createUploadTask(modelId, request, "test-user");

        assertNotNull(response);
        assertEquals("Pending", response.getStatus());
        UUID taskId = response.getTaskId();
        UUID versionId = response.getVersionId();

        Optional<UploadTask> taskOpt = uploadTaskRepository.findById(taskId);
        assertTrue(taskOpt.isPresent());
        UploadTask task = taskOpt.get();
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertEquals(0, task.getProgress());
        assertEquals(0L, task.getVersion());
        assertVersionStatus(versionId, VersionStatus.NO_WEIGHT);

        String jobName = "upload-" + taskId;
        var job = kubernetesClient().batch().v1().jobs().inNamespace("default").withName(jobName).get();
        assertNotNull(job, "K8s Job should be created");

        uploadApplicationService.onJobRunning(taskId.toString());

        assertTaskStatus(taskId, TaskStatus.RUNNING);
        assertVersionStatus(versionId, VersionStatus.UPLOADING);

        task = uploadTaskRepository.findById(taskId).get();
        assertEquals(1L, task.getVersion(), "Version should increment after status update");

        uploadApplicationService.onJobCompleted(taskId.toString());

        assertTaskStatus(taskId, TaskStatus.COMPLETED);
        assertVersionStatus(versionId, VersionStatus.AVAILABLE);

        task = uploadTaskRepository.findById(taskId).get();
        assertEquals(100, task.getProgress());
        assertEquals(2L, task.getVersion());
    }

    @Test
    @DisplayName("FF-2: validation failure flow create validation fail terminal")
    void ff2_validationFailureFlow() {
        UploadTaskCreateRequest request = buildNfsRequest();
        UploadTaskResponse response = uploadApplicationService.createUploadTask(modelId, request, "test-user");

        UUID taskId = response.getTaskId();
        UUID versionId = response.getVersionId();
        assertEquals("Pending", response.getStatus());

        uploadApplicationService.onJobFailed(taskId.toString(), "Source directory not found or empty");

        assertTaskStatus(taskId, TaskStatus.FAILED);
        assertVersionStatus(versionId, VersionStatus.UPLOAD_FAILED);

        UploadTask task = uploadTaskRepository.findById(taskId).get();
        assertNotNull(task.getErrorMessage());
        assertTrue(task.getErrorMessage().contains("Source directory not found"));
    }

    @Test
    @DisplayName("FF-3: pause resume flow create running pause resume complete")
    void ff3_pauseResumeFlow() {
        UploadTaskCreateRequest request = buildPvcRequest();
        UploadTaskResponse response = uploadApplicationService.createUploadTask(modelId, request, "test-user");

        UUID taskId = response.getTaskId();
        UUID versionId = response.getVersionId();

        uploadApplicationService.onJobRunning(taskId.toString());
        assertTaskStatus(taskId, TaskStatus.RUNNING);
        assertVersionStatus(versionId, VersionStatus.UPLOADING);

        uploadApplicationService.pauseUploadTask(modelId, taskId);
        assertTaskStatus(taskId, TaskStatus.PAUSED);

        String jobName = "upload-" + taskId;
        var job = kubernetesClient().batch().v1().jobs().inNamespace("default").withName(jobName).get();
        assertNull(job, "Job should be deleted after pause");

        uploadApplicationService.resumeUploadTask(modelId, taskId);
        assertTaskStatus(taskId, TaskStatus.PENDING);

        job = kubernetesClient().batch().v1().jobs().inNamespace("default").withName(jobName).get();
        assertNotNull(job, "Job should be recreated after resume");

        uploadApplicationService.onJobRunning(taskId.toString());
        assertTaskStatus(taskId, TaskStatus.RUNNING);

        uploadApplicationService.onJobCompleted(taskId.toString());
        assertTaskStatus(taskId, TaskStatus.COMPLETED);
        assertVersionStatus(versionId, VersionStatus.AVAILABLE);

        UploadTask task = uploadTaskRepository.findById(taskId).get();
        assertEquals(100, task.getProgress());
    }

    @Test
    @DisplayName("FF-4: user cancel flow create running user cancel")
    void ff4_userCancelFlow() {
        UploadTaskCreateRequest request = buildNfsRequest();
        UploadTaskResponse response = uploadApplicationService.createUploadTask(modelId, request, "test-user");

        UUID taskId = response.getTaskId();
        UUID versionId = response.getVersionId();

        uploadApplicationService.onJobRunning(taskId.toString());
        assertTaskStatus(taskId, TaskStatus.RUNNING);

        uploadApplicationService.cancelUploadTask(modelId, taskId);
        assertTaskStatus(taskId, TaskStatus.CANCELLED);
        assertVersionStatus(versionId, VersionStatus.UPLOAD_FAILED);

        String jobName = "upload-" + taskId;
        var job = kubernetesClient().batch().v1().jobs().inNamespace("default").withName(jobName).get();
        assertNull(job, "Job should be deleted after cancel");
    }

    @Test
    @DisplayName("FF-5: copy failure flow create running copy failed")
    void ff5_copyFailureFlow() {
        UploadTaskCreateRequest request = buildNfsRequest();
        UploadTaskResponse response = uploadApplicationService.createUploadTask(modelId, request, "test-user");

        UUID taskId = response.getTaskId();
        UUID versionId = response.getVersionId();

        uploadApplicationService.onJobRunning(taskId.toString());
        assertTaskStatus(taskId, TaskStatus.RUNNING);

        uploadApplicationService.onJobFailed(taskId.toString(), "rsync failed: connection reset by peer");
        assertTaskStatus(taskId, TaskStatus.FAILED);
        assertVersionStatus(versionId, VersionStatus.UPLOAD_FAILED);

        UploadTask task = uploadTaskRepository.findById(taskId).get();
        assertNotNull(task.getErrorMessage());
        assertTrue(task.getErrorMessage().contains("rsync failed"));
    }

    @Test
    @DisplayName("FF-6: progress update flow create running progress updates complete")
    void ff6_progressUpdateFlow() {
        UploadTaskCreateRequest request = buildPvcRequest();
        UploadTaskResponse response = uploadApplicationService.createUploadTask(modelId, request, "test-user");

        UUID taskId = response.getTaskId();
        UUID versionId = response.getVersionId();

        uploadApplicationService.onJobRunning(taskId.toString());
        assertTaskStatus(taskId, TaskStatus.RUNNING);

        UploadTask task = uploadTaskRepository.findById(taskId).get();
        assertEquals(0, task.getProgress());

        uploadApplicationService.updateProgress(taskId.toString(), 45);
        task = uploadTaskRepository.findById(taskId).get();
        assertEquals(45, task.getProgress());

        uploadApplicationService.updateProgress(taskId.toString(), 78);
        task = uploadTaskRepository.findById(taskId).get();
        assertEquals(78, task.getProgress());

        uploadApplicationService.onJobCompleted(taskId.toString());
        assertTaskStatus(taskId, TaskStatus.COMPLETED);
        assertVersionStatus(versionId, VersionStatus.AVAILABLE);

        task = uploadTaskRepository.findById(taskId).get();
        assertEquals(100, task.getProgress());
    }
}
