package com.huawei.modellite.repository.infrastructure.k8s;

import com.huawei.modellite.repository.infrastructure.config.WeightImportConfig;
import com.huawei.modellite.repository.weighttask.domain.dto.JobStatus;
import com.huawei.modellite.repository.weighttask.domain.dto.UploadJobSpec;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.api.model.batch.v1.JobConditionBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@EnableKubernetesMockClient(crud = true)
class Fabric8K8sJobServiceTest {

    private KubernetesClient client;
    private Fabric8K8sJobService service;
    private WeightImportConfig config;

    @BeforeEach
    void setUp() {
        config = new WeightImportConfig();
        config.setImage("file-copier:test");
        config.setNamespace("default");
        config.setCpuRequest("100m");
        config.setMemoryRequest("128Mi");
        service = new Fabric8K8sJobService(client, config);
    }

    @Test
    @DisplayName("should create NFS Job with ConfigMap, NFS volume and correct labels")
    void should_createNfsJob_withConfigMapAndNfsVolume() {
        UploadJobSpec spec = UploadJobSpec.ofNfs(
                "task-001", "model-001", "version-001",
                "192.168.1.100:/exports/data", "target-pvc", "sub/path",
                "file-copier:custom", "test-ns",
                List.of(".safetensors", ".bin")
        );

        service.createUploadJob(spec);

        Job job = client.batch().v1().jobs().inNamespace("test-ns").withName("upload-task-001").get();
        assertNotNull(job);
        assertEquals("upload-task-001", job.getMetadata().getName());
        assertEquals("test-ns", job.getMetadata().getNamespace());
        assertEquals("task-001", job.getMetadata().getLabels().get("modellite/upload-task-id"));
        assertEquals("modellite-file-copier", job.getMetadata().getLabels().get("app"));
        assertEquals(Integer.valueOf(86400), job.getSpec().getTtlSecondsAfterFinished());
        assertEquals("file-copier:custom", job.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());

        ConfigMap cm = client.configMaps().inNamespace("test-ns").withName("upload-config-task-001").get();
        assertNotNull(cm);
        assertEquals("NFS", cm.getData().get("SOURCE_TYPE"));
        assertEquals("/source", cm.getData().get("SOURCE_PATH"));
        assertEquals("/target/sub/path", cm.getData().get("TARGET_PATH"));
        assertEquals(".safetensors,.bin", cm.getData().get("ALLOWED_SUFFIXES"));

        List<Volume> volumes = job.getSpec().getTemplate().getSpec().getVolumes();
        assertEquals(2, volumes.size());

        Volume targetVol = volumes.stream().filter(v -> "target-pvc".equals(v.getName())).findFirst().orElseThrow();
        assertEquals("target-pvc", targetVol.getPersistentVolumeClaim().getClaimName());

        Volume sourceVol = volumes.stream().filter(v -> "source-volume".equals(v.getName())).findFirst().orElseThrow();
        assertEquals("192.168.1.100", sourceVol.getNfs().getServer());
        assertEquals("/exports/data", sourceVol.getNfs().getPath());
    }

    @Test
    @DisplayName("should be idempotent when creating the same job twice")
    void should_beIdempotent_onDuplicateCreate() {
        UploadJobSpec spec = UploadJobSpec.ofNfs(
                "task-002", "model-002", "version-002",
                "192.168.1.100:/exports/data", "target-pvc", "",
                null, null,
                List.of(".pt")
        );

        service.createUploadJob(spec);
        service.createUploadJob(spec);

        List<Job> jobs = client.batch().v1().jobs().inNamespace("default").list().getItems();
        assertEquals(1, jobs.size());
        assertEquals("upload-task-002", jobs.get(0).getMetadata().getName());
    }

