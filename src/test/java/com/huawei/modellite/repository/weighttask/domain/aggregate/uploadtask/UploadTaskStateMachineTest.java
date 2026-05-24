package com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.TaskStatus;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UploadTask 状态机完整覆盖测试")
class UploadTaskStateMachineTest {

    private static final Set<TaskStatus> TERMINAL_STATUSES = Set.of(
            TaskStatus.COMPLETED, TaskStatus.FAILED, TaskStatus.CANCELLED
    );

    private static UploadTask createDefaultTask() {
        return UploadTask.createUploadTask(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                SourcePath.ofNfs("10.0.0.1", "/data/models"),
                "/target/path", "testUser"
        );
    }

    private static UploadTask createTaskInStatus(TaskStatus status) {
        UploadTask task = createDefaultTask();
        switch (status) {
            case PENDING -> { /* default */ }
            case RUNNING -> task.start();
            case PAUSED -> {
                task.start();
                task.pause();
            }
            case COMPLETED -> {
                task.start();
                task.complete();
            }
            case FAILED -> task.fail("setup error");
            case CANCELLED -> task.cancel();
        }
        return task;
    }

    private static void executeOperation(UploadTask task, String operation) {
        switch (operation) {
            case "start" -> task.start();
            case "pause" -> task.pause();
            case "resume" -> task.resume();
            case "cancel" -> task.cancel();
            case "complete" -> task.complete();
            case "fail" -> task.fail("test error");
            case "updateProgress" -> task.updateProgress(50);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }

    static Stream<Arguments> stateTransitionMatrix() {
        List<Arguments> args = new ArrayList<>();

        for (TaskStatus status : TaskStatus.values()) {
            if (status == TaskStatus.PENDING || status == TaskStatus.PAUSED) {
                args.add(Arguments.of(status, "start", TaskStatus.RUNNING, null));
            } else if (TERMINAL_STATUSES.contains(status)) {
                args.add(Arguments.of(status, "start", null, ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED));
            } else {
                args.add(Arguments.of(status, "start", null, ErrorCode.UPLOAD_TASK_STATUS_CONFLICT));
            }

            if (status == TaskStatus.RUNNING) {
                args.add(Arguments.of(status, "pause", TaskStatus.PAUSED, null));
            } else if (TERMINAL_STATUSES.contains(status)) {
                args.add(Arguments.of(status, "pause", null, ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED));
            } else {
                args.add(Arguments.of(status, "pause", null, ErrorCode.UPLOAD_TASK_STATUS_CONFLICT));
            }

            if (status == TaskStatus.PAUSED) {
                args.add(Arguments.of(status, "resume", TaskStatus.PENDING, null));
            } else if (TERMINAL_STATUSES.contains(status)) {
                args.add(Arguments.of(status, "resume", null, ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED));
            } else {
                args.add(Arguments.of(status, "resume", null, ErrorCode.UPLOAD_TASK_STATUS_CONFLICT));
            }

            if (status == TaskStatus.PENDING || status == TaskStatus.RUNNING || status == TaskStatus.PAUSED) {
                args.add(Arguments.of(status, "cancel", TaskStatus.CANCELLED, null));
            } else {
                args.add(Arguments.of(status, "cancel", null, ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED));
            }

            if (status == TaskStatus.RUNNING) {
                args.add(Arguments.of(status, "complete", TaskStatus.COMPLETED, null));
            } else if (TERMINAL_STATUSES.contains(status)) {
                args.add(Arguments.of(status, "complete", null, ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED));
            } else {
                args.add(Arguments.of(status, "complete", null, ErrorCode.UPLOAD_TASK_STATUS_CONFLICT));
            }

            if (status == TaskStatus.PENDING || status == TaskStatus.RUNNING) {
                args.add(Arguments.of(status, "fail", TaskStatus.FAILED, null));
            } else if (TERMINAL_STATUSES.contains(status)) {
                args.add(Arguments.of(status, "fail", null, ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED));
            } else {
                args.add(Arguments.of(status, "fail", null, ErrorCode.UPLOAD_TASK_STATUS_CONFLICT));
            }

            if (status == TaskStatus.RUNNING) {
                args.add(Arguments.of(status, "updateProgress", TaskStatus.RUNNING, null));
            } else if (TERMINAL_STATUSES.contains(status)) {
                args.add(Arguments.of(status, "updateProgress", null, ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED));
            } else {
                args.add(Arguments.of(status, "updateProgress", null, ErrorCode.UPLOAD_TASK_STATUS_CONFLICT));
            }
        }

        return args.stream();
    }

    @ParameterizedTest(name = "[{index}] {0} + {1} -> expectedStatus={2}, expectedError={3}")
    @MethodSource("stateTransitionMatrix")
    @DisplayName("状态转换矩阵: 6状态 x 7操作 = 42个用例")
    void stateTransition_shouldFollowRules(TaskStatus initialStatus, String operation,
                                            TaskStatus expectedStatus, String expectedErrorCode) {
        UploadTask task = createTaskInStatus(initialStatus);

        if (expectedErrorCode != null) {
            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> executeOperation(task, operation),
                    "操作 " + operation + " 在状态 " + initialStatus + " 下应抛出异常");
            assertEquals(expectedErrorCode, ex.getCode(),
                    "操作 " + operation + " 在状态 " + initialStatus + " 下应抛出错误码 " + expectedErrorCode);
        } else {
            executeOperation(task, operation);
            assertEquals(expectedStatus, task.getStatus(),
                    "操作 " + operation + " 在状态 " + initialStatus + " 下应转换到 " + expectedStatus);

            if ("complete".equals(operation)) {
                assertEquals(100, task.getProgress(), "complete 后 progress 应为 100");
            }

            if ("fail".equals(operation)) {
                assertEquals("test error", task.getErrorMessage(), "fail 后应设置 errorMessage");
            }
        }
    }

