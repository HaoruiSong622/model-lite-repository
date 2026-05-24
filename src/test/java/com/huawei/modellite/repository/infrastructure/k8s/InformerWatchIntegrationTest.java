package com.huawei.modellite.repository.infrastructure.k8s;

import com.huawei.modellite.repository.weighttask.domain.service.TaskEventCallback;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobConditionBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InformerWatchIntegrationTest {

    @Mock
    private TaskEventCallback taskEventCallback;

    @Mock
    private LeaderElectionService leaderElectionService;

    @Mock
    private KubernetesClient kubernetesClient;

    private JobInformerService jobInformerService;
    private JobInformerService.JobEventHandler eventHandler;

    private static final String NAMESPACE = "default";
    private static final String TASK_ID = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        when(leaderElectionService.isLeader()).thenReturn(true);
        jobInformerService = new JobInformerService(
                kubernetesClient, taskEventCallback, leaderElectionService, NAMESPACE
        );
        eventHandler = (JobInformerService.JobEventHandler) jobInformerService.createEventHandler();
    }

    @Test
    @DisplayName("Informer should trigger onJobRunning when Job becomes active")
    void informer_shouldTriggerOnJobRunning_whenJobBecomesActive() {
        Job pendingJob = createJob(TASK_ID, 0, null, null);
        Job runningJob = createJob(TASK_ID, 1, null, null);

        eventHandler.onUpdate(pendingJob, runningJob);

        verify(taskEventCallback).onJobRunning(TASK_ID);
    }

    @Test
    @DisplayName("Informer should trigger onJobCompleted when Job completes")
    void informer_shouldTriggerOnJobCompleted_whenJobCompletes() {
        Job runningJob = createJob(TASK_ID, 1, null, null);
        Job completedJob = createJob(TASK_ID, 0, "Complete", "True");

        eventHandler.onUpdate(runningJob, completedJob);

        verify(taskEventCallback).onJobCompleted(TASK_ID);
    }

    @Test
    @DisplayName("Informer should trigger onJobFailed when Job fails")
    void informer_shouldTriggerOnJobFailed_whenJobFails() {
        Job runningJob = createJob(TASK_ID, 1, null, null);
        Job failedJob = createJob(TASK_ID, 0, "Failed", "True", "ImagePullBackOff");

        eventHandler.onUpdate(runningJob, failedJob);

        verify(taskEventCallback).onJobFailed(TASK_ID, "ImagePullBackOff");
    }

    @Test
    @DisplayName("Informer should not trigger callbacks when instance is not leader")
    void informer_shouldNotTriggerCallbacks_whenNotLeader() {
        when(leaderElectionService.isLeader()).thenReturn(false);

        Job pendingJob = createJob(TASK_ID, 0, null, null);
        Job runningJob = createJob(TASK_ID, 1, null, null);

        eventHandler.onUpdate(pendingJob, runningJob);

        verify(taskEventCallback, never()).onJobRunning(anyString());
        verify(taskEventCallback, never()).onJobCompleted(anyString());
        verify(taskEventCallback, never()).onJobFailed(anyString(), anyString());
    }

    @Test
    @DisplayName("Informer should handle Job deletion as failure if not terminal")
    void informer_shouldHandleJobDeletionAsFailure_whenNotTerminal() {
        Job runningJob = createJob(TASK_ID, 1, null, null);

        eventHandler.onDelete(runningJob, false);

        verify(taskEventCallback).onJobFailed(TASK_ID, "Job deleted unexpectedly");
    }

    @Test
    @DisplayName("Informer should suppress duplicate phase transitions")
    void informer_shouldSuppressDuplicatePhaseTransitions() {
        Job runningJob1 = createJob(TASK_ID, 1, null, null);
        Job runningJob2 = createJob(TASK_ID, 1, null, null);

        eventHandler.onUpdate(runningJob1, runningJob2);

        verify(taskEventCallback, times(1)).onJobRunning(TASK_ID);
    }

    private Job createJob(String taskId, int active, String conditionType, String conditionStatus) {
        return createJob(taskId, active, conditionType, conditionStatus, null);
    }

    private Job createJob(String taskId, int active, String conditionType, String conditionStatus, String message) {
        JobBuilder builder = new JobBuilder()
                .withNewMetadata()
                .withName("upload-" + taskId)
                .withNamespace(NAMESPACE)
                .addToLabels("app", "modellite-file-copier")
                .addToLabels("modellite/upload-task-id", taskId)
                .endMetadata();

        JobStatusBuilder statusBuilder = new JobStatusBuilder();
        if (active > 0) {
            statusBuilder.withActive(active);
        }
        if (conditionType != null && conditionStatus != null) {
            JobConditionBuilder conditionBuilder = new JobConditionBuilder()
                    .withType(conditionType)
                    .withStatus(conditionStatus);
            if (message != null) {
                conditionBuilder.withMessage(message);
            }
            statusBuilder.withConditions(conditionBuilder.build());
        }

        Job job = builder.build();
        job.setStatus(statusBuilder.build());
        return job;
    }
}
