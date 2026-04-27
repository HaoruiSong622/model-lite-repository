package com.huawei.modellite.repository.modelweight.domain.service;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.modelweight.domain.aggregate.category.Category;
import com.huawei.modellite.repository.modelweight.domain.aggregate.category.ModelType;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.Model;
import com.huawei.modellite.repository.modelweight.domain.repository.CategoryRepository;
import com.huawei.modellite.repository.modelweight.domain.repository.ModelRepository;
import com.huawei.modellite.repository.modelweight.domain.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelDomainServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private TagRepository tagRepository;

    private ModelDomainService service;

    @BeforeEach
    void setUp() {
        service = new ModelDomainService(categoryRepository, modelRepository, tagRepository);
    }

    // ========== validateModelCreation ==========

    @Nested
    @DisplayName("validateModelCreation tests")
    class ValidateModelCreationTests {

        @Test
        @DisplayName("should pass when all validations succeed")
        void should_pass_whenAllValid() {
            UUID categoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();
            Category category = createCategoryWithModelType(categoryId, typeId);

            when(categoryRepository.findByIdWithTypes(categoryId)).thenReturn(Optional.of(category));
            when(modelRepository.existsByCategoryAndTypeAndName(categoryId, typeId, "model-name")).thenReturn(false);
            when(modelRepository.countByResourceGroup("rg-1")).thenReturn(0L);
            when(modelRepository.countAll()).thenReturn(0L);

            assertDoesNotThrow(() ->
                    service.validateModelCreation("model-name", categoryId, typeId, "rg-1"));
        }

        @Test
        @DisplayName("should throw CATEGORY_NOT_FOUND when category does not exist")
        void should_throw_whenCategoryNotFound() {
            UUID categoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();

            when(categoryRepository.findByIdWithTypes(categoryId)).thenReturn(Optional.empty());

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> service.validateModelCreation("name", categoryId, typeId, "rg"));

            assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getCode());
            verify(modelRepository, never()).existsByCategoryAndTypeAndName(any(), any(), any());
        }

        @Test
        @DisplayName("should throw MODEL_TYPE_NOT_BELONG_TO_CATEGORY when type not in category")
        void should_throw_whenTypeNotBelongToCategory() {
            UUID categoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();
            Category category = new Category(categoryId, "NLP", "desc", false);

            when(categoryRepository.findByIdWithTypes(categoryId)).thenReturn(Optional.of(category));

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> service.validateModelCreation("name", categoryId, typeId, "rg"));

            assertEquals(ErrorCode.MODEL_TYPE_NOT_BELONG_TO_CATEGORY, ex.getCode());
        }

        @Test
        @DisplayName("should throw MODEL_NAME_EXISTS when name already used in same category+type")
        void should_throw_whenNameExists() {
            UUID categoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();
            Category category = createCategoryWithModelType(categoryId, typeId);

            when(categoryRepository.findByIdWithTypes(categoryId)).thenReturn(Optional.of(category));
            when(modelRepository.existsByCategoryAndTypeAndName(categoryId, typeId, "dup-name")).thenReturn(true);

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> service.validateModelCreation("dup-name", categoryId, typeId, "rg"));

            assertEquals(ErrorCode.MODEL_NAME_EXISTS, ex.getCode());
        }

        @Test
        @DisplayName("should throw MODEL_CAPACITY_EXCEEDED when resource group at limit")
        void should_throw_whenResourceGroupCapacityExceeded() {
            UUID categoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();
            Category category = createCategoryWithModelType(categoryId, typeId);

            when(categoryRepository.findByIdWithTypes(categoryId)).thenReturn(Optional.of(category));
            when(modelRepository.existsByCategoryAndTypeAndName(categoryId, typeId, "name")).thenReturn(false);
            when(modelRepository.countByResourceGroup("rg-full")).thenReturn(100L);

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> service.validateModelCreation("name", categoryId, typeId, "rg-full"));

            assertEquals(ErrorCode.MODEL_CAPACITY_EXCEEDED, ex.getCode());
        }

        @Test
        @DisplayName("should pass when resource group count is 99 (below limit)")
        void should_pass_whenResourceGroupCount99() {
            UUID categoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();
            Category category = createCategoryWithModelType(categoryId, typeId);

            when(categoryRepository.findByIdWithTypes(categoryId)).thenReturn(Optional.of(category));
            when(modelRepository.existsByCategoryAndTypeAndName(categoryId, typeId, "name")).thenReturn(false);
            when(modelRepository.countByResourceGroup("rg-99")).thenReturn(99L);
            when(modelRepository.countAll()).thenReturn(0L);

            assertDoesNotThrow(() ->
                    service.validateModelCreation("name", categoryId, typeId, "rg-99"));
        }

        @Test
        @DisplayName("should throw MODEL_GLOBAL_CAPACITY_EXCEEDED when global count at limit")
        void should_throw_whenGlobalCapacityExceeded() {
            UUID categoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();
            Category category = createCategoryWithModelType(categoryId, typeId);

            when(categoryRepository.findByIdWithTypes(categoryId)).thenReturn(Optional.of(category));
            when(modelRepository.existsByCategoryAndTypeAndName(categoryId, typeId, "name")).thenReturn(false);
            when(modelRepository.countByResourceGroup("rg")).thenReturn(50L);
            when(modelRepository.countAll()).thenReturn(1000L);

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> service.validateModelCreation("name", categoryId, typeId, "rg"));

            assertEquals(ErrorCode.MODEL_GLOBAL_CAPACITY_EXCEEDED, ex.getCode());
        }

        @Test
        @DisplayName("should pass when global count is 999 (below limit)")
        void should_pass_whenGlobalCount999() {
            UUID categoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();
            Category category = createCategoryWithModelType(categoryId, typeId);

            when(categoryRepository.findByIdWithTypes(categoryId)).thenReturn(Optional.of(category));
            when(modelRepository.existsByCategoryAndTypeAndName(categoryId, typeId, "name")).thenReturn(false);
            when(modelRepository.countByResourceGroup("rg")).thenReturn(50L);
            when(modelRepository.countAll()).thenReturn(999L);

            assertDoesNotThrow(() ->
                    service.validateModelCreation("name", categoryId, typeId, "rg"));
        }
    }

    // ========== validateModelModification ==========

    @Nested
    @DisplayName("validateModelModification tests")
    class ValidateModelModificationTests {

        @Test
        @DisplayName("should pass when model exists and no category/type change")
        void should_pass_whenNoCategoryTypeChange() {
            UUID modelId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();
            Model model = createModel(modelId, "name", categoryId, typeId, "rg");

            when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));

            assertDoesNotThrow(() ->
                    service.validateModelModification(modelId, categoryId, typeId));
        }

        @Test
        @DisplayName("should throw MODEL_NOT_FOUND when model does not exist")
        void should_throw_whenModelNotFound() {
            UUID modelId = UUID.randomUUID();

            when(modelRepository.findById(modelId)).thenReturn(Optional.empty());

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> service.validateModelModification(modelId, UUID.randomUUID(), UUID.randomUUID()));

            assertEquals(ErrorCode.MODEL_NOT_FOUND, ex.getCode());
        }

        @Test
        @DisplayName("should pass when categoryId changes and new category+type are valid")
        void should_pass_whenCategoryChangeValid() {
            UUID modelId = UUID.randomUUID();
            UUID oldCategoryId = UUID.randomUUID();
            UUID newCategoryId = UUID.randomUUID();
            UUID newTypeId = UUID.randomUUID();
            Model model = createModel(modelId, "name", oldCategoryId, UUID.randomUUID(), "rg");
            Category newCategory = createCategoryWithModelType(newCategoryId, newTypeId);

            when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
            when(categoryRepository.findByIdWithTypes(newCategoryId)).thenReturn(Optional.of(newCategory));
            when(modelRepository.existsByCategoryAndTypeAndName(newCategoryId, newTypeId, "name")).thenReturn(false);

            assertDoesNotThrow(() ->
                    service.validateModelModification(modelId, newCategoryId, newTypeId));
        }

        @Test
        @DisplayName("should throw CATEGORY_NOT_FOUND when new category does not exist")
        void should_throw_whenNewCategoryNotFound() {
            UUID modelId = UUID.randomUUID();
            UUID oldCategoryId = UUID.randomUUID();
            UUID newCategoryId = UUID.randomUUID();
            UUID newTypeId = UUID.randomUUID();
            Model model = createModel(modelId, "name", oldCategoryId, UUID.randomUUID(), "rg");

            when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
            when(categoryRepository.findByIdWithTypes(newCategoryId)).thenReturn(Optional.empty());

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> service.validateModelModification(modelId, newCategoryId, newTypeId));

            assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getCode());
        }

        @Test
        @DisplayName("should throw MODEL_TYPE_NOT_BELONG_TO_CATEGORY when new type not in new category")
        void should_throw_whenNewTypeNotInNewCategory() {
            UUID modelId = UUID.randomUUID();
            UUID oldCategoryId = UUID.randomUUID();
            UUID newCategoryId = UUID.randomUUID();
            UUID newTypeId = UUID.randomUUID();
            Model model = createModel(modelId, "name", oldCategoryId, UUID.randomUUID(), "rg");
            Category newCategory = new Category(newCategoryId, "CV", "desc", false);

            when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
            when(categoryRepository.findByIdWithTypes(newCategoryId)).thenReturn(Optional.of(newCategory));

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> service.validateModelModification(modelId, newCategoryId, newTypeId));

            assertEquals(ErrorCode.MODEL_TYPE_NOT_BELONG_TO_CATEGORY, ex.getCode());
        }

        @Test
        @DisplayName("should throw MODEL_NAME_EXISTS when name already used in new category+type")
        void should_throw_whenNameExistsInNewCombo() {
            UUID modelId = UUID.randomUUID();
            UUID oldCategoryId = UUID.randomUUID();
            UUID newCategoryId = UUID.randomUUID();
            UUID newTypeId = UUID.randomUUID();
            Model model = createModel(modelId, "dup-name", oldCategoryId, UUID.randomUUID(), "rg");
            Category newCategory = createCategoryWithModelType(newCategoryId, newTypeId);

            when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
            when(categoryRepository.findByIdWithTypes(newCategoryId)).thenReturn(Optional.of(newCategory));
            when(modelRepository.existsByCategoryAndTypeAndName(newCategoryId, newTypeId, "dup-name")).thenReturn(true);

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> service.validateModelModification(modelId, newCategoryId, newTypeId));

            assertEquals(ErrorCode.MODEL_NAME_EXISTS, ex.getCode());
        }

        @Test
        @DisplayName("should only validate categoryId change when typeId stays same but category changes")
        void should_validate_whenOnlyCategoryChanges() {
            UUID modelId = UUID.randomUUID();
            UUID oldCategoryId = UUID.randomUUID();
            UUID newCategoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();
            Model model = createModel(modelId, "name", oldCategoryId, typeId, "rg");
            Category newCategory = createCategoryWithModelType(newCategoryId, typeId);

            when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
            when(categoryRepository.findByIdWithTypes(newCategoryId)).thenReturn(Optional.of(newCategory));
            when(modelRepository.existsByCategoryAndTypeAndName(newCategoryId, typeId, "name")).thenReturn(false);

            assertDoesNotThrow(() ->
                    service.validateModelModification(modelId, newCategoryId, typeId));
        }

        @Test
        @DisplayName("should validate when only typeId changes within same category")
        void should_validate_whenOnlyTypeChanges() {
            UUID modelId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID oldTypeId = UUID.randomUUID();
            UUID newTypeId = UUID.randomUUID();
            Model model = createModel(modelId, "name", categoryId, oldTypeId, "rg");
            Category category = createCategoryWithModelType(categoryId, newTypeId);

            when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
            when(categoryRepository.findByIdWithTypes(categoryId)).thenReturn(Optional.of(category));
            when(modelRepository.existsByCategoryAndTypeAndName(categoryId, newTypeId, "name")).thenReturn(false);

            assertDoesNotThrow(() ->
                    service.validateModelModification(modelId, categoryId, newTypeId));
        }
    }

    // ========== Helper methods ==========

    private Category createCategoryWithModelType(UUID categoryId, UUID typeId) {
        Category category = new Category(categoryId, "NLP", "desc", false);
        try {
            java.lang.reflect.Field field = Category.class.getDeclaredField("modelTypes");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ModelType> types = (List<ModelType>) field.get(category);
            types.add(new ModelType(typeId, "LLM", "desc", false));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return category;
    }

    private Model createModel(UUID modelId, String name, UUID categoryId, UUID typeId, String resourceGroup) {
        try {
            java.lang.reflect.Constructor<Model> ctor = Model.class.getDeclaredConstructor(
                    UUID.class, String.class, String.class, UUID.class, UUID.class, String.class,
                    String.class, String.class, String.class, String.class, Long.class,
                    List.class, List.class);
            ctor.setAccessible(true);
            return ctor.newInstance(modelId, name, "desc", categoryId, typeId, resourceGroup,
                    "user", "author", "series", "size", 4096L,
                    new ArrayList<>(), new ArrayList<>());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
