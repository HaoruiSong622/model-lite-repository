package com.huawei.modellite.repository.modelweight.domain.aggregate.category;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category("NLP", "Natural Language Processing");
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("should create Category with correct fields")
        void should_createCategory_withCorrectFields() {
            assertNotNull(category.getCategoryId());
            assertEquals("NLP", category.getName());
            assertEquals("Natural Language Processing", category.getDescription());
            assertFalse(category.getIsBuiltIn());
            assertNotNull(category.getModelTypes());
            assertTrue(category.getModelTypes().isEmpty());
        }

        @Test
        @DisplayName("should create Category with isBuiltIn flag")
        void should_createCategory_withBuiltInFlag() {
            Category builtIn = new Category("System", "System category", true);
            assertTrue(builtIn.getIsBuiltIn());
        }

        @Test
        @DisplayName("should throw exception when name is null")
        void should_throwException_whenNameIsNull() {
            assertThrows(ModelLiteException.class,
                    () -> new Category(null, "desc"));
        }

        @Test
        @DisplayName("should throw exception when name is empty")
        void should_throwException_whenNameIsEmpty() {
            assertThrows(ModelLiteException.class,
                    () -> new Category("", "desc"));
        }

        @Test
        @DisplayName("should throw exception when name exceeds 100 characters")
        void should_throwException_whenNameExceeds100() {
            String longName = "a".repeat(101);
            assertThrows(ModelLiteException.class,
                    () -> new Category(longName, "desc"));
        }

        @Test
        @DisplayName("should allow name with exactly 100 characters")
        void should_allowNameWith100Characters() {
            String name = "a".repeat(100);
            Category cat = new Category(name, "desc");
            assertEquals(name, cat.getName());
        }
    }

    @Nested
    @DisplayName("addModelType tests")
    class AddModelTypeTests {

        @Test
        @DisplayName("should add ModelType successfully")
        void should_addModelType_successfully() {
            ModelType modelType = category.addModelType("LLM", "Large Language Model");

            assertNotNull(modelType);
            assertEquals("LLM", modelType.getName());
            assertEquals("Large Language Model", modelType.getDescription());
            assertFalse(modelType.getIsBuiltIn());
            assertEquals(1, category.getModelTypes().size());
        }

        @Test
        @DisplayName("should throw MODEL_TYPE_NAME_EXISTS when adding duplicate name")
        void should_throw_whenAddingDuplicateName() {
            category.addModelType("LLM", "desc1");

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> category.addModelType("LLM", "desc2"));

            assertEquals(ErrorCode.MODEL_TYPE_NAME_EXISTS, exception.getCode());
        }

        @Test
        @DisplayName("should allow different names in same category")
        void should_allowDifferentNames() {
            category.addModelType("LLM", "desc1");
            category.addModelType("CV", "desc2");

            assertEquals(2, category.getModelTypes().size());
        }
    }

    @Nested
    @DisplayName("removeModelType tests")
    class RemoveModelTypeTests {

        @Test
        @DisplayName("should remove ModelType successfully")
        void should_removeModelType_successfully() {
            ModelType modelType = category.addModelType("LLM", "desc");

            category.removeModelType(modelType.getTypeId(), false);

            assertTrue(category.getModelTypes().isEmpty());
        }

        @Test
        @DisplayName("should throw MODEL_TYPE_NOT_FOUND when typeId does not exist")
        void should_throw_whenTypeIdNotFound() {
            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> category.removeModelType(UUID.randomUUID(), false));

            assertEquals(ErrorCode.MODEL_TYPE_NOT_FOUND, exception.getCode());
        }

        @Test
        @DisplayName("should throw MODEL_TYPE_BUILTIN when removing built-in ModelType")
        void should_throw_whenRemovingBuiltIn() {
            ModelType builtInType = new ModelType(UUID.randomUUID(), "BuiltIn", "built-in", true);
            java.lang.reflect.Field field;
            try {
                field = Category.class.getDeclaredField("modelTypes");
                field.setAccessible(true);
                java.util.List<ModelType> types = new java.util.ArrayList<>();
                types.add(builtInType);
                field.set(category, types);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> category.removeModelType(builtInType.getTypeId(), false));

            assertEquals(ErrorCode.MODEL_TYPE_BUILTIN, exception.getCode());
        }

        @Test
        @DisplayName("should throw CATEGORY_HAS_MODELS when ModelType has model references")
        void should_throw_whenHasModelReference() {
            ModelType modelType = category.addModelType("LLM", "desc");

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> category.removeModelType(modelType.getTypeId(), true));

            assertEquals(ErrorCode.CATEGORY_HAS_MODELS, exception.getCode());
        }
    }
}
