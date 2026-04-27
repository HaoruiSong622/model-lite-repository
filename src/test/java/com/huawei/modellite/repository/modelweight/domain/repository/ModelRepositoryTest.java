package com.huawei.modellite.repository.modelweight.domain.repository;

import com.huawei.modellite.repository.integration.AbstractIntegrationTest;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.Model;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.ModelVersion;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.StoragePath;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.TrainingMetadata;
import com.huawei.modellite.repository.modelweight.domain.vo.ModelQueryCondition;
import com.huawei.modellite.repository.modelweight.domain.vo.PageResult;

import org.junit.jupiter.api.BeforeEach;
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
class ModelRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID categoryId;
    private UUID typeId;
    private UUID tagId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        typeId = UUID.randomUUID();
        tagId = UUID.randomUUID();

        jdbcTemplate.update(
            "INSERT INTO category (id, name, description, is_builtin) VALUES (?, ?, ?, ?)",
            categoryId, "TestCategory", "Test", false
        );
        jdbcTemplate.update(
            "INSERT INTO model_type (id, category_id, name, description, is_builtin) VALUES (?, ?, ?, ?, ?)",
            typeId, categoryId, "TestType", "Test", false
        );
        jdbcTemplate.update(
            "INSERT INTO tag (id, name, tag_type, is_builtin) VALUES (?, ?, ?, ?)",
            tagId, "TestTag", "custom", false
        );
    }

    @Test
    void shouldSaveModel() {
        // given
        Model model = Model.createModel("TestModel", "Desc", categoryId, typeId, "default", "user", "author", "series", null, 2048L);

        // when
        modelRepository.save(model);

        // then
        Optional<Model> found = modelRepository.findById(model.getModelId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("TestModel");
    }

    @Test
    void shouldFindById() {
        // given
        Model model = Model.createModel("FindById", "Desc", categoryId, typeId, "default", "user", "author", "series", null, 2048L);
        modelRepository.save(model);

        // when
        Optional<Model> found = modelRepository.findById(model.getModelId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("FindById");
        assertThat(found.get().getVersions()).isEmpty();
    }

    @Test
    void shouldFindByIdWithVersions() {
        // given
        Model model = Model.createModel("FindWithVersions", "Desc", categoryId, typeId, "default", "user", "author", "series", null, 2048L);
        modelRepository.save(model);

        // when
        Optional<Model> found = modelRepository.findByIdWithVersions(model.getModelId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getVersions()).hasSize(1);
        assertThat(found.get().getVersions().get(0).getVersionNumber()).isEqualTo(1);
    }

    @Test
    void shouldFindVersionById() {
        // given
        Model model = Model.createModel("VersionModel", "Desc", categoryId, typeId, "default", "user", "author", "series", null, 2048L);
        modelRepository.save(model);
        UUID versionId = model.getVersions().get(0).getVersionId();

        // when
        Optional<ModelVersion> found = modelRepository.findVersionById(model.getModelId(), versionId);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getVersionNumber()).isEqualTo(1);
    }

    @Test
    void shouldFindByCondition() {
        // given
        Model model1 = Model.createModel("AlphaModel", "Desc1", categoryId, typeId, "rg1", "user", "author", "series", null, 2048L);
        modelRepository.save(model1);

        Model model2 = Model.createModel("BetaModel", "Desc2", categoryId, typeId, "rg2", "user", "author", "series", null, 2048L);
        modelRepository.save(model2);

        ModelQueryCondition condition = new ModelQueryCondition();
        condition.setKeyword("Alpha");
        condition.setPage(1);
        condition.setPageSize(10);

        // when
        PageResult<Model> result = modelRepository.findByCondition(condition);

        // then
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getName()).isEqualTo("AlphaModel");
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
    }

    @Test
    void shouldFindByResourceGroups() {
        // given
        Model model1 = Model.createModel("ModelRG1", "Desc", categoryId, typeId, "rg1", "user", "author", "series", null, 2048L);
        modelRepository.save(model1);

        Model model2 = Model.createModel("ModelRG2", "Desc", categoryId, typeId, "rg2", "user", "author", "series", null, 2048L);
        modelRepository.save(model2);

        ModelQueryCondition condition = new ModelQueryCondition();
        condition.setPage(1);
        condition.setPageSize(10);

        // when
        PageResult<Model> result = modelRepository.findByResourceGroups(List.of("rg1"), condition);

        // then
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getName()).isEqualTo("ModelRG1");
    }

    @Test
    void shouldCheckExistsByCategoryAndTypeAndName() {
        // given
        Model model = Model.createModel("ExistsModel", "Desc", categoryId, typeId, "default", "user", "author", "series", null, 2048L);
        modelRepository.save(model);

        // then
        assertThat(modelRepository.existsByCategoryAndTypeAndName(categoryId, typeId, "ExistsModel")).isTrue();
        assertThat(modelRepository.existsByCategoryAndTypeAndName(categoryId, typeId, "NotExists")).isFalse();
    }

    @Test
    void shouldCountByResourceGroup() {
        // given
        Model model = Model.createModel("CountModel", "Desc", categoryId, typeId, "rg-special", "user", "author", "series", null, 2048L);
        modelRepository.save(model);

        // then
        assertThat(modelRepository.countByResourceGroup("rg-special")).isEqualTo(1);
        assertThat(modelRepository.countByResourceGroup("nonexistent")).isEqualTo(0);
    }

    @Test
    void shouldCountAll() {
        // given
        long before = modelRepository.countAll();
        Model model = Model.createModel("CountAllModel", "Desc", categoryId, typeId, "default", "user", "author", "series", null, 2048L);
        modelRepository.save(model);

        // then
        assertThat(modelRepository.countAll()).isEqualTo(before + 1);
    }

    @Test
    void shouldUpdateModel() {
        // given
        Model model = Model.createModel("UpdateModel", "Desc", categoryId, typeId, "default", "user", "author", "series", null, 2048L);
        modelRepository.save(model);

        model.modifyMetadata("NewDesc", "NewAuthor", "NewSeries", null, 4096L);
        modelRepository.update(model);

        // then
        Optional<Model> found = modelRepository.findById(model.getModelId());
        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("NewDesc");
        assertThat(found.get().getAuthor()).isEqualTo("NewAuthor");
        assertThat(found.get().getSeriesName()).isEqualTo("NewSeries");
        assertThat(found.get().getMaxSeqLength()).isEqualTo(4096L);
    }

    @Test
    void shouldUpdateVersion() {
        // given
        Model model = Model.createModel("UpdateVersionModel", "Desc", categoryId, typeId, "default", "user", "author", "series", null, 2048L);
        modelRepository.save(model);
        ModelVersion version = model.getVersions().get(0);

        ModelVersion updatedVersion = new ModelVersion(
            version.getVersionId(),
            version.getVersionNumber(),
            StoragePath.ofPvc("new-pvc"),
            "new-type",
            version.getStatus(),
            true,
            false,
            new TrainingMetadata("frame", "type", "strategy", 100L, "0.01", "v1")
        );

        // when
        modelRepository.updateVersion(updatedVersion);

        // then
        Optional<ModelVersion> found = modelRepository.findVersionById(model.getModelId(), version.getVersionId());
        assertThat(found).isPresent();
        assertThat(found.get().getWeightType()).isEqualTo("new-type");
        assertThat(found.get().getIsRegistered()).isTrue();
    }

    @Test
    void shouldFindTagIdsByModelId() {
        // given
        Model model = Model.createModel("TagModel", "Desc", categoryId, typeId, "default", "user", "author", "series", null, 2048L);
        model.setTagIds(List.of(tagId));
        modelRepository.save(model);

        // when
        List<UUID> tagIds = modelRepository.findTagIdsByModelId(model.getModelId());

        // then
        assertThat(tagIds).containsExactly(tagId);
    }

    @Test
    void shouldReturnEmptyWhenModelNotFound() {
        // when
        Optional<Model> found = modelRepository.findById(UUID.randomUUID());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindAll() {
        // given
        modelRepository.save(Model.createModel("All1", "Desc", categoryId, typeId, "default", "user", "author", "series", null, 2048L));
        modelRepository.save(Model.createModel("All2", "Desc", categoryId, typeId, "default", "user", "author", "series", null, 2048L));

        // when
        List<Model> models = modelRepository.findAll();

        // then
        assertThat(models.stream().map(Model::getName)).contains("All1", "All2");
    }
}
