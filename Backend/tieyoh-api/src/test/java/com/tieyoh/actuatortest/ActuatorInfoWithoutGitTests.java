package com.tieyoh.actuatortest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-001 AC-10: an artifact built without git metadata (no git.properties)
 * still serves /actuator/info with version and build time, and omits the git
 * section entirely rather than erroring or showing placeholder values.
 */
@SpringBootTest(classes = ActuatorTestApplication.class,
		properties = { ActuatorTestApplication.NO_DEV_DATABASE_URL, ActuatorTestApplication.NO_FLYWAY,
				"spring.info.git.location=classpath:req-001-no-such-git.properties" })
@AutoConfigureMockMvc
class ActuatorInfoWithoutGitTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void infoOmitsGitSectionWhenNoGitMetadata() throws Exception {
		mockMvc.perform(get("/actuator/info"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.git").doesNotExist())
			.andExpect(jsonPath("$.build.version").isNotEmpty())
			.andExpect(jsonPath("$.build.time").isNotEmpty());
	}

}
