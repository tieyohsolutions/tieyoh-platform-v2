package com.tieyoh.api.actuator;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.info.GitProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REQ-001: even if a git.properties carrying every field the git plugin can
 * produce ended up on the classpath, the contributor only reports short
 * commit id, commit time and a boolean dirty flag. Synthetic data only.
 */
class MinimalGitInfoContributorTests {

	@Test
	void reportsOnlyApprovedFieldsFromAFullGitProperties() {
		Properties source = new Properties();
		source.setProperty("branch", "feature/REQ-999-secret-work");
		source.setProperty("commit.id", "0123456789abcdef0123456789abcdef01234567");
		source.setProperty("commit.id.abbrev", "0123456");
		source.setProperty("commit.id.describe", "v1-3-g0123456-dirty");
		source.setProperty("commit.time", "2026-10-14T12:34:56Z");
		source.setProperty("commit.message.full", "Some message");
		source.setProperty("commit.user.name", "Jane Example");
		source.setProperty("commit.user.email", "jane@example.invalid");
		source.setProperty("commit.author.time", "2026-10-14T12:34:56Z");
		source.setProperty("build.user.name", "builder");
		source.setProperty("build.user.email", "builder@example.invalid");
		source.setProperty("build.host", "build-host.example.invalid");
		source.setProperty("remote.origin.url", "git@example.invalid:org/repo.git");
		source.setProperty("tags", "v1");
		source.setProperty("dirty", "true");

		Map<String, Object> git = contribute(source);

		assertThat(git.keySet()).containsExactlyInAnyOrder("commit", "dirty");
		assertThat(git.get("dirty")).isEqualTo(Boolean.TRUE);
		@SuppressWarnings("unchecked")
		Map<String, Object> commit = (Map<String, Object>) git.get("commit");
		assertThat(commit.keySet()).containsExactlyInAnyOrder("id", "time");
		assertThat(commit.get("id")).isEqualTo("0123456");
		assertThat(commit.get("time").toString()).isEqualTo("2026-10-14T12:34:56Z");
	}

	@Test
	void dirtyFalseIsReportedAsBooleanFalse() {
		Properties source = new Properties();
		source.setProperty("commit.id.abbrev", "abcdef0");
		source.setProperty("commit.time", "2026-10-14T12:34:56Z");
		source.setProperty("dirty", "false");

		assertThat(contribute(source).get("dirty")).isEqualTo(Boolean.FALSE);
	}

	@Test
	void dirtyIsOmittedWhenNotKnown() {
		Properties source = new Properties();
		source.setProperty("commit.id.abbrev", "abcdef0");
		source.setProperty("commit.time", "2026-10-14T12:34:56Z");

		assertThat(contribute(source)).doesNotContainKey("dirty");
	}

	@Test
	void gitSectionIsOmittedWhenGitPropertiesIsEmpty() {
		// What the git plugin writes when building without a .git directory.
		Info.Builder builder = new Info.Builder();
		new BuildInfoConfiguration.MinimalGitInfoContributor(new GitProperties(new Properties())).contribute(builder);

		assertThat(builder.build().getDetails()).doesNotContainKey("git");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> contribute(Properties source) {
		Info.Builder builder = new Info.Builder();
		new BuildInfoConfiguration.MinimalGitInfoContributor(new GitProperties(source)).contribute(builder);
		return (Map<String, Object>) builder.build().getDetails().get("git");
	}

}
