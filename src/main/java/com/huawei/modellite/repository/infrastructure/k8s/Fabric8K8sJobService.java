package com.huawei.modellite.repository.infrastructure.k8s;

import com.huawei.modellite.repository.common.enums.SourceType;
import com.huawei.modellite.repository.infrastructure.config.WeightImportConfig;
import com.huawei.modellite.repository.weighttask.domain.dto.JobStatus;
import com.huawei.modellite.repository.weighttask.domain.dto.UploadJobSpec;
import com.huawei.modellite.repository.weighttask.domain.service.K8sJobService;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(KubernetesClient.class)
public class Fabric8K8sJobService implements K8sJobService {

    private static final String LABEL_UPLOAD_TASK_ID = "modellite/upload-task-id";
    private static final String LABEL_APP = "app";
    private static final String APP_NAME = "modellite-file-copier";
    private static final String CONTAINER_NAME = "file-copier";
    private static final String CONFIG_MAP_NAME_PREFIX = "upload-config-";
    private static final String SECRET_NAME_PREFIX = "upload-secret-";
    private static final int TTL_SECONDS_AFTER_FINISHED = 86400;
    private static final String SOURCE_MOUNT_PATH = "/source";
    private static final String TARGET_MOUNT_PATH = "/target";

    private final KubernetesClient kubernetesClient;
    private final WeightImportConfig weightImportConfig;

    @Override
    public void createUploadJob(UploadJobSpec spec) {
        String jobName = getJobName(spec.getTaskId());
        String namespace = resolveNamespace(spec);

        if (jobExists(spec.getTaskId())) {
            log.info("Job {} already exists in namespace {}, skipping creation", jobName, namespace);
            return;
        }

        createConfigMap(spec, namespace);
        if (spec.getSourceType() == SourceType.CIFS) {
            createSecret(spec, namespace);
        }

        Job job = buildJob(spec, namespace);
        kubernetesClient.batch().v1().jobs().inNamespace(namespace).resource(job).create();
        log.info("Created Job {} in namespace {}", jobName, namespace);
    }

    @Override
    public void deleteJob(String taskId) {
        String jobName = getJobName(taskId);
        String namespace = resolveNamespace(null);

        if (!jobExists(taskId)) {
            log.info("Job {} does not exist in namespace {}, skipping deletion", jobName, namespace);
            return;
        }

        kubernetesClient.batch().v1().jobs().inNamespace(namespace).withName(jobName).delete();
        log.info("Deleted Job {} in namespace {}", jobName, namespace);
    }

    @Override
    public void deleteJobResources(String taskId) {
        String namespace = resolveNamespace(null);
        String jobName = getJobName(taskId);
        String configMapName = getConfigMapName(taskId);
        String secretName = getSecretName(taskId);

        if (jobExists(taskId)) {
            kubernetesClient.batch().v1().jobs().inNamespace(namespace).withName(jobName).delete();
            log.info("Deleted Job {} in namespace {}", jobName, namespace);
        }

        var configMap = kubernetesClient.configMaps().inNamespace(namespace).withName(configMapName).get();
        if (configMap != null) {
            kubernetesClient.configMaps().inNamespace(namespace).withName(configMapName).delete();
            log.info("Deleted ConfigMap {} in namespace {}", configMapName, namespace);
        }

        var secret = kubernetesClient.secrets().inNamespace(namespace).withName(secretName).get();
        if (secret != null) {
            kubernetesClient.secrets().inNamespace(namespace).withName(secretName).delete();
            log.info("Deleted Secret {} in namespace {}", secretName, namespace);
        }
    }

    @Override
    public JobStatus getJobStatus(String taskId) {
        String jobName = getJobName(taskId);
        String namespace = resolveNamespace(null);

        Job job = kubernetesClient.batch().v1().jobs().inNamespace(namespace).withName(jobName).get();
        if (job == null) {
            return JobStatus.NOT_FOUND;
        }

        if (job.getStatus() != null && job.getStatus().getConditions() != null) {
            for (JobCondition condition : job.getStatus().getConditions()) {
                if ("Complete".equals(condition.getType()) && "True".equals(condition.getStatus())) {
                    return JobStatus.COMPLETE;
                }
                if ("Failed".equals(condition.getType()) && "True".equals(condition.getStatus())) {
                    return JobStatus.FAILED;
                }
            }
        }

        Integer active = job.getStatus() != null ? job.getStatus().getActive() : null;
        if (active != null && active > 0) {
            return JobStatus.RUNNING;
        }

        return JobStatus.PENDING;
    }

