package com.huawei.modellite.repository.infrastructure.k8s;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskReconcilerTest {

    @Mock
    private UploadTaskRepository uploadTaskRepository;

    @Mock
    private K8sJobService k8sJobService;

    @Mock
    private TaskEventCallback taskEventCallback;

    @Mock
    private LeaderElectionService leaderElectionService;

    private TaskReconciler reconciler;

    @BeforeEach
    void setUp() {
        reconciler = new TaskReconciler(uploadTaskRepository, k8sJobService, taskEventCallback, leaderElectionService);
    }

    @Test
    @DisplayName("reconcile should skip when not leader")
    void reconcileSkipsWhenNotLeader() {
        when(leaderElectionService.isLeader()).thenReturn(false);

        reconciler.reconcile();

        verifyNoInteractions(uploadTaskRepository);
        verifyNoInteractions(k8sJobService);
    }

    @Test
    @DisplayName("scanPendingTasks should rebuild Job when Job does not exist")
    void scanPendingTasksRebuildsJobWhenMissing() {
        UUID taskId = UUID.randomUUID();
        UploadTask task = createMockTask(taskId, TaskStatus.PENDING);
        when(uploadTaskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of(task));
        when(k8sJobService.jobExists(taskId.toString())).thenReturn(false);

        reconciler.scanPendingTasks();

        verify(k8sJobService).createUploadJob(any());
    }

    @Test
    @DisplayName("scanPendingTasks should skip when Job exists")
    void scanPendingTasksSkipsWhenJobExists() {
        UUID taskId = UUID.randomUUID();
        UploadTask task = createMockTask(taskId, TaskStatus.PENDING);
        when(uploadTaskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of(task));
        when(k8sJobService.jobExists(taskId.toString())).thenReturn(true);

        reconciler.scanPendingTasks();

        verify(k8sJobService, never()).createUploadJob(any());
    }

    @Test
    @DisplayName("scanRunningTasks should call onJobCompleted when Job is COMPLETE")
    void scanRunningTasksCallsOnJobCompleted() {
        UUID taskId = UUID.randomUUID();
        UploadTask task = createMockTask(taskId, TaskStatus.RUNNING);
        when(uploadTaskRepository.findByStatus(TaskStatus.RUNNING)).thenReturn(List.of(task));
        when(k8sJobService.getJobStatus(taskId.toString())).thenReturn(JobStatus.COMPLETE);

        reconciler.scanRunningTasks();

        verify(taskEventCallback).onJobCompleted(taskId.toString());
    }

    @Test
    @DisplayName("scanRunningTasks should call onJobFailed when Job is FAILED")
    void scanRunningTasksCallsOnJobFailed() {
        UUID taskId = UUID.randomUUID();
        UploadTask task = createMockTask(taskId, TaskStatus.RUNNING);
        when(uploadTaskRepository.findByStatus(TaskStatus.RUNNING)).thenReturn(List.of(task));
        when(k8sJobService.getJobStatus(taskId.toString())).thenReturn(JobStatus.FAILED);
        when(k8sJobService.getJobPodLogs(anyString(), anyInt())).thenReturn("error: disk full");

        reconciler.scanRunningTasks();

        verify(taskEventCallback).onJobFailed(eq(taskId.toString()), anyString());
    }

    @Test
    @DisplayName("scanRunningTasks should call onJobFailed when Job is NOT_FOUND")
    void scanRunningTasksCallsOnJobFailedWhenNotFound() {
        UUID taskId = UUID.randomUUID();
        UploadTask task = createMockTask(taskId, TaskStatus.RUNNING);
        when(uploadTaskRepository.findByStatus(TaskStatus.RUNNING)).thenReturn(List.of(task));
        when(k8sJobService.getJobStatus(taskId.toString())).thenReturn(JobStatus.NOT_FOUND);

        reconciler.scanRunningTasks();

        verify(taskEventCallback).onJobFailed(eq(taskId.toString()), contains("disappeared"));
    }

    @Test
    @DisplayName("scanRunningTasks should handle optimistic lock conflict gracefully")
    void scanRunningTasksHandlesOptimisticLockConflict() {
        UUID taskId = UUID.randomUUID();
        UploadTask task = createMockTask(taskId, TaskStatus.RUNNING);
        when(uploadTaskRepository.findByStatus(TaskStatus.RUNNING)).thenReturn(List.of(task));
        when(k8sJobService.getJobStatus(taskId.toString())).thenReturn(JobStatus.COMPLETE);
        doThrow(new RuntimeException("optimistic lock conflict"))
                .when(taskEventCallback).onJobCompleted(taskId.toString());

        // Should not throw - exception is caught internally
        reconciler.scanRunningTasks();
    }

    @Test
    @DisplayName("scanTerminalTasks should cleanup resources for old terminal tasks")
    void scanTerminalTasksCleansUpResources() {
        UUID taskId = UUID.randomUUID();
        UploadTask task = createMockTask(taskId, TaskStatus.COMPLETED);
        when(uploadTaskRepository.findTerminalTasksOlderThan(anyLong())).thenReturn(List.of(task));

        reconciler.scanTerminalTasks();

        verify(k8sJobService).deleteJobResources(taskId.toString());
    }

    @Test
    @DisplayName("scanPausedTasks should not throw for paused tasks")
    void scanPausedTasksDoesNotThrow() {
        UUID taskId = UUID.randomUUID();
        UploadTask task = createMockTask(taskId, TaskStatus.PAUSED);
        when(uploadTaskRepository.findByStatus(TaskStatus.PAUSED)).thenReturn(List.of(task));

        // Should not throw
        reconciler.scanPausedTasks();
    }

    private UploadTask createMockTask(UUID taskId, TaskStatus status) {
        UploadTask task = mock(UploadTask.class);
        when(task.getTaskId()).thenReturn(taskId);
        when(task.getModelId()).thenReturn(UUID.randomUUID());
        when(task.getVersionId()).thenReturn(UUID.randomUUID());
        when(task.getStatus()).thenReturn(status);
        when(task.getTargetPath()).thenReturn("test/path");
        when(task.getSourcePath()).thenReturn(mock(com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.SourcePath.class));
        when(task.getSourcePath().getSourceType()).thenReturn(com.huawei.modellite.repository.common.enums.SourceType.NFS);
        when(task.getSourcePath().getPath()).thenReturn("nfs-server:/path");
        return task;
    }
}
