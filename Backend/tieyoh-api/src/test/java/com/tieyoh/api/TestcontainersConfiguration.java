package com.tieyoh.api;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

/**
 * DEF-001: tests that need the real database schema get a throwaway
 * PostgreSQL 16 container (same major version as the dev database) instead
 * of the dev database. {@link ServiceConnection} points the DataSource and
 * Flyway at the container, overriding spring.datasource.* entirely. The
 * container is removed when the test JVM exits. Requires a running Docker
 * daemon.
 *
 * <p>Tests importing this must also add the test-only seed location, see
 * {@link #FLYWAY_LOCATIONS}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	/**
	 * Real migrations plus the test-only synthetic seed in
	 * src/test/resources/db/testdata (needed by V28-V30).
	 */
	public static final String FLYWAY_LOCATIONS = "spring.flyway.locations=classpath:db/migration,classpath:db/testdata";

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
	}

}
