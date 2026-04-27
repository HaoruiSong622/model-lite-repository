package com.huawei.modellite.repository.modelweight.domain.repository;

import com.huawei.modellite.repository.modelweight.domain.aggregate.category.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Category repository interface - defines persistence operations for Category aggregate root.
 */
public interface CategoryRepository {

    /**
     * Save a category (including cascading save of ModelTypes).
     */
    void save(Category category);

    /**
     * Find category by ID (without ModelType list).
     */
    Optional<Category> findById(UUID categoryId);

    /**
     * Find category by ID (with ModelType list).
     */
    Optional<Category> findByIdWithTypes(UUID categoryId);

    /**
     * Find all categories (without ModelType list).
     */
    List<Category> findAll();

    /**
     * Find all categories (with ModelType list).
     */
    List<Category> findAllWithTypes();

    /**
     * Check if a category with the given name exists.
     */
    boolean existsByName(String name);

    /**
     * Check if there are any models referencing this category.
     */
    boolean hasModelReference(UUID categoryId);

    /**
     * Check if there are any models referencing this model type.
     */
    boolean hasModelReferenceByTypeId(UUID typeId);

    /**
     * Delete category by ID (including cascading delete of ModelTypes).
     */
    void deleteById(UUID categoryId);

    /**
     * Delete a specific model type by its ID.
     */
    void deleteModelTypeById(UUID typeId);
}
