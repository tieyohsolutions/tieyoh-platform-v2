package com.tieyoh.api;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full application context against a throwaway Testcontainers PostgreSQL
 * database, never the dev database (DEF-001).
 */
@SpringBootTest(properties = TestcontainersConfiguration.FLYWAY_LOCATIONS)
@Import(TestcontainersConfiguration.class)
class TieyohApiApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PostgreSQLContainer postgres;

	@Autowired
	private Flyway flyway;

	@Test
	void contextLoads() {
	}

	@Test
	void usesThrowawayContainerDatabaseNotDevDatabase() throws Exception {
		try (var connection = dataSource.getConnection()) {
			String url = connection.getMetaData().getURL();
			assertThat(url).isEqualTo(postgres.getJdbcUrl());
			assertThat(url).doesNotContain("localhost:5432/tieyoh");
		}
	}

	@Test
	void allMigrationsAppliedOnTestDatabase() {
		assertThat(flyway.info().pending()).isEmpty();
		Integer wolfCafeRows = new JdbcTemplate(dataSource)
			.queryForObject("SELECT count(*) FROM merchant WHERE brand_name = 'The Wolf Cafe'", Integer.class);
		assertThat(wolfCafeRows).isEqualTo(1);
	}

}
