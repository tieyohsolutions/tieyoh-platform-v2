# DEF-001 — TieyohApiApplicationTests.contextLoads connects to the dev database and runs Flyway

| Field | Value |
|---|---|
| Status | Open |
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
_(Mississippi appends: root cause, change made, commit hash, test added)_

## Verification
_(Missouri appends: re-test date, result)_
