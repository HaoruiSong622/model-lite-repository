package com.huawei.modellite.repository.infrastructure.k8s;

import com.huawei.modellite.repository.common.enums.TaskStatus;
import com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask.UploadTask;
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
 * Integration-style test for TaskReconciler with mocked K8s and DB layers.
 * Full K8s Mock Server integration would require @EnableKubernetesMockClient setup.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskReconcilerIntegrationTest {

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
    @DisplayName("Full scan cycle: pending rebuild + running complete + terminal cleanup")
    void fullScanCycle() {
        UUID pendingTaskId = UUID.randomUUID();
        UUID runningTaskId = UUID.randomUUID();
        UUID terminalTaskId = UUID.randomUUID();

        UploadTask pendingTask = createMockTask(pendingTaskId, TaskStatus.PENDING);
        UploadTask runningTask = createMockTask(runningTaskId, TaskStatus.RUNNING);
        UploadTask terminalTask = createMockTask(terminalTaskId, TaskStatus.COMPLETED);

        when(leaderElectionService.isLeader()).thenReturn(true);

        when(uploadTaskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of(pendingTask));
        when(uploadTaskRepository.findByStatus(TaskStatus.RUNNING)).thenReturn(List.of(runningTask));
        when(uploadTaskRepository.findByStatus(TaskStatus.PAUSED)).thenReturn(List.of());
        when(uploadTaskRepository.findTerminalTasksOlderThan(anyLong())).thenReturn(List.of(terminalTask));

        when(k8sJobService.jobExists(pendingTaskId.toString())).thenReturn(false);
        when(k8sJobService.getJobStatus(runningTaskId.toString()))
                .thenReturn(com.huawei.modellite.repository.weighttask.domain.dto.JobStatus.COMPLETE);

        reconciler.reconcile();

        verify(k8sJobService).createUploadJob(any());
        verify(taskEventCallback).onJobCompleted(runningTaskId.toString());
        verify(k8sJobService).deleteJobResources(terminalTaskId.toString());
    }

    @Test
    @DisplayName("Running task with Job NOT_FOUND should trigger fail callback")
    void runningTaskJobNotFoundTriggersFail() {
        UUID taskId = UUID.randomUUID();
        UploadTask task = createMockTask(taskId, TaskStatus.RUNNING);

        when(uploadTaskRepository.findByStatus(TaskStatus.RUNNING)).thenReturn(List.of(task));
        when(uploadTaskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of());
        when(uploadTaskRepository.findByStatus(TaskStatus.PAUSED)).thenReturn(List.of());
        when(uploadTaskRepository.findTerminalTasksOlderThan(anyLong())).thenReturn(List.of());
        when(leaderElectionService.isLeader()).thenReturn(true);
        when(k8sJobService.getJobStatus(taskId.toString()))
                .thenReturn(com.huawei.modellite.repository.weighttask.domain.dto.JobStatus.NOT_FOUND);

        reconciler.reconcile();

        verify(taskEventCallback).onJobFailed(eq(taskId.toString()), contains("disappeared"));
    }

    @Test
    @DisplayName("Non-leader should skip all scan operations")
    void nonLeaderSkipsAllScans() {
        when(leaderElectionService.isLeader()).thenReturn(false);

        reconciler.reconcile();

        verify(uploadTaskRepository, never()).findByStatus(any());
        verify(uploadTaskRepository, never()).findTerminalTasksOlderThan(anyLong());
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
