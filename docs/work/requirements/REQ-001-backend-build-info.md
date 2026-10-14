# REQ-001 — Show backend build information on /actuator/info

| Field | Value |
|---|---|
| Status | Approved |
| Jira type | Story |
| Priority | Low |
| Platforms | Backend |
| Requested by | Hany |
| Created | 2026-10-14 |
| Branch | feature/REQ-001-backend-build-info |

## Background
Hany's request (verbatim):

> "Add build information (app version, git commit, build time) to the backend's /actuator/info endpoint so I can confirm which backend version is running. Backend only, no mobile changes."

**What exists today** (observed in `Backend/tieyoh-api`):
- `spring-boot-starter-actuator` is already a dependency in `pom.xml` (Spring Boot 4.1.0 parent). The test companion `spring-boot-starter-actuator-test` is also present.
- `src/main/resources/application.properties` has **no `management.*` settings**, so the Spring Boot defaults apply: actuator lives at `/actuator` on the same port as the API, and only `/actuator/health` is exposed over HTTP. `/actuator/info` is therefore **not reachable today** (expected 404; Missouri to confirm as a baseline).
- The project version in `pom.xml` is `0.0.1-SNAPSHOT`, artifact `tieyoh-api`.
- There is **no request authentication layer** on the backend: Spring Security web is not a dependency (only `spring-security-crypto`), and there are no servlet filters or interceptors. Any actuator endpoint exposed over HTTP is reachable without credentials by anyone who can reach the server.
- No build plugin currently produces build or git metadata. There is no Dockerfile in the backend folder.
- There are no existing tests covering actuator endpoints.

## User story
As an **admin** (Hany, operating Tieyoh), I want **the running backend to report its version, the git commit it was built from, and when it was built** so that **I can confirm exactly which backend version is deployed in a given environment**.

## Acceptance criteria

### Core behavior
- **AC-1 — Endpoint reachable.** Given the backend is running, when a client sends `GET /actuator/info`, then the response is HTTP 200 with a JSON body (subject to the access decision in Open question 1).
- **AC-2 — App version.** Given the backend was built from the current `pom.xml`, when `/actuator/info` is called, then the response includes the application version, and it exactly matches the `<version>` in `Backend/tieyoh-api/pom.xml` (today `0.0.1-SNAPSHOT`). It also includes the artifact/application name (`tieyoh-api`).
- **AC-3 — Build time.** Given a build was produced at time T, when `/actuator/info` is called, then the response includes a build timestamp in ISO-8601 format with an explicit UTC offset/zone, within a few minutes of T.
- **AC-4 — Build time is fixed at build, not at startup.** Given one built artifact is started, stopped, and started again, when `/actuator/info` is called after each start, then the build timestamp is identical both times.
- **AC-5 — Git commit.** Given the backend is built from a git checkout, when `/actuator/info` is called, then the response includes the commit identifier, and it matches `git rev-parse --short HEAD` (or the full hash, per Open question 2) for the checked-out source at build time.
- **AC-6 — Git commit time.** Given the backend is built from a git checkout, when `/actuator/info` is called, then the response includes the commit timestamp of that commit (ISO-8601 with zone). *(Subject to Open question 4.)*
- **AC-7 — New commit, new values.** Given a new commit is made and the backend is rebuilt and restarted, when `/actuator/info` is called, then the commit identifier and build time both differ from the previous build.
- **AC-8 — Both run modes.** Given the backend is started either via `./mvnw spring-boot:run` or via the packaged jar (`java -jar target/tieyoh-api-*.jar`), when `/actuator/info` is called, then AC-2 through AC-6 hold in both cases.

### No git metadata available
- **AC-9 — Build does not fail without git.** Given the backend source is built from a directory with no `.git` folder (e.g., a source archive or a container build context that excludes `.git`), when `./mvnw package` runs, then the build succeeds.
- **AC-10 — Endpoint still works without git.** Given a build produced without git metadata, when `/actuator/info` is called, then the response is HTTP 200, still includes the app version and build time, and the git fields are either absent or clearly marked as unavailable (per Open question 6). The endpoint must not error, and must not show stale or made-up commit values.

### Exposure and data minimization (fintech controls)
- **AC-11 — Only approved fields.** Given any build, when `/actuator/info` is called, then the response contains only the approved fields: app name, app version, build time, commit id, and any items Hany approves in Open questions 3–5. Nothing else.
- **AC-12 (negative) — No sensitive git details.** Given a build from a git checkout, when `/actuator/info` is called, then the response does **not** contain: the git remote/origin URL, commit author or committer names or emails, the commit message, the build machine user or hostname, local file paths, or a list of modified/untracked files.
- **AC-13 (negative) — No environment or config leakage.** Given the backend is running with real environment variables (e.g., `DB_PASSWORD`, `GOOGLE_WALLET_*`), when `/actuator/info` is called, then the response does not contain any environment variables, configuration property values, datasource details, secrets, or file paths. Java runtime and OS details are excluded unless Hany approves them (Open question 5).
- **AC-14 (negative) — No other actuator endpoints newly exposed.** Given this change is deployed, when a client calls `/actuator/env`, `/actuator/configprops`, `/actuator/beans`, `/actuator/mappings`, `/actuator/loggers`, `/actuator/heapdump`, `/actuator/threaddump`, `/actuator/metrics`, and `/actuator/shutdown`, then each returns 404 (not exposed), exactly as before this change.
- **AC-15 — Health unchanged.** Given this change is deployed, when a client calls `GET /actuator/health`, then the response status and body are the same as before this change.
- **AC-16 (negative) — Read-only.** Given the endpoint is exposed, when a client sends `POST`, `PUT`, `PATCH`, or `DELETE` to `/actuator/info`, then the request is rejected with a 4xx status and nothing changes.
- **AC-17 — Access rule enforced.** Given the access decision made in Open question 1, when an unauthorized caller (as defined by that decision) requests `/actuator/info`, then they do not receive build information. *(If Hany chooses "public, same as health", this AC becomes: an unauthenticated caller receives exactly the AC-11 fields and nothing more.)*

