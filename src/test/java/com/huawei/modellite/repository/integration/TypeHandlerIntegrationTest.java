package com.huawei.modellite.repository.integration;

import com.huawei.modellite.repository.common.enums.VersionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TypeHandlerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("should verify enum dbValue is stored as VARCHAR in database")
    void should_verifyEnumDbValueStored() {
        UUID categoryId = UUID.randomUUID();
        jdbcTemplate.execute(String.format(
            "INSERT INTO category (id, name) VALUES ('%s', 'TestCategory')", categoryId));

        UUID typeId = UUID.randomUUID();
        jdbcTemplate.execute(String.format(
            "INSERT INTO model_type (id, category_id, name) VALUES ('%s', '%s', 'TestType')", typeId, categoryId));

        UUID modelId = UUID.randomUUID();
        jdbcTemplate.execute(String.format(
            "INSERT INTO model (id, name, category_id, type_id, resource_group, create_user) " +
            "VALUES ('%s', 'TestModel', '%s', '%s', 'test-rg', 'test-user')", modelId, categoryId, typeId));

        UUID versionId = UUID.randomUUID();
        jdbcTemplate.execute(String.format(
            "INSERT INTO model_version (id, model_id, version_number, status) " +
            "VALUES ('%s', '%s', 1, 'Available')", versionId, modelId));

        String statusValue = jdbcTemplate.queryForObject(
            "SELECT status FROM model_version WHERE version_number = 1", String.class);
        assertEquals("Available", statusValue);
    }
}
