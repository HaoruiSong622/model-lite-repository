package com.huawei.modellite.repository.modelweight.domain.aggregate.category;

import com.huawei.modellite.repository.common.exception.ModelLiteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModelTypeTest {

    @Test
    @DisplayName("should create ModelType with correct fields")
    void should_createModelType_withCorrectFields() {
        UUID typeId = UUID.randomUUID();
        ModelType modelType = new ModelType(typeId, "LLM", "Large Language Model", false);

        assertEquals(typeId, modelType.getTypeId());
        assertEquals("LLM", modelType.getName());
        assertEquals("Large Language Model", modelType.getDescription());
        assertFalse(modelType.getIsBuiltIn());
    }

    @Test
    @DisplayName("should create built-in ModelType")
    void should_createBuiltInModelType() {
        ModelType modelType = new ModelType(UUID.randomUUID(), "BuiltIn", "desc", true);
        assertTrue(modelType.getIsBuiltIn());
    }

    @Test
    @DisplayName("should throw exception when name is null")
    void should_throwException_whenNameIsNull() {
        assertThrows(ModelLiteException.class,
                () -> new ModelType(UUID.randomUUID(), null, "desc", false));
    }

    @Test
    @DisplayName("should throw exception when name is empty")
    void should_throwException_whenNameIsEmpty() {
        assertThrows(ModelLiteException.class,
                () -> new ModelType(UUID.randomUUID(), "", "desc", false));
    }

    @Test
    @DisplayName("should throw exception when name exceeds 100 characters")
    void should_throwException_whenNameExceeds100() {
        String longName = "a".repeat(101);
        assertThrows(ModelLiteException.class,
                () -> new ModelType(UUID.randomUUID(), longName, "desc", false));
    }

    @Test
    @DisplayName("should allow name with exactly 100 characters")
    void should_allowNameWith100Characters() {
        String name = "a".repeat(100);
        ModelType modelType = new ModelType(UUID.randomUUID(), name, "desc", false);
        assertEquals(name, modelType.getName());
    }

    @Test
    @DisplayName("should allow null description")
    void should_allowNullDescription() {
        ModelType modelType = new ModelType(UUID.randomUUID(), "Test", null, false);
        assertNull(modelType.getDescription());
    }
}
