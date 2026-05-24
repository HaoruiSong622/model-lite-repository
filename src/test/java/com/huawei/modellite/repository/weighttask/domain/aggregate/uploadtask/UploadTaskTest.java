package com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.TaskStatus;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UploadTaskTest {

    private static UploadTask createDefaultTask() {
        return UploadTask.createUploadTask(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                SourcePath.ofNfs("10.0.0.1", "/data/models"),
                "/target/path",
                "testUser"
        );
    }

    @Nested
    @DisplayName("createUploadTask factory tests")
    class CreateUploadTaskTests {

        @Test
        @DisplayName("createUploadTask_normal → status=Pending, progress=0, version=0")
        void createUploadTask_normal() {
            UUID taskId = UUID.randomUUID();
            UUID modelId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();
            SourcePath sourcePath = SourcePath.ofNfs("10.0.0.1", "/data/models");

            UploadTask task = UploadTask.createUploadTask(taskId, modelId, versionId, sourcePath, "/target", "user");

            assertEquals(taskId, task.getTaskId());
            assertEquals(modelId, task.getModelId());
            assertEquals(versionId, task.getVersionId());
            assertEquals(sourcePath, task.getSourcePath());
            assertEquals("/target", task.getTargetPath());
            assertEquals(0, task.getProgress());
            assertEquals(TaskStatus.PENDING, task.getStatus());
            assertNull(task.getErrorMessage());
            assertEquals("user", task.getCreateUser());
            assertNotNull(task.getCreateTime());
            assertNotNull(task.getUpdateTime());
            assertEquals(0L, task.getVersion());
        }

        @Test
        @DisplayName("createUploadTask_nullSourcePath → throws exception")
        void createUploadTask_nullSourcePath() {
            assertThrows(ModelLiteException.class,
                    () -> UploadTask.createUploadTask(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                            null, "/target", "user"));
        }
    }

    @Nested
    @DisplayName("start() state transition tests")
    class StartTests {

        @Test
        @DisplayName("start_PendingToRunning")
        void start_PendingToRunning() {
            UploadTask task = createDefaultTask();
            task.start();
            assertEquals(TaskStatus.RUNNING, task.getStatus());
        }

        @Test
        @DisplayName("start_PausedToRunning")
        void start_PausedToRunning() {
            UploadTask task = createDefaultTask();
            task.start();
            task.pause();
            task.start();
            assertEquals(TaskStatus.RUNNING, task.getStatus());
        }

        @Test
        @DisplayName("start_Completed_throwsAlreadyTerminated (0102044)")
        void start_Completed_throwsAlreadyTerminated() {
            UploadTask task = createDefaultTask();
            task.start();
            task.complete();
            ModelLiteException ex = assertThrows(ModelLiteException.class, task::start);
            assertEquals(ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED, ex.getCode());
        }

        @Test
        @DisplayName("start_Running_throwsStatusConflict (0102037)")
        void start_Running_throwsStatusConflict() {
            UploadTask task = createDefaultTask();
            task.start();
            ModelLiteException ex = assertThrows(ModelLiteException.class, task::start);
            assertEquals(ErrorCode.UPLOAD_TASK_STATUS_CONFLICT, ex.getCode());
        }
    }

    @Nested
    @DisplayName("pause() state transition tests")
    class PauseTests {

        @Test
        @DisplayName("pause_RunningToPaused")
        void pause_RunningToPaused() {
            UploadTask task = createDefaultTask();
            task.start();
            task.pause();
            assertEquals(TaskStatus.PAUSED, task.getStatus());
        }

        @Test
        @DisplayName("pause_Pending_throwsStatusConflict")
        void pause_Pending_throwsStatusConflict() {
            UploadTask task = createDefaultTask();
            ModelLiteException ex = assertThrows(ModelLiteException.class, task::pause);
            assertEquals(ErrorCode.UPLOAD_TASK_STATUS_CONFLICT, ex.getCode());
        }
    }

    @Nested
    @DisplayName("resume() state transition tests")
    class ResumeTests {

        @Test
        @DisplayName("resume_PausedToPending")
        void resume_PausedToPending() {
            UploadTask task = createDefaultTask();
            task.start();
            task.pause();
            task.resume();
            assertEquals(TaskStatus.PENDING, task.getStatus());
        }

        @Test
        @DisplayName("resume_Running_throwsStatusConflict")
        void resume_Running_throwsStatusConflict() {
            UploadTask task = createDefaultTask();
            task.start();
            ModelLiteException ex = assertThrows(ModelLiteException.class, task::resume);
            assertEquals(ErrorCode.UPLOAD_TASK_STATUS_CONFLICT, ex.getCode());
        }
    }

    @Nested
    @DisplayName("cancel() state transition tests")
    class CancelTests {

        @Test
        @DisplayName("cancel_RunningToCancelled")
        void cancel_RunningToCancelled() {
            UploadTask task = createDefaultTask();
            task.start();
            task.cancel();
            assertEquals(TaskStatus.CANCELLED, task.getStatus());
        }

        @Test
        @DisplayName("cancel_PendingToCancelled")
        void cancel_PendingToCancelled() {
            UploadTask task = createDefaultTask();
            task.cancel();
            assertEquals(TaskStatus.CANCELLED, task.getStatus());
        }

        @Test
        @DisplayName("cancel_PausedToCancelled")
        void cancel_PausedToCancelled() {
            UploadTask task = createDefaultTask();
            task.start();
            task.pause();
            task.cancel();
            assertEquals(TaskStatus.CANCELLED, task.getStatus());
        }

        @Test
        @DisplayName("cancel_Completed_throwsAlreadyTerminated (0102044)")
        void cancel_Completed_throwsAlreadyTerminated() {
            UploadTask task = createDefaultTask();
            task.start();
            task.complete();
            ModelLiteException ex = assertThrows(ModelLiteException.class, task::cancel);
            assertEquals(ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED, ex.getCode());
        }

        @Test
        @DisplayName("cancel_Cancelled_throwsAlreadyTerminated (0102044)")
        void cancel_Cancelled_throwsAlreadyTerminated() {
            UploadTask task = createDefaultTask();
            task.cancel();
            ModelLiteException ex = assertThrows(ModelLiteException.class, task::cancel);
            assertEquals(ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED, ex.getCode());
        }
    }

    @Nested
    @DisplayName("complete() state transition tests")
    class CompleteTests {

        @Test
        @DisplayName("complete_RunningToCompleted_progress100")
        void complete_RunningToCompleted_progress100() {
            UploadTask task = createDefaultTask();
            task.start();
            task.complete();
            assertEquals(TaskStatus.COMPLETED, task.getStatus());
            assertEquals(100, task.getProgress());
        }

        @Test
        @DisplayName("complete_Pending_throwsStatusConflict")
        void complete_Pending_throwsStatusConflict() {
            UploadTask task = createDefaultTask();
            ModelLiteException ex = assertThrows(ModelLiteException.class, task::complete);
            assertEquals(ErrorCode.UPLOAD_TASK_STATUS_CONFLICT, ex.getCode());
        }
    }

    @Nested
    @DisplayName("fail() state transition tests")
    class FailTests {

        @Test
        @DisplayName("fail_RunningToFailed_withMessage")
        void fail_RunningToFailed_withMessage() {
            UploadTask task = createDefaultTask();
            task.start();
            task.fail("network error");
            assertEquals(TaskStatus.FAILED, task.getStatus());
            assertEquals("network error", task.getErrorMessage());
        }

        @Test
        @DisplayName("fail_PendingToFailed_withMessage")
        void fail_PendingToFailed_withMessage() {
            UploadTask task = createDefaultTask();
            task.fail("validation failed");
            assertEquals(TaskStatus.FAILED, task.getStatus());
            assertEquals("validation failed", task.getErrorMessage());
        }

        @Test
        @DisplayName("Failed状态再次fail拒绝 (0102044)")
        void fail_Failed_throwsAlreadyTerminated() {
            UploadTask task = createDefaultTask();
            task.fail("first error");
            ModelLiteException ex = assertThrows(ModelLiteException.class, () -> task.fail("second error"));
            assertEquals(ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED, ex.getCode());
        }
    }

    @Nested
    @DisplayName("updateProgress() tests")
    class UpdateProgressTests {

        @Test
        @DisplayName("updateProgress_normal_30to50")
        void updateProgress_normal_30to50() {
            UploadTask task = createDefaultTask();
            task.start();
            task.updateProgress(30);
            assertEquals(30, task.getProgress());
            task.updateProgress(50);
            assertEquals(50, task.getProgress());
        }

        @Test
        @DisplayName("updateProgress_regressionIgnored_50to30_stays50")
        void updateProgress_regressionIgnored_50to30_stays50() {
            UploadTask task = createDefaultTask();
            task.start();
            task.updateProgress(50);
            task.updateProgress(30);
            assertEquals(50, task.getProgress());
        }

        @Test
        @DisplayName("updateProgress_outOfRange_150_throws (0102040)")
        void updateProgress_outOfRange_150_throws() {
            UploadTask task = createDefaultTask();
            task.start();
            ModelLiteException ex = assertThrows(ModelLiteException.class, () -> task.updateProgress(150));
            assertEquals(ErrorCode.UPLOAD_TASK_INVALID_PROGRESS, ex.getCode());
        }

        @Test
        @DisplayName("updateProgress_Pending_throwsStatusConflict")
        void updateProgress_Pending_throwsStatusConflict() {
            UploadTask task = createDefaultTask();
            ModelLiteException ex = assertThrows(ModelLiteException.class, () -> task.updateProgress(50));
            assertEquals(ErrorCode.UPLOAD_TASK_STATUS_CONFLICT, ex.getCode());
        }
    }

    @Nested
    @DisplayName("isTerminal() tests")
    class IsTerminalTests {

        @Test
        @DisplayName("isTerminal_Completed_true")
        void isTerminal_Completed_true() {
            UploadTask task = createDefaultTask();
            task.start();
            task.complete();
            assertTrue(task.isTerminal());
        }

        @Test
        @DisplayName("isTerminal_Failed_true")
        void isTerminal_Failed_true() {
            UploadTask task = createDefaultTask();
            task.fail("error");
            assertTrue(task.isTerminal());
        }

        @Test
        @DisplayName("isTerminal_Cancelled_true")
        void isTerminal_Cancelled_true() {
            UploadTask task = createDefaultTask();
            task.cancel();
            assertTrue(task.isTerminal());
        }

        @Test
        @DisplayName("isTerminal_Running_false")
        void isTerminal_Running_false() {
            UploadTask task = createDefaultTask();
            task.start();
            assertFalse(task.isTerminal());
        }

        @Test
        @DisplayName("isTerminal_Pending_false")
        void isTerminal_Pending_false() {
            UploadTask task = createDefaultTask();
            assertFalse(task.isTerminal());
        }
    }
}
