package com.tieyoh.api;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DEF-001 regression: on the test classpath the default datasource URL (the
 * one any test gets unless a Testcontainers {@code @ServiceConnection}
 * overrides it) must not be the dev database. Loads the real config files
 * the same way the application does, with no beans and no connection.
 */
class TestDatabaseGuardTests {

	@Test
	void defaultDatasourceUrlInTestsIsNeverTheDevDatabase() {
		try (var context = new SpringApplicationBuilder(Empty.class).web(WebApplicationType.NONE)
			.logStartupInfo(false)
			.run()) {
			String url = context.getEnvironment().getProperty("spring.datasource.url");
			assertThat(url).doesNotContain("localhost:5432").doesNotContain("/tieyoh").endsWith(".invalid:1/none");
		}
	}

	/** Plain bean source: no component scan, no auto-configuration. */
	static class Empty {

	}

}