    @Override
    public String getJobPodLogs(String taskId, int tailLines) {
        String podName = getJobPodName(taskId);
        if (podName == null) {
            return "";
        }
        String namespace = resolveNamespace(null);
        return kubernetesClient.pods().inNamespace(namespace).withName(podName).tailingLines(tailLines).getLog();
    }

    @Override
    public boolean jobExists(String taskId) {
        String jobName = getJobName(taskId);
        String namespace = resolveNamespace(null);
        return kubernetesClient.batch().v1().jobs().inNamespace(namespace).withName(jobName).get() != null;
    }

    @Override
    public String getJobPodName(String taskId) {
        String namespace = resolveNamespace(null);
        Map<String, String> labels = getJobLabels(taskId);
        List<Pod> pods = kubernetesClient.pods().inNamespace(namespace).withLabels(labels).list().getItems();
        if (pods.isEmpty()) {
            return null;
        }
        return pods.get(0).getMetadata().getName();
    }

    private Job buildJob(UploadJobSpec spec, String namespace) {
        String jobName = getJobName(spec.getTaskId());
        Map<String, String> labels = getJobLabels(spec.getTaskId());
        String image = resolveImage(spec);

        ContainerBuilder containerBuilder = new ContainerBuilder()
                .withName(CONTAINER_NAME)
                .withImage(image)
                .withEnvFrom(new EnvFromSourceBuilder()
                        .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                                .withName(getConfigMapName(spec.getTaskId()))
                                .build())
                        .build())
                .withVolumeMounts(buildVolumeMounts(spec))
                .withResources(new ResourceRequirementsBuilder()
                        .withRequests(Map.of(
                                "cpu", new Quantity(weightImportConfig.getCpuRequest()),
                                "memory", new Quantity(weightImportConfig.getMemoryRequest())
                        ))
                        .build());

        if (spec.getSourceType() == SourceType.CIFS) {
            containerBuilder.addNewEnv()
                    .withName("CIFS_USERNAME")
                    .withNewValueFrom()
                    .withNewSecretKeyRef()
                    .withName(getSecretName(spec.getTaskId()))
                    .withKey("username")
                    .endSecretKeyRef()
                    .endValueFrom()
                    .endEnv()
                    .addNewEnv()
                    .withName("CIFS_PASSWORD")
                    .withNewValueFrom()
                    .withNewSecretKeyRef()
                    .withName(getSecretName(spec.getTaskId()))
                    .withKey("password")
                    .endSecretKeyRef()
                    .endValueFrom()
                    .endEnv();
        }

        return new JobBuilder()
                .withNewMetadata()
                .withName(jobName)
                .withNamespace(namespace)
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withTtlSecondsAfterFinished(TTL_SECONDS_AFTER_FINISHED)
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withRestartPolicy("OnFailure")
                .withContainers(containerBuilder.build())
                .withVolumes(buildVolumes(spec))
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private List<VolumeMount> buildVolumeMounts(UploadJobSpec spec) {
        List<VolumeMount> mounts = new java.util.ArrayList<>();
        mounts.add(new VolumeMountBuilder().withName("target-pvc").withMountPath(TARGET_MOUNT_PATH).build());

        if (spec.getSourceType() == SourceType.NFS || spec.getSourceType() == SourceType.PVC) {
            mounts.add(new VolumeMountBuilder().withName("source-volume").withMountPath(SOURCE_MOUNT_PATH).build());
        }

        return mounts;
    }

    private List<Volume> buildVolumes(UploadJobSpec spec) {
        List<Volume> volumes = new java.util.ArrayList<>();
        volumes.add(new VolumeBuilder()
                .withName("target-pvc")
                .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder()
                        .withClaimName(spec.getTargetPvcName())
                        .build())
                .build());

