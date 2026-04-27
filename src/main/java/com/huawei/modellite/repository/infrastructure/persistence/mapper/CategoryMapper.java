package com.huawei.modellite.repository.infrastructure.persistence.mapper;

import com.huawei.modellite.repository.modelweight.domain.aggregate.category.Category;
import com.huawei.modellite.repository.modelweight.domain.aggregate.category.ModelType;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface CategoryMapper {

    void insertCategory(Category category);

    void insertModelType(@Param("categoryId") UUID categoryId, @Param("modelType") ModelType modelType);

    Category selectById(UUID categoryId);

    Category selectByIdWithTypes(UUID categoryId);

    List<Category> selectAll();

    List<Category> selectAllWithTypes();

    int countByName(@Param("name") String name);

    int countModelByCategoryId(@Param("categoryId") UUID categoryId);

    int countModelByTypeId(@Param("typeId") UUID typeId);

    void deleteById(UUID categoryId);

    void deleteModelTypesByCategoryId(@Param("categoryId") UUID categoryId);

    void deleteModelTypeById(UUID typeId);
}
