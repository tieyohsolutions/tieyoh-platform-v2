# SEC-001 — Actuator exposure and info contributors can be widened by deployment config, with no code guard or auth backstop

| Field | Value |
|---|---|
| Status | Open |
| Jira type | Security Bug |
| Severity | Low |
| Category | OWASP A05:2021 Security Misconfiguration / CWE-200, CWE-16 |
| Linked requirement | REQ-001 |
| Location | Backend/tieyoh-api/src/main/resources/application.properties:20-25 (branch feature/REQ-001-backend-build-info) |
| Found by | Eureka (Security) |
| Created | 2026-10-14 |

## Description
REQ-001 restricts actuator using properties only:
`management.endpoints.web.exposure.include=health,info` and `management.info.{env,java,os,process,ssl}.enabled=false`.
Spring Boot lets any later property source override these: OS environment variables
(`MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE`, `MANAGEMENT_INFO_ENV_ENABLED`,
`MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS`), `SPRING_APPLICATION_JSON`, command-line args,
or an external `application.properties` / profile file. The backend has no
authentication layer, so nothing else stands between such an override and the internet.

Verified on a scratch build of the branch (not the dev server) by running the
REQ-001 test class with environment variables set:
- `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=*` — `/actuator/threaddump` and the other
  endpoints are served (test `otherActuatorEndpointsAreNotExposed` fails).
- `MANAGEMENT_INFO_ENV_ENABLED=true` + any `INFO_*` variable — its value shows up in `/actuator/info`.
- `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=always` — health details are exposed.
- `MANAGEMENT_INFO_GIT_MODE=full` — **no effect** (git contributor is correctly pinned in
  `BuildInfoConfiguration`). That is the right pattern and the other controls should follow it.

This is not a regression. The same override existed before this change. REQ-001 does
make actuator part of the public surface, though, and the new tests only protect the
packaged config. They can't detect an override set at deploy time.

## Exploit scenario
An operator (or a future Dockerfile/Helm chart, or a copied tutorial snippet) sets
`MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=*` to debug something in a deployed environment.
An unauthenticated attacker then downloads `/actuator/heapdump`, which contains the DB password
(`DB_PASSWORD`), the Google Wallet service-account private key once loaded, and any in-memory
customer/merchant data, or uses `/actuator/env`, `/actuator/loggers` (change log levels), or
`/actuator/shutdown` if it is enabled. For a money-holding fintech that means direct ledger DB
compromise. The likelihood depends on a misconfiguration, so the severity is Low, but the
impact if it happens would be Critical.

## Remediation
Enforce the allow-list in code so configuration can only narrow it, never widen it:
1. Register an `EndpointFilter<ExposableWebEndpoint>` bean (in `BuildInfoConfiguration` or a new
   `ActuatorHardeningConfiguration`) that only matches endpoint ids `health` and `info`. The web
   endpoint discoverer applies every `EndpointFilter` bean in addition to the property-based
   include/exclude filter, so `exposure.include=*` can't add anything.
2. Pin the info output the same way the git contributor already is: either disable the
   auto-configured `env`/`java`/`os`/`process`/`ssl` contributors in code, or fail startup
   (e.g. an `ApplicationListener<ApplicationReadyEvent>` / `@PostConstruct` check) if any
   `InfoContributor` bean other than build and the minimal git contributor is present.
3. Fail startup if `management.endpoint.health.show-details` / `show-components` is anything
   other than `never` (or pin it in code), until an auth layer exists.
4. Also consider `management.endpoints.access.default=none` with explicit
   `management.endpoint.health.access=read-only` and `management.endpoint.info.access=read-only`
   as an extra layer.
5. Add tests that start the context with `management.endpoints.web.exposure.include=*`,
   `management.info.env.enabled=true` and `management.endpoint.health.show-details=always` and still
   assert only health/info are exposed and info/health bodies are unchanged.

The long-term fix is the backend auth REQ (actuator behind admin auth or a separate
management port bound to an internal interface). Do that before the first production launch.

## Regulatory note
An unauthenticated heap dump or env dump exposes DB credentials and signing keys for a
system that holds customer funds. That is a PCI DSS Req. 2/6/7 hardening concern and a
data-privacy breach vector. No direct Reg E / money transmission impact from this change itself.

## Developer fix notes
_(Mississippi appends)_

## Verification
_(Eureka appends)_
