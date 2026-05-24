package com.huawei.modellite.repository.integration;

import com.huawei.modellite.repository.common.enums.VersionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@Import(K8sJobServiceTestConfig.class)
@TestPropertySource(properties = {"test.context.isolation=type-handler"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TypeHandlerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM upload_task");
        jdbcTemplate.execute("DELETE FROM model_version");
        jdbcTemplate.execute("DELETE FROM model_tag");
        jdbcTemplate.execute("DELETE FROM model");
        jdbcTemplate.execute("DELETE FROM model_type");
        jdbcTemplate.execute("DELETE FROM category");
    }

    @Test
    @DisplayName("should verify enum dbValue is stored as VARCHAR in database")
    void should_verifyEnumDbValueStored() {
        UUID categoryId = UUID.randomUUID();
        String categoryName = "TestCategory-" + categoryId;
        jdbcTemplate.execute(String.format(
            "INSERT INTO category (id, name) VALUES ('%s', '%s')", categoryId, categoryName));

        UUID typeId = UUID.randomUUID();
        String typeName = "TestType-" + typeId;
        jdbcTemplate.execute(String.format(
            "INSERT INTO model_type (id, category_id, name) VALUES ('%s', '%s', '%s')", typeId, categoryId, typeName));

        UUID modelId = UUID.randomUUID();
        String modelName = "TestModel-" + modelId;
        jdbcTemplate.execute(String.format(
            "INSERT INTO model (id, name, category_id, type_id, resource_group, create_user) " +
            "VALUES ('%s', '%s', '%s', '%s', 'test-rg', 'test-user')", modelId, modelName, categoryId, typeId));

        UUID versionId = UUID.randomUUID();
        jdbcTemplate.execute(String.format(
            "INSERT INTO model_version (id, model_id, version_number, status) " +
            "VALUES ('%s', '%s', 1, 'Available')", versionId, modelId));

        String statusValue = jdbcTemplate.queryForObject(
            "SELECT status FROM model_version WHERE id = ?", String.class, versionId.toString());
        assertEquals("Available", statusValue);
    }
}
