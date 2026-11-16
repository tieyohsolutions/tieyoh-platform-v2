# DEF-001 — TieyohApiApplicationTests.contextLoads connects to the dev database and runs Flyway

| Field | Value |
|---|---|
| Status | Closed — verified |
| Jira type | Bug |
| Severity | Medium |
| Linked requirement | REQ-001 (AC-18 context; pre-existing, not caused by REQ-001) |
| Branch / commit tested | feature/REQ-001-backend-build-info @ c0878e3 |
| Found by | Missouri (QA) |
| Created | 2026-10-14 |

## Summary
The pre-existing `@SpringBootTest` `contextLoads` test uses the default `application.properties`, which points at the dev database (`localhost:5432/tieyoh`) and has Flyway enabled, so `./mvnw test` connects to and migrates the dev database (violates the project rule "tests never write to the dev database").

## Steps to reproduce
1. Read `Backend/tieyoh-api/src/test/java/com/tieyoh/api/TieyohApiApplicationTests.java`: plain `@SpringBootTest`, no profile, no Testcontainers.
2. Read `src/main/resources/application.properties`: `spring.datasource.url=jdbc:postgresql://localhost:5432/tieyoh`, `spring.flyway.enabled=true`.
3. Running `./mvnw test` therefore starts the full context against the dev DB. (Not executed by QA, to protect the dev database; confirmed by static inspection.)

## Expected result
Tests run against a throwaway database (Testcontainers Postgres 16 or a `tieyoh_test` DB under a `test` profile) and never touch the dev database.

## Actual result
Full `./mvnw test` hits the dev database and runs Flyway migrations on it.

## Evidence
```
TieyohApiApplicationTests.java: @SpringBootTest class with empty contextLoads()
application.properties: spring.datasource.url=jdbc:postgresql://localhost:5432/tieyoh ; spring.flyway.enabled=true
no src/test/resources/, no test profile, no Testcontainers dependency
```
Consequence for REQ-001: AC-18 ("when `./mvnw test` executes ... must not write to the dev database") is met by the new actuator tests themselves (verified, no DB), but the suite as a whole still is not safe to run with `./mvnw test`.

## Developer fix notes
_Mississippi, 2026-10-14, commit 1aba418 on feature/REQ-001-backend-build-info_

- **Root cause:** `TieyohApiApplicationTests` was a plain `@SpringBootTest` with no test datasource, so it used `spring.datasource.url=jdbc:postgresql://localhost:5432/tieyoh` from `application.properties` and ran Flyway on the dev DB.
- **Change:**
  - `TestcontainersConfiguration` (new, test-only): a throwaway `postgres:16` container exposed via `@ServiceConnection`, which overrides `spring.datasource.*` for DataSource and Flyway. `TieyohApiApplicationTests` imports it. Dependencies `spring-boot-testcontainers` and `testcontainers-postgresql` are test scope, with versions from the Spring Boot 4.1.0 parent (Testcontainers 2.0.5). **Requires a running Docker daemon for `./mvnw test`.** Without Docker this test fails; it never falls back to the dev DB.
  - V28-V30 need exactly one merchant named 'The Wolf Cafe'. A test-only Flyway location `src/test/resources/db/testdata/V27_1__test_seed_synthetic_wolf_cafe.sql` inserts one synthetic placeholder row (name only, made-up tmid/source). Tests enable it through `spring.flyway.locations`. It isn't on the application classpath, so it never runs on real databases. No existing migration was modified, and nothing comes from `Backend/merchant-imports/`.
  - Guard: `src/test/resources/config/application.properties` (test classpath only, higher precedence than `classpath:/application.properties`) sets the default test datasource URL to an unresolvable `.invalid` host. A test that starts a DataSource without the container fails with UnknownHostException and can't reach localhost:5432. I confirmed this with a temporary probe test, which was not committed.
- **Tests added:** `TieyohApiApplicationTests.usesThrowawayContainerDatabaseNotDevDatabase` (the JDBC URL is the container's, not the dev DB), `allMigrationsAppliedOnTestDatabase` (no pending migrations, seed row present), and `TestDatabaseGuardTests` (the effective default test datasource URL is never the dev DB).
- **Verified:** full `./mvnw clean package` passes (45 tests, 0 failures) in a fresh clone. A read-only query of the dev DB `flyway_schema_history` afterwards still shows 30 rows, last installed 2026-09-25.

## Verification
_Missouri, 2026-10-14, re-test of 1aba418/8e39bf5 in a separate scratch clone (not the dev target dir)._

Result: **PASS - Closed.**

- Full `./mvnw -B clean package` with Docker: BUILD SUCCESS, 45 tests, 0 failures (TieyohApiApplicationTests 3, TestDatabaseGuardTests 1, actuator tests 41).
- Dev DB read-only snapshot (`docker exec tieyoh-postgres psql ... select`) before and after the full run (and after the no-Docker runs): `flyway_schema_history` 30 rows, max installed_on `2026-09-25 10:15:00.558144`, max rank 30; `pg_stat_database.tup_inserted` 106 and `tup_updated` 15 unchanged; active sessions 11 unchanged. No Flyway activity on the dev DB.
- Test log shows Testcontainers starting `postgres:16` (JDBC URL `localhost:64865/test`) and Flyway applying 31 migrations (30 + test seed) to that container, never localhost:5432.
- Seed migration `db/testdata/V27_1__...sql` is under `src/test/resources`; `unzip -l` on the packaged jar shows no `testdata`/`V27_1` entries (only the 30 real `db/migration` files). `application.properties` sets no `spring.flyway.locations`, so the main app uses only `classpath:db/migration`.
- Without Docker: ran the test with the Docker socket blocked (macOS sandbox-exec deny on the docker unix sockets). Result: `Could not find a valid Docker environment`, 3 errors, build FAILED (rc=1). No fallback to localhost:5432; dev DB counters unchanged.
- Note: `DOCKER_HOST=bad` alone does not simulate this, because Testcontainers falls back to /var/run/docker.sock; that is Testcontainers behaviour, not a fallback to the dev DB.
