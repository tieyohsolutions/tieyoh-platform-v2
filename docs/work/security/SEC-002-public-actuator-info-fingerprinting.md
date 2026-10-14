# SEC-002 — /actuator/info is public and lets outsiders fingerprint the deployed build

| Field | Value |
|---|---|
| Status | Open (approved design decision. Hany to mark "Risk accepted (by Hany)" at triage) |
| Jira type | Security Bug |
| Severity | Info |
| Category | OWASP A05:2021 Security Misconfiguration / CWE-200 (Exposure of Sensitive Information) |
| Linked requirement | REQ-001 |
| Location | Backend/tieyoh-api/src/main/resources/application.properties:20 (branch feature/REQ-001-backend-build-info) |
| Found by | Eureka (Security) |
| Created | 2026-10-14 |

## Description
`GET /actuator/info` is now reachable without authentication and returns the build name,
version, build time, short commit id, commit time and the dirty flag. These are exactly the
fields Hany approved in REQ-001 Decisions 1–4. Verified on a branch build: no author/committer
names or emails, remote URL, commit message, branch, build host/user, tags, env, Java/OS or
datasource data appear in the response, in the packaged `git.properties` /
`META-INF/build-info.properties`, or in the Maven build log (checked even with `-X` debug).
Semgrep `spring-actuator-dangerous-endpoints-enabled` flags this line, and it is the same accepted item.

## Exploit scenario
An attacker can tell exactly which commit is running, when it was built, and whether it was
built from uncommitted code. If the source ever leaks, or a dependency CVE is announced, they
can match it against the deployed build and time an attack to land before a redeploy.
The dirty flag also shows when a deployed build came from a developer's uncommitted
working tree. On its own this gives no access to money, PII or credentials.

## Remediation
No code change needed now (approved trade-off). Track for the pre-production gate:
- When the backend auth REQ lands, move `/actuator/info` behind admin auth or onto a
  management port bound to an internal interface (`management.server.port` +
  `management.server.address`).
- Release builds should not come from a dirty tree. Consider failing release/CI builds when
  `git.dirty=true` instead of publishing it.

## Regulatory note
None directly. Version fingerprinting matters for PCI DSS Req. 6 (patch exposure window) once
card data is in scope.

## Developer fix notes
_(Mississippi appends)_

## Verification
_(Eureka appends)_
