package com.huawei.modellite.repository.modelweight.application.service;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.modelweight.application.dto.CategoryRequest;
import com.huawei.modellite.repository.modelweight.application.dto.CategoryResponse;
import com.huawei.modellite.repository.modelweight.domain.aggregate.category.Category;
import com.huawei.modellite.repository.modelweight.domain.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryApplicationServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new CategoryApplicationService(categoryRepository);
    }

    @Nested
    @DisplayName("createCategory tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("should create category successfully")
        void should_createCategory_successfully() {
            CategoryRequest request = new CategoryRequest();
            request.setName("NLP");
            request.setDescription("Natural Language Processing");

            when(categoryRepository.existsByName("NLP")).thenReturn(false);

            CategoryResponse response = service.createCategory(request);

            assertNotNull(response);
            assertEquals("NLP", response.getName());
            assertEquals("Natural Language Processing", response.getDescription());
            assertFalse(response.getIsBuiltin());
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("should throw CATEGORY_NAME_EXISTS when name already exists")
        void should_throw_whenNameExists() {
            CategoryRequest request = new CategoryRequest();
            request.setName("NLP");
            request.setDescription("desc");

            when(categoryRepository.existsByName("NLP")).thenReturn(true);

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.createCategory(request));

            assertEquals(ErrorCode.CATEGORY_NAME_EXISTS, exception.getCode());
            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteCategory tests")
    class DeleteCategoryTests {

        @Test
        @DisplayName("should delete category successfully")
        void should_deleteCategory_successfully() {
            UUID categoryId = UUID.randomUUID();
            Category category = new Category(categoryId, "NLP", "desc", false);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(categoryRepository.hasModelReference(categoryId)).thenReturn(false);

            service.deleteCategory(categoryId);

            verify(categoryRepository).deleteById(categoryId);
        }

        @Test
        @DisplayName("should throw CATEGORY_NOT_FOUND when category does not exist")
        void should_throw_whenCategoryNotFound() {
            UUID categoryId = UUID.randomUUID();

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.deleteCategory(categoryId));

            assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.getCode());
        }

        @Test
        @DisplayName("should throw CATEGORY_BUILTIN when deleting built-in category")
        void should_throw_whenBuiltInCategory() {
            UUID categoryId = UUID.randomUUID();
            Category category = new Category(categoryId, "System", "desc", true);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.deleteCategory(categoryId));

            assertEquals(ErrorCode.CATEGORY_BUILTIN, exception.getCode());
            verify(categoryRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("should throw CATEGORY_HAS_MODELS when category has model references")
        void should_throw_whenHasModelReferences() {
            UUID categoryId = UUID.randomUUID();
            Category category = new Category(categoryId, "NLP", "desc", false);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(categoryRepository.hasModelReference(categoryId)).thenReturn(true);

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.deleteCategory(categoryId));

            assertEquals(ErrorCode.CATEGORY_HAS_MODELS, exception.getCode());
            verify(categoryRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("removeModelTypeFromCategory tests")
    class RemoveModelTypeFromCategoryTests {

        @Test
        @DisplayName("should remove model type successfully")
        void should_removeModelType_successfully() {
            UUID categoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();
            Category category = new Category(categoryId, "NLP", "desc", false);
            category.addModelType("LLM", "desc");
            try {
                java.lang.reflect.Field field = Category.class.getDeclaredField("modelTypes");
                field.setAccessible(true);
                java.util.List<com.huawei.modellite.repository.modelweight.domain.aggregate.category.ModelType> types = (java.util.List<com.huawei.modellite.repository.modelweight.domain.aggregate.category.ModelType>) field.get(category);
                types.clear();
                com.huawei.modellite.repository.modelweight.domain.aggregate.category.ModelType modelType = new com.huawei.modellite.repository.modelweight.domain.aggregate.category.ModelType(typeId, "LLM", "desc", false);
                types.add(modelType);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            when(categoryRepository.findByIdWithTypes(categoryId)).thenReturn(Optional.of(category));
            when(categoryRepository.hasModelReferenceByTypeId(typeId)).thenReturn(false);

            service.removeModelTypeFromCategory(categoryId, typeId);

            verify(categoryRepository).save(category);
        }

        @Test
        @DisplayName("should throw CATEGORY_NOT_FOUND when category does not exist")
        void should_throw_whenCategoryNotFound() {
            UUID categoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();

            when(categoryRepository.findByIdWithTypes(categoryId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.removeModelTypeFromCategory(categoryId, typeId));

            assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.getCode());
        }
    }

    @Nested
    @DisplayName("getCategoryById tests")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("should return category successfully")
        void should_returnCategory_successfully() {
            UUID categoryId = UUID.randomUUID();
            Category category = new Category(categoryId, "NLP", "desc", false);

            when(categoryRepository.findByIdWithTypes(categoryId)).thenReturn(Optional.of(category));

            CategoryResponse response = service.getCategoryById(categoryId);

            assertNotNull(response);
            assertEquals("NLP", response.getName());
        }

        @Test
        @DisplayName("should throw CATEGORY_NOT_FOUND when category does not exist")
        void should_throw_whenCategoryNotFound() {
            UUID categoryId = UUID.randomUUID();

            when(categoryRepository.findByIdWithTypes(categoryId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.getCategoryById(categoryId));

            assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.getCode());
        }
    }

    @Nested
    @DisplayName("getAllCategories tests")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("should return all categories")
        void should_returnAllCategories() {
            Category category1 = new Category("NLP", "desc1");
            Category category2 = new Category("CV", "desc2");

            when(categoryRepository.findAllWithTypes()).thenReturn(List.of(category1, category2));

            List<CategoryResponse> responses = service.getAllCategories();

            assertEquals(2, responses.size());
        }

        @Test
        @DisplayName("should return empty list when no categories")
        void should_returnEmptyList() {
            when(categoryRepository.findAllWithTypes()).thenReturn(List.of());

            List<CategoryResponse> responses = service.getAllCategories();

            assertTrue(responses.isEmpty());
        }
    }
}
