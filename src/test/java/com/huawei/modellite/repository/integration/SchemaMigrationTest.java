package com.huawei.modellite.repository.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaMigrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("should create all 10 core tables")
    void should_createAllCoreTables() {
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename",
            String.class
        );

        assertTrue(tables.contains("category"), "Missing table: category");
        assertTrue(tables.contains("model_type"), "Missing table: model_type");
        assertTrue(tables.contains("tag"), "Missing table: tag");
        assertTrue(tables.contains("model"), "Missing table: model");
        assertTrue(tables.contains("model_version"), "Missing table: model_version");
        assertTrue(tables.contains("model_tag"), "Missing table: model_tag");
        assertTrue(tables.contains("model_type_tag"), "Missing table: model_type_tag");
        assertTrue(tables.contains("version_lock"), "Missing table: version_lock");
        assertTrue(tables.contains("upload_task"), "Missing table: upload_task");
        assertTrue(tables.contains("convert_task"), "Missing table: convert_task");
    }

    @Test
    @DisplayName("should have flyway_schema_history table")
    void should_haveFlywayHistory() {
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename = 'flyway_schema_history'",
            String.class
        );
        assertFalse(tables.isEmpty(), "Missing flyway_schema_history table");
    }
}
