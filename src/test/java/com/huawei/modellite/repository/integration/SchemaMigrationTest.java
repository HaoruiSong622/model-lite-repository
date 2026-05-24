package com.huawei.modellite.repository.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@Import(K8sJobServiceTestConfig.class)
class SchemaMigrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("should create all 10 core tables")
    void should_createAllCoreTables() {
        List<String> allTables = jdbcTemplate.queryForList(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'public'",
            String.class
        );

        String[] expectedTables = {
            "CATEGORY", "MODEL_TYPE", "TAG", "MODEL", "MODEL_VERSION",
            "MODEL_TAG", "MODEL_TYPE_TAG", "VERSION_LOCK", "UPLOAD_TASK", "CONVERT_TASK"
        };

        for (String expected : expectedTables) {
            boolean found = allTables.stream()
                .anyMatch(t -> t.equalsIgnoreCase(expected));
            assertTrue(found, "Missing table: " + expected);
        }
    }
}
