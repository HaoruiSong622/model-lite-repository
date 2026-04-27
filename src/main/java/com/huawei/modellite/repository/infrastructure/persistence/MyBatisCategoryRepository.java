package com.huawei.modellite.repository.infrastructure.persistence;

import com.huawei.modellite.repository.modelweight.domain.aggregate.category.Category;
import com.huawei.modellite.repository.modelweight.domain.aggregate.category.ModelType;
import com.huawei.modellite.repository.modelweight.domain.repository.CategoryRepository;
import com.huawei.modellite.repository.infrastructure.persistence.mapper.CategoryMapper;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MyBatisCategoryRepository implements CategoryRepository {

    private final CategoryMapper categoryMapper;

    public MyBatisCategoryRepository(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public void save(Category category) {
        categoryMapper.insertCategory(category);
        for (ModelType modelType : category.getModelTypes()) {
            categoryMapper.insertModelType(category.getCategoryId(), modelType);
        }
    }

    @Override
    public Optional<Category> findById(UUID categoryId) {
        return Optional.ofNullable(categoryMapper.selectById(categoryId));
    }

    @Override
    public Optional<Category> findByIdWithTypes(UUID categoryId) {
        return Optional.ofNullable(categoryMapper.selectByIdWithTypes(categoryId));
    }

    @Override
    public List<Category> findAll() {
        return categoryMapper.selectAll();
    }

    @Override
    public List<Category> findAllWithTypes() {
        return categoryMapper.selectAllWithTypes();
    }

    @Override
    public boolean existsByName(String name) {
        return categoryMapper.countByName(name) > 0;
    }

    @Override
    public boolean hasModelReference(UUID categoryId) {
        return categoryMapper.countModelByCategoryId(categoryId) > 0;
    }

    @Override
    public boolean hasModelReferenceByTypeId(UUID typeId) {
        return categoryMapper.countModelByTypeId(typeId) > 0;
    }

    @Override
    public void deleteById(UUID categoryId) {
        categoryMapper.deleteModelTypesByCategoryId(categoryId);
        categoryMapper.deleteById(categoryId);
    }

    @Override
    public void deleteModelTypeById(UUID typeId) {
        categoryMapper.deleteModelTypeById(typeId);
    }
}