### Testing
- **AC-18 — Automated test.** Given the backend test suite runs, when `./mvnw test` executes, then an automated test verifies that `/actuator/info` returns the required fields (AC-2, AC-3, AC-5 shape) and does not contain the forbidden fields from AC-12 and AC-13. The test must not write to the dev database and must not depend on real merchant data.

## Out of scope
- Any Android or iOS changes (including showing the backend version in the apps).
- Introducing a backend authentication/authorization layer (Spring Security) in general. If Hany wants `/actuator/info` restricted beyond what can be done without one, that should be a separate REQ.
- Changing the app versioning scheme (e.g., moving off `0.0.1-SNAPSHOT`, semantic versioning, release tagging).
- CI/CD pipeline setup, Dockerfile creation, or deployment changes.
- Exposing any other actuator endpoints or changing `/actuator/health`.

## Money & data impact
- Touches balances / ledger / funding / redemption / ACH? **No** — read-only operational metadata; no money movement or ledger data involved.
- Touches PII or card data? **No by design, but flagged.** Git metadata can carry personal data (author/committer names and emails) — AC-12 forbids exposing it.
- Merchant data? **No.** Must not reference or include anything from `Backend/merchant-imports/`.
- Compliance / security considerations to flag for Eureka:
  - The backend has **no request authentication layer today**; anything exposed under `/actuator` is public to anyone who can reach the server. Exposing the exact commit id lets an outsider fingerprint the deployed version. Low risk on its own (the repo is private), but it is a new information-disclosure surface.
  - Care is needed so that exposing `info` does not accidentally expose other actuator endpoints (AC-14) or environment/config contents (AC-13).
  - Git info contributors in Spring Boot can include more fields than requested if configured to "full" mode; Eureka should check the actual response against AC-11/AC-12.

## UI / API notes
- **Endpoint affected:** `GET /actuator/info` on the `tieyoh-api` backend (same host/port as the API today). Changes from "not exposed" to "exposed" with build and git sections.
- **Endpoints that must not change:** `GET /actuator/health` and all API endpoints.
- **Illustrative response shape (not a design mandate):** a `build` section (name, version, time) and a `git` section (commit id, commit time). Exact key names follow whatever Spring Boot's standard info contributors produce, so standard tooling can read them.
- No screens affected.

## Assumptions
- **A-1:** "App version" means the Maven project version in `pom.xml`. (Inferred.)
- **A-2:** Build time is wanted in UTC. (Inferred.)
- **A-3:** The same exposure and fields apply in every environment (local, dev, prod); there are no per-environment differences. (Inferred.)
- **A-4:** Hany is the only intended consumer for now (manual checks via browser or curl); no monitoring system needs a specific format. (Inferred.)
- **A-5:** `/actuator/info` currently returns 404 because the config uses Spring Boot defaults. This comes from reading the config and still needs a live check by Missouri before the change.

## Open questions for Hany
1. **Who may access `/actuator/info`?** The backend has no login/auth layer today, so "exposed" means public to anyone who can reach the server, just like `/actuator/health` is now. Options: (a) public, with only the minimal fields in AC-11; (b) reachable only on a separate management port / internal network; (c) behind authentication, which needs a separate REQ to add auth.
   **Recommended default:** (a) public, minimal fields only. Revisit once backend auth exists or before the first production launch.
2. **Short or full commit hash?** **Recommended default:** short (7 characters, the same as `git log --oneline`). That is enough to identify the build and reveals slightly less.
3. **Include the git branch name?** Branch names like `feature/REQ-001-…` reveal internal work items. **Recommended default:** omit. The commit id is enough to identify the build.
4. **Include the commit time and a "dirty" flag?** The dirty flag is a yes/no showing the build had uncommitted changes. It never lists file names. **Recommended default:** include the commit time. Include the dirty flag as a yes/no only, because it tells you when the commit id doesn't fully describe what's running.
5. **Include Java runtime / OS details** (e.g., Java version, OS name)? **Recommended default:** omit. You didn't ask for it, and it widens what outsiders can fingerprint.
6. **When git metadata is unavailable at build time, should the git section be left out or show "unknown"?** And should a missing commit ever fail the build, for example for release builds? **Recommended default:** leave the git section out, never fail the build, and still show the version and build time.

## Decisions (Hany, 2026-10-14)
Hany approved the requirement with all recommended defaults:
1. Access: public (same as `/actuator/health`), minimal AC-11 fields only. Revisit when backend auth exists or before first production launch.
2. Commit hash: short (7 characters).
3. Branch name: omitted.
4. Commit time: included. Dirty flag: included (yes/no only, no file list).
5. Java runtime / OS details: omitted.
6. No git metadata: omit the git section, never fail the build, still show version and build time.

Approved fields are therefore: app name, app version, build time, short commit id, commit time, dirty flag.

## History
- 2026-10-14 — Created by Crocodile
- 2026-10-14 — Approved by Hany (all recommended defaults)
