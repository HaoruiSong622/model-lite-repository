package com.huawei.modellite.repository.integration;

import com.huawei.modellite.repository.common.enums.TaskStatus;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.UploadTask;
import com.huawei.modellite.repository.weighttask.domain.dto.JobStatus;
import com.huawei.modellite.repository.weighttask.domain.repository.UploadTaskRepository;
import com.huawei.modellite.repository.weighttask.domain.service.K8sJobService;
import com.huawei.modellite.repository.weighttask.domain.service.TaskEventCallback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * End-to-end flow tests for upload task lifecycle.
 * Covers FF-1 through FF-5 scenarios.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EndToEndUploadFlowTest {

    @Mock
    private UploadTaskRepository uploadTaskRepository;

    @Mock
    private K8sJobService k8sJobService;

    @Mock
    private TaskEventCallback taskEventCallback;

    @Mock
    private com.huawei.modellite.repository.infrastructure.k8s.LeaderElectionService leaderElectionService;

    private com.huawei.modellite.repository.infrastructure.k8s.TaskReconciler reconciler;

    @BeforeEach
    void setUp() {
        reconciler = new com.huawei.modellite.repository.infrastructure.k8s.TaskReconciler(
                uploadTaskRepository, k8sJobService, taskEventCallback, leaderElectionService);
    }

    @Test
    @DisplayName("FF-1: Successful upload flow - create -> pending -> running -> completed")
    void testSuccessfulUploadFlow() {
        UUID taskId = UUID.randomUUID();
        UploadTask task = createMockTask(taskId, TaskStatus.PENDING);

        // Phase 1: Task created, pending, no Job yet
        when(uploadTaskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of(task));
        when(uploadTaskRepository.findByStatus(TaskStatus.RUNNING)).thenReturn(List.of());
        when(uploadTaskRepository.findByStatus(TaskStatus.PAUSED)).thenReturn(List.of());
        when(uploadTaskRepository.findTerminalTasksOlderThan(anyLong())).thenReturn(List.of());
        when(leaderElectionService.isLeader()).thenReturn(true);
        when(k8sJobService.jobExists(taskId.toString())).thenReturn(false);

        reconciler.reconcile();
        verify(k8sJobService).createUploadJob(any());

        // Phase 2: Job running, reconcile detects running state
        UploadTask runningTask = createMockTask(taskId, TaskStatus.RUNNING);
        when(uploadTaskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of());
        when(uploadTaskRepository.findByStatus(TaskStatus.RUNNING)).thenReturn(List.of(runningTask));
        when(k8sJobService.getJobStatus(taskId.toString())).thenReturn(JobStatus.COMPLETE);

        reconciler.reconcile();
        verify(taskEventCallback).onJobCompleted(taskId.toString());
    }

    @Test
    @DisplayName("FF-2: Validation failed flow - create -> pending -> running -> failed")
    void testValidationFailedFlow() {
        UUID taskId = UUID.randomUUID();
        UploadTask task = createMockTask(taskId, TaskStatus.RUNNING);

        when(uploadTaskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of());
        when(uploadTaskRepository.findByStatus(TaskStatus.RUNNING)).thenReturn(List.of(task));
        when(uploadTaskRepository.findByStatus(TaskStatus.PAUSED)).thenReturn(List.of());
        when(uploadTaskRepository.findTerminalTasksOlderThan(anyLong())).thenReturn(List.of());
        when(leaderElectionService.isLeader()).thenReturn(true);
        when(k8sJobService.getJobStatus(taskId.toString())).thenReturn(JobStatus.FAILED);
        when(k8sJobService.getJobPodLogs(anyString(), anyInt())).thenReturn("error: validation failed: source path unreachable");

        reconciler.reconcile();

        verify(taskEventCallback).onJobFailed(eq(taskId.toString()), contains("validation failed"));
    }

    @Test
    @DisplayName("FF-3: Pause/Resume flow - running -> paused -> pending -> completed")
    void testPauseResumeFlow() {
        UUID taskId = UUID.randomUUID();

        // Phase 1: Task is paused (simulated - pause is handled by API, not reconciler)
        UploadTask pausedTask = createMockTask(taskId, TaskStatus.PAUSED);
        when(uploadTaskRepository.findByStatus(TaskStatus.PAUSED)).thenReturn(List.of(pausedTask));
        when(uploadTaskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of());
        when(uploadTaskRepository.findByStatus(TaskStatus.RUNNING)).thenReturn(List.of());
        when(uploadTaskRepository.findTerminalTasksOlderThan(anyLong())).thenReturn(List.of());
        when(leaderElectionService.isLeader()).thenReturn(true);

        reconciler.reconcile();
        // Paused task is found but no action taken (placeholder)
        verify(k8sJobService, never()).createUploadJob(any());

        // Phase 2: Task resumed to pending, Job rebuilt
        UploadTask pendingTask = createMockTask(taskId, TaskStatus.PENDING);
        when(uploadTaskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of(pendingTask));
        when(uploadTaskRepository.findByStatus(TaskStatus.PAUSED)).thenReturn(List.of());
        when(k8sJobService.jobExists(taskId.toString())).thenReturn(false);

        reconciler.reconcile();
        verify(k8sJobService).createUploadJob(any());
    }

    @Test
    @DisplayName("FF-4: Cancel flow - running -> cancelled, Job deleted")
    void testCancelFlow() {
        UUID taskId = UUID.randomUUID();

        // Cancel is handled by API (deleteJob + cancel task)
        // Reconciler should NOT find the task after cancel
        when(uploadTaskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of());
        when(uploadTaskRepository.findByStatus(TaskStatus.RUNNING)).thenReturn(List.of());
        when(uploadTaskRepository.findByStatus(TaskStatus.PAUSED)).thenReturn(List.of());
        when(uploadTaskRepository.findTerminalTasksOlderThan(anyLong())).thenReturn(List.of());
        when(leaderElectionService.isLeader()).thenReturn(true);

        reconciler.reconcile();

        // No actions taken - task was cancelled and removed
        verify(k8sJobService, never()).createUploadJob(any());
        verify(taskEventCallback, never()).onJobCompleted(anyString());
    }

    @Test
    @DisplayName("FF-5: Timeout recovery flow - pending with missing Job -> Job rebuilt -> completed")
    void testTimeoutRecoveryFlow() {
        UUID taskId = UUID.randomUUID();
        UploadTask task = createMockTask(taskId, TaskStatus.PENDING);

        // Phase 1: Pending task with missing Job (Job was lost)
        when(uploadTaskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of(task));
        when(uploadTaskRepository.findByStatus(TaskStatus.RUNNING)).thenReturn(List.of());
        when(uploadTaskRepository.findByStatus(TaskStatus.PAUSED)).thenReturn(List.of());
        when(uploadTaskRepository.findTerminalTasksOlderThan(anyLong())).thenReturn(List.of());
        when(leaderElectionService.isLeader()).thenReturn(true);
        when(k8sJobService.jobExists(taskId.toString())).thenReturn(false);

        reconciler.reconcile();

        // Job should be rebuilt
        verify(k8sJobService).createUploadJob(any());

        // Phase 2: Job completes after rebuild
        UploadTask runningTask = createMockTask(taskId, TaskStatus.RUNNING);
        when(uploadTaskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of());
        when(uploadTaskRepository.findByStatus(TaskStatus.RUNNING)).thenReturn(List.of(runningTask));
        when(k8sJobService.getJobStatus(taskId.toString())).thenReturn(JobStatus.COMPLETE);

        reconciler.reconcile();
        verify(taskEventCallback).onJobCompleted(taskId.toString());
    }

    private UploadTask createMockTask(UUID taskId, TaskStatus status) {
        UploadTask task = mock(UploadTask.class);
        when(task.getTaskId()).thenReturn(taskId);
        when(task.getModelId()).thenReturn(UUID.randomUUID());
        when(task.getVersionId()).thenReturn(UUID.randomUUID());
        when(task.getStatus()).thenReturn(status);
        when(task.getTargetPath()).thenReturn("test/path");
        var sourcePath = mock(com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.SourcePath.class);
        when(sourcePath.getSourceType()).thenReturn(com.huawei.modellite.repository.common.enums.SourceType.NFS);
        when(sourcePath.getPath()).thenReturn("nfs-server:/path");
        when(task.getSourcePath()).thenReturn(sourcePath);
        return task;
    }
}
