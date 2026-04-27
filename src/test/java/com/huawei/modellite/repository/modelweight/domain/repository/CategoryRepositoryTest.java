package com.huawei.modellite.repository.modelweight.domain.repository;

import com.huawei.modellite.repository.integration.AbstractIntegrationTest;
import com.huawei.modellite.repository.modelweight.domain.aggregate.category.Category;
import com.huawei.modellite.repository.modelweight.domain.aggregate.category.ModelType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Sql(scripts = "/sql/cleanup-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CategoryRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveCategoryWithModelTypes() {
        // given
        Category category = new Category("LLM", "Large Language Models", false);
        category.addModelType("GPT", "Generative Pre-trained Transformer");
        category.addModelType("BERT", "Bidirectional Encoder Representations");

        // when
        categoryRepository.save(category);

        // then
        Optional<Category> found = categoryRepository.findByIdWithTypes(category.getCategoryId());
        assertThat(found).isPresent();
        Category loaded = found.get();
        assertThat(loaded.getName()).isEqualTo("LLM");
        assertThat(loaded.getModelTypes()).hasSize(2);
        assertThat(loaded.getModelTypes().stream().map(ModelType::getName))
                .containsExactlyInAnyOrder("GPT", "BERT");
    }

    @Test
    void shouldFindByIdWithoutTypes() {
        // given
        Category category = new Category("CV", "Computer Vision", false);
        category.addModelType("ResNet", "Residual Network");
        categoryRepository.save(category);

        // when
        Optional<Category> found = categoryRepository.findById(category.getCategoryId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("CV");
        assertThat(found.get().getModelTypes()).isEmpty();
    }

    @Test
    void shouldFindByIdWithTypes() {
        // given
        Category category = new Category("NLP", "Natural Language Processing", false);
        category.addModelType("Transformer", "Attention-based model");
        categoryRepository.save(category);

        // when
        Optional<Category> found = categoryRepository.findByIdWithTypes(category.getCategoryId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getModelTypes()).hasSize(1);
        assertThat(found.get().getModelTypes().get(0).getName()).isEqualTo("Transformer");
    }

    @Test
    void shouldReturnEmptyWhenCategoryNotFound() {
        // when
        Optional<Category> found = categoryRepository.findById(UUID.randomUUID());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindAllWithoutTypes() {
        // given
        categoryRepository.save(new Category("Audio", "Audio Models", false));
        categoryRepository.save(new Category("Video", "Video Models", false));

        // when
        List<Category> categories = categoryRepository.findAll();

        // then
        assertThat(categories).hasSize(2);
        assertThat(categories.stream().map(Category::getName))
                .containsExactlyInAnyOrder("Audio", "Video");
        categories.forEach(c -> assertThat(c.getModelTypes()).isEmpty());
    }

    @Test
    void shouldFindAllWithTypes() {
        // given
        Category cat1 = new Category("RL", "Reinforcement Learning", false);
        cat1.addModelType("DQN", "Deep Q-Network");
        categoryRepository.save(cat1);

        Category cat2 = new Category("SL", "Supervised Learning", false);
        categoryRepository.save(cat2);

        // when
        List<Category> categories = categoryRepository.findAllWithTypes();

        // then
        assertThat(categories).hasSize(2);
        Category loadedRl = categories.stream()
                .filter(c -> c.getName().equals("RL"))
                .findFirst()
                .orElseThrow();
        assertThat(loadedRl.getModelTypes()).hasSize(1);

        Category loadedSl = categories.stream()
                .filter(c -> c.getName().equals("SL"))
                .findFirst()
                .orElseThrow();
        assertThat(loadedSl.getModelTypes()).isEmpty();
    }

    @Test
    void shouldCheckExistsByName() {
        // given
        categoryRepository.save(new Category("Exists", "Test", false));

        // then
        assertThat(categoryRepository.existsByName("Exists")).isTrue();
        assertThat(categoryRepository.existsByName("NotExists")).isFalse();
    }

    @Test
    void shouldCheckHasModelReference() {
        // given
        Category category = new Category("WithModel", "Test", false);
        category.addModelType("Type1", "Desc");
        categoryRepository.save(category);

        UUID typeId = category.getModelTypes().get(0).getTypeId();

        // Insert a model referencing this category
        jdbcTemplate.update(
            "INSERT INTO model (id, name, description, category_id, type_id, resource_group, create_user, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            UUID.randomUUID(), "TestModel", "", category.getCategoryId(), typeId, "default", "user", false
        );

        // then
        assertThat(categoryRepository.hasModelReference(category.getCategoryId())).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNoModelReference() {
        // given
        Category category = new Category("NoModel", "Test", false);
        categoryRepository.save(category);

        // then
        assertThat(categoryRepository.hasModelReference(category.getCategoryId())).isFalse();
    }

    @Test
    void shouldCheckHasModelReferenceByTypeId() {
        // given
        Category category = new Category("TypeRef", "Test", false);
        category.addModelType("Type1", "Desc");
        categoryRepository.save(category);

        UUID typeId = category.getModelTypes().get(0).getTypeId();

        // Insert a model referencing this type
        jdbcTemplate.update(
            "INSERT INTO model (id, name, description, category_id, type_id, resource_group, create_user, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            UUID.randomUUID(), "TestModel", "", category.getCategoryId(), typeId, "default", "user", false
        );

        // then
        assertThat(categoryRepository.hasModelReferenceByTypeId(typeId)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNoModelReferenceByTypeId() {
        // given
        Category category = new Category("NoTypeRef", "Test", false);
        category.addModelType("Type1", "Desc");
        categoryRepository.save(category);

        UUID typeId = category.getModelTypes().get(0).getTypeId();

        // then - no model inserted
        assertThat(categoryRepository.hasModelReferenceByTypeId(typeId)).isFalse();
    }

    @Test
    void shouldDeleteCategoryAndCascadeModelTypes() {
        // given
        Category category = new Category("ToDelete", "Test", false);
        category.addModelType("Type1", "Desc");
        categoryRepository.save(category);
        UUID categoryId = category.getCategoryId();

        // when
        categoryRepository.deleteById(categoryId);

        // then
        assertThat(categoryRepository.findById(categoryId)).isEmpty();
        // Verify model_types are also deleted
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM model_type WHERE category_id = ?",
            Integer.class, categoryId
        );
        assertThat(count).isZero();
    }

    @Test
    void shouldDeleteModelTypeById() {
        // given
        Category category = new Category("DeleteType", "Test", false);
        category.addModelType("TypeToDelete", "Desc");
        category.addModelType("TypeToKeep", "Desc");
        categoryRepository.save(category);

        UUID typeIdToDelete = category.getModelTypes().stream()
                .filter(mt -> mt.getName().equals("TypeToDelete"))
                .findFirst()
                .map(ModelType::getTypeId)
                .orElseThrow();

        // when
        categoryRepository.deleteModelTypeById(typeIdToDelete);

        // then
        Optional<Category> found = categoryRepository.findByIdWithTypes(category.getCategoryId());
        assertThat(found).isPresent();
        assertThat(found.get().getModelTypes()).hasSize(1);
        assertThat(found.get().getModelTypes().get(0).getName()).isEqualTo("TypeToKeep");
    }
}
