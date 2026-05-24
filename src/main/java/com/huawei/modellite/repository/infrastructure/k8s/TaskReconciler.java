package com.huawei.modellite.repository.infrastructure.k8s;

import com.huawei.modellite.repository.common.enums.SourceType;
import com.huawei.modellite.repository.common.enums.TaskStatus;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.SourcePath;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.UploadTask;
import com.huawei.modellite.repository.weighttask.domain.dto.JobStatus;
import com.huawei.modellite.repository.weighttask.domain.dto.UploadJobSpec;
import com.huawei.modellite.repository.weighttask.domain.repository.UploadTaskRepository;
import com.huawei.modellite.repository.weighttask.domain.service.K8sJobService;
import com.huawei.modellite.repository.weighttask.domain.service.TaskEventCallback;

import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@ConditionalOnBean(KubernetesClient.class)
public class TaskReconciler {

    private final UploadTaskRepository uploadTaskRepository;
    private final K8sJobService k8sJobService;
    private final TaskEventCallback taskEventCallback;
    private final LeaderElectionService leaderElectionService;

    private static final long TERMINAL_CLEANUP_AGE_MS = 24 * 60 * 60 * 1000L;
    private static final List<String> DEFAULT_ALLOWED_SUFFIXES = Arrays.asList(
            ".bin", ".json", ".safetensors", ".model", ".pt", ".pth", ".onnx", ".gguf",
            ".txt", ".md", ".py", ".sh", ".yaml", ".yml", ".toml", ".cfg", ".ini",
            ".index", ".meta", ".data", ".ckpt", ".pb", ".h5", ".pkl", ".joblib");

    public TaskReconciler(UploadTaskRepository uploadTaskRepository,
                          K8sJobService k8sJobService,
                          TaskEventCallback taskEventCallback,
                          LeaderElectionService leaderElectionService) {
        this.uploadTaskRepository = uploadTaskRepository;
        this.k8sJobService = k8sJobService;
        this.taskEventCallback = taskEventCallback;
        this.leaderElectionService = leaderElectionService;
    }

    @Scheduled(fixedDelay = 30000)
    public void reconcile() {
        if (!leaderElectionService.isLeader()) {
            log.debug("Not leader, skipping reconciliation");
            return;
        }
        log.debug("Starting task reconciliation...");
        scanPendingTasks();
        scanRunningTasks();
        scanPausedTasks();
        scanTerminalTasks();
        log.debug("Task reconciliation complete");
    }

    void scanPendingTasks() {
        List<UploadTask> pendingTasks = uploadTaskRepository.findByStatus(TaskStatus.PENDING);
        for (UploadTask task : pendingTasks) {
            try {
                if (!k8sJobService.jobExists(task.getTaskId().toString())) {
                    log.info("Pending task {} has no Job, rebuilding", task.getTaskId());
                    rebuildJob(task);
                    continue;
                }

                JobStatus jobStatus = k8sJobService.getJobStatus(task.getTaskId().toString());
                if (jobStatus == JobStatus.RUNNING) {
                    log.info("Pending task {} Job is now running, calling onJobRunning", task.getTaskId());
                    taskEventCallback.onJobRunning(task.getTaskId().toString());
                } else {
                    log.debug("Pending task {} has existing Job (status={}), waiting", task.getTaskId(), jobStatus);
                }
            } catch (Exception e) {
                log.warn("Error reconciling pending task {}: {}", task.getTaskId(), e.getMessage());
            }
        }
    }

    void scanRunningTasks() {
        List<UploadTask> runningTasks = uploadTaskRepository.findByStatus(TaskStatus.RUNNING);
        for (UploadTask task : runningTasks) {
            try {
                JobStatus jobStatus = k8sJobService.getJobStatus(task.getTaskId().toString());
                switch (jobStatus) {
                    case COMPLETE:
                        log.info("Running task {} Job complete, marking completed", task.getTaskId());
                        taskEventCallback.onJobCompleted(task.getTaskId().toString());
                        break;
                    case FAILED:
                        String logs = k8sJobService.getJobPodLogs(task.getTaskId().toString(), 50);
                        String errorMessage = extractErrorMessage(logs);
                        log.info("Running task {} Job failed, marking failed", task.getTaskId());
                        taskEventCallback.onJobFailed(task.getTaskId().toString(), errorMessage);
                        break;
                    case NOT_FOUND:
                        log.warn("Running task {} Job not found, marking failed", task.getTaskId());
                        taskEventCallback.onJobFailed(task.getTaskId().toString(), "Job disappeared unexpectedly");
                        break;
                    case RUNNING:
                        parseAndReportProgress(task);
                        break;
                    case PENDING:
                        break;
                }
            } catch (Exception e) {
                log.warn("Error reconciling running task {}: {}", task.getTaskId(), e.getMessage());
            }
        }
    }

