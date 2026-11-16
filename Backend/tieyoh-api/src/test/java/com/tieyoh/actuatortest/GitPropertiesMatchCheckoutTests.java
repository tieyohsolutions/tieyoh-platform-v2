package com.tieyoh.actuatortest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * REQ-001 AC-5 / Decision 4, DEF-002 and DEF-003 regression: the
 * git.properties produced by this build describes the checkout the build ran
 * in, whether that is a normal clone, the main checkout or a linked git
 * worktree. Compared against the git CLI run in the same directory. Skipped
 * when the build had no git metadata (AC-9/AC-10, covered elsewhere) or git
 * is not installed.
 */
class GitPropertiesMatchCheckoutTests {

	private static final Path PROJECT_DIR = Path.of("").toAbsolutePath();

	private Properties gitProperties;

	@BeforeEach
	void loadGitProperties() throws IOException {
		ClassPathResource resource = new ClassPathResource("git.properties");
		assumeTrue(resource.exists(), "built without git metadata");
		gitProperties = new Properties();
		try (var in = resource.getInputStream()) {
			gitProperties.load(in);
		}
		assumeTrue(git("--version") != null, "git CLI not available");
	}

	@Test
	void containsOnlyApprovedKeys() {
		assertThat(gitProperties.stringPropertyNames()).containsExactlyInAnyOrder("git.commit.id.abbrev",
				"git.commit.time", "git.dirty");
	}

	@Test
	void commitIdIsHeadOfThisCheckout() throws Exception {
		assertThat(gitProperties.getProperty("git.commit.id.abbrev")).isEqualTo(git("rev-parse", "--short=7", "HEAD"));
	}

	@Test
	void dirtyMatchesWorkingTreeIncludingUntrackedFiles() throws Exception {
		String status = git("--no-optional-locks", "status", "--porcelain", "--untracked-files=normal");
		assertThat(status).isNotNull();
		assertThat(gitProperties.getProperty("git.dirty")).isEqualTo(String.valueOf(!status.isBlank()));
	}

	/** Output of a git command run in the project directory, or null if it failed. */
	private static String git(String... args) {
		try {
			Process process = new ProcessBuilder(concat(args)).directory(PROJECT_DIR.toFile())
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.start();
			String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
				return null;
			}
			return out;
		}
		catch (IOException ex) {
			return null;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	private static List<String> concat(String... args) {
		return Stream.concat(Stream.of("git"), Stream.of(args)).toList();
	}

}