    static Stream<Arguments> isTerminalMatrix() {
        return Stream.of(
                Arguments.of(TaskStatus.PENDING, false),
                Arguments.of(TaskStatus.RUNNING, false),
                Arguments.of(TaskStatus.PAUSED, false),
                Arguments.of(TaskStatus.COMPLETED, true),
                Arguments.of(TaskStatus.FAILED, true),
                Arguments.of(TaskStatus.CANCELLED, true)
        );
    }

    @ParameterizedTest(name = "[{index}] isTerminal({0}) = {1}")
    @MethodSource("isTerminalMatrix")
    @DisplayName("isTerminal() 对所有 6 种状态的返回值")
    void isTerminal_shouldReturnCorrectValue(TaskStatus status, boolean expected) {
        UploadTask task = createTaskInStatus(status);
        assertEquals(expected, task.isTerminal());
    }

    @Nested
    @DisplayName("进度相关额外测试")
    class ProgressTests {

        @Test
        @DisplayName("进度单调递增: 10->30->50->80")
        void updateProgress_shouldBeMonotonicallyIncreasing() {
            UploadTask task = createDefaultTask();
            task.start();

            task.updateProgress(10);
            assertEquals(10, task.getProgress());

            task.updateProgress(30);
            assertEquals(30, task.getProgress());

            task.updateProgress(50);
            assertEquals(50, task.getProgress());

            task.updateProgress(80);
            assertEquals(80, task.getProgress());
        }

        @Test
        @DisplayName("进度回退应被忽略: 50->30 保持 50")
        void updateProgress_regressionShouldBeIgnored() {
            UploadTask task = createDefaultTask();
            task.start();
            task.updateProgress(50);

            task.updateProgress(30);

            assertEquals(50, task.getProgress(), "进度回退应保持原值");
        }

        @Test
        @DisplayName("进度范围校验: -1 抛出 0102040")
        void updateProgress_negative_throwsInvalidProgress() {
            UploadTask task = createDefaultTask();
            task.start();
            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> task.updateProgress(-1));
            assertEquals(ErrorCode.UPLOAD_TASK_INVALID_PROGRESS, ex.getCode());
        }

        @Test
        @DisplayName("进度范围校验: 101 抛出 0102040")
        void updateProgress_over100_throwsInvalidProgress() {
            UploadTask task = createDefaultTask();
            task.start();
            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> task.updateProgress(101));
            assertEquals(ErrorCode.UPLOAD_TASK_INVALID_PROGRESS, ex.getCode());
        }

        @Test
        @DisplayName("进度边界值: 0 允许")
        void updateProgress_zero_isAllowed() {
            UploadTask task = createDefaultTask();
            task.start();
            task.updateProgress(0);
            assertEquals(0, task.getProgress());
        }

        @Test
        @DisplayName("进度边界值: 100 允许")
        void updateProgress_100_isAllowed() {
            UploadTask task = createDefaultTask();
            task.start();
            task.updateProgress(100);
            assertEquals(100, task.getProgress());
        }
    }

    @Nested
    @DisplayName("终态保护额外测试")
    class TerminalStateProtectionTests {

        @Test
        @DisplayName("COMPLETED 终态下任何操作均抛 0102044 (以 updateProgress 为例)")
        void completedState_anyOperation_throwsAlreadyTerminated() {
            UploadTask task = createDefaultTask();
            task.start();
            task.complete();

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> task.updateProgress(50));
            assertEquals(ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED, ex.getCode());
        }

        @Test
        @DisplayName("FAILED 终态下再次 fail 拒绝 (0102044)")
        void failedState_failAgain_throwsAlreadyTerminated() {
            UploadTask task = createDefaultTask();
            task.fail("first error");

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> task.fail("second error"));
            assertEquals(ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED, ex.getCode());
        }

        @Test
        @DisplayName("CANCELLED 终态下 start 拒绝 (0102044)")
        void cancelledState_start_throwsAlreadyTerminated() {
            UploadTask task = createDefaultTask();
            task.cancel();

            ModelLiteException ex = assertThrows(ModelLiteException.class, task::start);
            assertEquals(ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED, ex.getCode());
        }
    }

    @Nested
    @DisplayName("设计文档特定约束验证")
    class DesignDocConstraintTests {

        @Test
        @DisplayName("7.1.22: Paused->Running 转换允许")
        void designDoc_7_1_22_pausedToRunning_isAllowed() {
            UploadTask task = createDefaultTask();
            task.start();
            task.pause();
            assertEquals(TaskStatus.PAUSED, task.getStatus());

            task.start();
            assertEquals(TaskStatus.RUNNING, task.getStatus());
        }

        @Test
        @DisplayName("7.1.23: Failed 状态再次 fail 拒绝（终态保护）")
        void designDoc_7_1_23_failedState_failAgain_rejected() {
            UploadTask task = createDefaultTask();
            task.fail("first");
            assertEquals(TaskStatus.FAILED, task.getStatus());
            assertTrue(task.isTerminal());

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> task.fail("second"));
            assertEquals(ErrorCode.UPLOAD_TASK_ALREADY_TERMINATED, ex.getCode());
        }
    }
}
