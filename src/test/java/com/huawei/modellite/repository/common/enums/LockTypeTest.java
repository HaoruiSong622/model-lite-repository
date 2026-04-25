package com.huawei.modellite.repository.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LockTypeTest {

    @Test
    @DisplayName("should return correct enum when fromDbValue with valid values")
    void should_returnCorrectEnum_when_fromDbValue() {
        assertEquals(LockType.INFERENCE, LockType.fromDbValue("Inference"));
        assertEquals(LockType.TRAINING, LockType.fromDbValue("Training"));
        assertEquals(LockType.EVALUATION, LockType.fromDbValue("Evaluation"));
        assertEquals(LockType.DEVELOPMENT, LockType.fromDbValue("Development"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when fromDbValue with invalid value")
    void should_throwException_when_fromDbValueInvalid() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> LockType.fromDbValue("INVALID")
        );
        assertTrue(exception.getMessage().contains("Unknown LockType: INVALID"));
    }

    @Test
    @DisplayName("should have unique dbValue for each enum constant")
    void should_haveUniqueDbValues() {
        long distinctCount = java.util.Arrays.stream(LockType.values())
            .map(LockType::getDbValue)
            .distinct()
            .count();
        assertEquals(LockType.values().length, distinctCount);
    }

    @Test
    @DisplayName("should return correct displayName")
    void should_returnCorrectDisplayName() {
        assertEquals("推理服务", LockType.INFERENCE.getDisplayName());
        assertEquals("训练任务", LockType.TRAINING.getDisplayName());
    }
}
