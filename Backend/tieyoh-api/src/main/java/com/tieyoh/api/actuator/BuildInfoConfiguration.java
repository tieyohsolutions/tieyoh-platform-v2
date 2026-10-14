package com.tieyoh.api.actuator;

import java.util.Map;

import org.springframework.boot.actuate.info.GitInfoContributor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoPropertiesInfoContributor.Mode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * REQ-001: git section of {@code /actuator/info}.
 *
 * <p>Replaces Spring Boot's auto-configured {@link GitInfoContributor} (which
 * backs off when a bean of this type exists) so that:
 * <ul>
 *   <li>the mode is pinned to {@link Mode#SIMPLE} in code, so setting
 *       {@code management.info.git.mode=full} can't widen the output;</li>
 *   <li>the approved {@code dirty} flag is reported as a JSON boolean
 *       (SIMPLE mode alone only reports branch, commit id and commit time).</li>
 * </ul>
 * The build only writes {@code git.commit.id.abbrev}, {@code git.commit.time}
 * and {@code git.dirty} into git.properties (see pom.xml), so no branch, author,
 * remote or message data is available here in the first place.
 *
 * <p>When the artifact was built without git metadata, git.properties is
 * missing (bean not created) or, defensively, empty (contributor adds
 * nothing), so the git section is omitted.
 */
@Configuration(proxyBeanMethods = false)
public class BuildInfoConfiguration {

	@Bean
	@ConditionalOnResource(resources = "${spring.info.git.location:classpath:git.properties}")
	GitInfoContributor gitInfoContributor(GitProperties gitProperties) {
		return new MinimalGitInfoContributor(gitProperties);
	}

	static final class MinimalGitInfoContributor extends GitInfoContributor {

		MinimalGitInfoContributor(GitProperties properties) {
			super(properties, Mode.SIMPLE);
		}

		@Override
		public void contribute(Info.Builder builder) {
			// Without git metadata the build writes no git.properties at all
			// (see pom.xml). Defensively, an empty or partial git.properties
			// also omits the git section rather than reporting empty/null
			// values.
			if (getProperties().getShortCommitId() == null) {
				return;
			}
			super.contribute(builder);
		}

		@Override
		protected void postProcessContent(Map<String, Object> content) {
			super.postProcessContent(content);
			// Branch is never generated at build time; drop it defensively in
			// case a git.properties with a branch ever ends up on the classpath.
			content.remove("branch");
			String dirty = getProperties().get("dirty");
			if (dirty != null) {
				content.put("dirty", Boolean.parseBoolean(dirty));
			}
		}

	}

}