        if (spec.getSourceType() == SourceType.NFS) {
            String[] parts = parseNfsPath(spec.getSourcePath());
            volumes.add(new VolumeBuilder()
                    .withName("source-volume")
                    .withNfs(new NFSVolumeSourceBuilder()
                            .withServer(parts[0])
                            .withPath(parts[1])
                            .build())
                    .build());
        } else if (spec.getSourceType() == SourceType.PVC) {
            volumes.add(new VolumeBuilder()
                    .withName("source-volume")
                    .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder()
                            .withClaimName(spec.getSourcePath())
                            .build())
                    .build());
        }

        return volumes;
    }

    private String[] parseNfsPath(String sourcePath) {
        if (sourcePath == null || !sourcePath.contains(":")) {
            throw new IllegalArgumentException("Invalid NFS source path: " + sourcePath);
        }
        int colonIndex = sourcePath.indexOf(':');
        return new String[]{sourcePath.substring(0, colonIndex), sourcePath.substring(colonIndex + 1)};
    }

    private void createConfigMap(UploadJobSpec spec, String namespace) {
        String configMapName = getConfigMapName(spec.getTaskId());
        Map<String, String> data = new HashMap<>();
        data.put("SOURCE_TYPE", spec.getSourceType().getDbValue());
        data.put("SOURCE_PATH", resolveSourcePath(spec));
        data.put("TARGET_PATH", resolveTargetPath(spec));
        data.put("ALLOWED_SUFFIXES", String.join(",", spec.getAllowedSuffixes()));

        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(configMapName)
                .withNamespace(namespace)
                .withLabels(getJobLabels(spec.getTaskId()))
                .endMetadata()
                .withData(data)
                .build();

        kubernetesClient.configMaps().inNamespace(namespace).resource(configMap).createOrReplace();
    }

    private void createSecret(UploadJobSpec spec, String namespace) {
        String secretName = getSecretName(spec.getTaskId());
        Map<String, String> data = new HashMap<>();
        data.put("username", spec.getCifsUsername());
        data.put("password", spec.getCifsPassword());

        Secret secret = new SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(namespace)
                .withLabels(getJobLabels(spec.getTaskId()))
                .endMetadata()
                .withStringData(data)
                .build();

        kubernetesClient.secrets().inNamespace(namespace).resource(secret).createOrReplace();
    }

    private String resolveSourcePath(UploadJobSpec spec) {
        if (spec.getSourceType() == SourceType.NFS || spec.getSourceType() == SourceType.PVC) {
            return SOURCE_MOUNT_PATH;
        }
        return spec.getSourcePath();
    }

    private String resolveTargetPath(UploadJobSpec spec) {
        if (spec.getTargetSubPath() != null && !spec.getTargetSubPath().isEmpty()) {
            return TARGET_MOUNT_PATH + "/" + spec.getTargetSubPath();
        }
        return TARGET_MOUNT_PATH;
    }

    private String resolveNamespace(UploadJobSpec spec) {
        if (spec != null && spec.getNamespace() != null && !spec.getNamespace().isEmpty()) {
            return spec.getNamespace();
        }
        return weightImportConfig.getNamespace();
    }

    private String resolveImage(UploadJobSpec spec) {
        if (spec.getImage() != null && !spec.getImage().isEmpty()) {
            return spec.getImage();
        }
        return weightImportConfig.getImage();
    }

    private String getJobName(String taskId) {
        return "upload-" + taskId;
    }

    private String getConfigMapName(String taskId) {
        return CONFIG_MAP_NAME_PREFIX + taskId;
    }

    private String getSecretName(String taskId) {
        return SECRET_NAME_PREFIX + taskId;
    }

    private Map<String, String> getJobLabels(String taskId) {
        Map<String, String> labels = new HashMap<>();
        labels.put(LABEL_UPLOAD_TASK_ID, taskId);
        labels.put(LABEL_APP, APP_NAME);
        return labels;
    }
}
