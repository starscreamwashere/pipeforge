package com.pipeforge;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Flyway migrations actually run against a real PostgreSQL
 * (Phase 1.2 — "migrations run"). Shares the Testcontainers context with
 * {@link PipeforgeApplicationTests}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DatabaseMigrationTests {

    @Autowired
    Flyway flyway;

    @Test
    void baselineMigrationIsApplied() {
        var appliedVersions = Arrays.stream(flyway.info().applied())
                .map(info -> info.getVersion() == null ? null : info.getVersion().getVersion())
                .toList();

        assertThat(appliedVersions).contains("1");
    }
}
