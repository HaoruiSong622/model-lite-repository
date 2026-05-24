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

/**
 * Tests for {@link JobInformerService.JobEventHandler}.
 * Directly tests the event handler logic without requiring a running K8s informer.
 */
@ExtendWith(MockitoExtension.class)
class JobInformerServiceTest {

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
    @DisplayName("onUpdate should call onJobRunning when Job transitions to RUNNING")
    void onUpdate_shouldCallOnJobRunning_whenJobTransitionsToRunning() {
        Job pendingJob = createJob(TASK_ID, 0, null, null);
        Job runningJob = createJob(TASK_ID, 1, null, null);

        eventHandler.onUpdate(pendingJob, runningJob);

        verify(taskEventCallback).onJobRunning(TASK_ID);
    }

    @Test
    @DisplayName("onUpdate should call onJobCompleted when Job transitions to COMPLETE")
    void onUpdate_shouldCallOnJobCompleted_whenJobTransitionsToComplete() {
        Job runningJob = createJob(TASK_ID, 1, null, null);
        Job completedJob = createJob(TASK_ID, 0, "Complete", "True");

        eventHandler.onUpdate(runningJob, completedJob);

        verify(taskEventCallback).onJobCompleted(TASK_ID);
    }

    @Test
    @DisplayName("onUpdate should call onJobFailed when Job transitions to FAILED")
    void onUpdate_shouldCallOnJobFailed_whenJobTransitionsToFailed() {
        Job runningJob = createJob(TASK_ID, 1, null, null);
        Job failedJob = createJob(TASK_ID, 0, "Failed", "True", "BackoffLimitExceeded");

        eventHandler.onUpdate(runningJob, failedJob);

        verify(taskEventCallback).onJobFailed(TASK_ID, "BackoffLimitExceeded");
    }

    @Test
    @DisplayName("onUpdate should not call callbacks when not leader")
    void onUpdate_shouldNotCallCallbacks_whenNotLeader() {
        when(leaderElectionService.isLeader()).thenReturn(false);
        Job pendingJob = createJob(TASK_ID, 0, null, null);
        Job runningJob = createJob(TASK_ID, 1, null, null);

        eventHandler.onUpdate(pendingJob, runningJob);

        verifyNoInteractions(taskEventCallback);
    }

    @Test
    @DisplayName("onUpdate should ignore duplicate phase transitions")
    void onUpdate_shouldIgnoreDuplicatePhaseTransitions() {
        Job runningJob1 = createJob(TASK_ID, 1, null, null);
        Job runningJob2 = createJob(TASK_ID, 1, null, null);

        eventHandler.onUpdate(runningJob1, runningJob2);

        verify(taskEventCallback, times(1)).onJobRunning(TASK_ID);
    }

    @Test
    @DisplayName("onDelete should call onJobFailed when Job deleted unexpectedly")
    void onDelete_shouldCallOnJobFailed_whenJobDeletedUnexpectedly() {
        Job runningJob = createJob(TASK_ID, 1, null, null);

        eventHandler.onDelete(runningJob, false);

        verify(taskEventCallback).onJobFailed(TASK_ID, "Job deleted unexpectedly");
    }

    @Test
    @DisplayName("onDelete should not call onJobFailed when completed Job is deleted")
    void onDelete_shouldNotCallOnJobFailed_whenCompletedJobDeleted() {
        Job completedJob = createJob(TASK_ID, 0, "Complete", "True");

        eventHandler.onDelete(completedJob, false);

        verify(taskEventCallback, never()).onJobFailed(anyString(), anyString());
    }

    @Test
    @DisplayName("onAdd should track Job as PENDING without calling callbacks")
    void onAdd_shouldTrackJobAsPending_withoutCallingCallbacks() {
        Job pendingJob = createJob(TASK_ID, 0, null, null);

        eventHandler.onAdd(pendingJob);

        verifyNoInteractions(taskEventCallback);
    }

    @Test
    @DisplayName("Full lifecycle: PENDING → RUNNING → COMPLETE")
    void fullLifecycle_pendingToRunningToComplete() {
        Job pendingJob = createJob(TASK_ID, 0, null, null);
        Job runningJob = createJob(TASK_ID, 1, null, null);
        Job completedJob = createJob(TASK_ID, 0, "Complete", "True");

        eventHandler.onAdd(pendingJob);
        verifyNoInteractions(taskEventCallback);

        eventHandler.onUpdate(pendingJob, runningJob);
        verify(taskEventCallback).onJobRunning(TASK_ID);

        eventHandler.onUpdate(runningJob, completedJob);
        verify(taskEventCallback).onJobCompleted(TASK_ID);
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
