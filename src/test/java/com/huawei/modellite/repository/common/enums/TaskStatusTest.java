package com.huawei.modellite.repository.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskStatusTest {

    @Test
    @DisplayName("should return correct enum when fromDbValue with valid values")
    void should_returnCorrectEnum_when_fromDbValue() {
        assertEquals(TaskStatus.PENDING, TaskStatus.fromDbValue("Pending"));
        assertEquals(TaskStatus.RUNNING, TaskStatus.fromDbValue("Running"));
        assertEquals(TaskStatus.PAUSED, TaskStatus.fromDbValue("Paused"));
        assertEquals(TaskStatus.COMPLETED, TaskStatus.fromDbValue("Completed"));
        assertEquals(TaskStatus.FAILED, TaskStatus.fromDbValue("Failed"));
        assertEquals(TaskStatus.CANCELLED, TaskStatus.fromDbValue("Cancelled"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when fromDbValue with invalid value")
    void should_throwException_when_fromDbValueInvalid() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TaskStatus.fromDbValue("INVALID")
        );
        assertTrue(exception.getMessage().contains("Unknown TaskStatus: INVALID"));
    }

    @Test
    @DisplayName("should have unique dbValue for each enum constant")
    void should_haveUniqueDbValues() {
        long distinctCount = java.util.Arrays.stream(TaskStatus.values())
            .map(TaskStatus::getDbValue)
            .distinct()
            .count();
        assertEquals(TaskStatus.values().length, distinctCount);
    }

    @Test
    @DisplayName("should return correct displayName")
    void should_returnCorrectDisplayName() {
        assertEquals("待执行", TaskStatus.PENDING.getDisplayName());
        assertEquals("已完成", TaskStatus.COMPLETED.getDisplayName());
    }
}