    void scanPausedTasks() {
        List<UploadTask> pausedTasks = uploadTaskRepository.findByStatus(TaskStatus.PAUSED);
        for (UploadTask task : pausedTasks) {
            log.debug("Paused task {} found", task.getTaskId());
        }
    }

    void scanTerminalTasks() {
        List<UploadTask> oldTerminalTasks = uploadTaskRepository.findTerminalTasksOlderThan(TERMINAL_CLEANUP_AGE_MS);
        for (UploadTask task : oldTerminalTasks) {
            try {
                log.info("Cleaning up terminal task {} resources", task.getTaskId());
                k8sJobService.deleteJobResources(task.getTaskId().toString());
            } catch (Exception e) {
                log.warn("Error cleaning up terminal task {}: {}", task.getTaskId(), e.getMessage());
            }
        }
    }

    private void rebuildJob(UploadTask task) {
        String taskIdStr = task.getTaskId().toString();
        String modelIdStr = task.getModelId().toString();
        String versionIdStr = task.getVersionId().toString();
        String targetPvcName = "model-weights";
        String targetSubPath = task.getTargetPath();

        UploadJobSpec spec;
        SourcePath sourcePath = task.getSourcePath();
        SourceType sourceType = sourcePath.getSourceType();

        if (sourceType == SourceType.NFS) {
            spec = UploadJobSpec.ofNfs(taskIdStr, modelIdStr, versionIdStr,
                    sourcePath.getPath(), targetPvcName, targetSubPath,
                    "modellite/file-copier:latest", "default", DEFAULT_ALLOWED_SUFFIXES);
        } else if (sourceType == SourceType.CIFS) {
            spec = UploadJobSpec.ofCifs(taskIdStr, modelIdStr, versionIdStr,
                    sourcePath.getPath(), targetPvcName, targetSubPath,
                    sourcePath.getCredentials().getUsername(),
                    sourcePath.getCredentials().getPassword(),
                    "modellite/file-copier:latest", "default", DEFAULT_ALLOWED_SUFFIXES);
        } else {
            spec = UploadJobSpec.ofPvc(taskIdStr, modelIdStr, versionIdStr,
                    sourcePath.getPath(), targetPvcName, targetSubPath,
                    "modellite/file-copier:latest", "default", DEFAULT_ALLOWED_SUFFIXES);
        }

        k8sJobService.createUploadJob(spec);
    }

    private void parseAndReportProgress(UploadTask task) {
        try {
            String logs = k8sJobService.getJobPodLogs(task.getTaskId().toString(), 10);
            if (logs != null && !logs.isEmpty()) {
                String[] lines = logs.split("\n");
                for (int i = lines.length - 1; i >= 0; i--) {
                    String line = lines[i].trim();
                    if (line.startsWith("PROGRESS:")) {
                        try {
                            int percent = Integer.parseInt(line.replaceAll("[^0-9]", ""));
                            taskEventCallback.updateProgress(task.getTaskId().toString(), percent);
                        } catch (NumberFormatException e) {
                            log.debug("Failed to parse progress from log line: {}", line);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse progress for task {}: {}", task.getTaskId(), e.getMessage());
        }
    }

    private String extractErrorMessage(String logs) {
        if (logs == null || logs.isEmpty()) {
            return "Job failed with no output";
        }
        return logs.length() > 500 ? logs.substring(logs.length() - 500) : logs;
    }

    /**
     * Called by JobInformerService when a Job transitions to RUNNING state.
     * Validates the task exists and delegates to TaskEventCallback.
     */
    public void onJobRunning(String taskId) {
        log.info("Reconciler: Job running event for task {}", taskId);
        taskEventCallback.onJobRunning(taskId);
    }

    /**
     * Called by JobInformerService when a Job transitions to COMPLETE state.
     * Validates the task exists and delegates to TaskEventCallback.
     */
    public void onJobCompleted(String taskId) {
        log.info("Reconciler: Job completed event for task {}", taskId);
        taskEventCallback.onJobCompleted(taskId);
    }

    /**
     * Called by JobInformerService when a Job transitions to FAILED state.
     * Enriches the error message by reading Pod logs (design doc §3.9 阶段 6),
     * then delegates to TaskEventCallback with the enriched message.
     */
    public void onJobFailed(String taskId, String initialErrorMessage) {
        log.info("Reconciler: Job failed event for task {}, initial error: {}", taskId, initialErrorMessage);
        String enrichedMessage = enrichErrorMessage(taskId, initialErrorMessage);
        taskEventCallback.onJobFailed(taskId, enrichedMessage);
    }

    private String enrichErrorMessage(String taskId, String initialErrorMessage) {
        try {
            String logs = k8sJobService.getJobPodLogs(taskId, 50);
            if (logs != null && !logs.isEmpty()) {
                String logError = extractErrorMessage(logs);
                return initialErrorMessage + " | Pod logs: " + logError;
            }
        } catch (Exception e) {
            log.debug("Could not fetch Pod logs for task {}: {}", taskId, e.getMessage());
        }
        return initialErrorMessage;
    }
}
