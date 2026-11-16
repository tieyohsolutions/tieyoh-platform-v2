package com.tieyoh.actuatortest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-001: /actuator/info content and actuator exposure, using the real
 * application.properties and the build-info.properties / git.properties
 * produced by this build. No database is involved (see
 * {@link ActuatorTestApplication}).
 */
@SpringBootTest(classes = ActuatorTestApplication.class,
		properties = { ActuatorTestApplication.NO_DEV_DATABASE_URL, ActuatorTestApplication.NO_FLYWAY })
@AutoConfigureMockMvc
class ActuatorEndpointExposureTests {

	private static final Pattern ISO_UTC_INSTANT = Pattern
		.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$");

	private static final Pattern SHORT_COMMIT_ID = Pattern.compile("^[0-9a-f]{7}$");

	/** Words that must never appear anywhere in the /actuator/info body (AC-12, AC-13). */
	private static final Set<String> FORBIDDEN_FRAGMENTS = Set.of("branch", "user", "email", "author",
			"committer", "remote", "origin", "message", "host", "tags", "describe", "closest", "java", "jvm",
			"os", "process", "pid", "env", "datasource", "password", "secret", "wallet", "group", "artifact",
			"path", "jdbc", "localhost");

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private MockMvc mockMvc;

	@Test
	void infoReturnsOnlyApprovedBuildFields() throws Exception {
		Map<String, Object> body = getInfo();

		assertThat(body.keySet()).isSubsetOf("build", "git");
		@SuppressWarnings("unchecked")
		Map<String, Object> build = (Map<String, Object>) body.get("build");
		assertThat(build).isNotNull();
		assertThat(build.keySet()).containsExactlyInAnyOrder("name", "version", "time");
		assertThat(build.get("name")).isEqualTo("tieyoh-api");
		assertThat(build.get("version")).isEqualTo(pomVersion());
		assertThat((String) build.get("time")).matches(ISO_UTC_INSTANT);
	}

	@Test
	void infoGitSectionMatchesBuildMetadata() throws Exception {
		Map<String, Object> body = getInfo();

		if (!gitMetadataWasGenerated()) {
			// Built without a .git directory (AC-9/AC-10): git section omitted.
			assertThat(body).doesNotContainKey("git");
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> git = (Map<String, Object>) body.get("git");
		assertThat(git).isNotNull();
		assertThat(git.keySet()).containsExactlyInAnyOrder("commit", "dirty");
		assertThat(git.get("dirty")).isInstanceOf(Boolean.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> commit = (Map<String, Object>) git.get("commit");
		assertThat(commit.keySet()).containsExactlyInAnyOrder("id", "time");
		assertThat((String) commit.get("id")).matches(SHORT_COMMIT_ID);
		assertThat((String) commit.get("time")).matches(ISO_UTC_INSTANT);
	}

	@Test
	void infoContainsNoSensitiveOrEnvironmentDetails() throws Exception {
		String raw = mockMvc.perform(get("/actuator/info"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			.toLowerCase();

		for (String fragment : FORBIDDEN_FRAGMENTS) {
			assertThat(keysAndValues(raw)).as("forbidden fragment '%s' in %s", fragment, raw)
				.doesNotContain(fragment);
		}
	}

	@Test
	void buildTimeIsFixedAtBuildNotAtStartup() throws Exception {
		Map<String, Object> first = getInfo();
		Thread.sleep(1100);
		Map<String, Object> second = getInfo();
		assertThat(second.get("build")).isEqualTo(first.get("build"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "POST", "PUT", "PATCH", "DELETE" })
	void infoIsReadOnly(String method) throws Exception {
		Map<String, Object> before = getInfo();
		int status = mockMvc.perform(request(HttpMethod.valueOf(method), "/actuator/info"))
			.andReturn()
			.getResponse()
			.getStatus();
		assertThat(status).isBetween(400, 499);
		assertThat(getInfo()).isEqualTo(before);
	}

	@ParameterizedTest
	@ValueSource(strings = { "env", "configprops", "beans", "mappings", "loggers", "heapdump", "threaddump",
			"metrics", "shutdown", "flyway", "conditions", "scheduledtasks", "caches", "sbom", "startup",
			"httpexchanges", "auditevents", "logfile", "prometheus", "quartz", "sessions", "integrationgraph" })
	void otherActuatorEndpointsAreNotExposed(String endpoint) throws Exception {
		mockMvc.perform(get("/actuator/" + endpoint)).andExpect(status().isNotFound());
	}

	@Test
	void shutdownCannotBeTriggered() throws Exception {
		mockMvc.perform(request(HttpMethod.POST, "/actuator/shutdown")).andExpect(status().isNotFound());
	}

	@Test
	void healthIsUnchanged() throws Exception {
		String body = mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		// Defaults untouched (show-details/show-components=never): status and
		// probe group names only, no components or details.
		Map<String, Object> health = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {
		});
		assertThat(health.get("status")).isEqualTo("UP");
		assertThat(health.keySet()).isSubsetOf("status", "groups");
	}

	@Test
	void actuatorRootListsOnlyHealthAndInfo() throws Exception {
		Map<String, Object> body = parse(mockMvc.perform(get("/actuator")).andExpect(status().isOk()).andReturn());
		@SuppressWarnings("unchecked")
		Map<String, Object> links = (Map<String, Object>) body.get("_links");
		assertThat(links.keySet()).allMatch((name) -> name.equals("self") || name.startsWith("health")
				|| name.equals("info"));
	}

	private Map<String, Object> getInfo() throws Exception {
		return parse(mockMvc.perform(get("/actuator/info")).andExpect(status().isOk()).andReturn());
	}

	private Map<String, Object> parse(MvcResult result) throws Exception {
		assertThat(result.getResponse().getContentType()).contains("json");
		return objectMapper.readValue(result.getResponse().getContentAsString(),
				new TypeReference<Map<String, Object>>() {
				});
	}

	/**
	 * Strips timestamps and the commit hash so a hex id or date can't produce
	 * a false match, leaving keys and textual values to scan.
	 */
	private static String keysAndValues(String raw) {
		return raw.replaceAll("\"\\d{4}-\\d{2}-\\d{2}t[0-9:.]+z\"", "\"\"")
			.replaceAll("\"[0-9a-f]{7,40}\"", "\"\"");
	}

	private static boolean gitMetadataWasGenerated() throws Exception {
		ClassPathResource resource = new ClassPathResource("git.properties");
		if (!resource.exists()) {
			return false;
		}
		Properties properties = new Properties();
		try (var in = resource.getInputStream()) {
			properties.load(in);
		}
		return properties.getProperty("git.commit.id.abbrev") != null;
	}

	private static String pomVersion() throws Exception {
		var document = DocumentBuilderFactory.newInstance()
			.newDocumentBuilder()
			.parse(Files.newInputStream(Path.of("pom.xml")));
		var children = document.getDocumentElement().getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i) instanceof Element element && element.getTagName().equals("version")) {
				return element.getTextContent().trim();
			}
		}
		throw new IllegalStateException("No <version> in pom.xml");
	}

}
