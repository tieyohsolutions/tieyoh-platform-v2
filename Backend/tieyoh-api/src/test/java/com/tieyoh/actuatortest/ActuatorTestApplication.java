package com.tieyoh.actuatortest;

import com.tieyoh.api.actuator.BuildInfoConfiguration;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Minimal application for the REQ-001 actuator tests: the real
 * application.properties, real actuator/web auto-configuration and the real
 * {@link BuildInfoConfiguration}, but no DataSource, JPA or Flyway, so these
 * tests never connect to (let alone write to) any database.
 *
 * <p>Deliberately lives outside {@code com.tieyoh.api} so the main
 * application's component scan never picks it up in other tests.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(excludeName = {
		"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
		"org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
		"org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
		"org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration" })
@Import(BuildInfoConfiguration.class)
public class ActuatorTestApplication {

	/**
	 * Belt and braces on top of the exclusions above: if anything still tried
	 * to open a connection it would hit an unresolvable host, never the dev
	 * database on localhost:5432.
	 */
	public static final String NO_DEV_DATABASE_URL = "spring.datasource.url=jdbc:postgresql://db.invalid:1/none";

	public static final String NO_FLYWAY = "spring.flyway.enabled=false";

}
