package com.huawei.modellite.repository.infrastructure.k8s;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that registers a Fabric8 SharedInformer to watch K8s Job events
 * and dispatch them to {@link TaskReconciler}.
 *
 * <p>Watches Jobs with label {@code app=modellite-file-copier} in the configured namespace.
 * Only the leader instance actively watches; non-leader instances keep the informer registered
 * but do not process events (the leader election check is performed per-event).</p>
 *
 * <p>This provides event-driven (real-time) status updates as a complement to the
 * {@link TaskReconciler} polling mechanism — the "dual insurance" design.</p>
 */
@Slf4j
@Component
@ConditionalOnBean(KubernetesClient.class)
public class JobInformerService {

    private static final String LABEL_APP = "app";
    private static final String APP_NAME = "modellite-file-copier";
    private static final long RESYNC_PERIOD_MS = 60_000L;

    private final KubernetesClient kubernetesClient;
    private final TaskReconciler taskReconciler;
    private final LeaderElectionService leaderElectionService;
    private final String namespace;

    private SharedIndexInformer<Job> jobInformer;

    private final Map<String, String> lastKnownPhase = new ConcurrentHashMap<>();

    public JobInformerService(KubernetesClient kubernetesClient,
                               TaskReconciler taskReconciler,
                               LeaderElectionService leaderElectionService,
                               @Value("${weight-import.namespace:default}") String namespace) {
        this.kubernetesClient = kubernetesClient;
        this.taskReconciler = taskReconciler;
        this.leaderElectionService = leaderElectionService;
        this.namespace = namespace;
    }

    @PostConstruct
    public void start() {
        var batchAPI = kubernetesClient.batch();
        if (batchAPI == null) {
            log.warn("KubernetesClient.batch() returned null, skipping Job informer registration");
            return;
        }

        log.info("Starting Job informer for namespace: {}", namespace);

        Map<String, String> labels = Map.of(LABEL_APP, APP_NAME);

        jobInformer = batchAPI.v1().jobs()
                .inNamespace(namespace)
                .withLabels(labels)
                .inform(new JobEventHandler(), RESYNC_PERIOD_MS);

        log.info("Job informer started with resync period: {}ms", RESYNC_PERIOD_MS);
    }

    @PreDestroy
    public void stop() {
        if (jobInformer != null) {
            log.info("Stopping Job informer");
            jobInformer.close();
            jobInformer = null;
        }
    }

    ResourceEventHandler<Job> createEventHandler() {
        return new JobEventHandler();
    }

    class JobEventHandler implements ResourceEventHandler<Job> {

        @Override
        public void onAdd(Job job) {
            if (!leaderElectionService.isLeader()) {
                return;
            }
            String taskId = extractTaskId(job);
            if (taskId == null) {
                return;
            }
            log.debug("Informer: Job added for task {}", taskId);
            lastKnownPhase.put(taskId, "PENDING");
        }

        @Override
        public void onUpdate(Job oldJob, Job newJob) {
            if (!leaderElectionService.isLeader()) {
                return;
            }
            String taskId = extractTaskId(newJob);
            if (taskId == null) {
                return;
            }

            String newPhase = determinePhase(newJob);
            String oldPhase = lastKnownPhase.put(taskId, newPhase);

            if (newPhase.equals(oldPhase)) {
                return;
            }

            log.debug("Informer: Job phase transition for task {}: {} -> {}", taskId, oldPhase, newPhase);

            switch (newPhase) {
                case "RUNNING":
                    log.info("Informer: Job started running for task {}", taskId);
                    taskReconciler.onJobRunning(taskId);
                    break;
                case "COMPLETE":
                    log.info("Informer: Job completed for task {}", taskId);
                    taskReconciler.onJobCompleted(taskId);
                    break;
                case "FAILED":
                    String errorMessage = extractErrorMessage(newJob);
                    log.info("Informer: Job failed for task {}: {}", taskId, errorMessage);
                    taskReconciler.onJobFailed(taskId, errorMessage);
                    break;
                default:
                    log.debug("Informer: Unhandled phase {} for task {}", newPhase, taskId);
            }
        }

        @Override
        public void onDelete(Job job, boolean deletedFinalStateUnknown) {
            if (!leaderElectionService.isLeader()) {
                return;
            }
            String taskId = extractTaskId(job);
            if (taskId == null) {
                return;
            }
            log.debug("Informer: Job deleted for task {}", taskId);
            lastKnownPhase.remove(taskId);

            String phase = determinePhase(job);
            if (!"COMPLETE".equals(phase) && !"FAILED".equals(phase)) {
                log.warn("Informer: Job deleted unexpectedly for task {}", taskId);
                taskReconciler.onJobFailed(taskId, "Job deleted unexpectedly");
            }
        }
    }

    private String extractTaskId(Job job) {
        if (job.getMetadata() == null || job.getMetadata().getLabels() == null) {
            return null;
        }
        return job.getMetadata().getLabels().get("modellite/upload-task-id");
    }

    private String determinePhase(Job job) {
        if (job.getStatus() == null || job.getStatus().getConditions() == null) {
            Integer active = job.getStatus() != null ? job.getStatus().getActive() : null;
            return (active != null && active > 0) ? "RUNNING" : "PENDING";
        }

        for (JobCondition condition : job.getStatus().getConditions()) {
            if ("Complete".equals(condition.getType()) && "True".equals(condition.getStatus())) {
                return "COMPLETE";
            }
            if ("Failed".equals(condition.getType()) && "True".equals(condition.getStatus())) {
                return "FAILED";
            }
        }

        Integer active = job.getStatus().getActive();
        if (active != null && active > 0) {
            return "RUNNING";
        }

        return "PENDING";
    }

    private String extractErrorMessage(Job job) {
        if (job.getStatus() != null && job.getStatus().getConditions() != null) {
            for (JobCondition condition : job.getStatus().getConditions()) {
                if ("Failed".equals(condition.getType()) && "True".equals(condition.getStatus())) {
                    if (condition.getMessage() != null && !condition.getMessage().isEmpty()) {
                        return condition.getMessage();
                    }
                    if (condition.getReason() != null && !condition.getReason().isEmpty()) {
                        return condition.getReason();
                    }
                }
            }
        }
        return "Job failed";
    }
}