    @Test
    @DisplayName("should map all 5 job statuses correctly")
    void should_mapAllFiveJobStatuses() {
        String namespace = "default";

        assertEquals(JobStatus.NOT_FOUND, service.getJobStatus("task-not-found"));

        createMockJob("task-pending", namespace, null, 0, 0);
        assertEquals(JobStatus.PENDING, service.getJobStatus("task-pending"));

        createMockJob("task-running", namespace, null, 1, 0);
        assertEquals(JobStatus.RUNNING, service.getJobStatus("task-running"));

        JobCondition completeCondition = new JobConditionBuilder()
                .withType("Complete")
                .withStatus("True")
                .build();
        createMockJob("task-complete", namespace, List.of(completeCondition), 0, 0);
        assertEquals(JobStatus.COMPLETE, service.getJobStatus("task-complete"));

        JobCondition failedCondition = new JobConditionBuilder()
                .withType("Failed")
                .withStatus("True")
                .build();
        createMockJob("task-failed", namespace, List.of(failedCondition), 0, 1);
        assertEquals(JobStatus.FAILED, service.getJobStatus("task-failed"));
    }

    @Test
    @DisplayName("should be idempotent when deleting non-existent job")
    void should_beIdempotent_onDeleteNonExistentJob() {
        assertDoesNotThrow(() -> service.deleteJob("task-non-existent"));
    }

    @Test
    @DisplayName("should clean up ConfigMap and Secret when deleting job resources")
    void should_cleanUpConfigMapAndSecret_onDeleteJobResources() {
        String taskId = "task-cleanup";
        String ns = "default";

        UploadJobSpec spec = UploadJobSpec.ofCifs(
                taskId, "model-003", "version-003",
                "//server/share", "target-pvc", "sub",
                "user", "pass",
                null, null,
                List.of(".safetensors")
        );
        service.createUploadJob(spec);

        assertNotNull(client.batch().v1().jobs().inNamespace(ns).withName("upload-" + taskId).get());
        assertNotNull(client.configMaps().inNamespace(ns).withName("upload-config-" + taskId).get());
        assertNotNull(client.secrets().inNamespace(ns).withName("upload-secret-" + taskId).get());

        service.deleteJobResources(taskId);

        assertNull(client.batch().v1().jobs().inNamespace(ns).withName("upload-" + taskId).get());
        assertNull(client.configMaps().inNamespace(ns).withName("upload-config-" + taskId).get());
        assertNull(client.secrets().inNamespace(ns).withName("upload-secret-" + taskId).get());
    }

    @Test
    @DisplayName("should read pod logs for a job")
    void should_readPodLogs_forJob() {
        String taskId = "task-logs";
        String ns = "default";
        String podName = "upload-pod-" + taskId;

        UploadJobSpec spec = UploadJobSpec.ofNfs(
                taskId, "model-004", "version-004",
                "192.168.1.100:/exports/data", "target-pvc", "",
                null, null,
                List.of(".bin")
        );
        service.createUploadJob(spec);

        Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName(podName)
                .withNamespace(ns)
                .withLabels(Map.of(
                        "modellite/upload-task-id", taskId,
                        "app", "modellite-file-copier"
                ))
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("file-copier")
                .withImage("test")
                .endContainer()
                .endSpec()
                .build();
        client.pods().inNamespace(ns).resource(pod).create();

        assertEquals(podName, service.getJobPodName(taskId));
    }

    @Test
    @DisplayName("should check job existence correctly")
    void should_checkJobExistence_correctly() {
        String existingTaskId = "task-exists";
        String nonExistentTaskId = "task-not-exists";

        UploadJobSpec spec = UploadJobSpec.ofPvc(
                existingTaskId, "model-005", "version-005",
                "source-pvc", "target-pvc", "path",
                null, null,
                List.of(".onnx")
        );
        service.createUploadJob(spec);

        assertTrue(service.jobExists(existingTaskId));
        assertFalse(service.jobExists(nonExistentTaskId));
    }

