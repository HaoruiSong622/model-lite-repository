package com.huawei.modellite.repository.integration;

import com.huawei.modellite.repository.common.enums.VersionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

class TypeHandlerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("should verify enum dbValue is stored as VARCHAR in database")
    void should_verifyEnumDbValueStored() {
        jdbcTemplate.execute("INSERT INTO category (name) VALUES ('TestCategory')");

        String categoryId = jdbcTemplate.queryForObject(
            "SELECT id FROM category WHERE name = 'TestCategory'", String.class);
        jdbcTemplate.execute(String.format(
            "INSERT INTO model_type (category_id, name) VALUES ('%s', 'TestType')", categoryId));

        String typeId = jdbcTemplate.queryForObject(
            "SELECT id FROM model_type WHERE name = 'TestType'", String.class);
        jdbcTemplate.execute(String.format(
            "INSERT INTO model (name, category_id, type_id, resource_group, create_user) " +
            "VALUES ('TestModel', '%s', '%s', 'test-rg', 'test-user')", categoryId, typeId));

        String modelId = jdbcTemplate.queryForObject(
            "SELECT id FROM model WHERE name = 'TestModel'", String.class);
        jdbcTemplate.execute(String.format(
            "INSERT INTO model_version (model_id, version_number, status) " +
            "VALUES ('%s', 1, 'Available')", modelId));

        String statusValue = jdbcTemplate.queryForObject(
            "SELECT status FROM model_version WHERE version_number = 1", String.class);
        assertEquals("Available", statusValue);
    }
}