    @Test
    @DisplayName("should create CIFS Job with Secret containing credentials")
    void should_createCifsJob_withSecret() {
        UploadJobSpec spec = UploadJobSpec.ofCifs(
                "task-cifs", "model-006", "version-006",
                "//server/share/path", "target-pvc", "dest",
                "cifsUser", "cifsPass",
                null, null,
                List.of(".safetensors", ".pt")
        );

        service.createUploadJob(spec);

        String ns = "default";

        Job job = client.batch().v1().jobs().inNamespace(ns).withName("upload-task-cifs").get();
        assertNotNull(job);

        Secret secret = client.secrets().inNamespace(ns).withName("upload-secret-task-cifs").get();
        assertNotNull(secret);
        assertEquals("cifsUser", secret.getStringData().get("username"));
        assertEquals("cifsPass", secret.getStringData().get("password"));

        ConfigMap cm = client.configMaps().inNamespace(ns).withName("upload-config-task-cifs").get();
        assertNotNull(cm);
        assertEquals("CIFS", cm.getData().get("SOURCE_TYPE"));
        assertEquals("//server/share/path", cm.getData().get("SOURCE_PATH"));

        List<EnvVar> envVars = job.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
        boolean hasUsername = envVars.stream().anyMatch(e -> "CIFS_USERNAME".equals(e.getName())
                && e.getValueFrom() != null
                && e.getValueFrom().getSecretKeyRef() != null
                && "upload-secret-task-cifs".equals(e.getValueFrom().getSecretKeyRef().getName()));
        boolean hasPassword = envVars.stream().anyMatch(e -> "CIFS_PASSWORD".equals(e.getName())
                && e.getValueFrom() != null
                && e.getValueFrom().getSecretKeyRef() != null
                && "upload-secret-task-cifs".equals(e.getValueFrom().getSecretKeyRef().getName()));
        assertTrue(hasUsername, "Job should have CIFS_USERNAME env var from Secret");
        assertTrue(hasPassword, "Job should have CIFS_PASSWORD env var from Secret");

        List<Volume> volumes = job.getSpec().getTemplate().getSpec().getVolumes();
        boolean hasSourceVolume = volumes.stream().anyMatch(v -> "source-volume".equals(v.getName()));
        assertFalse(hasSourceVolume, "CIFS job should not have source-volume");
    }

    @Test
    @DisplayName("should create PVC Job with source PVC mounted")
    void should_createPvcJob_withSourcePvcVolume() {
        UploadJobSpec spec = UploadJobSpec.ofPvc(
                "task-pvc", "model-007", "version-007",
                "source-pvc-name", "target-pvc", "sub/dir",
                null, null,
                List.of(".bin", ".pt")
        );

        service.createUploadJob(spec);

        Job job = client.batch().v1().jobs().inNamespace("default").withName("upload-task-pvc").get();
        assertNotNull(job);

        List<Volume> volumes = job.getSpec().getTemplate().getSpec().getVolumes();
        Volume sourceVol = volumes.stream().filter(v -> "source-volume".equals(v.getName())).findFirst().orElseThrow();
        assertEquals("source-pvc-name", sourceVol.getPersistentVolumeClaim().getClaimName());

        ConfigMap cm = client.configMaps().inNamespace("default").withName("upload-config-task-pvc").get();
        assertNotNull(cm);
        assertEquals("PVC", cm.getData().get("SOURCE_TYPE"));
    }

    @Test
    @DisplayName("should fall back to WeightImportConfig for image and namespace")
    void should_fallbackToConfig_forImageAndNamespace() {
        UploadJobSpec spec = UploadJobSpec.ofNfs(
                "task-fallback", "model-008", "version-008",
                "192.168.1.100:/data", "target-pvc", "",
                null, null,
                List.of(".safetensors")
        );

        service.createUploadJob(spec);

        Job job = client.batch().v1().jobs().inNamespace("default").withName("upload-task-fallback").get();
        assertNotNull(job);
        assertEquals("file-copier:test", job.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());
        assertEquals("default", job.getMetadata().getNamespace());
    }

    private void createMockJob(String taskId, String namespace,
                                List<JobCondition> conditions, int active, int failed) {
        String jobName = "upload-" + taskId;
        Map<String, String> labels = Map.of(
                "modellite/upload-task-id", taskId,
                "app", "modellite-file-copier"
        );

        Job job = new JobBuilder()
                .withNewMetadata()
                .withName(jobName)
                .withNamespace(namespace)
                .withLabels(labels)
                .endMetadata()
                .build();

        if (conditions != null || active > 0 || failed > 0) {
            io.fabric8.kubernetes.api.model.batch.v1.JobStatus status =
                    new io.fabric8.kubernetes.api.model.batch.v1.JobStatus();
            status.setConditions(conditions);
            status.setActive(active);
            status.setFailed(failed);
            job.setStatus(status);
        }

        client.batch().v1().jobs().inNamespace(namespace).resource(job).create();
    }
}
