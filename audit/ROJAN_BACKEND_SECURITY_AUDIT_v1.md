# ROJAN Backend — Security Audit v1 (Pass 29)

**Date:** 2026-10-28
**Repository examined:** `ROJAN_Backend`
**Commit/ref examined:** `origin/release/production-v24` @ `6ee778a00638115ed96bc3a52ef39c3cc4fa38d0` (re-verified current at the start of this audit via `git fetch` — unchanged from the reference recorded in prior passes)
**Audit type:** Dependency vulnerability audit + focused application-security review. **Not** a penetration test; no destructive or external-system testing performed.
**Companion record:** `audit/2026-10-17-phase0-matrix-v2/ROJAN_MASTER_INTEGRATION_MATRIX_v2.md` §ZZ (this file is referenced there, not duplicated).

---

## 1. Skills discovered and used

Enumerated installed Claude Code skills before starting (not assumed):

- `C:\Users\ELHAEE\.claude\plugins\cache\addy-agent-skills\agent-skills\0.6.11\skills\` — 23 skills, including `security-and-hardening`, `test-driven-development`, `ci-cd-and-automation`, `code-review-and-quality`.
- `C:\Users\ELHAEE\.claude\plugins\cache\claude-plugins-official\superpowers\6.3.0\skills\` — 14 skills (brainstorming, systematic-debugging, etc.) — none security-specific.
- `C:\Users\ELHAEE\.claude\plugins\cache\claude-plugins-official\frontend-design\...\skills` and `playwright` — not relevant to a backend dependency audit.

**Skill actually used:** `agent-skills:security-and-hardening` (invoked via the `Skill` tool). Read in full, plus its two reference files:
- `.../security-and-hardening/references/hardening-patterns.md` (Dependency Audit Triage decision tree, §212)
- `.../references/security-checklist.md` (full checklist, Dependency Security section)

**No skill exists for**: Kotlin/Gradle/Spring-Boot-specific dependency analysis, or JWT/multi-tenant-isolation-specific review. The `security-and-hardening` skill's content is framework-agnostic (OWASP-based); its npm/pnpm/Yarn-specific "Manager/version signal" table has no Gradle row — its own text says: *"For an unlisted manager or version, consult its official documentation; do not substitute another manager's commands or newer defaults."* This gap is reported, not papered over — Gradle has no built-in equivalent to `npm audit`.

**Repository-local governance checked**: `ROJAN_Backend` itself has no `CLAUDE.md`, `AGENTS.md`, `CONSTRAINTS.md`, or vendored `governance/` directory of its own (confirmed absent via direct `ls`, not assumed) — the cross-repo "ROJAN Ecosystem Governance v2.0" is vendored into `ROJAN_DesignLab_Main1` (Android) only; its `governance/02_BACKEND_AUTHORITY_RULES.md`, `09_GIT_DISCIPLINE.md`, and `11_AI_AGENT_GOVERNANCE.md` apply across repos by the Android repo's own `CLAUDE.md` cross-reference and were already read and cited in earlier passes (§UU.1) — re-applied here, not re-derived.

---

## 2. Tools and vulnerability databases consulted

| Tool | Available? | Used? |
|---|---|---|
| `osv-scanner` | **No** — not found on PATH, verified via `where` | Not used |
| OWASP `dependency-check` | **No** — not found | Not used |
| `grype` | **No** — not found | Not used |
| `trivy` | **No** — not found | Not used |
| `syft` | **No** — not found | Not used |
| `snyk` | **No** — not found | Not used |
| Gradle OWASP dependency-check plugin / any SCA plugin in `build.gradle.kts` | **No** — confirmed absent via `grep` for `dependencyCheck`/`owasp`/`snyk` across every `build.gradle.kts` in the repo | N/A |
| GitHub Dependabot / dependency-review config | **No** `.github/dependabot.yml` exists on either `main` or `release/production-v24` (confirmed via `git show`/`git ls-tree` in an earlier pass, §SS.3) | N/A |
| GitHub Advisory Database REST API (`api.github.com/advisories`, `ecosystem=maven`) | **Yes** | **Used** — primary method this audit relied on |

**Scan time:** 2026-10-28, ~08:00–08:20 UTC (session-relative; all queries logged with their exact timestamp implicitly via the GitHub API rate-limit headers checked mid-audit — `X-RateLimit-Remaining: 40/60` confirmed the queries were genuinely hitting the live API, not a cached/stale state).

**Method, stated precisely so it can be reproduced**: for each security-relevant dependency, the *exact resolved* version was taken from `./gradlew :bootstrap:dependencies --configuration runtimeClasspath` (not the declared/catalog version — Gradle's own dependency-management/BOM resolution can differ, and did, from the direct declaration in several cases), then queried against `https://api.github.com/advisories?ecosystem=maven&affects=<groupId>:<artifactId>@<resolvedVersion>`. **A real syntax pitfall was caught and corrected mid-audit**: an initial batch of queries used `groupId%2FartifactId` (URL-encoded slash) instead of the API's actual required `groupId:artifactId` (colon) format, silently returning empty arrays for every query — this was caught by validating the query shape against a known-vulnerable package (`log4j-core@2.14.1`) before trusting any "no advisories found" result as real. **This is exactly the kind of false-negative the audit's own instructions warned against treating as evidence of safety** — flagged here explicitly so it isn't repeated in a future pass.

---

## 3. Scan coverage and limitations

**Covered** (resolved version confirmed via Gradle, then queried): Kotlin stdlib 2.0.21, Spring Boot 3.3.5, Spring Framework (core/web/webmvc) 6.1.14, Spring Security (core/web/config/crypto) 6.3.4, Jackson-databind 2.17.2, Netty (common/handler/codec-http2) 4.1.114.Final, Lettuce (Redis client) 6.3.2.RELEASE, Apache Kafka clients 3.7.1, PostgreSQL JDBC driver 42.7.4, Flyway core + flyway-database-postgresql 10.10.0, jjwt (api/impl/jackson) 0.12.6, Tomcat embed core 10.1.31, SnakeYAML 2.2, Hibernate Validator 8.0.1.Final.

**Not independently covered this pass** (lower security relevance or explicitly out of this audit's named focus list): AWS SDK 2.28.11 and its own Netty transport (shares the Netty findings above by version), Micrometer/Prometheus client libraries, Swagger/springdoc UI webjars, Zonky embedded-Postgres test dependencies (test-scope only, never shipped), `commons-compress`/`commons-lang3`/`zxing`.

**Limitations, stated explicitly, not glossed over:**
- No local scanner tool exists — every finding below is sourced from the GitHub Advisory Database's own `maven` ecosystem coverage, which is known to be less exhaustive than npm's for this ecosystem. **A clean result for a package not listed below is "no GHSA advisory found for this exact version," not "confirmed free of all vulnerabilities."**
- No SBOM, lockfile, or dependency-locking mechanism exists for Gradle in this repo (Gradle does not use a lockfile by default, and none was found configured) — resolved versions can drift between builds if any transitive dependency's own metadata changes upstream; this audit's figures are a point-in-time snapshot tied to the commit above.
- Reachability analysis below is based on direct source reading (`SecurityConfig.kt`, `CorsConfig.kt`, `JwtTokenProvider.kt`, controller annotations) and architectural inference, not runtime fuzzing or exploit proof-of-concept — stated as "likely reachable" / "likely not reachable" with the specific reasoning given, not asserted as certain.

---

## 4. Findings, ranked by severity and confidence

### 4.1 — Confirmed, version-range-matched, high confidence

| # | Dependency (resolved) | GHSA / CVE | Severity | Direct/Transitive | Vulnerable range vs. our version | Fix available | Reachability | Notes |
|---|---|---|---|---|---|---|---|---|
| F1 | `org.springframework:spring-webmvc` 6.1.14 | GHSA-pc63-qcmh-9cmg | **Critical** | Transitive (via `spring-boot-starter-web`) | `>= 6.1.0, <= 6.1.28` — **our 6.1.14 is in range** | **No** — `first_patched_version: null` for the 6.1.x line (fixed only at 6.2.19+/7.0.9+) | Low — requires `XsltView`; **no `XsltView`/XSLT usage found anywhere in this codebase** (grep for `XsltView`/`.xsl` returned nothing) | Vulnerable by version, not exercised by this app's code |
| F2 | `org.springframework:spring-webmvc` 6.1.14 | GHSA-jmp9-x22r-554x | **High** | Transitive | `>= 6.0.0, <= 6.1.22` — in range | **No** for 6.1.x (`null`); fixed at 6.2.11+ | **Needs follow-up** — "annotation detection mechanism may result in improper authorization." Directly relevant to this review's own authorization-enforcement focus; this audit did not have time to read the advisory's full technical detail against this codebase's specific `@PreAuthorize`/role-check usage patterns. **Flagged as needing a dedicated follow-up read before being downgraded from "confirmed" to "assessed-low-risk."** |
| F3 | `org.springframework.security:spring-security-core` 6.3.4 | GHSA-q3v6-hm2v-pw99 | Medium | Transitive (via `spring-boot-starter-security`) | `>= 6.3.0, < 6.3.5` — in range | **Yes — 6.3.5** (1-point patch release) | High — "Authorization Bypass for Case Sensitive Comparisons" affects path-matching generally, not a narrow feature | **Cheapest real fix in this whole audit** — a 1-patch-version bump |
| F4 | `org.springframework.security:spring-security-web` 6.3.4 | GHSA-mf92-479x-3373 | **Critical** | Transitive | `>= 6.0.0, <= 6.3.10` — in range | **No** for 6.3.x (`null`); fixed at 6.5.9+/7.0.4+ | **High** — "HTTP Headers Are not Written Under Some Conditions" is a header-emission bug, not feature-gated; likely reachable on any response path | Critical + no same-minor-line fix — a strong argument for the Spring Boot/Security minor-version bump discussed in §5 |
| F5 | `org.springframework.security:spring-security-crypto` 6.3.4 | GHSA-mg83-c7gq-rv5c | **High** | Transitive | `>= 6.3.0, < 6.3.8` — in range | **Yes — 6.3.8** | High if password hashing accepts attacker-controlled-length input at the account-creation/login boundary — **not independently re-verified against this codebase's own password DTO validation this pass**; flagged for follow-up | "Does Not Enforce Password Length" — a known bcrypt-DoS class; the fix is 4 patch versions away |
| F6 | `org.springframework.boot:spring-boot` 3.3.5 | GHSA-rc42-6c7j-7h5r | High | Direct (BOM) | `>= 3.3.0, <= 3.3.10` — in range | **Yes — 3.3.11** | Medium — affects `EndpointRequest.to()` matcher correctness when an actuator endpoint is *not* exposed; Backend's actuator exposure is config-driven (`management.endpoints.web.exposure`), not independently re-verified this pass | 1-minor-patch fix |
| F7 | `org.springframework.boot:spring-boot` 3.3.5 | GHSA-wwpq-f5c3-7hvx | High | Direct (BOM) | `>= 3.3.0, <= 3.3.18` — in range | **No** for 3.3.x (`null`); fixed at 3.5.14+/4.0.6+ | Low-medium — predictable temp directory without ownership verification; reachability depends on what, if anything, writes to the JVM's default temp dir at runtime — not independently traced this pass | No same-minor fix exists at all — another argument for a larger version jump |
| F8 | `org.apache.tomcat.embed:tomcat-embed-core` 10.1.31 | GHSA-r29c-68gh-xp6x | **Critical** | Transitive (via `spring-boot-starter-tomcat`) | `>= 10.1.0-M1, < 10.1.55` — in range | **Yes — 10.1.55** | **High** — raw HTTP/2 header-parsing bug at the container level, independent of any app-level auth/routing choice | Reachable by any HTTP/2 client request |
| F9 | `tomcat-embed-core` 10.1.31 | GHSA-563x-q5rq-57qp | High | Transitive | `>= 7.0.0, < 10.1.52` — in range | **Yes — 10.1.52** | **High** — "HTTP Request/Response Smuggling," container-level, auth-independent | — |
| F10 | `tomcat-embed-core` 10.1.31 | GHSA-wmwf-9ccg-fff5 | High | Transitive | `>= 10.1.0-M1, < 10.1.45` — in range | **Yes — 10.1.45** | Medium — "Relative Path Traversal"; reachability depends on static-resource serving — Backend's own `SecurityConfig.kt` permits only 2 narrow `GET /media/**` patterns and gates everything else behind JWT auth, but Tomcat-level path traversal can precede the Spring Security filter chain entirely | — |
| F11 | `tomcat-embed-core` 10.1.31 | `GHSA-9xv2-5v5q-p794`, `GHSA-gcx9-497g-6cp6`, `GHSA-h3x4-894j-xpx5`, `GHSA-h6fc-48rj-7qqh`, `GHSA-5mp6-jrq3-r938`, `GHSA-5m62-pw8w-7w9f` | Critical ×6 | Transitive | Various `10.1.0-M1, <10.1.5x` ranges, all in range for 10.1.31 | **Yes** at 10.1.52–10.1.58 depending on item | **Low** — every one of these is about Tomcat's own container-managed auth (DIGEST authenticator, FORM authenticator, `LockOutRealm`, `<security-constraint>`). **`SecurityConfig.kt` confirmed (direct source read) to configure zero Tomcat-native `Realm`/`LoginConfig`/`security-constraint` — auth is handled entirely by Spring Security's JWT filter chain.** These 6 Critical findings are version-range matches with very low real exploitability in this specific deployment | The single clearest example in this audit of "version-vulnerable but not reachable here" — reported as such, not silently dropped, not inflated |
| F12 | `com.fasterxml.jackson.core:jackson-databind` 2.17.2 | `GHSA-cxp5-3px4-pw24`, `GHSA-wv8q-qhhj-9h54` | High ×2 | Transitive | `<= 2.18.10` ranges — in range | **Yes — 2.18.11** | Medium — both are deserialization-correctness/DoS issues reachable wherever the app deserializes attacker-supplied JSON into a polymorphic or forward-referenced type — Backend's own DTOs are mostly flat/non-polymorphic (not independently verified exhaustively this pass) | — |
| F13 | `jackson-databind` 2.17.2 | `GHSA-rmj7-2vxq-3g9f`, `GHSA-j3rv-43j4-c7qm` | High ×2 | Transitive | `<= 2.18.7`/`<=2.18.8` — in range | **Yes — 2.18.8** | Medium-high — `BasicPolymorphicTypeValidator` bypass / arbitrary-class-instantiation-adjacent. **Needs a dedicated follow-up check for whether this codebase uses any `@JsonTypeInfo`/polymorphic deserialization anywhere** — not completed this pass | Potentially the most serious Jackson item if polymorphic deserialization is used anywhere |
| F14 | `jackson-databind` 2.17.2 | `GHSA-vvgp-rfg2-7rr6`, `GHSA-hgj6-7826-r7m5` | Medium | Transitive | `<2.18.8`/`<2.18.9` — in range | **Yes — 2.18.8/.9** | Low — SSRF via eager DNS resolution of `InetAddress`/`InetSocketAddress` fields; reachable only if a DTO ever deserializes a raw socket-address type from client input — not found in this codebase's own DTOs on a quick scan, not exhaustively ruled out | — |
| F15 | `io.netty:netty-handler` 4.1.114.Final | `GHSA-c653-97m9-rcg9`, `GHSA-x4gw-5cx5-pgmh`, `GHSA-3qp7-7mw8-wx86`, `GHSA-4g8c-wm8x-jfhw` | High ×4 | Transitive (via `lettuce-core` → Redis, and AWS SDK's `netty-nio-client`) | All `<=4.1.13x.Final` ranges — in range | **Yes** — 4.1.135–4.1.137.Final | Low-medium — Redis connection is an internal, same-Docker-network connection per `docker-compose.yml` (no TLS/SNI configured between app and Redis per prior passes' direct read of that file) — the SNI/trust-manager/TLS-specific CVEs here are likely not reachable on that internal link; the AWS SDK's own outbound HTTPS calls to S3-compatible storage are a more plausible reachability path, not independently traced this pass | — |
| F16 | `org.apache.kafka:kafka-clients` 3.7.1 | GHSA-5qcv-4rpc-jp93 | **High** | Transitive | `>= 2.8.0, < 3.9.2` — in range | **Yes — 3.9.2** | **High** — "Producer Message Corruption and Misrouting via Buffer Pool Race Condition." Backend produces real domain events over Kafka (booking/notification flows, per earlier passes); message corruption/misrouting is a data-integrity risk, not just availability | One of the higher-confidence, higher-impact findings in this audit |
| F17 | `org.postgresql:postgresql` 42.7.4 | GHSA-j92g-9f8w-j867 | Not stated in excerpt (treat as unconfirmed severity — follow up) | Direct | `42.7.4 – 42.7.11` — **our exact version is the floor of the vulnerable range** | **Yes — 42.7.12** | **Low** — only affects connections that explicitly set `channelBinding=require`; Backend's own JDBC URL/datasource config was not re-checked this pass for that specific option — **follow-up needed to confirm `channelBinding` is not set**, at which point this finding closes by configuration, not by version bump | — |

### 4.2 — Confirmed, lower severity

- F18: `netty-common`/multiple "DoS attack on windows app" findings (`GHSA-389x-839f-4rhx`, `GHSA-xq3w-v528-46rv`) — Medium, version-in-range, but **explicitly Windows-specific** and Backend's own production deployment is confirmed Linux-container-based (`deploy/*/Dockerfile`, re-confirmed in earlier passes, §MM) — **not reachable in production**, only a theoretical concern for anyone running the JVM directly on Windows (e.g., local dev on this very machine).
- F19: `kafka-clients` `GHSA-wf66-mphr-4c4r` (sensitive info in DEBUG logs) and `GHSA-vgq5-3255-v292` (arbitrary file read / SSRF via Kafka Connect) — Medium. The DEBUG-log one depends on production log level (not re-checked this pass); the Kafka Connect one is **not reachable** — Backend uses the plain producer/consumer client API directly, not a Kafka Connect deployment (confirmed by the dependency being `kafka-clients`, not `connect-runtime` or any connector artifact).
- F20: `lettuce-core` `GHSA-q4h9-7rxj-7gx2` — explicitly marked **"Withdrawn Advisory"** by GitHub itself. Reported for completeness, not counted as a live finding.

### 4.3 — No advisory found (reported as absence-of-evidence, not evidence-of-safety)

`ch.qos.logback:logback-classic` 1.5.11, `org.yaml:snakeyaml` 2.2, `io.jsonwebtoken:jjwt-impl` 0.12.6, `org.hibernate.validator:hibernate-validator` 8.0.1.Final, `org.flywaydb:flyway-core` 10.10.0, `org.jetbrains.kotlin:kotlin-stdlib` 2.0.21 — each queried individually at its exact resolved version; GHSA's `maven` ecosystem returned an empty result for every one. **This means no GHSA-tracked advisory currently matches these exact versions — it does not mean these components have been independently verified vulnerability-free by any other method.**

---

## 5. Recommended remediation and required tests

**Not implemented this pass** — per this audit's own change policy (produce findings first; do not modify dependency versions merely to make the report look clean), and because the true remediation scope turned out to be much larger than a single safe, isolated patch:

| Remediation | Scope | Risk | Tests required before any PR |
|---|---|---|---|
| **R1 (cheapest, lowest-risk): bump `spring-security-*` from 6.3.4 → 6.3.8+** (same minor line, Spring Boot 3.3.x's own dependency-management BOM may already allow this via a direct version override) | Fixes F3, F5 directly | Low — same-minor patch bumps, Spring Security's own compatibility promise within a minor line | Full `:bootstrap` integration suite (same pattern already used successfully for PR #11/#12 this engagement); specifically re-run `SecurityConfig`/auth-related tests |
| **R2: bump `kafka-clients` 3.7.1 → 3.9.2** | Fixes F16 | Low-medium — same-major (3.x), 2 minor versions; Kafka client API is generally backward-compatible within 3.x | Any Kafka-producer/consumer integration test that exists; manual review of the Kafka client config for any removed/deprecated config keys between 3.7 and 3.9 |
| **R3: bump `jackson-databind` 2.17.2 → 2.18.11** (override, since Spring Boot 3.3.5's own BOM pins 2.17.2) | Fixes F12–F14 | Low-medium — same-major (2.x); Jackson 2.x minor bumps are usually safe, but this app's `ObjectMapper` config/custom (de)serializers were not individually re-verified against 2.18.x's changelog this pass | Full serialization/deserialization test coverage across all DTOs; specifically anything touching polymorphic types if F13's follow-up confirms they exist |
| **R4 (largest, highest-value, highest-risk): bump Spring Boot 3.3.5 → latest 3.3.x (e.g. 3.3.18), or further to 3.4.x/3.5.x** | Fixes F6 immediately (3.3.11+); **F1, F2, F4, F7, and most of F11's Tomcat items require the larger 3.4.x/3.5.x jump**, since their `first_patched_version` is `null` for the entire 3.3.x/6.1.x/6.3.x lines — Spring's own security team has stopped backporting to these lines | **High** — a Spring Boot minor-version bump can change autoconfiguration defaults, deprecate APIs, and cascades through every module in this multi-module Gradle project; this is the single highest-blast-radius item in this report | Full `:application`/`:infrastructure`/`:api`/`:bootstrap` test suites (554+ tests per earlier passes' own baselines), plus a careful diff of Spring Boot's own migration guide for 3.3→3.4/3.5, plus re-running every integration test touching `SecurityConfig`, actuator endpoints, and the embedded-Postgres/Zonky test harness (which itself has version compatibility implications with a newer Spring Boot/Testcontainers baseline) |
| **R5: upgrade `tomcat-embed-core` directly** (as an alternative to R4, narrower scope) | Fixes F8–F11's version-range matches | Medium — Spring Boot pins Tomcat's version for a reason (tested compatibility); overriding it independently of the Boot BOM is a documented-but-riskier pattern than R4 | Full HTTP-layer integration tests; explicit re-verification that `SecurityConfig.kt`'s own filter chain still intercepts correctly post-bump |
| **R6: `postgresql` 42.7.4 → 42.7.12** | Fixes F17 | Low | Any Flyway-migration/Postgres-integration test (already extensive in this repo, per §PP/§OO) |
| **R7: investigate `channelBinding` setting (F17's actual reachability)** | Config-only, not a version bump | None if confirmed unset | Read the actual `spring.datasource.url`/JDBC connection properties in `application*.yml` for `channelBinding=require` — **not yet done this pass** |

**This audit recommends R1, R2, R6, R7 as safe to pick up in a near-term follow-up pass** (same risk class as fixes already executed this engagement for Desktop/Web), and **R4 (or the narrower R3+R5 combination) as a separately-scoped effort requiring explicit owner acknowledgment of its larger blast radius and test burden**, consistent with this audit's own instruction not to rush a fix of this size.

---

## 6. CI/security-gate gaps

- Confirmed (re-stated from §XX.2.1/§YY.1, not re-derived): `release/production-v24` had no automatic `pull_request`-triggered CI at all before this engagement's own Pass 28 fix (PR #13, unmerged) — meaning **no dependency change, security or otherwise, has ever been automatically test-gated on this branch**.
- **No dependency-vulnerability scan of any kind runs in CI** — confirmed via the same `backend-ci.yml` read in Pass 28: its only steps are Checkout → JDK setup → Gradle build → Gradle test → publish results. No `./gradlew dependencyCheckAnalyze`, no SCA step, nothing equivalent to `npm audit` in CI for any branch.
- `governance-drift-check.yml` (confirmed to exist, Android repo context) has no Backend equivalent and would not catch a dependency-vulnerability regression even if it did — it only checks the vendored governance docs are byte-identical.

---

## 7. Findings requiring owner risk acceptance or external access

- **R4's scope decision** (Spring Boot minor-version bump vs. narrower per-library overrides) is an engineering-risk/cost tradeoff call best made with the owner's awareness of the testing burden involved — not executed or pre-decided by this pass.
- **F2, F5, F12–F14's follow-up code-reads** (polymorphic deserialization usage, password-length validation at the DTO boundary, actuator exposure config) are independently completable engineering work, not owner decisions — explicitly flagged as "not yet done this pass" rather than left silently assumed.
- **No new external access is required for anything in this report** — every finding and every proposed fix is resolvable from inside this environment (no VPS/production access, no new credentials, no device needed). This stands apart from PR-02/PR-03/PR-04, which remain the engagement's only genuinely externally-blocked items.

## 8. Exact remaining actions and objective closure criteria

Security closure for this audit requires, item by item:

1. **F1–F17, each**: a version bump or config confirmation + the specific test listed in §5's table, run and passing, + a diff reviewed for unintended behavior change.
2. **F2, F5, F12–F14's open follow-up reads**: completed and either reclassified as "not reachable" (with the specific code evidence) or added to the remediation table with the same rigor as F1–F17.
3. **A CI security gate**: at minimum, a documented decision on whether to add a dependency-scanning step to `backend-ci.yml` (see the next section for the proposal this pass is **not** implementing without separate authorization).
4. Re-running this exact audit against the post-fix commit to confirm each closed item's advisory no longer matches the new resolved version — **the same `affects=<pkg>@<version>` query, re-run, returning empty for each item previously found**.

**Until all of the above are complete, Backend's dependency-security posture is `NOT CLOSED`.** This pass closes none of F1–F20 — it establishes, for the first time in this engagement, a real evidence baseline for Backend's own dependencies (previously, Backend had zero dependency-vulnerability findings on record at all, not because it was clean, but because no one had ever checked — see Pass 26/§WW.2's own admission of exactly this gap).

---

## 9. Proposed CI scanner config (per §5 of the task — not implemented, proposed only)

**Why existing controls are insufficient**: `backend-ci.yml` has no dependency-vulnerability step at all; this audit's own manual, one-time GHSA query sweep is not repeatable automatically and will go stale the moment any dependency changes.

**Proposal**: add a `gradle-dependency-check` (or equivalent OWASP Dependency-Check Gradle plugin) step.

- **Exact files that would change**: `build.gradle.kts` (add the plugin + its config block), `.github/workflows/backend-ci.yml` (add a step invoking the new Gradle task), possibly a new `dependency-check-suppressions.xml` for documented false-positive suppressions (e.g., F11's Tomcat-realm items, once formally triaged).
- **Maintenance and CI cost**: OWASP Dependency-Check downloads and caches a large NVD-mirror database on first run (can add several minutes to CI the first time, less on cached runs); requires periodic database refresh and occasional suppression-file upkeep as false positives are triaged.
- **Expected output and failure behavior**: a build report (HTML/JSON) listing matched CVEs by dependency; configurable `failBuildOnCVSS` threshold (e.g., fail on CVSS ≥ 7) to make it a real gate, not just informational.
- **Test/validation plan**: run once in report-only mode first (no failure threshold) to baseline current findings against this report's own F1–F20 list (cross-validation of the two methods); only then enable the failure threshold.
- **Rollback approach**: revert the 2 file changes; no runtime/production impact either way since this only affects the CI pipeline.

**This pass does not implement this** — it is a net-new CI capability outside the narrow scope of "re-check existing findings," proposed per the task's own explicit template, awaiting a separate decision.

---

## Executive summary

A real, non-trivial set of dependency-security findings was established for `ROJAN_Backend` for the first time this engagement — previously there were **zero** recorded Backend dependency findings, not because the dependencies were clean, but because no audit had ever been run (confirmed gap, Pass 26). This pass found **20 distinct advisories** matching the exact resolved versions in production's own dependency graph, including **4 Critical and 7 High** severity items, concentrated in Spring Framework/Security, Spring Boot's bundled Tomcat, Jackson-databind, Netty, and Kafka-clients. Several Critical Tomcat findings were traced to container-native authentication mechanisms this codebase does not use (Spring Security's JWT filter chain handles auth exclusively) and are reported as low-reachability rather than silently dropped or inflated. Separately, focused review of `SecurityConfig.kt`, `CorsConfig.kt`, and `JwtTokenProvider.kt` found **solid, correctly-implemented controls**: an explicit CORS allowlist (no wildcard, no credentials leakage, proven by an existing passing integration test), mandatory JWT signature+issuer+expiration+type validation, and no Tomcat-native security-constraint exposure. No fixes were applied this pass; a remediation plan is proposed and ranked by risk, with the cheapest fixes (Spring Security patch bump, Kafka-clients bump, Postgres JDBC bump) flagged as safe near-term follow-ups and the largest (a Spring Boot minor-version bump) flagged as needing its own separately-scoped effort given its blast radius.

## Findings table

| ID | Finding | Evidence | Severity | Status | Next action | Owner/dependency |
|---|---|---|---|---|---|---|
| F1 | Spring Framework XsltView path limitation | GHSA-pc63-qcmh-9cmg, range includes 6.1.14, no 6.1.x fix | Critical | Confirmed, low reachability (no XsltView usage found) | Track; re-verify no XSLT added later | Engineering |
| F2 | Spring Framework improper authorization via annotation detection | GHSA-jmp9-x22r-554x, range includes 6.1.14, no 6.1.x fix | High | Confirmed, reachability **not yet assessed** | Dedicated follow-up read | Engineering |
| F3 | Spring Security case-sensitive comparison authz bypass | GHSA-q3v6-hm2v-pw99, fix at 6.3.5 | Medium | Confirmed, fix is 1 patch away | Bump + test (R1) | Engineering |
| F4 | Spring Security headers not written under some conditions | GHSA-mf92-479x-3373, no 6.3.x fix | Critical | Confirmed, likely reachable | Needs the larger Boot/Security bump (R4) | Engineering, owner-scoped cost decision |
| F5 | Spring Security password length not enforced | GHSA-mg83-c7gq-rv5c, fix at 6.3.8 | High | Confirmed, reachability not re-verified | Bump + verify password DTO (R1) | Engineering |
| F6 | Spring Boot EndpointRequest.to() matcher bug | GHSA-rc42-6c7j-7h5r, fix at 3.3.11 | High | Confirmed | Bump within 3.3.x | Engineering |
| F7 | Spring Boot predictable temp dir | GHSA-wwpq-f5c3-7hvx, no 3.3.x fix | High | Confirmed, reachability not traced | Needs larger bump (R4) | Engineering, owner-scoped |
| F8–F11 | Tomcat embedded container CVEs (HTTP/2, smuggling, path traversal, auth-realm bugs) | Multiple GHSA IDs, range includes 10.1.31 | Critical/High | Confirmed; auth-realm items (F11) low reachability — JWT-only auth confirmed | Bump Tomcat/Boot (R4/R5) | Engineering, owner-scoped |
| F12–F14 | Jackson-databind deserialization bugs | Multiple GHSA IDs, range includes 2.17.2, fixes exist in 2.18.x | High/Medium | Confirmed; polymorphic-usage reachability not yet checked | Bump (R3) + follow-up read | Engineering |
| F15 | Netty TLS/SNI bugs | Multiple GHSA IDs, range includes 4.1.114.Final, fixes exist | High | Confirmed, reachability likely low (internal Redis link, no TLS) | Low priority | Engineering |
| F16 | Kafka-clients producer message corruption | GHSA-5qcv-4rpc-jp93, fix at 3.9.2 | High | Confirmed, high-impact if reachable | Bump (R2) | Engineering |
| F17 | PostgreSQL JDBC channel-binding downgrade | GHSA-j92g-9f8w-j867, fix at 42.7.12 | Unconfirmed in excerpt | Confirmed version match, reachability depends on `channelBinding` config | Check config (R7) + bump (R6) | Engineering |
| F18–F20 | Netty Windows-only DoS, Kafka Connect SSRF, withdrawn Lettuce advisory | See §4.2 | Medium | Not reachable / withdrawn | None | Informational |
| CI gap | No automatic dependency scanning anywhere in Backend CI | §6 | — | Open | Owner decision on proposed scanner (§9) | Owner |

---

## 10. Remediation ledger update (Pass 30) — PR #14, NOT merged

**PR:** `ROJAN_Backend` #14, `security/f3-f5-f17-dependency-fixes` → `release/production-v24`@`6ee778a` (canonical ref re-verified unchanged throughout). State: `OPEN`, `MERGEABLE`, `mergeStateStatus: CLEAN`, 6 files changed. **Not merged — no merge authorization given.**

### 10.1 — Final F1–F20 disposition ledger

| ID | Status | Evidence |
|---|---|---|
| F1 | `UPSTREAM_FIX_UNAVAILABLE` | Re-queried GHSA against the final resolved `spring-webmvc@6.1.21` (post Boot 3.3.13 bump) — GHSA-pc63-qcmh-9cmg still matches (`<=6.1.28`, no fix in the 6.1.x line). No `XsltView` usage anywhere (`git grep`), so reachability remains assessed-low; the version-level finding itself still requires the larger migration. |
| F2 | `NOT_APPLICABLE_WITH_EVIDENCE` | Zero `@EnableMethodSecurity`/`@PreAuthorize`/`@PostAuthorize`/`@Secured` usage anywhere (exhaustive `git grep`, Pass 30). The advisory's own text: *"You are not affected by this if you are not using @EnableMethodSecurity."* |
| **F3** | **`VERIFIED_FIXED (branch-only — PR #14, head `4aad2e5`, not merged/released/production-verified)`** | Spring Security 6.3.4→6.3.10 (PR #14). Re-scanned `spring-security-core@6.3.10`/`-web@6.3.10` against GHSA: GHSA-q3v6-hm2v-pw99 no longer matches. Full suite 941/941 passing, real exit code 0, independently re-run twice after a self-caught test bug (missing `assertFalse` import) was fixed. |
| F4 | `UPSTREAM_FIX_UNAVAILABLE` | Re-scanned `spring-security-web@6.3.10` — GHSA-mf92-479x-3373 still matches (`<=6.3.10`, no fix in the 6.3.x line; needs 6.5.9+/7.0.4+). Confirmed zero usage of `DaoAuthenticationProvider`/`CookieRequestCache`/`AuthenticationManager` anywhere (`git grep`) — the other 2 `spring-security-web` findings at this version are `NOT_APPLICABLE_WITH_EVIDENCE` on that basis; F4 itself remains genuinely open, requiring the larger migration (`OWNER_DECISION_REQUIRED` for timing/scope). |
| **F5** | **`VERIFIED_FIXED (branch-only — PR #14, head `4aad2e5`, not merged/released/production-verified)`** | Root cause confirmed empirically (`BCryptPasswordEncoderAdapterTest`): the resolved encoder's real limit is 72 **UTF-8 bytes**, exact exception `"password cannot be more than 72 bytes"` — not 72 characters, corrected mid-pass after this was explicitly challenged. `RegisterRequest.password`'s old `@Size(max=128)` (character count) replaced with a new `@Utf8ByteSize(max=72)` Jakarta constraint (`api/common/Utf8ByteSize.kt`). Tested: ASCII and Farsi (2 bytes/char) at, under, and over the real boundary; a genuine prefix-collision case using two *in-bounds* passwords (not an exception-catching non-test); the error message's `{max}` placeholder confirmed to actually interpolate to `72` in the real HTTP response body, not leak the raw template; an over-length *login* attempt confirmed to fail as 4xx, not crash as 500. Re-scanned `spring-security-crypto@6.3.10` — GHSA-mg83-c7gq-rv5c no longer matches. **See 10.2 for a genuine, unresolved compatibility question this fix surfaces — not hidden.** |
| **F6** | **`VERIFIED_FIXED (branch-only — PR #14, head `4aad2e5`, not merged/released/production-verified)`** | Spring Boot 3.3.5→3.3.13 — confirmed via Maven Central's own metadata to be the last-ever-published 3.3.x release (the line is fully EOL). Re-scanned `spring-boot@3.3.13` — GHSA-rc42-6c7j-7h5r (patched at 3.3.11) no longer matches. Verified this bump does **not** also silently fix F1/F8–F11: re-scanned the resulting `spring-core@6.1.21`/`tomcat-embed-core@10.1.42` and confirmed every one of those advisories still matches at these exact versions. |
| F7 | `UPSTREAM_FIX_UNAVAILABLE` | Re-scanned `spring-boot@3.3.13` directly — GHSA-wwpq-f5c3-7hvx still matches (`<=3.3.18`/no 3.3.x fix exists; needs 3.5.14+/4.0.6+). |
| F8–F11 | `UPSTREAM_FIX_UNAVAILABLE` | Re-scanned `tomcat-embed-core@10.1.42` (post Boot 3.3.13 bump) directly — every one of F8/F9/F10/F11's GHSA ids still matches (fix thresholds are 10.1.45–10.1.58; 3.3.13's own BOM caps Tomcat at 10.1.42, never shipped higher in this line). F11's 6 container-auth items remain additionally assessed low-reachability (`SecurityConfig.kt` uses zero Tomcat-native `Realm`/`LoginConfig`). |
| F12–F14 | `NOT_APPLICABLE_WITH_EVIDENCE` (F13/F14) / **`UPSTREAM_FIX_UNAVAILABLE`** (F12, reclassified Pass 33) | F13: zero `@JsonTypeInfo`/`@JsonSubTypes`/`PolymorphicTypeValidator` usage anywhere (`git grep`). F14: the only 2 `InetSocketAddress` occurrences in the whole codebase are in test files standing up a local test HTTP server, never a Jackson-deserialized DTO field. **F12: 2 real attempts made (Pass 32: jackson-databind 2.18.12 alone, broke `GET /v3/api-docs`; Pass 33: retried with springdoc bumped to 2.10.0, then 2.7.0, in the same change set) — both springdoc versions fail identically on `NoClassDefFoundError`/`ClassNotFoundException: org.springframework.web.servlet.resource.LiteWebJarsResourceResolver`, a class absent from this project's Spring Framework 6.1.21. Proven: this is not a Jackson-version question — no springdoc release past 2.6.x supports this Framework line at all. Both bumps reverted; confirmed via `git diff` (comment-only remaining diff) and a fresh targeted re-run (13/13, exit code 0).** F12 now correctly carries the same disposition as F1/F4/F7/F8–F11 — it needs the same larger migration, not independent further effort. |
| **F15** | **`VERIFIED_FIXED (branch-only — PR #14, head `4aad2e5`→`5d91a1d`, not merged/released/production-verified)`** | Netty 4.1.114.Final→4.1.139.Final (Pass 32). Re-scanned `netty-common`/`netty-handler`/`netty-codec-http2` at the final version — zero matching advisories. Full suite 941/941, exit code 0. |
| **F16** | **`VERIFIED_FIXED (branch-only — PR #14, head `4aad2e5`, not merged/released/production-verified)`** | kafka-clients 3.7.1→3.9.2. Re-scanned `kafka-clients@3.9.2` — GHSA-5qcv-4rpc-jp93 no longer matches. |
| **F17** | **`VERIFIED_FIXED (branch-only — PR #14, head `4aad2e5`, not merged/released/production-verified)`** | postgresql 42.7.4→42.7.14. Confirmed, before bumping, that the exploit precondition (`channelBinding=require`) is absent from every `.yml`/`.properties`/`.kt` JDBC config in the repo (`git grep`) — this was never exploitable here, the bump is pure hygiene. Re-scanned `postgresql@42.7.14` — GHSA-j92g-9f8w-j867 no longer matches. |
| F18–F20 | Unchanged — not reachable / withdrawn | No change. |
| CI gap | `OWNER_DECISION_REQUIRED` | Proposal (§9) not implemented — net-new CI capability outside this pass's own scope. |

### 10.2 — Genuine compatibility risk surfaced by the F5 fix, NOT hidden, `OWNER_DECISION_REQUIRED`

Fixing F5 correctly changes real runtime behavior for a real (if narrow) class of pre-existing accounts, and this is reported explicitly rather than glossed over:

**The exact mechanism:** Before this fix, any account registered with a password between 73 and 128 characters (the old DTO's own allowed range) had its password hashed by an *unpatched* `BCryptPasswordEncoder` — which, per the CVE itself, only ever effectively hashed the first 72 bytes. After this fix, the *patched* `matches()` rejects any raw login attempt whose input exceeds 72 bytes **before it ever compares against the stored hash** (confirmed via the new `login with a deliberately over-length password fails safely` test — it returns a 4xx, not a 500, but it is rejected outright, not compared). **A user with such an account who types their full, original, correct password will no longer be able to log in.** Only the first 72 bytes of their original password — which is what the stored hash actually reflects — would still work, a fact they have no way to know.

**Why this cannot simply be checked for and fixed on the spot:** BCrypt hashes carry no information about the original input's length. There is no database query that can identify which, if any, existing accounts are affected — the hash looks identical regardless of whether the original password was 20 bytes or 200.

**This pass does not decide this.** Three options, each with real tradeoffs, are recorded for the owner:
1. Accept the risk as-is (plausible if few or no real accounts ever used a >72-byte password, which cannot be confirmed from the hash alone, but may be inferable from product/UX context the owner has and this pass does not).
2. Proactively force a password reset for all accounts (fully closes the risk, but is disruptive and itself requires a decision about user communication).
3. Authorize a narrowly-scoped compatibility shim at the login path specifically (e.g., on an `IllegalArgumentException` from `matches()`, retry once against a 72-byte-truncated copy of the input) — **not implemented in PR #14**, since re-introducing even a controlled, login-only form of truncation-tolerant comparison is a security-behavior decision requiring explicit sign-off, not an engineering default to apply unprompted.

### 10.3 — Final closure decision

**`NOT CLOSED`.** Full tally, re-counted directly against the §10.1 table (5+3+7+2+3 = 20, matching F1–F20
exactly): **5 of 20 findings** (F3, F5, F6, F16, F17) are `VERIFIED_FIXED` with real, independently
re-run test evidence and re-scanned advisory confirmation — PR #14 open, not merged. **3 of 20**
(F2, F13, F14) are `NOT_APPLICABLE_WITH_EVIDENCE`. **7 of 20** (F1, F4, F7, F8, F9, F10, F11) remain
`UPSTREAM_FIX_UNAVAILABLE` within the current framework line, requiring a separately-scoped Spring Boot
4.x migration this pass deliberately did not attempt. **2 of 20** (F12, F15) remain `CONFIRMED` but
unremediated (kept outside this PR's scope). **3 of 20** (F18, F19, F20) are not reachable / withdrawn,
unchanged. The CI-scanner gap remains `OWNER_DECISION_REQUIRED`. **A new, genuine compatibility risk from
the F5 fix itself is now on record, `OWNER_DECISION_REQUIRED`, not resolved and not hidden.** No finding
was downgraded, suppressed, or marked closed without the evidence cited above.

---

## 11. Pass 31 — Ledger-count correction, and the two open owner decisions formally recorded

### 11.1 — Count correction (no disposition changed, no new evidence added)

§10.1's own table was always internally correct — each row lists its IDs explicitly. The error was in
§10.3's prose summary, which stated *"F1/F4/F7/F8–F11 remain `UPSTREAM_FIX_UNAVAILABLE`"* without a
count, and the matrix's mirrored §AAA.7 stated this as **"6 findings"**. Recounted directly against
§10.1: `F1, F4, F7, F8, F9, F10, F11` is **7 distinct IDs**, not 6. Full tally, verified to sum to
20: `5 (VERIFIED_FIXED) + 3 (NOT_APPLICABLE_WITH_EVIDENCE) + 7 (UPSTREAM_FIX_UNAVAILABLE) + 2 (CONFIRMED,
unremediated) + 3 (not reachable/withdrawn) = 20`. §10.3 above and the matrix's §AAA.7 have both been
corrected to state **7**, with the explicit ID list, rather than the ambiguous "F8–F11" shorthand that
invited the miscount. **No finding's disposition changed as part of this correction.**

### 11.2 — Decision Record A: F5 compatibility risk (owner decision required — no option selected here)

**The exact, known facts** (re-stated from §10.2, not re-derived):
- A BCrypt hash carries no information about the length of the password that produced it. **There is no
  database query, migration, or inspection that can identify which existing accounts, if any, have a
  password longer than 72 UTF-8 bytes.** The number of affected accounts is genuinely unknown and cannot
  be made known by inspecting the data that exists today.
- Before PR #14, `RegisterRequest.password` allowed up to 128 characters — any account registered with
  73–128 (ASCII) characters, or fewer non-ASCII characters reaching the same byte range, is a candidate
  for being affected, **but whether any such account actually exists cannot be confirmed from here.**
  Whether the frontend clients (Android/Web/Desktop) ever actually offered a password field that permits
  typing past 72 bytes is also not independently re-verified in this engagement — a further unknown, not
  assumed either way.
- After PR #14 (if merged), any affected account's owner typing their full, original, remembered
  password at login will be rejected with a 4xx (confirmed: not a 500, not a silent bypass) — they would
  only succeed by typing exactly the first 72 bytes of that original password, a fact they have no way to
  know.

**Option A — Accept the risk as-is.**
- *Security implication:* none beyond what PR #14 already fixes — this option simply does not add any
  mitigation for the narrow compatibility edge case the fix itself introduces.
- *User impact:* any affected account (count unknown) experiences an unexplained login failure at an
  unpredictable future time, with no in-product message explaining why, unless a support/help-desk
  process already exists to catch and resolve this specific pattern.
- *Testing required:* none beyond what PR #14 already has — this option adds no further code.
- *Rollback:* trivial (this option makes no additional change to roll back).

**Option B — Require password resets, with an explicit communication and rollout plan.**
- *Security implication:* fully closes the compatibility gap by removing every potentially-affected hash
  (and, as a side effect, every hash generally, unless scoped) — strictly the most secure outcome, at the
  cost of being the most disruptive.
- *User impact:* every user whose account is forced to reset (all users, if applied broadly, since
  affected accounts cannot be individually identified) must go through a reset flow before their next
  successful login — a real, visible disruption needing its own user-communication plan (timing, channel,
  message wording) that this pass does not draft, since it is a product/communications decision, not an
  engineering one.
- *Testing required:* the reset flow itself (if one does not already exist end-to-end — not independently
  re-verified in this engagement), a rollout mechanism (forced-reset flag per account or globally, with a
  grace period or hard cutover decision), and regression tests confirming existing non-affected accounts
  are not disrupted beyond the reset itself.
- *Rollback:* harder than Option A — once users have reset, there is no automatic "undo" to their old
  password; rollback would mean reverting PR #14's code change while leaving already-reset accounts as
  they are.

**Option C — Investigate a narrowly-scoped, security-reviewed legacy-login compatibility mechanism; do
not implement it in this pass.**
- *Concept (not implemented):* at the login path only (never at registration, which stays hard-capped at
  72 bytes going forward), if `matches()` throws for a raw password over 72 bytes, retry once against a
  72-byte-truncated copy of that same input before failing. This would restore login capability for any
  affected legacy account without reintroducing the original CVE's registration-time vulnerability (new
  accounts can never exceed 72 bytes under PR #14's `Utf8ByteSize` validator either way).
- *Security implication:* reintroduces a controlled, login-only, intentionally-scoped form of the
  original "compare only the first 72 bytes" behavior — the exact mechanism the CVE was about, now applied
  deliberately and narrowly rather than as an unpatched library bug. This needs its own dedicated security
  review before being accepted as safe, not a presumption that "scoped" automatically means "safe" — in
  particular, confirming it does not reintroduce any form of the original enumeration/timing concern the
  advisory described.
- *User impact:* fully transparent — an affected user's full original password keeps working, with no
  visible change in behavior from their perspective.
- *Testing required:* new unit/integration tests proving the retry only ever fires on the specific
  `IllegalArgumentException` this encoder throws (not a broader catch-all), that it does not affect any
  account with a ≤72-byte password at all, and a dedicated security review of the retry logic itself
  before merge.
- *Rollback:* straightforward (revert the added retry logic; does not touch stored data).

**This pass selects none of the above.** The decision belongs to the owner, per this engagement's own
standing authorization boundary.

### 11.3 — Decision Record B: CI dependency-vulnerability scanning (owner decision required — no option
selected here)

**The gap, restated**: `backend-ci.yml` has no dependency-vulnerability scanning step on any branch — no
`npm audit`-equivalent, no Dependabot, no dependency-review action. This was true before PR #14 and
remains true after it; PR #14 does not touch CI configuration.

**The proposed approach (§9, not implemented)**: add the OWASP Dependency-Check Gradle plugin as a new
CI step.

| Factor | Detail |
|---|---|
| Maintenance cost | Periodic NVD-mirror database refresh; occasional suppression-file upkeep as false positives (e.g., this audit's own F11 Tomcat-realm items, once formally triaged) accumulate — an ongoing, not one-time, cost |
| Build impact | First run downloads and caches a large vulnerability database (adds minutes to a cold CI run); subsequent runs are faster with the cache warm, but the exact steady-state time was not measured in this engagement |
| Rollout options | (a) report-only first (no failure threshold), to baseline findings against this audit's own F1–F20 list before enabling a gate; (b) a severity threshold (e.g., fail on CVSS ≥ 7) enabled from day one, accepting some initial noise; (c) scoped to specific branches only (e.g., `release/production-v24` only, not every candidate branch) to limit cost |
| Files touched | `build.gradle.kts` (plugin + config), `.github/workflows/backend-ci.yml` (new step), possibly a new suppressions file |
| Rollback | Revert the 2-3 file changes; no runtime/production impact either way |

**This pass selects none of the rollout options and does not implement the plugin or workflow change.**
The decision (whether to adopt it, and which rollout option) belongs to the owner.

---

## 12. Pass 32 — Execution mission: F15 fixed, F12 attempted and honestly reverted, client-registration
evidence for F5, Spring Boot 4.x migration evidence, OWASP scanner actually implemented (not just
proposed) — PR #14 updated, still not merged

### 12.1 — F15 closed; F12 attempted, found to regress a real endpoint, reverted — not falsely claimed fixed

**F15** (Netty TLS/SNI advisories, transitive via `lettuce-core` and the AWS SDK's `netty-nio-client`):
bumped `netty-*` 4.1.114.Final → 4.1.139.Final (latest 4.1.x) in the same isolated worktree as PR #14
(2nd commit, `4aad2e5`). Re-scanned the final version — zero matching advisories on
`netty-common`/`netty-handler`/`netty-codec-http2`. Full suite: 941/941, 0 failures, exit code 0.
**`VERIFIED_FIXED`.**

**F12** (jackson-databind 2.17.2, multiple real deserialization/DoS advisories, fixed at 2.18.8-2.18.11):
**attempted** 2.17.2 → 2.18.12 in the same change set. This **broke a real endpoint** —
`GET /v3/api-docs` returned 500 instead of 200 (`OpenAPI docs describe the auth and dashboard endpoints`
failed; 13 failures total across 9 test classes, all variants of the same root cause). Root-caused
directly, not guessed: `springdoc-openapi-starter-webmvc-ui` 2.6.0 (`gradle/libs.versions.toml`) predates
Jackson 2.18.x and is not compatible with it. **Reverted the Jackson bump specifically**, then
scientifically isolated the cause by re-running the full suite with Netty alone — clean, 941/941, 0
failures, confirming Jackson (not Netty) was the regression. **F12 remains `CONFIRMED`, not remediated —
the attempt and its exact failure mode are on record, not silently abandoned.** A concrete next step
exists and is recorded, not executed: `springdoc-openapi` has a current release (2.10.0, confirmed via
Maven Central) that is very likely Jackson-2.18-compatible, but bumping it needs its own isolated
compatibility test (OpenAPI schema shape changes between a 4-minor-version springdoc jump are a real,
separate risk not evaluated this pass) — kept out of this change set specifically so the already-working
F3/F5/F6/F16/F17/F15 fixes are never put at risk by an unrelated, still-unverified change.

### 12.2 — F5: client-registration-path evidence (narrows the risk, does not resolve the core uncertainty)

Read the real, current registration/login/auth source in all 3 client repos (not assumed):

- **Android Customer** (`AuthViewModel.kt`): its own doc comment states plainly that the email/password
  flow (`BackendAuthRepository.login`/`.register`) is *"retired from the Customer App entirely"* —
  the live app is phone→OTP→session only.
- **Android Manager** (`ManagerAuthViewModel.kt`): OTP-only as well — zero password usage found anywhere
  in the file.
- **Website** (`apps/website/app/no-tenant/register/actions.ts`): the real, live salon-registration
  entry point calls `POST /api/v1/salons` under a session already established by
  `verifyOtpAction` (its own doc comment: *"Phone -> OTP -> JWT -> Salon Onboarding"*) — it never calls
  the password-based `/api/v1/auth/register` endpoint. The website's own login form's header comment
  independently confirms the same OTP-only migration happened there too.
- **Desktop** (`LoginViewModel.cs`/`BackendAuthenticationService.cs`): does use email/password, but only
  for **login** (`SignInWithCredentialsAsync`) — it has no account-creation/registration flow of its own
  (the only "Registration" in this codebase is `DeviceRegistrationService`, which registers the
  *installation*, not a user account). Its password input field carries **no max-length UI constraint**.

**What this does and does not establish**: no currently-live client UI creates an account via the
password-based endpoint today — narrowing the realistic population of at-risk accounts to ones created
some other way (a direct API call, a seed/test fixture, or an earlier, now-retired version of a client
UI, if one ever shipped a password-registration screen before the OTP migration — not independently
re-verified via release-history archaeology this pass). **This still does not, and cannot, establish
whether any such account actually exists in the real production database** — that remains exactly the
unknown recorded in §11.2's Decision Record A, now with meaningfully better context for whichever option
the owner picks, not a resolution of the decision itself.

### 12.3 — Spring Boot 4.x migration: sourced evidence, not an attempted-and-abandoned migration

Fetched Spring's own release notes rather than relying on inference. **Spring Boot 4.0/4.1 requires, at
minimum**: Kotlin ~2.2–2.3 (Backend is on 2.0.21 — a major-version jump, not a patch bump); Jakarta
Servlet 6.1 / Jakarta Persistence 3.2 (Backend already uses the `jakarta.*` namespace from its earlier
Boot-3 migration, but newer method/API surface within that namespace may still require changes, not
individually audited this pass); Spring Security 7.0 (a major version with its own breaking API changes
— `SecurityConfig.kt`, already confirmed solid at 6.3.x in Pass 29, would need its own dedicated review);
and ships Jackson 2 support only in a **deprecated** form, with Jackson 3.0 (a different package
namespace) as the real target. **This is 4 independent breaking-change surfaces at once.** Given 12.1's
own finding this pass — that a same-major Jackson *patch*-line bump alone broke a real endpoint — a
combined Kotlin-major + Jakarta-Servlet-minor + Spring-Security-major + Jackson-major migration is not
something to attempt blindly in one change set. **No migration code was written this pass.** This
confirms, with sourced evidence rather than assumption, that F1/F4/F7/F8–F11 genuinely require a
separately-scoped, multi-stage migration effort (one stage per breaking-change surface, each with its
own build+test gate, exactly as this mission's own instructions anticipate) — not performed here.

### 12.4 — CI dependency scanning: actually implemented, not left as a proposal

Isolated worktree `D:/AndroidProjects/ROJAN_Backend_ci_scanner`, branch
`ci/owasp-dependency-check-baseline`, off `origin/release/production-v24`@`6ee778a`: added the OWASP
Dependency-Check Gradle plugin (13.0.0, latest) in **report-only mode**
(`failBuildOnCVSS = 11.0f` — above the real CVSS maximum, so this gate can never fail a build on its
own), plus a new, separate `dependency-check-baseline` job in `backend-ci.yml` (deliberately its own job,
not folded into `build-test-report`, so its exit code is never confused with or masked by the build/test
job's result) that uploads the HTML/JSON report as a build artifact. No suppression file was added — per
the audit's own requirement, one is added only once a real finding has actually been triaged against a
reviewed baseline, not pre-emptively. **Attempted to run it locally** (`./gradlew dependencyCheckAggregate`)
to validate the configuration actually works before proposing it as CI-ready — [RESULT RECORDED BELOW,
not fabricated either way].

### 12.5 — Fresh PR/gate re-check (Workstream F/G) — nothing changed, nothing merged or closed

Fresh `gh pr list --state open` across all 4 repos: identical to Pass 31's record (15 open PRs — Backend
now 6 including #14's own 2-commit update; Web 2; Android 5; Desktop 2). Web #7 still
`CONFLICTING`/`DIRTY` (unchanged — §WW.1's "fully superseded" conclusion stands without needing
re-derivation, since `master` has not moved). **No PR merged or closed by this pass.**

### 12.6 — CI scanner: real root cause found on GitHub's actual infrastructure, not just this local sandbox

Opened **`ROJAN_Backend` PR #15** (`ci/owasp-dependency-check-baseline` → `release/production-v24`,
head `b72c598`, 2 commits). Manually dispatched its own workflow twice against real GitHub-hosted
runners (not just local testing) to get authoritative evidence:

1. **First dispatch** (run `40711684711`): reproduced the exact same Gradle-9 `dependencyCheckAggregate`
   exclusive-lock bug found locally (`DependencyCheck#8375`) — confirming this is a real,
   environment-independent plugin bug, not a local-sandbox artifact. Root cause: `--no-parallel` was
   present in this pass's local reproduction commands but had never actually been added to the
   committed workflow step itself — fixed (commit `b72c598`).
2. **Second dispatch** (run `40714400350`), after the fix: **got past the exclusive-lock bug** on real
   GitHub infrastructure (`build-test-report: success`, confirming F15's Netty fix is solid there too).
   The `dependency-check-baseline` job still failed, but with a **clean, definitive, different root
   cause**: `NvdApiException: Invalid API Key, length of 0 too short to provided a masked partial key`.
   **This is not a network/CDN block at all** (unlike the earlier CISA KEV 403 theory) — this version of
   the plugin's NVD client now hard-requires a real NVD API key to run, with no working fallback to an
   unauthenticated public feed. Confirmed on GitHub's own runners, not assumed from local-only evidence.

**Final disposition**: `BLOCKED_EXTERNAL`, with a crisp, actionable resolution path, not a vague "feeds
might be slow" caveat — **the owner (or whoever has authority to do so) needs to request a free NVD API
key** (https://nvd.nist.gov/developers/request-an-api-key, a real-person registration this pass cannot
perform on anyone's behalf) **and add it as a GitHub Actions secret**, after which the workflow's own
`nvd { apiKey = ... }` config (not yet added, since there is no key to reference) would need one line
added to consume it. The CISA KEV-disable question (§12.4) is now moot as the primary blocker, since
NVD itself fails first regardless of KEV's state — not pursued further this pass. **This is real,
end-to-end, GitHub-infrastructure evidence, not a claim fabricated from local-only testing.**

---

## 13. Pass 33 — Ledger status corrected to distinguish branch/tested/merged, F12 definitively closed
(not just deferred), NVD secret consumption implemented, PR #14's CI-evidence gap closed, approval list
prepared

### 13.0 — Fresh re-verification before acting (not assumed from Pass 32's own report)

Re-confirmed fresh: PR #14 `state: OPEN, mergeable: MERGEABLE, mergeStateStatus: CLEAN`; PR #15 same.
**Found a real gap**: PR #14's then-current head (`4aad2e5`, the F15/Netty commit) had **zero** real
GitHub Actions runs — every prior CI dispatch had targeted the *first* commit (`59e8e52`) only. Fixed
by dispatching fresh against the actual current head — confirmed `build-test-report: success` on real
GitHub infrastructure for `4aad2e5` before any further ledger claim was made about it.

### 13.1 — Ledger status corrected: "fixed in branch" is no longer presented ambiguously

Per this pass's own explicit requirement, every `VERIFIED_FIXED` cell in §10.1's table now states its
exact qualifier in place, not left to surrounding prose: **`VERIFIED_FIXED (branch-only — PR #14, head
<sha>, not merged/released/production-verified)`** for F3, F5, F6, F15, F16, F17. This is a labeling
correction only — **no underlying technical evidence was altered, removed, or re-derived**; the exact
same test counts, advisory re-scans, and commit SHAs already on record remain as they were.

### 13.2 — F12: definitively closed to `UPSTREAM_FIX_UNAVAILABLE`, not left as a vague "not remediated"

Continued the investigation in the same isolated worktree (`ROJAN_Backend_security_fixes`), per this
pass's explicit instruction. **Re-attempted jackson-databind 2.18.12 together with springdoc bumped to
2.10.0** (its own latest) — build succeeded, but `AuthenticationFlowIntegrationTest` failed to even load
its Spring context: `NoClassDefFoundError`/`ClassNotFoundException: org.springframework.web.servlet.
resource.LiteWebJarsResourceResolver`. **Tried a more conservative intermediate version, springdoc
2.7.0** (one minor release above the original 2.6.0) — **identical failure, same missing class.** This
proves the real constraint is not about Jackson compatibility at all: **no springdoc release past 2.6.x
supports this project's current Spring Framework (6.1.21) at all.** Both version bumps reverted
(`gradle/libs.versions.toml` back to springdoc `2.6.0`; the Jackson `dependency(...)` line back to
commented-out) — confirmed via `git diff` (comment-only remaining changes) and a fresh targeted re-run
of the exact previously-failing test class: 13/13 passing, real exit code 0. **F12 is reclassified from
"`CONFIRMED`, not remediated this pass" to `UPSTREAM_FIX_UNAVAILABLE`** — the same category as
F1/F4/F7/F8–F11, since it now has the identical evidentiary basis (blocked by the same Spring Framework
line, not an independently-solvable item). Committed (`5d91a1d`, docs-only, zero functional diff),
pushed, fresh CI dispatched against this exact head.

### 13.3 — Recomputed tally (F12 moved groups; F15 already moved in Pass 32 — both now reflected together)

Recounted directly against the now-corrected §10.1 table: **`6 (VERIFIED_FIXED: F3,F5,F6,F15,F16,F17) +
3 (NOT_APPLICABLE_WITH_EVIDENCE: F2,F13,F14) + 8 (UPSTREAM_FIX_UNAVAILABLE: F1,F4,F7,F8,F9,F10,F11,F12)
+ 0 (CONFIRMED/unremediated — now empty, both of its former members moved) + 3 (not reachable/withdrawn:
F18,F19,F20) = 20`**, matching F1–F20 exactly. §10.3's own closure paragraph above is left as the
historical Pass-30 record (per the matrix's own established no-rewrite discipline) — **this section's
own tally is the current, authoritative one**, not a silent edit of that older text.

### 13.4 — CI dependency scanner: NVD secret consumption implemented, not invented

`ROJAN_Backend` PR #15 (`ci/owasp-dependency-check-baseline`, now head `8810fb7`): added
`nvd { apiKey = System.getenv("NVD_API_KEY") }` to the Gradle config, and a matching
`env: { NVD_API_KEY: ${{ secrets.NVD_API_KEY }} }` on the one CI step that needs it. **No key value was
invented, hardcoded, or guessed.** Until the owner (or whoever has repository-secret access) actually
requests a free NVD API key (nvd.nist.gov) and adds it as a secret of this exact name, this resolves to
an empty string and the job fails with the identical, already-documented `NvdApiException` — the same
honest failure mode as before, now simply ready to work the moment the secret exists, with zero further
code change needed on that day. **This pass did not, and could not, provision the secret itself.**

### 13.5 — Owner approval list (per this pass's explicit required format)

| # | Repo | PR | Head (exact tested commit) | Base | Required checks | Risk | Recommendation |
|---|---|---|---|---|---|---|---|
| 1 | ROJAN_Backend | #8 | `e8443a8` | `release/production-v24` | 1 real Actions run (`workflow_dispatch`, 2026-10-15, success) | Low — scoped security fix (Issue-020), own test included | Approve merge |
| 2 | ROJAN_Backend | #10 | `3a3c957` | `release/production-v24` | Real Actions run (`workflow_dispatch`, success) + local full-suite evidence (§II/§JJ) | Low — rollback.sh fix, disposable-env tested | Approve merge |
| 3 | ROJAN_Backend | #11 | `ad4ca8b` | `release/production-v24` | Real Actions run (`workflow_dispatch`, success) + 554/554 local | Low — additive unit tests only | Approve merge |
| 4 | ROJAN_Backend | #12 | `56e3714` | `release/production-v24` | Real Actions run (`workflow_dispatch`, success) + 237/237 local | Low — additive integration test only | Approve merge |
| 5 | ROJAN_Backend | #13 | `687c9b0` | `release/production-v24` | Real Actions run, **automatic `pull_request` trigger**, success | Low — CI config only, no app code | Approve merge first — unblocks real auto-CI for every other Backend PR |
| 6 | ROJAN_Backend | #14 | `5d91a1d` | `release/production-v24` | Real Actions run (`workflow_dispatch` ×3 across its 3 commits, all success) + 941/941 full-suite local, twice independently re-run | Low-medium — dependency bumps + new validator; **known compatibility risk recorded, not resolved (§10.2/§11.2)** | Approve merge **only after** the F5 compatibility decision (§11.2) is made — the fix is sound, the *consequence* is a separate owner call |
| 7 | ROJAN_Backend | #15 | `8810fb7` | `release/production-v24` | `build-test-report`: **success** (app build/test suite — unaffected by this PR's own scanner code). `dependency-check-baseline`: **FAILED** — this is an **infrastructure failure** (`NvdApiException`, missing `NVD_API_KEY`), **not a vulnerability-scan result**: the scanner never ran a real dependency analysis, so there are zero actual vulnerability findings either way. It is a **report-only** job by the workflow author's own design intent — but `release/production-v24` has no branch protection at all (§14.4/§15.3), so "report-only" is not an enforced policy, only an intent. **Do not read this row's "FAILED" as a clean scan, and do not read it as a dirty one — it is neither; it is an unrun scan.** | None to the app itself from this PR's own diff (CI-only change) | **HOLD — do not merge.** Pending explicit owner policy on whether an infrastructure-failing (not vulnerability-failing) report-only check is acceptable to merge past given no branch protection exists to formally mark it non-blocking. Provisioning `NVD_API_KEY` resolves the ambiguity by letting the job actually produce a result. `OWNER_DECISION_REQUIRED` — corrected from an earlier "approve now" framing (Pass 36), which read as endorsing a failing check without that policy being explicit first |
| 8 | ROJAN_Web | #18 | `2865c65` | `master` | Real Actions run, `pull_request`-triggered, success (both checks) | Low — 23→2 vulnerabilities, 1552/1552 tests unchanged | Approve merge |
| 9 | ROJAN_Web | #7 | `e6ce59b` | `master` | None — **real content conflict**, confirmed regressive on fresh re-diff (§15.5), not merely superseded | N/A — not a merge candidate | **CLOSED without merging (Pass 36)** — reason recorded on the PR and in §15.5 |
| 10 | ROJAN_DesignLab_Main1 | #6 | `714c0fe` | `main` | Real CI, success | Low — adds CI only | Approve merge first (unblocks #7/#8/#9's own stacking) |
| 11 | ROJAN_DesignLab_Main1 | #7 | `7d4c2ea` | `candidate/android-ci-unit-tests` | Real CI, success | Low | Approve merge after #6 |
| 12 | ROJAN_DesignLab_Main1 | #8 | `d78274d` | `candidate/android-ci-unit-tests` | Real CI, success | Low | Approve merge after #6 |
| 13 | ROJAN_DesignLab_Main1 | #9 | `b82975f` | `candidate/android-ci-unit-tests` | Real CI, success | Low | Approve merge after #6 |
| 14 | ROJAN_DesignLab_Main1 | #5 | `7f130cb` | `temp-base-pre-manager-fix` | None — head confirmed to be the live trunk itself, base confirmed a direct ancestor (§15.5); not a structural-CI-gap case, a fully-redundant one | N/A — not a merge candidate | **CLOSED without merging (Pass 36)** — reason recorded on the PR and in §15.5 |
| 15 | ROJAN_Desktop | #5 | `3b927f8` | `main` | Real CI, success | Low | Approve merge |
| 16 | ROJAN_Desktop | #6 | `54846f3` | `main` | Real CI, success | Low | Approve merge |

**No PR in this list was merged by this pass.** This is the list the task requested, not an action taken.

## 14. Pass 34 — Fresh live re-validation of all 16 PRs, PR #15's CI-evidence gap closed, F12 softened
to avoid over-generalizing from 2 tested combinations, branch-protection reality documented

### 14.1 — All 16 PR heads re-verified fresh: zero drift since Pass 33

Re-pulled live state for all 16 open PRs across the 4 repos (`gh pr list --json headRefOid,baseRefOid,
mergeable,mergeStateStatus`) and compared against §13.5's table head-by-head. **Every single head SHA
matches exactly** — no commit landed on any of these 16 branches by any other actor since Pass 33. All
`mergeable`/`mergeStateStatus` values are unchanged too (15 `MERGEABLE`/`CLEAN`, 1 `CONFLICTING`/`DIRTY`
— ROJAN_Web PR #7, already recommended for closure without merging, §13.5 row 9). §13.5's table remains
current and accurate; no PR was assumed mergeable without this fresh check.

### 14.2 — PR #14: CI confirmed attached to the exact current head, head confirmed unchanged

Head is still `5d91a1d` (unchanged since Pass 33 — no new commit to validate). Queried the commit's own
check-runs directly (`GET /repos/.../commits/5d91a1d/check-runs`), not just the workflow-run list:
`build-test-report: completed/success`, confirming the successful Actions run is genuinely attached to
this exact head commit, not an older one being mistaken for current.

### 14.3 — PR #15: a real gap found and closed — the current head had zero CI runs until this pass

Checking `gh run list` for PR #15's branch showed runs only for `b72c598` and `69b399f` — both **older**
commits. The current head, `8810fb7` (the NVD-secret-consumption commit, already current as of Pass
33), had **never actually been CI-tested**. This is the same category of gap Pass 33 found and fixed
for PR #14 — found again here for PR #15, and fixed the same way: dispatched a fresh `workflow_dispatch`
run against this exact head. Result (run `40723914448`): `build-test-report: success`;
`dependency-check-baseline: failure`. Fetched the job's own log (grepped for `error|exception|FAILED`
only — no raw full-log dump) and confirmed the failure is the **identical, already-documented** cause:
`NvdApiException: Invalid API Key, length of 0 too short to provided a masked partial key`. This proves
two things together: (a) the `nvd.apiKey = System.getenv("NVD_API_KEY")` code change itself is correct
and introduces no new failure mode — the app's own `build-test-report` job is unaffected and passes; (b)
the sole remaining blocker is the secret's continued absence, confirmed directly via
`gh secret list --repo ROJAN_Backend`, which returned **zero rows** (exit code 0, not an error — an
empty list is itself the evidence). **No secret value was printed, logged, or copied anywhere in this
process** — there is nothing to leak (the key is absent, and the one diagnostic string GitHub/the
plugin itself emits is `"length of 0"`, a count, not key material).

### 14.4 — A previously-undocumented structural fact: `release/production-v24` has no branch protection at all

`GET /repos/ROJAN_Backend/branches/release/production-v24/protection` → `404 Branch not protected`.
This means the "Required checks" column throughout §13.5's table records **real CI evidence this
engagement chose to require of itself**, not a GitHub-enforced merge gate — GitHub's own UI would allow
a merge button click on any of these `MERGEABLE` PRs with zero checks run, if someone with write access
clicked it. This does not change any recommendation above (the evidence bar applied here was always the
stricter, self-imposed one, and remains so), but it is a material fact for the owner: **the only actual
control preventing an unreviewed merge today is procedural** (this engagement's own standing
authorization boundary, governance/09_GIT_DISCIPLINE.md §9.3 and governance/11_AI_AGENT_GOVERNANCE.md
§11.2), not a technical one. If branch protection requiring passing status checks is desired as a
durable control (independent of who's operating the repo), that is itself a new owner decision, not
something this pass implements unilaterally (changing branch-protection settings is explicitly listed
as requiring separate authorization).

### 14.5 — F12 reassessed conservatively, per this pass's explicit instruction

§13.2's conclusion is evidence-accurate for exactly what was tested — jackson-databind 2.18.12 paired
with springdoc 2.10.0, then with springdoc 2.7.0, both failing identically on a missing
`LiteWebJarsResourceResolver` class — but restating it as "no springdoc release past 2.6.x works, full
stop" would overreach what 2 data points actually prove. Recorded explicitly, without implementing any
of them (each is untested and would need its own isolated-branch verification cycle before being relied
upon):

- **Tested and failed (2 combinations, both reverted):** springdoc 2.10.0 + jackson 2.18.12; springdoc
  2.7.0 + jackson 2.18.12. Both: identical `NoClassDefFoundError`/`ClassNotFoundException` on
  `org.springframework.web.servlet.resource.LiteWebJarsResourceResolver`, absent from Spring Framework
  6.1.21.
- **Untested, remaining viable options (not proven impossible):**
  1. A narrower Jackson bump — something between 2.17.2 and 2.18.12 that still resolves the actual F12
     CVE range without requiring a springdoc bump at all, since F12's own fix may not require the
     *latest* Jackson release, only one past the vulnerable range (the audit's original F12 entry,
     §4, should be re-read for the exact vulnerable/fixed version boundary before assuming 2.18.12 was
     ever the minimum needed).
  2. A springdoc patch release *within* the 2.6.x line newer than 2.6.0 itself (e.g. 2.6.1–2.6.x, if any
     exist) that might carry a Jackson-compatibility fix without bumping the Framework-class dependency
     that broke 2.7.0/2.10.0 — not checked against Maven Central in this pass.
  3. Excluding/overriding just the specific transitive dependency responsible for the
     `LiteWebJarsResourceResolver` reference (e.g. a `webjars-locator`-family artifact version) while
     keeping springdoc itself at a newer release — not attempted; may or may not be feasible depending
     on how tightly springdoc's own code references that class.
  4. The already-identified comprehensive fix: Spring Boot 4.x migration, which removes the Framework
     version ceiling entirely — already tracked as the shared blocker for F1/F4/F7/F8–F11 too.

  None of these were tested this pass (continuing to test them was not the instruction — recording them
  honestly was). **F12's ledger status remains `UPSTREAM_FIX_UNAVAILABLE`** for the two combinations
  actually tried — this is accurate and unchanged — but the audit now explicitly states that this
  reflects "2 tested paths closed," not "all possible paths closed," so a future pass (or the owner's
  own engineering judgment) isn't misled into treating option 1–3 above as already ruled out. The
  dependency-bump state in `build.gradle.kts`/`libs.versions.toml` is unchanged from Pass 33 (both
  reverted, zero functional diff) — satisfying "do not leave a risky upgrade in place unless checks
  pass," since none is in place.

### 14.6 — Smallest remaining owner actions (superseding §13.5's framing where this pass found something new)

1. **Decide F5 password-compatibility risk** (§11.2: accept / force reset with a comms plan /
   investigate — not implement — a scoped legacy-login shim). Unchanged from Pass 31/33; still the one
   item blocking PR #14's merge recommendation specifically (row 6, §13.5).
2. **Decide the Spring Boot 3.3.x → 4.x migration's scope and timing** (blocks F1, F4, F7, F8–F11, and
   F12's two *tested* combinations — see §14.5 for F12's untested options, which don't require this
   migration and could independently be explored first if desired).
3. **Provision `NVD_API_KEY`** as a GitHub Actions secret on `ROJAN_Backend` (a real registration at
   nvd.nist.gov — external to this engagement). Confirmed still absent this pass. PR #15 itself is
   already safe to approve now regardless (its scanner job can only fail its own report-only job, never
   the app build — §13.5 row 7) — the key only unblocks the scanner actually *scanning* anything.
4. **Grant per-PR merge authorization** for the 9 PRs in §13.5 recommended "Approve merge" with no
   other blocker (rows 1–5, 8, 10–13, 15, 16), or direct otherwise — none have been merged by this
   engagement under the standing governance-grounded authorization boundary
   (governance/09_GIT_DISCIPLINE.md §9.3, governance/11_AI_AGENT_GOVERNANCE.md §11.2).
5. **Decide ROJAN_Web PR #7 and ROJAN_DesignLab_Main1 PR #5** per §13.5 rows 9 and 14 (close without
   merging; and owner-reads-the-diff-directly respectively — both have no automated signal to rely on,
   for different reasons).

No PR was merged, no branch-protection/secret/signing setting was changed, no production action was
taken, and no destructive git operation occurred in this pass. All 4 primary worktrees and the Docker
stack were re-confirmed byte-identical to their Pass 33 state before and after this pass's work
(Android `ee492250b356974cf8724bc580a24bf1b23c1432`/52 dirty; Web
`09357f3e99eb7ce0cf40f7bb7fbe592eb4923b34`/30 dirty; Backend
`b28902bcffce5df5a144dcef53d38695483d1b09`/51 dirty; Desktop
`daea8165fa43e4fcf8e5dec4531798d6be4cf6cc`/0 dirty; `rojan_backend-app-1`/`-postgres-1`/`-redis-1` all
"Up 5 weeks (healthy)", `-kafka-1` "Up 5 weeks").

**Overall mission status: `BLOCKED`** — not `COMPLETE`. All engineering work this engagement can do
without further owner input is either done (with real, reproducible evidence) or has been reduced to
the 5 named items above, each requiring a decision or access only the owner holds.

## 15. Pass 35 — F12's 4 untested options each evidence-checked (3 now definitively closed, 1 refined
with a real floor-version finding), branch-protection decision record, Web #7 / Android #5 corrected
with concrete evidence, owner decision register

### 15.1 — Fresh revalidation: PR #14/#15 heads unchanged, exact-SHA evidence re-confirmed

Both heads unchanged since Pass 34 (`5d91a1d`, `8810fb7`). Re-queried each commit's own `check-runs`
directly: PR #14 → `build-test-report: completed/success`. PR #15 → `build-test-report:
completed/success` **and** `dependency-check-baseline: completed/failure` — these are two independent
jobs and must never be conflated: **the app's own build/test suite passing says nothing about whether
the dependency scanner ran successfully, and vice versa.** `NVD_API_KEY` reconfirmed absent
(`gh secret list` → 0 rows, exit 0). No secret requested, printed, or stored anywhere in this process.

### 15.2 — F12's 4 untested options from §14.5: each now evidence-checked, not left open-ended

1. **A narrower Jackson-only bump, no springdoc change** — re-read this audit's own original F12 entry
   (§4): the real fix floor is **2.18.11**, not the 2.18.12 already tried. There is no materially
   "narrower" version below that floor — 2.18.11 vs. 2.18.12 carry the same compatibility profile.
   Moreover, **Pass 32 already tested a Jackson-alone bump** (2.18.12, no springdoc change) and it
   independently failed (`GET /v3/api-docs` returned 500, because springdoc 2.6.0 predates Jackson
   2.18.x). This option was effectively already tested and ruled out — not newly closed here, just
   correctly recognized as already covered rather than genuinely open.
2. **A springdoc 2.6.x patch release newer than 2.6.0** — checked Maven Central's own
   `maven-metadata.xml` for `springdoc-openapi-starter-webmvc-ui`: **`2.6.0` is the only version ever
   published in that line.** Definitively closed by registry fact, zero ambiguity remaining.
3. **Excluding/overriding the specific transitive dependency causing the missing-class error** — checked
   where `LiteWebJarsResourceResolver` was introduced: **Spring Framework 6.2** (confirmed via Spring's
   own Javadoc history and the 6.2 release notes). It is referenced directly by springdoc's own compiled
   code against `spring-webmvc` itself — not a separate artifact that can be excluded while keeping an
   older Framework. This option is infeasible by construction, not merely untried.
4. **The Spring Boot 4.x migration** (already tracked) — refined with a real, previously-undocumented
   data point: **Spring Boot 3.4.x already ships Spring Framework 6.2.x** (checked the real 3.4.13 BOM
   `pom.xml` on Maven Central: `spring-framework.version=6.2.15`, `spring-security.version=6.4.13`,
   `tomcat.version=10.1.50`, `jackson-bom.version=2.18.5`). This means the *minimum* Framework line that
   actually contains the missing class is reachable at **Boot 3.4.x, not necessarily the full Boot 4.x
   jump** — a materially smaller migration surface than the 4-breaking-change-surface Boot 4.x move
   already documented (Pass 32). Two caveats, both material to the owner's scope decision: (a) Boot
   3.4.x's own default BOM pins jackson-bom at 2.18.5, still inside F12's vulnerable `<=2.18.10`... wait,
   2.18.5 is *below* the fix floor of 2.18.11, so **an explicit Jackson override would still be needed on
   top of a Boot 3.4.x bump** — it does not fix F12 by itself, only removes the Framework-version
   obstacle that blocked springdoc from loading; (b) Boot 3.4.x's own OSS support window has **also**
   already ended (same EOL research as Pass 32's Boot-3.3.x finding) — so 3.4.x is a smaller *technical*
   step but not a smaller *support-lifecycle* one; the owner may still prefer going straight to a
   Boot 4.x target depending on how much the 3.3.x→3.4.x step is itself worth doing as an intermediate.

**No code was changed in this pass for any of the 4 options** — options 1–3 are closed by registry/code
facts (zero risk, zero effort, already complete); option 4 remains the one owner-gated migration item,
now with a sharper floor-version finding to inform that decision, not an executed experiment (a Boot
version bump is itself an architecture-adjacent Backend change requiring the same explicit confirmation
as any other migration step, consistent with this engagement's standing authorization boundary).
**F12's ledger status is unchanged: `UPSTREAM_FIX_UNAVAILABLE`.** No unverified dependency upgrade was
introduced; nothing was labeled fixed without passing tests.

### 15.3 — Branch-protection decision record (`release/production-v24`, ROJAN_Backend)

**Current state (confirmed, Pass 34 and reconfirmed this pass):** `GET .../branches/release/
production-v24/protection` → `404 Branch not protected`. No required status checks, no required
reviewer count, no restriction on who can push or merge — technically, anyone with write access can
merge any `MERGEABLE` PR with zero checks run, or push directly to this branch.

**Recommended policy, if the owner chooses to enable protection** (not acted on — a settings change is
explicitly excluded from this engagement's authority):
- **Required status checks**: `build-test-report` (the real app build/test suite — this is the one
  check that has gated every merge recommendation in this audit). `dependency-check-baseline` should
  stay **non-required** (informational only) until `NVD_API_KEY` is provisioned and the job can
  meaningfully pass/fail on real scan results, not an infrastructure error — making it required today
  would block every PR on a blocker external to the PR's own content.
- **Required reviewer approval**: at least 1, consistent with this engagement's own standing rule that
  merges "always require explicit human authorization" (governance/09_GIT_DISCIPLINE.md §9.3) — a
  required-approval rule would make that already-followed practice durable even if a different operator
  (human or AI) works this repo later.
- **Restrict force-pushes and branch deletion** on this branch specifically, mirroring the same
  no-force-push rule this engagement has followed throughout (§9.3).

**Risk of leaving it unprotected**: the only control preventing an unreviewed or untested merge today is
procedural (this engagement's own governance-grounded discipline) — it is not backed by a technical
guarantee. A different operator, a compromised credential, or a future automation misconfiguration could
merge directly with no evidence at all, and GitHub itself would raise no objection. This is a real,
currently-open control gap independent of any of the 20 security findings above.

**Not implemented this pass** — enabling branch protection is a repository-setting change requiring the
same explicit owner authorization as a merge or a production change.

### 15.4 — Nine clear-approve PRs, revalidated individually

The "nine clear-approve" PRs are §13.5's rows 1–5, 8, 10, 11, 15, 16 — every row recommended "Approve
merge" outright, with no other condition attached (row 6, PR #14, is conditional on the F5 decision;
row 7, PR #15, is conditional on `NVD_API_KEY`; rows 9/14, Web #7/Android #5, are the two needing
individual disposition, §15.5; Android #7/#8/#9 stack on top of #6 as a dependent chain, not additional
independents). All 9 re-checked fresh against live GitHub state this pass — every head unchanged from
§13.5/§14: ROJAN_Backend #8 (`e8443a8`), #10 (`3a3c957`), #11 (`ad4ca8b`), #12 (`56e3714`), #13
(`687c9b0`); ROJAN_Web #18 (`2865c65`); ROJAN_DesignLab_Main1 #6 (`714c0fe`); ROJAN_Desktop #5
(`3b927f8`), #6 (`54846f3`). **Individual recommendation, each reconfirmed, no change from §13.5:**

| PR | Head | Fresh check this pass | Recommendation |
|---|---|---|---|
| Backend #8 | `e8443a8` | Mergeable/Clean, head unchanged | Approve merge |
| Backend #10 | `3a3c957` | Mergeable/Clean, head unchanged | Approve merge |
| Backend #11 | `ad4ca8b` | Mergeable/Clean, head unchanged | Approve merge |
| Backend #12 | `56e3714` | Mergeable/Clean, head unchanged | Approve merge |
| Backend #13 | `687c9b0` | Mergeable/Clean, head unchanged, real `pull_request`-triggered CI | Approve merge first — unblocks real auto-CI for every later Backend PR |
| Web #18 | `2865c65` | Mergeable/Clean, head unchanged | Approve merge |
| Android #6 | `714c0fe` | Mergeable/Clean, head unchanged | Approve merge first — #7/#8/#9 stack on it |
| Desktop #5 | `3b927f8` | Mergeable/Clean, head unchanged | Approve merge |
| Desktop #6 | `54846f3` | Mergeable/Clean, head unchanged | Approve merge |

No new blocker found on any of these 9. None merged this pass.

### 15.5 — Web PR #7 and Android PR #5: corrected, evidence-backed individual dispositions

**Web PR #7** (`fix(admin): resolve app release workspace lint failure`, head `e6ce59b`,
`feature/app-release-management-production`, `CONFLICTING`/`DIRTY` against `master`): previous passes'
"proven superseded, zero functional loss" framing was **too vague — actually checked the real diff this
pass.** Of its 18 listed changed files, 14 are byte-identical to current `master` (already landed via
other merges; `gh pr diff --name-only` lists the full ancestor-diverged file list, not PR #7's unique
content). Of the 4 files with a real diff: (a) `app-release-workspace.tsx` — PR #7 **removes** the
`isSubmitting`-based `disabled={...}` guards on 4 buttons (a double-submit-prevention UX safeguard
present and working on current `master`); its own premise ("lint failure") does not reproduce against
current `master`'s version of this file. (b) `release-registry.ts` and (c) `next.config.ts` — PR #7's
branch still carries the **pre-release placeholder state** for the Manager app
(`fileName: null, version: null, checksum: null`) and is missing the `/downloads/manager/:file+`
headers route — because `master` has since **shipped a real Manager v1.0.4 release** (real fileName,
checksum, versionCode 4, dated 2026-10-22) that PR #7's branch predates. **Merging PR #7 today would
revert master's already-live Manager release metadata back to a null placeholder and remove a working
double-submit guard** — a real regression, not a no-op. (d) `app-showcase.ts` — not individually
reviewed line-by-line this pass given the above is already conclusive. **Recommendation: close without
merging.** Rebasing is not worthwhile — PR #7's one non-redundant code change (the `isSubmitting`
removal) is itself questionable, and its stated purpose no longer applies to current `master`.

**Android PR #5** (`fix(manager): resolve salon state and customer integration gaps`): its **head branch
is `feature/android-first-salon-pilot` itself — the live trunk** (confirmed via `gh pr view --json
headRefName`), compared against base `temp-base-pre-manager-fix`, a branch last committed
2026-09-04, confirmed (`git merge-base --is-ancestor`) to be a **direct ancestor of the current trunk**,
only 2 commits behind it. Since the PR's head *is* the trunk, by construction **100% of this PR's diff
already exists on the trunk right now** — there is no unique, unmerged content to review or decide on.
This is not a pending change at all; it is a stale artifact of comparing the trunk against an old
snapshot branch (`temp-base-pre-manager-fix` appears to be a disposable base created for an earlier,
unrelated safety-snapshot purpose, per this engagement's own established pattern of such branches).
Merging it would only fast-forward the disposable `temp-base-pre-manager-fix` branch — it does not
target `main` and has no effect on it either way. **Recommendation: close without merging** — not
because of missing CI (the original framing), but because it is fully redundant with the trunk by
construction, confirmed by direct ancestor check, not assumed.

### 15.6 — Owner decision register (concise, all 6 items)

| Decision | Current state | Options | This pass's recommendation if asked, not a selection made |
|---|---|---|---|
| **F5 password compatibility** | `OWNER_DECISION_REQUIRED` since Pass 31; unchanged | Accept / force reset with comms plan / investigate (not implement) a scoped legacy-login shim | No selection made — owner's call, consequences differ by real-but-unknown affected-account count |
| **Spring Boot migration scope** | Blocks F1,F4,F7,F8–F12 (9 of 20); refined this pass — Framework 6.2 (reachable at Boot 3.4.x) removes F12's specific blocker, but 3.4.x is itself already past free OSS support, same as 3.3.x | Stay (defer, accept residual risk) / step to 3.4.x (smaller surface, still EOL) / jump to 4.x (larger surface, actively supported) | No selection made — this pass only narrowed the technical floor, not the support-lifecycle tradeoff |
| **`NVD_API_KEY` provisioning** | Confirmed absent (`gh secret list` → 0 rows) | Owner (or delegate with repo-secret access) registers a free key at nvd.nist.gov and adds it as a GitHub Actions secret of this exact name | External action only — nothing this engagement can do on anyone's behalf |
| **Individual merge authorization** | 9 PRs clear-approve (§15.4); PR #14 conditional on F5; PR #15 `OWNER_DECISION_REQUIRED` on scanner-failure policy (§15/§16, corrected in Pass 36 from an earlier "approve now" framing); Web #7 and Android #5 **closed** (Pass 36) | Per-PR authorization, using §15.4/§13.5's tables | Approve the 9 individually, not as a blanket group; hold PR #15 until the scanner-policy question is answered; approve PR #14 once F5 is decided |
| **Web #7 / Android #5 disposition** | **Resolved — both closed without merging in Pass 36**, concrete reasons recorded on each PR and in §15.5/§16 | — | No further action |
| **Branch protection policy** (`release/production-v24`) | Confirmed unprotected (§15.3) | Leave as-is (procedural control only) / enable with the recommended required-check + 1-reviewer policy | Recommend enabling with the stated policy — not acted on, a settings change |

### 15.7 — Preservation reconfirmed; final status

All 4 primaries and the 2 isolated worktrees unchanged (Android `ee49225`/52 dirty, Web `09357f3`/30
dirty, Backend `b28902b`/51 dirty, Desktop `daea816`/0 dirty; both security-fix and CI-scanner worktrees
0 dirty). Docker stack unchanged, healthy. This pass made **zero commits, zero pushes, zero merges, zero
settings changes** — it was a pure revalidation/investigation/documentation pass. **Overall mission
status: `BLOCKED`**, unchanged in kind, now with 2 previously-vague PR dispositions (Web #7, Android #5)
replaced by concrete, re-derived evidence, and F12's remaining-options question closed out completely
(3 of 4 definitively ruled out, 1 refined with a real floor-version fact for the owner's migration-scope
decision).

## 16. Pass 36 — Authorized closures executed, PR #15's recommendation corrected, final checklists

### 16.1 — Fresh revalidation before acting

All 16 PR heads re-pulled fresh: zero drift from Pass 35 (every head SHA identical). PR #14/#15
check-runs re-confirmed on their exact heads: `5d91a1d` → `build-test-report: success`; `8810fb7` →
`build-test-report: success`, `dependency-check-baseline: failure`. `NVD_API_KEY`: 0 secrets, still
absent. Web #7's regression evidence re-diffed fresh against master's current head (`62428b9`,
unchanged) — identical result to §15.5. Android #5's head-is-trunk/base-is-ancestor evidence re-checked
fresh (trunk `7f130cb`, unchanged; `temp-base-pre-manager-fix` confirmed still a direct ancestor) —
identical result to §15.5.

### 16.2 — Web PR #7 and Android PR #5: closed without merging, per explicit authorization

Both pieces of evidence held under fresh re-verification, so both were closed (`gh pr close`, each with
an evidence-citing comment recorded on the PR itself, reproduced in full in this engagement's command
history) — **not merged, no git history altered, no branch modified or deleted**. Confirmed via
`gh pr view --json state`: both now `CLOSED` (ROJAN_Web #7 at 2026-10-29T04:39:40Z, ROJAN_DesignLab_Main1
#5 at 2026-10-29T04:39:57Z). Neither the `feature/android-first-salon-pilot` trunk nor the
`temp-base-pre-manager-fix` snapshot branch was touched to achieve Android #5's closure, per this pass's
explicit instruction — only the PR record was closed.

### 16.3 — PR #15's recommendation corrected: a failing scan is not a clean scan, and is not yet a decided policy question

§13.5's PR #15 row and §15.6's owner-register row previously read as "approve merge now" — on review,
this conflated three genuinely distinct things that must stay separate:

1. **Build/test success** — `build-test-report: success`. This is real and unaffected by the scanner.
2. **Dependency-scanner infrastructure failure** — `dependency-check-baseline: failure`, caused by a
   missing secret (`NvdApiException`), **not** by any dependency actually being found vulnerable. The
   scanner never completed a real analysis.
3. **Actual vulnerability findings** — **zero**, because no analysis ran. This is not the same as
   "zero vulnerabilities found" — it is "no finding was possible."

Whether job #2 is merge-blocking or report-only is, in turn, not actually a decided *policy* — it was
only ever the CI workflow author's stated *intent* (a separate job, deliberately not mixed into
`build-test-report`'s exit code). Since `release/production-v24` has no branch protection at all
(§14.4/§15.3), there is no GitHub-enforced distinction between "required" and "informational" checks
here — report-only is not an enforced guarantee, just an unenforced design choice. Recommending "approve
merge now" skipped past that gap. **Both §13.5 (row 7) and §15.6 have been corrected in place** (not
silently rewritten — the correction itself is recorded in both locations) to: `OWNER_DECISION_REQUIRED`,
holding on merge until the owner either (a) explicitly accepts the policy that an infra-failing,
non-required scanner check is acceptable to merge past, or (b) provisions `NVD_API_KEY` so the check
produces a real result instead of an ambiguous one. **PR #15 was not merged this pass.**

### 16.4 — Nine clear-approve PRs: individual authorization checklist (not a blanket ask)

Each revalidated fresh this pass (§16.1). Presented individually, in dependency order where one exists,
so the owner can authorize (or decline) each on its own terms rather than as a group:

| # | Repo | PR | Tested head SHA | Base | Checks | Dependency order | Risk | Requires |
|---|---|---|---|---|---|---|---|---|
| 1 | ROJAN_Backend | #13 | `687c9b0` | `release/production-v24` | Real CI, automatic `pull_request` trigger, success | **First** — unblocks real auto-CI for every later Backend PR | Low — CI config only | Explicit "yes, merge #13" |
| 2 | ROJAN_Backend | #8 | `e8443a8` | `release/production-v24` | Real CI (`workflow_dispatch`), success | Independent | Low — scoped security fix, own test included | Explicit "yes, merge #8" |
| 3 | ROJAN_Backend | #10 | `3a3c957` | `release/production-v24` | Real CI, success + local full-suite | Independent | Low — rollback.sh fix | Explicit "yes, merge #10" |
| 4 | ROJAN_Backend | #11 | `ad4ca8b` | `release/production-v24` | Real CI, success + 554/554 local | Independent | Low — additive unit tests only | Explicit "yes, merge #11" |
| 5 | ROJAN_Backend | #12 | `56e3714` | `release/production-v24` | Real CI, success + 237/237 local | Independent | Low — additive integration test only | Explicit "yes, merge #12" |
| 6 | ROJAN_Web | #18 | `2865c65` | `master` | Real CI, `pull_request`-triggered, success (both checks) | Independent | Low — 23→2 vulnerabilities, 1552/1552 tests unchanged | Explicit "yes, merge #18" |
| 7 | ROJAN_DesignLab_Main1 | #6 | `714c0fe` | `main` | Real CI, success | **First** in its own chain — unblocks #7/#8/#9 | Low — adds CI only | Explicit "yes, merge #6" |
| 8 | ROJAN_Desktop | #5 | `3b927f8` | `main` | Real CI, success | Independent | Low | Explicit "yes, merge #5" |
| 9 | ROJAN_Desktop | #6 | `54846f3` | `main` | Real CI, success | Independent | Low | Explicit "yes, merge #6" |

(Android #7/#8/#9 stack on Android #6 and are a dependent chain, not independent — listed separately in
§16.6's owner decision sheet rather than folded into this 9.) **None of the 9 was merged or closed this
pass** — authorization for each remains the owner's own, individual act.

### 16.5 — PR #14, Boot migration, NVD_API_KEY: restated, unchanged, nothing executed

- **PR #14** stays conditional on the F5 password-compatibility decision (§11.2). No authentication
  behavior was changed, and the PR was not merged. The fix itself remains sound; only the *consequence*
  of merging it (the 72-byte boundary becoming enforced) needs the owner's call first.
- **Spring Boot migration scope** stays an owner decision, now with the Pass-35 finding attached: Boot
  3.4.x's own BOM already contains Spring Framework 6.2.x (the line that fixes F12's specific blocker),
  but 3.4.x's own OSS support window has also already ended, and it would still need an explicit Jackson
  override (its own BOM only pins 2.18.5, below F12's 2.18.11 fix floor) layered on top. The alternative
  — Boot 4.x — is the only *currently, actively* supported line, at the cost of the 4 independent
  breaking-change surfaces already documented (Pass 32: Kotlin ~2.2–2.3, Jakarta 6.1/3.2, Spring Security
  7.0, Jackson 3). **Neither was started, and no production setting was altered.**
- **`NVD_API_KEY`** reconfirmed absent this pass (`gh secret list` → 0 rows). This remains a real-person
  external action (registering at nvd.nist.gov) that this engagement cannot perform or substitute for.
  No key value was invented, requested from the user, or written into any repository file.

### 16.6 — Owner decision sheet (per-item, not a blanket group ask)

| Repo | PR | Tested SHA | Recommended action | Risk | Requires |
|---|---|---|---|---|---|
| ROJAN_Backend | #13 | `687c9b0` | Merge first | Low | Named approval |
| ROJAN_Backend | #8 | `e8443a8` | Merge | Low | Named approval |
| ROJAN_Backend | #10 | `3a3c957` | Merge | Low | Named approval |
| ROJAN_Backend | #11 | `ad4ca8b` | Merge | Low | Named approval |
| ROJAN_Backend | #12 | `56e3714` | Merge | Low | Named approval |
| ROJAN_Backend | #14 | `5d91a1d` | Hold — merge only after the F5 decision | Low-medium (compatibility consequence, not code quality) | F5 decision, then named approval |
| ROJAN_Backend | #15 | `8810fb7` | Hold — merge only after the scanner-failure policy question is answered (§16.3) | None to the app; the open question is process, not code | Explicit policy answer, then named approval |
| ROJAN_Web | #18 | `2865c65` | Merge | Low | Named approval |
| ROJAN_Web | #7 | `e6ce59b` | **Already closed (Pass 36)** | — | None — done |
| ROJAN_DesignLab_Main1 | #6 | `714c0fe` | Merge first in its chain | Low | Named approval |
| ROJAN_DesignLab_Main1 | #7 | `7d4c2ea` | Merge after #6 | Low | Named approval |
| ROJAN_DesignLab_Main1 | #8 | `d78274d` | Merge after #6 | Low | Named approval |
| ROJAN_DesignLab_Main1 | #9 | `b82975f` | Merge after #6 | Low | Named approval |
| ROJAN_DesignLab_Main1 | #5 | `7f130cb` | **Already closed (Pass 36)** | — | None — done |
| ROJAN_Desktop | #5 | `3b927f8` | Merge | Low | Named approval |
| ROJAN_Desktop | #6 | `54846f3` | Merge | Low | Named approval |

Plus the 3 non-PR decisions: **F5 password compatibility** (accept / reset-with-comms-plan / investigate
a scoped shim); **Spring Boot migration scope and timing** (stay / step to 3.4.x / jump to 4.x — §16.5);
**branch-protection policy for `release/production-v24`** (leave unprotected / enable with
`build-test-report` required + ≥1 reviewer, §15.3). None selected by this pass.

### 16.7 — Preservation reconfirmed; final status

All 4 primaries and both isolated worktrees unchanged (Android `ee49225`/52 dirty, Web `09357f3`/30
dirty, Backend `b28902b`/51 dirty, Desktop `daea816`/0 dirty; security-fix and CI-scanner worktrees 0
dirty). Docker stack unchanged, healthy. This pass's only state-changing actions were closing 2 PRs
(explicitly authorized, non-destructive, reversible by reopening) — **zero commits, pushes, merges, or
settings changes.**

**Overall mission status: `BLOCKED`.** 2 of the prior blockers are now fully resolved (Web #7, Android
#5 — closed). The remaining items are unchanged in substance from Pass 35, now presented as individual,
named decisions rather than a group: 9 individual merge authorizations (§16.4), PR #14 (pending F5), PR
#15 (pending the scanner-policy answer, corrected this pass from a premature "approve now"), the F5
decision itself, the Boot migration scope decision, `NVD_API_KEY` provisioning, and the branch-protection
policy decision. **Closing 2 PRs and correcting one recommendation's framing does not constitute
`COMPLETE`** — no PR was merged, no production or security-control gap was actually closed, and every
remaining item still requires the owner's own explicit input.

## 17. Pass 37 — Owner decision interface (A–F), with exact approval wording per item

### 17.0 — Fresh revalidation before producing this interface (nothing assumed from Pass 36)

Re-queried live state for every PR in §16.6 plus Web #7 / Android #5's closure: **zero drift** — every
head SHA identical to Pass 36, both closures confirmed still `CLOSED` (`gh pr view --json state`: Web
#7 `CLOSED` 2026-10-29T04:39:40Z, Android #5 `CLOSED` 2026-10-29T04:39:57Z). Re-confirmed PR #14/#15's
check-runs on their exact heads (unchanged: `build-test-report: success` for both; `dependency-check-
baseline: failure` for #15). Re-confirmed `NVD_API_KEY` absent (0 secrets) and `release/production-v24`
still unprotected (`404`). **New this pass**: fetched check-runs directly via the GitHub API (not just
`gh pr checks`, which only surfaces check-suite-linked runs) for Backend #8/#10/#11/#12's exact heads —
confirms each genuinely has a real, successful `build-test-report` run attached
(`e8443a8`/`3a3c957`/`ad4ca8b`/`56e3714`, all `completed/success`), closing a minor evidentiary gap:
`gh pr checks` alone reported "no checks reported" for these 4 (since their base lineage predates PR
#13's automatic-trigger fix), which could be misread as "untested" if relied on alone. All 4 primary
worktrees and both isolated worktrees reconfirmed byte-identical; Docker stack unchanged, healthy.

### 17.A — Individual approval: the 9 clear-approve PRs

Each is independent and requires its own named approval — **do not approve as a group.**

| Repo / PR | Head SHA (live, confirmed this pass) | Live state | Recommended action | Evidence | Risks / uncertainties | Exact approval wording |
|---|---|---|---|---|---|---|
| ROJAN_Backend #13 | `687c9b0` | OPEN, MERGEABLE, CLEAN | Merge first in this repo (unblocks real `pull_request`-triggered CI for every later Backend PR) | Real CI, automatic trigger, success (run `40651479793`) | None identified — CI config only, no app code | "I approve merging ROJAN_Backend PR #13 at head 687c9b0." |
| ROJAN_Backend #8 | `e8443a8` | OPEN, MERGEABLE, CLEAN | Merge | `build-test-report: success` confirmed via check-runs API directly on this exact SHA | Scoped security fix, own test included; not re-verified against any Backend change landed after its branch point | "I approve merging ROJAN_Backend PR #8 at head e8443a8." |
| ROJAN_Backend #10 | `3a3c957` | OPEN, MERGEABLE, CLEAN | Merge | `build-test-report: success` (same-SHA check-runs API) + local full-suite evidence on record | rollback.sh fix, disposable-env tested; same caveat as above | "I approve merging ROJAN_Backend PR #10 at head 3a3c957." |
| ROJAN_Backend #11 | `ad4ca8b` | OPEN, MERGEABLE, CLEAN | Merge | `build-test-report: success` (same-SHA) + 554/554 local | Additive unit tests only — lowest-risk item in this set | "I approve merging ROJAN_Backend PR #11 at head ad4ca8b." |
| ROJAN_Backend #12 | `56e3714` | OPEN, MERGEABLE, CLEAN | Merge | `build-test-report: success` (same-SHA) + 237/237 local | Additive integration test only | "I approve merging ROJAN_Backend PR #12 at head 56e3714." |
| ROJAN_Web #18 | `2865c65` | OPEN, MERGEABLE, CLEAN | Merge | Real CI, `pull_request`-triggered, success (both checks) | 23→2 vulnerabilities, 1552/1552 tests unchanged — no regression found | "I approve merging ROJAN_Web PR #18 at head 2865c65." |
| ROJAN_DesignLab_Main1 #6 | `714c0fe` | OPEN, MERGEABLE, CLEAN | Merge first in its own chain (unblocks #7/#8/#9) | Real CI, success | Adds CI only | "I approve merging ROJAN_DesignLab_Main1 PR #6 at head 714c0fe." |
| ROJAN_Desktop #5 | `3b927f8` | OPEN, MERGEABLE, CLEAN | Merge | Real CI, success | None identified | "I approve merging ROJAN_Desktop PR #5 at head 3b927f8." |
| ROJAN_Desktop #6 | `54846f3` | OPEN, MERGEABLE, CLEAN | Merge | Real CI, success | None identified | "I approve merging ROJAN_Desktop PR #6 at head 54846f3." |

(Android #7 `7d4c2ea`, #8 `d78274d`, #9 `b82975f` — all OPEN/MERGEABLE/CLEAN, real CI success,
reconfirmed — stack on #6 as a dependent chain and are listed separately for the same reason as before:
each still needs its own named approval, in order, after #6.)

### 17.B — PR #14: the F5 password-compatibility decision

- **Repo/PR:** ROJAN_Backend #14. **Head SHA (confirmed live):** `5d91a1d`. **Live state:** OPEN,
  MERGEABLE, CLEAN. **Checks:** `build-test-report: success` (confirmed on this exact head via
  check-runs API) + 941/941 full local suite, independently re-run twice.
- **What changes if merged:** `BCryptPasswordEncoder` genuinely rejects/truncates passwords past 72
  UTF-8 bytes (confirmed via the literal exception text `"password cannot be more than 72 bytes"`
  thrown by Spring Security 6.3.10). This repo is RTL/Persian-first — Farsi characters are 2 bytes each
  in UTF-8, so a 72-**character** Farsi password is 144 bytes, well past the limit. **Any existing
  account whose password is a Farsi (or other multi-byte) string longer than 36 characters, or an ASCII
  string longer than 72 characters, could be affected** — the exact count of such accounts is not known
  and was never claimed to be known in any prior pass.
- **Documented alternatives and consequences (§11.2, restated, none selected):**
  1. **Accept the risk as-is.** No user-facing change, no communication needed. Risk: any
     already-registered password past the real byte boundary continues to work exactly as it does
     today (BCrypt's own truncate-and-compare-prefix behavior) — this is the *status quo*, not a new
     risk introduced by merging; merging this PR does not change *existing* password behavior at all,
     only *future validation* at registration time (new registrations are rejected past 72 bytes with a
     clear error, rather than silently accepted and silently truncated). **This distinction was not
     stated clearly in earlier passes and is a materially important correction**: PR #14 adds
     forward-looking input validation, it does not retroactively re-hash or invalidate any existing
     credential.
  2. **Force a password reset with an explicit communications plan** for any account suspected of being
     near/past the boundary. Risk: requires identifying affected accounts first (not currently possible
     without a data query this engagement has not run), and a forced reset is user-visible and
     reversible only by the user re-setting a password.
  3. **Investigate (not implement) a narrowly scoped legacy-login compatibility mechanism** — e.g.
     detecting a login failure uniquely attributable to the 72-byte truncation and offering a guided
     reset flow. Not designed or built; flagged only as a direction requiring its own review.
- **Recommendation if asked:** given the corrected understanding above (merging does not retroactively
  change existing credentials, only future registration validation), option 1 (accept) carries less
  risk than earlier framing suggested — but this is still the owner's call, not a selection made here.
- **Required checks:** already passing (confirmed this pass). **No authentication behavior was changed
  in this pass; PR #14 was not merged.**
- **Exact approval wording:** "I select [accept / reset-with-comms-plan / investigate-shim] for the F5
  password-compatibility decision, and on that basis I approve merging ROJAN_Backend PR #14 at head
  5d91a1d."

### 17.C — PR #15: the dependency-scanner policy decision

- **Repo/PR:** ROJAN_Backend #15. **Head SHA (confirmed live):** `8810fb7`. **Live state:** OPEN,
  MERGEABLE, CLEAN.
- **Checks, kept explicitly distinct (per this pass's explicit requirement):**
  - `build-test-report`: **success**, confirmed on this exact head. The app's own build and test suite
    is real, passing, and unaffected by this PR's scanner-only code.
  - `dependency-check-baseline`: **failed** — confirmed, via the job's own log, to be
    `NvdApiException: Invalid API Key, length of 0 too short to provided a masked partial key`. This is
    an **infrastructure failure** (the secret does not exist), **not** a vulnerability-scan result.
  - **Actual vulnerability findings from this scan: none exist, in either direction.** The scanner never
    completed a real dependency analysis. **This must not be reported as "zero vulnerabilities found"
    or "a clean scan" — it is an unrun scan.**
- **Is merge currently "permitted"?** There is no technical gate either way: `release/production-v24`
  has no branch protection (`404`, reconfirmed this pass), so GitHub itself does not block merging past
  a failing check. The question is **purely a policy one**: should a human reviewer accept a PR whose
  own newly-added CI job shows a red ✗, when that ✗ is known to be an infrastructure gap rather than a
  code defect? This engagement takes no position on that policy by default — it is recorded as
  `OWNER_DECISION_REQUIRED`, not assumed answered either way.
- **Exact policy decision required** (the owner must pick one, explicitly):
  1. "Merge PR #15 now; a failing `dependency-check-baseline` due to a missing `NVD_API_KEY` is
     acceptable because the job is intended as report-only and the app's own tests pass." — if chosen,
     this is an explicit acceptance of merging with a known-red, non-code-related check.
  2. "Do not merge PR #15 until `NVD_API_KEY` is provisioned and the scanner produces an actual
     pass/fail result." — defers merge until the external action in §17.E is completed.
- **Exact approval wording:** "I select policy [1 / 2] for PR #15's dependency-scanner check, and [on
  that basis I approve merging ROJAN_Backend PR #15 at head 8810fb7 now / I will revisit PR #15 once
  NVD_API_KEY is provisioned]."

### 17.D — Spring Boot / Jackson migration scope and supported-version strategy

- **Not a PR** — a scoping decision blocking F1, F4, F7, F8–F12 (9 of 20 security findings).
- **Documented compatibility findings (restated, nothing new executed this pass):** current line is
  Boot 3.3.13 / Framework 6.1.21 — both already past free OSS support. F12 specifically is blocked by
  springdoc needing `LiteWebJarsResourceResolver`, a class introduced only in Framework 6.2 (confirmed,
  Pass 35). **Alternatives:**
  1. **Stay on 3.3.x.** Zero migration effort, but F1/F4/F7/F8–F12 remain `UPSTREAM_FIX_UNAVAILABLE`
     indefinitely, and the line itself has no vendor support.
  2. **Step to Boot 3.4.x.** Smaller technical surface (ships Framework 6.2.x, resolving F12's specific
     Framework-version blocker) — but 3.4.x's own OSS support has *also* already ended, and an explicit
     Jackson override (3.4.13's own BOM only pins 2.18.5, below F12's 2.18.11 fix floor) would still be
     required on top. Whether this step also resolves F1/F4/F7/F8–F11 was never independently verified
     — those findings' own fix requirements were not re-derived against a 3.4.x target in this
     engagement.
  3. **Jump to Boot 4.x/4.1.x.** The only *currently, actively supported* line. Carries 4 independent
     breaking-change surfaces already documented (Pass 32): Kotlin ~2.2–2.3 (current: 2.0.21), Jakarta
     Servlet 6.1/Persistence 3.2, Spring Security 7.0 (breaking changes), and Jackson 2 deprecated in
     favor of Jackson 3 (different package namespace).
- **Recommendation if asked:** given that 3.4.x is itself already unsupported, and the real floor-version
  question for F12 is already answered (6.2+), the choice is really between "accept continued exposure
  on an unsupported line" and "commit to the 4.x migration directly" — a 3.4.x stepping-stone mainly
  buys incremental testing surface, not a durable support posture.
- **Not started. No production dependency was changed.**
- **Exact approval wording:** "I authorize scoping work for a Spring Boot [3.4.x / 4.x] migration,
  to begin as a separate, isolated-branch investigation with its own build/test gates — not yet a merge
  or production change."

### 17.E — `NVD_API_KEY` provisioning

- **Current state, reconfirmed this pass:** `gh secret list --repo ROJAN_Backend` → 0 rows. Confirmed
  absent, not merely "not yet checked."
- **Required external action:** a person with repository-secret access registers a free API key at
  nvd.nist.gov and adds it as a GitHub Actions secret on `ROJAN_Backend` named exactly `NVD_API_KEY`.
  **This engagement cannot perform this action, has not attempted to, and has never invented, requested,
  printed, or stored any secret value anywhere in chat or in any repository file.**
- **Verification method once provisioned (safe, evidence-only):** re-run `gh secret list` to confirm
  presence by name only (never by value), then dispatch `backend-ci.yml` against PR #15's current head
  and inspect the `dependency-check-baseline` job's own conclusion and log for an actual scan result
  (pass, or a real CVE finding) rather than the `NvdApiException` seen today.
- **Exact approval wording:** no approval wording applies here — this is an external action, not a
  decision. ("NVD_API_KEY has been added to ROJAN_Backend's Actions secrets" is the fact this engagement
  needs reported, at which point verification can proceed.)

### 17.F — Branch-protection policy for `release/production-v24`

- **Live configuration, reconfirmed this pass:** `GET .../branches/release/production-v24/protection` →
  `404 Branch not protected`. No required status checks, no required reviewer count, no push
  restriction, no force-push/deletion restriction.
- **Recommendation (restated from §15.3, not acted on):** require `build-test-report` as a required
  status check; require ≥1 reviewer approval; restrict force-push and deletion on this branch. Keep
  `dependency-check-baseline` **non-required** until `NVD_API_KEY` exists and the job can produce a real
  result — making it required today would block every PR on an external prerequisite unrelated to that
  PR's own content.
- **Risk of no action:** the only control preventing an unreviewed or untested merge today is procedural
  (this engagement's own governance-grounded discipline) — not a technical guarantee. **Not changed this
  pass** — a branch-protection change is a repository-setting change requiring the same explicit
  authorization as a merge.
- **Exact approval wording:** "I authorize enabling branch protection on ROJAN_Backend's
  release/production-v24 branch, requiring the build-test-report status check and at least 1 reviewer
  approval, with force-push and branch deletion restricted."

### 17.G — Closure ledger (precise state distinctions, per this pass's explicit requirement)

| Item | FIXED (in a branch) | CI_VERIFIED | MERGED | RELEASED | PRODUCTION_VERIFIED |
|---|---|---|---|---|---|
| F3, F5, F6, F15, F16, F17 (Backend, PR #14) | Yes — `5d91a1d` | Yes — `build-test-report: success` on exact head | **No** | **No** | **No** |
| F1, F4, F7, F8–F12 (8 findings) | **No** — `UPSTREAM_FIX_UNAVAILABLE`, requires migration | N/A | N/A | N/A | N/A |
| CI dependency scanner (PR #15) | Yes, code-complete — `8810fb7` | **Partially** — `build-test-report: success`; `dependency-check-baseline` cannot produce a real result without `NVD_API_KEY` | **No** | **No** | **No** |
| ROJAN_Backend #8/#10/#11/#12/#13 | Yes, each on its own head | Yes, each confirmed via check-runs API on its exact head | **No** | **No** | **No** |
| ROJAN_Web #18 | Yes | Yes, `pull_request`-triggered, both checks | **No** | **No** | **No** |
| ROJAN_DesignLab_Main1 #6/#7/#8/#9 | Yes, each | Yes, each | **No** | **No** | **No** |
| ROJAN_Desktop #5/#6 | Yes, each | Yes, each | **No** | **No** | **No** |
| Web #7 | N/A | N/A | **No — CLOSED without merging** | N/A | N/A |
| Android #5 | N/A | N/A | **No — CLOSED without merging** | N/A | N/A |
| Branch protection (`release/production-v24`) | N/A | N/A | N/A | N/A | **Not enabled** |

**No item in this engagement has ever reached `MERGED`, `RELEASED`, or `PRODUCTION_VERIFIED`.** A
passing test suite or a successful CI run is evidence of `CI_VERIFIED` only — it is explicitly not
treated as, and must never be reported as, proof of release readiness.

### 17.8 — Preservation reconfirmed; final status

All 4 primaries and both isolated worktrees confirmed byte-identical before and after this pass's
work (Android `ee49225`/52 dirty, Web `09357f3`/30 dirty, Backend `b28902b`/51 dirty, Desktop
`daea816`/0 dirty; security-fix and CI-scanner worktrees 0 dirty). Docker stack unchanged, healthy.
**This pass made zero commits, pushes, merges, closures, or settings changes** — it was a pure
revalidation and owner-decision-interface-authoring pass. No newly discovered drift or regression was
found anywhere (every re-check matched Pass 36 exactly, with one evidentiary refinement: Backend
#8/#10/#11/#12's CI evidence is now confirmed via the check-runs API directly, closing the small gap
where `gh pr checks` alone under-reported them as having "no checks").

**Overall mission status: `BLOCKED`.** Nothing in this pass was approved, merged, or closed — every
item in §17.A–F remains exactly as open as it was at the start of this pass, now with the exact wording
needed to close each one individually. **Documenting a decision interface is not the same as resolving
the decision** — status remains `BLOCKED` until the owner actually provides the approvals in §17.A–F,
or until `NVD_API_KEY` is actually provisioned and verified.

## 18. Pass 38 — Authorized execution: 10 PRs merged, branch protection enabled, Boot 4.x investigation started

### 18.0 — Preflight: fresh state confirmed before any write action

Every PR in scope re-queried fresh (head SHA, base, mergeable, review state, check-runs on the exact
head): all 11 matched their §17 reference SHA exactly, all `OPEN`/`MERGEABLE`/`CLEAN`, zero pending
reviews. All 4 primary worktrees and Docker reconfirmed at their preserved baseline before the first
write operation (Android `ee49225`/52, Web `09357f3`/30, Backend `b28902b`/51, Desktop `daea816`/0;
Docker stack unchanged, healthy).

### 18.1 — Merges executed (10 total — 9 clear-approve + PR #14 under the ACCEPT decision)

| Repo | PR | Tested head | Merge method | Merge commit SHA | Timestamp (UTC) |
|---|---|---|---|---|---|
| ROJAN_Backend | #13 | `687c9b0` | merge commit | `f37bd2a` | 2026-10-29T05:03:45Z |
| ROJAN_Backend | #8 | `e8443a8` | merge commit | `dd95c90` | 2026-10-29T05:04:50Z |
| ROJAN_Backend | #10 | `3a3c957` | merge commit | `ace1286` | 2026-10-29T05:05:24Z |
| ROJAN_Backend | #11 | `ad4ca8b` | merge commit | `ef5396e` | 2026-10-29T05:06:56Z |
| ROJAN_Backend | #12 | `56e3714` | merge commit | `e1e6e4d` | 2026-10-29T05:07:48Z |
| ROJAN_Backend | #14 | `5d91a1d` | merge commit | `0d30e48` | 2026-10-29T05:09:10Z |
| ROJAN_Web | #18 | `2865c65` | merge commit | `81ab126` | 2026-10-29T05:09:59Z |
| ROJAN_DesignLab_Main1 | #6 | `714c0fe` | merge commit | `d4e4591` | 2026-10-29T05:10:54Z |
| ROJAN_Desktop | #5 | `3b927f8` | merge commit | `2d860a6` | 2026-10-29T05:11:21Z |
| ROJAN_Desktop | #6 | `54846f3` | merge commit | `60a7e9c` | 2026-10-29T05:12:12Z |

Each merge was immediately preceded by its own fresh mergeability re-check (several transiently reported
`UNKNOWN` right after a preceding same-repo merge while GitHub recomputed the new base tip — each was
waited out to a confirmed `MERGEABLE` before proceeding, never merged on an `UNKNOWN` read) and
immediately followed by a `state: MERGED` confirmation read. Backend #13 merged first as the explicit
CI-trigger prerequisite; #8/#10/#11/#12/#14 were re-verified mergeable against the new tip after #13
landed (all were, with zero conflicts — confirming the earlier audit's own file-scope-disjoint
expectation). **PR #15 was confirmed untouched throughout** (`state: OPEN`, head `8810fb7`, unchanged).
**Android #7/#8/#9 confirmed untouched and still `OPEN`** after Android #6 merged — explicitly out of
scope, not merged, approved, closed, or modified. Web #7 and Android #5's prior closures were not
reopened or altered.

### 18.2 — Post-merge CI verification, per repo, honestly distinguished

- **ROJAN_Backend**: PR #13's own fix means `release/production-v24` now auto-triggers on push. The
  final tip (`0d30e48`) was real-CI-verified twice: once via the automatic push trigger (run
  `40821728019`, `build-test-report: success`) and once via an explicit `workflow_dispatch` against the
  same tip — both passed. **This is genuine post-merge CI evidence on the exact final commit**, not an
  inference from pre-merge PR checks.
- **ROJAN_DesignLab_Main1**: `main`'s new tip (`d4e4591`) auto-triggered real CI on push:
  `governance-drift-check: success`, `Android CI (unit tests): success`. Same standard met.
- **ROJAN_Web and ROJAN_Desktop: a newly discovered, honestly-reported gap.** Checking each repo's
  actual workflow trigger blocks (not assumed): `ROJAN_Web/web-ci.yml` has `push: branches-ignore:
  [master]` — it explicitly **excludes** master from its push trigger; `web-production-deploy.yml` has
  no bare CI trigger at all, only a `workflow_dispatch` gated by its own `deploy` input (a deploy
  pipeline, not a CI check). `ROJAN_Desktop/ci.yml` has `push: branches: [phase/**, feature/**,
  fix/**]` — `main` is not in that list — and `release.yml` is tag-triggered only. **Neither workflow
  has a `workflow_dispatch` trigger at all, so this pass could not manually dispatch one either.**
  Checked directly via `GET /repos/.../commits/<merge-sha>/check-runs` on both new tips (`81ab126`,
  `60a7e9c`): **zero check-runs exist on either merge commit.** This is not a failure to look hard
  enough — it is a structural absence, confirmed by reading the actual trigger YAML. **The only CI
  evidence for these 2 merges is the pre-merge PR-head check (already passing, already recorded) — the
  merge commit itself has never been independently re-verified by any CI mechanism, and none exists to
  run.** This is the same category of gap originally found and fixed for `ROJAN_Backend` via PR #13 —
  now newly discovered, undocumented until this pass, in 2 other repos. **Not fixed in this pass** (out
  of this pass's authorized scope); recorded as a new finding for a future pass/owner decision.

### 18.3 — Branch protection: applied and independently re-verified

Pre-change state (re-confirmed): `404 Branch not protected`. First attempt at applying the authorized
policy failed cleanly (`422`, boolean/integer fields sent as strings) — **no partial state was ever
applied**; fixed by sending a proper typed JSON payload. Applied via `PUT .../protection`. **Independent
post-change read** (a fresh `GET`, not the write response):

```
strict: true
contexts: ["build-test-report"]
required_approving_review_count: 1
allow_force_pushes: false
allow_deletions: false
```

Every one of the 4 explicitly authorized controls is confirmed live: the verified `build-test-report`
check is required and strict (branch must be up to date with the base), ≥1 approving review is
required, force-pushes are blocked, branch deletion is blocked. `dependency-check-baseline` was **not**
made required, per the explicit instruction to hold that until the NVD prerequisite is resolved.
**Side effect observed and explained, not a malfunction**: PR #15 (the only Backend PR still open)
now reads `mergeStateStatus: BEHIND` — expected and correct, since `strict: true` requires a PR's base
to be caught up with the 6 commits just merged ahead of it; this does not block eventual merge, it
requires an update/rebase first, consistent with holding PR #15 anyway.

### 18.4 — Spring Boot 4.x migration investigation (isolated, non-production, not merged)

**Isolated worktree created** (not a primary worktree): `D:/AndroidProjects/ROJAN_Backend_boot4_migration`,
branch `investigation/spring-boot-4x-migration`, based on the new `release/production-v24` tip
(`0d30e48`) — includes every fix just merged. All existing worktrees were listed first to confirm this
is a genuinely new, dedicated one, not a reused one with unrelated prior state.

**Real dependency inventory** (current, confirmed from the actual files): Kotlin `2.0.21`, Spring Boot
`3.3.13`, springdoc `2.6.0`, JJWT `0.12.6`, JUnit `5.11.3`. **Real target-version inventory**, read
directly from Spring Boot 4.1.2's own published `pom.xml` on Maven Central (not estimated): Kotlin
`2.3.21`, Spring Framework `7.0.10`, Spring Security `7.1.2`, Jackson-bom `3.1.7`, Jakarta Servlet
`6.1.0`, Jakarta Persistence `3.2.0`, Hibernate `7.4.13`, Tomcat `11.0.27`.

**Real, demonstrated compile results** (not assumptions) — bumped only `springBoot` and `kotlin` in
`libs.versions.toml` (2-line diff, nothing else), ran `./gradlew compileKotlin`, real exit codes
captured both times:
- With Boot 4.1.2 alone (Kotlin still 2.0.21): real `BUILD FAILED` — a Kotlin build-tools-API classpath
  snapshot transform failure against `kotlin-stdlib-2.0.21.jar`, before any Spring-level code is even
  reached.
- With Boot 4.1.2 **and** Kotlin bumped to the exact `2.3.21` Boot's own BOM requires: **`domain`,
  `application`, `infrastructure`, and `api` modules all compiled successfully** — only Java-deprecation
  warnings were emitted (Kafka's `JsonSerializer`, Spring Data Redis's `Jackson2JsonRedisSerializer` /
  `GenericJackson2JsonRedisSerializer`, all "deprecated in Java" — consistent with, but not proof of,
  the expected Jackson-ecosystem evolution). **`bootstrap` then failed for real**:
  `BackendApplication.kt:4: Unresolved reference 'domain'` on
  `import org.springframework.boot.autoconfigure.domain.EntityScan` — Spring Boot 4.x has restructured
  `spring-boot-autoconfigure` into feature-specific modules, and this import path no longer resolves.
  **This is a newly, concretely demonstrated breaking change, not previously documented at this level of
  specificity** — `@EntityScan`'s correct Boot-4.x import location was not identified in this pass.

**Explicitly distinguishing demonstrated compatibility from assumptions**: domain/application/
infrastructure/api compiling is real, demonstrated evidence. **Nothing about Jackson 3's actual
namespace impact, Spring Security 7's actual API-shape impact on `SecurityConfig`/`JwtTokenProvider`,
or springdoc's actual behavior under Framework 7.0.10 was demonstrated** — the build never reached those
code paths before `bootstrap` failed. These remain exactly what they were before this pass: documented
*expectations*, not verified outcomes.

**Comparison against F1, F4, F7, F8–F12**: **none of these findings is marked fixed.** The build does
not yet compile to completion, let alone run the test suite, so no finding's actual remediation can be
confirmed — a target-framework-version change alone, without passing compatibility and security tests,
is explicitly not sufficient (per this pass's own instruction) to close any of them.

**Staged migration gates, defined for a future pass/implementation PR** (none yet passed beyond Gate 1):
1. ✅ *(demonstrated this pass)* `domain`/`application`/`infrastructure`/`api` compile under Boot 4.1.2 +
   Kotlin 2.3.21.
2. ⬜ Resolve the `@EntityScan` relocation (and any other `bootstrap`-module breakage) to get the full
   module graph compiling.
3. ⬜ Full local test suite green (941+ tests) under the Boot 4.x dependency graph.
4. ⬜ Jackson 3 namespace migration verified across every DTO, custom (de)serializer, and the `jjwt-
   jackson` integration specifically (JJWT's own Jackson-version compatibility was not checked this
   pass).
5. ⬜ Spring Security 7 API-shape re-verification of `SecurityConfig`/`JwtTokenProvider`/CORS config —
   Security 6→7 is a major version with its own documented breaking changes, not yet read in this pass.
6. ⬜ Re-scan F1/F4/F7/F8–F12 against the new, fully-compiling dependency graph with real advisory
   queries — only then may any of them be reclassified, and only with that evidence attached.

**Future implementation-PR criteria**: all 6 gates pass locally, in this same isolated worktree or a
successor branch; a full owner review of every security-relevant diff; and the same explicit, named
merge authorization every other PR in this engagement has required — no different standard for this
one merely because it is larger.

**Not authorized and not done**: no production dependency was changed, no migration code was merged,
no deployment occurred. The worktree's 2-line experimental diff (`libs.versions.toml`) remains
uncommitted and unpushed, left in place at `D:/AndroidProjects/ROJAN_Backend_boot4_migration` for direct
inspection or continuation.

### 18.5 — Preservation reconfirmed; closure ledger; final status

All 4 primaries and the 2 previously-used isolated worktrees (`ROJAN_Backend_security_fixes`,
`ROJAN_Backend_ci_scanner`) confirmed byte-identical throughout this pass — before, during, and after
every merge and the branch-protection change (Android `ee49225`/52 dirty, Web `09357f3`/30 dirty,
Backend `b28902b`/51 dirty, Desktop `daea816`/0 dirty; both 0 dirty). Docker stack unchanged, healthy.
The new `ROJAN_Backend_boot4_migration` worktree has exactly the 1 expected, documented 2-line diff. **No
primary branch was rewritten, no history altered, no dirty work discarded, no worktree reset.**

**Closure ledger, precise (extending §17.G with this pass's real outcomes):**

| Item | FIXED | CI_VERIFIED | MERGED | RELEASED | PRODUCTION_VERIFIED |
|---|---|---|---|---|---|
| Backend #13/#8/#10/#11/#12/#14 (incl. F3,F5,F6,F15,F16,F17) | Yes | Yes — pre-merge, **and** real post-merge CI on the final tip `0d30e48` | **Yes** (6 merges, §18.1) | **No** | **No** |
| Web #18 | Yes | Yes — pre-merge only; **no post-merge CI mechanism exists** (§18.2) | **Yes** | **No** | **No** |
| Android #6 | Yes | Yes — pre-merge, **and** real post-merge CI on the exact new tip `d4e4591` | **Yes** | **No** | **No** |
| Desktop #5/#6 | Yes | Yes — pre-merge only; **no post-merge CI mechanism exists** (§18.2) | **Yes** | **No** | **No** |
| Branch protection (`release/production-v24`) | N/A | N/A | N/A | N/A | **Applied and independently verified live** (§18.3) — a control being live is not the same as a release |
| Backend #15 | Code-complete, `8810fb7` | Partial — `build-test-report` success, `dependency-check-baseline` cannot produce a real result without `NVD_API_KEY` | **No — held, per explicit instruction** | N/A | N/A |
| F1, F4, F7, F8–F12 (migration-blocked findings) | **No** | N/A | N/A | N/A | N/A |
| Boot 4.x migration | Investigation started, Gate 1 of 6 passed (§18.4) | N/A | **No** | **No** | **No** |
| Web #7, Android #5 | N/A | N/A | **Closed, not merged (Pass 36, reconfirmed untouched this pass)** | N/A | N/A |

**No merge in this engagement's history has ever been followed by a release or a production
verification.** A merge is evidence of `MERGED` only; it is explicitly not read as `RELEASED` or
`PRODUCTION_VERIFIED` anywhere in this ledger.

**New drift/regression found this pass**: none on any of the 11 revalidated PRs (all matched their
reference SHA exactly). **New finding discovered**: the Web/Desktop post-merge CI gap (§18.2) —
structural, confirmed via trigger-file inspection, not previously documented at this level of detail.

**Overall mission status: `BLOCKED`.** Real, verified progress was made — 10 PRs genuinely merged with
evidence, a real branch-protection control is now live and independently confirmed, and a real (if
early-stage) Boot 4.x compatibility signal now exists. None of this is `RELEASED` or
`PRODUCTION_VERIFIED`, and it must not be read as either. Remaining, unresolved: PR #15 (held, per
instruction); `NVD_API_KEY` provisioning (external); the Boot 4.x migration's remaining 5 of 6 gates;
the newly discovered Web/Desktop post-merge CI gap; and Android #7/#8/#9, left exactly as they were,
awaiting their own separate authorization.

## 19. Pass 39 — Web/Desktop CI gap repaired (2 new PRs, not merged), Boot 4.x migration advanced
through Gates 2–3 with 5 real, independently-verified API relocations found and fixed in the
isolated worktree, NVD/preservation reconfirmed

### 19.1 — Priority 1: Web and Desktop post-merge CI gap — smallest safe fix, 2 new PRs opened, not merged

Re-read both repos' actual workflow trigger YAML fresh (not assumed from §18.2). Confirmed: `ROJAN_Web`'s
default branch is `master`; `ROJAN_Desktop`'s is `main`. `web-ci.yml` had no deploy capability at all
(no Docker/GHCR/SSH step, by its own header comment) and `web-production-deploy.yml` is the only
deploy-capable workflow, gated by its own `deploy` dispatch input — neither touched. `ci.yml`
(Desktop) only builds and tests; `release.yml` (tag-triggered only) is the sole publish-capable
workflow — not touched.

**Smallest safe fix, each in a new dedicated isolated worktree/branch** (not a primary worktree):
- `ROJAN_Web`, worktree `ROJAN_Web_ci_postmerge_fix`, branch `ci/postmerge-trigger-master`: removed
  `push: branches-ignore: [master]`'s exclusion (kept the `paths` filter) and added
  `workflow_dispatch: {}`. 2-line removal / 1-line addition.
- `ROJAN_Desktop`, worktree `ROJAN_Desktop_ci_postmerge_fix`, branch `ci/postmerge-trigger-main`: added
  `main` to `push.branches` and added `workflow_dispatch: {}`.

**Real test evidence, captured before opening either PR:**
- Web: `workflow_dispatch` run on exact head `f3006c1` — run `40824756615`, **`completed/success`**.
  (A second, automatically push-triggered run on the same ref, `40824678930`, was cancelled by this
  repo's own pre-existing `concurrency: cancel-in-progress: true` group when the dispatch run started
  — a benign supersession, not a failure, recorded as such rather than omitted.)
- Desktop: `workflow_dispatch` run on exact head `9cd8512` — run `40824777890`, **`completed/success`**.

**2 new PRs opened, each with its exact diff, test evidence, risk, and target branch — neither merged:**
- `ROJAN_Web` **PR #19** → `master`, head `f3006c1`.
- `ROJAN_Desktop` **PR #7** → `main`, head `9cd8512`.

**Not retroactively claimed**: Web #18 (merge `81ab126`) and Desktop #5/#6 (merge `2d860a6`/`60a7e9c`)
remain exactly as recorded in §18 — `MERGED`, pre-merge `CI_VERIFIED` only. This pass does not alter
that record. Once PR #19/#7 are merged (owner authorization required, not granted this pass), the next
pass must dispatch/observe a fresh run against the then-current tip and record that specific SHA and
result — not infer it from this pass's branch-level evidence.

### 19.2 — Priority 2: Spring Boot 4.x investigation continued in the same isolated worktree, Gates 2 and 3 advanced with real, verified evidence

Reconfirmed first: `ROJAN_Backend_boot4_migration`, branch `investigation/spring-boot-4x-migration`,
had exactly the 2-line diff recorded in §18.4 and no other change — isolation and continuity confirmed
before touching anything further.

**Gate 2 (resolve `bootstrap`'s compile break) — passed, with the real fix independently verified
against the actual Boot 4.1.2 artifact, not just an AI-summarized answer** (the same discipline this
audit's own methodology section already requires): `EntityScan` was relocated to
`org.springframework.boot.persistence.autoconfigure.EntityScan` — confirmed by downloading
`spring-boot-persistence-4.1.2.jar` from Maven Central directly and listing its real class entries,
not merely trusting a search summary. Applied the 1-line import fix. **`compileKotlin` across the full
module graph (`domain`/`application`/`infrastructure`/`api`/`bootstrap`): real exit code 0,
`BUILD SUCCESSFUL`.**

**Gate 3's compile prerequisite (test sources) — passed, 3 more real relocations found and
independently verified the same way, applied mechanically once each pattern was proven:**
1. `TestRestTemplate` moved to `org.springframework.boot.resttestclient.TestRestTemplate` (new
   `spring-boot-resttestclient` artifact, pulled in via a new `spring-boot-starter-webmvc-test` test
   dependency) — confirmed by downloading and listing the real jar. Verified the fix pattern on 1
   representative file first (compile errors for that exact file disappeared, next file's errors
   appeared unchanged), **then** applied the identical, now-proven-mechanical import fix across the
   remaining 31 affected test files via a single `sed` pass — not a blind mass-edit, a verified pattern
   repeated.
2. `UriComponentsBuilder.fromHttpUrl()` removed in favor of `fromUriString()` (deprecated since 6.2,
   removed at 7.0) — a direct, official rename; applied to the 1 affected file
   (`PlatformAuthoritySalonFlowIntegrationTest.kt`).
3. `@SpyBean` removed in favor of `@MockitoSpyBean` from
   `org.springframework.test.context.bean.override.mockito` (Spring Framework's own test module, not
   Spring Boot's) — confirmed by downloading `spring-test-7.0.10.jar` and listing its real classes.
   **Checked the one documented caveat first** (this annotation cannot be used inside a
   `@TestConfiguration` class without restructuring) — confirmed the 1 affected usage
   (`SalonNotificationFlowIntegrationTest.kt`) is a plain test-class field, not inside a
   `@TestConfiguration`, so the rename is genuinely safe here, not assumed safe.

**`compileTestKotlin` across the full module graph: real exit code 0, `BUILD SUCCESSFUL`.** Gate 3's
compile prerequisite is fully met.

**Full test suite run (Gate 3 proper) — real, substantial findings, each root-caused to an exact class
or dependency, not a guess:**
- First run: all 34 `bootstrap` integration tests failed identically at Spring context startup —
  root-caused (via the actual JUnit XML report, not the truncated console summary) to
  `ClassNotFoundException: org.springframework.security.web.servlet.util.matcher.
  PathPatternRequestMatcher$Builder`. **Confirmed via the real dependency tree**
  (`./gradlew :bootstrap:dependencies`) that this project's own F3/F5 security-fix override
  (`spring-security-{core,config,web,crypto}:6.3.10`, necessary against Boot 3.3.x's older BOM) was
  **downgrading** Spring Security from Boot 4.1.2's own managed `7.1.2` back to `6.3.10` — a version
  that predates this class. **Commented out the override** (not deleted — the reasoning is preserved
  in place) since Security 7.1.2 already supersedes the original F3/F5 fix floor entirely.
- Second run: 1 new real compile break surfaced in `infrastructure`'s own code (not a test):
  `BCryptPasswordEncoderAdapter.kt`'s `encode()` override — Spring Security 7's
  `PasswordEncoder.encode()` is now JSpecify-annotated `@Nullable` at the interface level, where the
  concrete `BCryptPasswordEncoder` never actually returns null. Fixed with a non-null assertion
  (`!!`), not a behavior change — documented in place as to why.
- Third run: all 34 tests still failed, now on `SchemaManagementException: Schema validation: missing
  table [app_releases]` — the Flyway migration for that table (`V42__app_releases.sql`, confirmed to
  exist) never ran. Root-caused: Boot 4.x extracted Flyway's own Spring Boot autoconfiguration out of
  `spring-boot-autoconfigure` into a new, separate `spring-boot-flyway` module (confirmed present in
  Boot 4.1.2's own BOM module list) — without it, `FlywayAutoConfiguration` never activates. Added the
  dependency.
- Fourth run: all 34 tests still failed, now on `NoSuchBeanDefinitionException: No qualifying bean of
  type 'com.fasterxml.jackson.databind.ObjectMapper'` — Boot 4.x's default Jackson auto-configuration
  now provides a Jackson **3.x** `ObjectMapper` (different namespace), not the classic 2.x one this
  codebase's own Jackson 2 usage (`jackson-module-kotlin`, JJWT's Jackson integration, etc.) requires.
  **Identified and applied Spring's own official, documented stop-gap**: the `spring-boot-jackson2`
  module (keeps the classic `ObjectMapper` auto-configured) and `spring-boot-autoconfigure-classic`
  (restores Boot-3.x-style classpath-driven auto-configuration generally, which would also have
  resolved the Flyway gap above automatically, discovered after already fixing that one module-by-
  module — both are real, officially documented, explicitly deprecated-but-supported compatibility
  modules per Spring's own migration guide, not improvised). Added both.
- Fifth run: all 34 tests still failed, now on `NoClassDefFoundError: io.lettuce.core.resource.
  DefaultClientResources` → `ExceptionInInitializerError` → `NoClassDefFoundError:
  io/netty/channel/socket/SocketProtocolFamily`. Root cause pattern matches the Spring Security
  override exactly: this project's own **F15 security-fix override** (`netty-*:4.1.139.Final`,
  necessary against Boot 3.3.x's older, vulnerable Netty line) is very likely now pinning an older
  Netty line than Boot 4.1.2's own BOM expects for its Lettuce/Redis integration — **not yet removed or
  confirmed this pass**; recorded as the precise next step rather than guessed at further.

**Explicitly, no finding was marked fixed.** F1, F4, F7, F8–F12 remain exactly as dispositioned in
§18.4 — none has been re-scanned against a fully green test suite, because the suite is not yet green.
`infrastructure`'s own unit tests (38 tests across its own test classes, confirmed via fresh, non-cached
JUnit XML output) do pass cleanly under this exact dependency graph — real, narrower evidence than the
full-suite claim, reported at its own true scope rather than generalized.

**Updated 6-gate staging plan (supersedes §18.4's listing):**
1. ✅ `domain`/`application`/`infrastructure`/`api` compile under Boot 4.1.2 + Kotlin 2.3.21.
2. ✅ Full module graph (including `bootstrap`) compiles — `EntityScan` relocation fixed.
3. 🔶 **In progress, not complete**: full test suite run. Test-source compile is fully fixed (3
   relocations found/verified/applied). Runtime: 4 real blockers found and fixed in sequence
   (Security-version override, BCrypt nullability, Flyway module, Jackson2/classic compat modules);
   a 5th (Netty/Lettuce version mismatch, same override pattern as Security) is identified but **not
   yet fixed** — the next concrete step, not a new unknown.
4. ⬜ Jackson 3 namespace migration — **deliberately deferred**, not attempted, via the official
   `spring-boot-jackson2` stop-gap instead. Recorded as a conscious choice with its own real property
   (`spring-boot-jackson2` is explicitly marked for eventual removal upstream) rather than silently
   substituted for the original gate.
5. ⬜ Spring Security 7 API-shape re-verification beyond the 1 nullability fix already found —
   `SecurityConfig.kt`/`JwtTokenProvider.kt`/CORS config have not yet been read against Security 7.1.2
   specifically.
6. ⬜ Re-scan F1/F4/F7/F8–F12 against the fully resolved dependency graph once Gate 3 is actually green.

**Nothing committed, pushed, merged, or deployed from this worktree.** All changes remain uncommitted
in `D:/AndroidProjects/ROJAN_Backend_boot4_migration` (37 files touched: 2 build files, 1 versions
catalog, 1 application-main file, 1 infrastructure-main file, 32 test files) — a complete, inspectable,
reproducible record of every fix applied and why.

### 19.3 — Priority 3: NVD scanner prerequisite — reconfirmed, not overstated

PR #15: `state: OPEN`, head `8810fb7` (unchanged), `mergeable: MERGEABLE`, `mergeStateStatus: BEHIND`
(expected, per §18.3's `strict: true` branch protection — not a regression). `NVD_API_KEY`:
`gh secret list` → 0 rows, confirmed absent by name only; no value ever requested, printed, or stored.
**Not merged. HOLD remains the owner's selected disposition, unchanged.**

### 19.4 — Priority 4: preservation reconfirmed; branch protection reconfirmed live; out-of-scope items untouched

All 4 primaries and the 2 previously-used isolated worktrees (`ROJAN_Backend_security_fixes`,
`ROJAN_Backend_ci_scanner`) byte-identical throughout (Android `ee49225`/52 dirty, Web `09357f3`/30
dirty, Backend `b28902b`/51 dirty, Desktop `daea816`/0 dirty; both 0 dirty). Docker stack unchanged,
healthy. The `ROJAN_Backend_boot4_migration` worktree's dirty state is this pass's own intended,
documented deliverable, not drift. Branch protection on `release/production-v24` re-read live and
confirmed identical to §18.3's authorized settings (`build-test-report` required + strict, 1 reviewer
required, force-push and deletion blocked) — **not modified this pass.** Android #7/#8/#9 reconfirmed
untouched, still `OPEN`, unmodified. No primary branch was rewritten, no history altered, no dirty work
discarded, no worktree reset, no clean/stash performed anywhere.

### 19.5 — Closure ledger addendum and final status

| Item | State |
|---|---|
| ROJAN_Web PR #19 (CI fix) | Opened, tested (`f3006c1`, real dispatch success) — **not merged** |
| ROJAN_Desktop PR #7 (CI fix) | Opened, tested (`9cd8512`, real dispatch success) — **not merged** |
| Web #18 / Desktop #5/#6 | Unchanged: `MERGED`, pre-merge `CI_VERIFIED` only — **not retroactively upgraded** |
| Boot 4.x migration | Gates 1–2 fully passed; Gate 3 in progress (test-compile fixed, 4 of ~5 known runtime blockers fixed, 1 identified not yet fixed); Gates 4–6 not reached — **no finding marked fixed** |
| PR #15 | `OPEN`, unmerged, `HOLD` unchanged; `NVD_API_KEY` confirmed still absent |
| Branch protection | Unchanged, reconfirmed live |
| Android #7/#8/#9 | Unchanged, untouched, still `OPEN`, out of scope |

**No RELEASED or PRODUCTION_VERIFIED claim is made anywhere in this pass.** **Overall mission status:
`BLOCKED`.** Real, substantial, independently-verified progress was made on both the CI gap and the
Boot 4.x investigation — neither constitutes completion. Remaining: owner authorization for PR #19/#7;
the Netty/Lettuce override removal and the rest of Gate 3, plus Gates 4–6, in the Boot 4.x worktree;
`NVD_API_KEY` provisioning; and every item already carried forward from §18.5 unchanged.

## 20. Pass 40 — PR #19's trigger discrepancy resolved with evidence (none existed), PR #7 fully
re-verified, post-merge-CI evidence requirements specified, Boot 4.x Gate 3 substantially advanced
(Spring context now loads; 211 real, root-caused failures remain, concentrated in Jackson 3 HTTP
message conversion)

### 20.1 — Priority 1: Web PR #19's "trigger discrepancy" — resolved: no discrepancy in the PR itself

Queried the PR's exact head (`f3006c1`, unchanged) and fetched the workflow file's real content
**directly from that commit** via `GET /repos/.../contents/.github/workflows/web-ci.yml?ref=f3006c1`
(not the diff rendering, not master's own copy, not any cached view): the `push` trigger's `on:` block
at this exact commit has **no `branches-ignore` key at all** — it was genuinely removed, exactly as
§19.1 reported. **For contrast**, the same file read at `ref=master` still shows
`branches-ignore: [master]` — correctly, because the PR has not been merged yet. **This is the
complete explanation for any appearance of a discrepancy**: viewing `master`'s own current file (not
the PR's head) shows the pre-fix content, as it must until the PR merges. No correction was needed,
none was made — confirmed via real artifact reads, not inferred from prose.

**Workflow syntax, inputs, and permissions, read directly from the exact head:** `workflow_dispatch:
{}` (no inputs — a plain, no-argument manual trigger); `permissions: contents: read` (unchanged,
minimal, no write/deploy scope added by this change).

**One genuine, newly-found detail, fully explained rather than left as an unexplained red mark**:
`gh pr view 19` reports `mergeStateStatus: UNSTABLE`. Root cause: 3 real runs exist on this exact head
— `pull_request` (`40825330275`, `success`), `workflow_dispatch` (`40824756615`, `success`, already
reported §19.1), and the automatic `push` run (`40824678930`, `cancelled` — superseded by this
workflow's own `concurrency: cancel-in-progress: true` group when the dispatch run started seconds
later on the same ref). `gh pr checks` renders that `cancelled` conclusion as `fail` in its compact
view. **`ROJAN_Web` has no branch protection at all** — confirmed this pass via
`GET /repos/.../branches/master/protection` → `403 Upgrade to GitHub Pro` (this repository's current
plan does not support the feature, so none of this engagement's "required checks" could ever be
GitHub-enforced here regardless of intent) — so `UNSTABLE` means exactly "mergeable, but at least one
non-required check is non-green," which is the already-explained cancellation, not a new blocker.

**PR #19 was not merged.** It is correctly understood to have no separate merge authorization beyond
what Pass 39 already stated.

### 20.2 — Priority 2: Desktop PR #7 — fully re-verified, clean

Head unchanged (`9cd8512`). Read the workflow file directly from this exact commit: `main` is present
in `push.branches` alongside `phase/**`/`feature/**`/`fix/**`; `workflow_dispatch: {}` present; the
only job (`build-and-test`) has no publish/release step — `release.yml` (the only publish-capable
workflow, tag-triggered only) is confirmed untouched (`gh pr diff --name-only` lists only `ci.yml`).
**2 real runs on this exact head**: `pull_request` (`40825377151`, `success`) and `workflow_dispatch`
(`40824777890`, `success`, already reported §19.1). `mergeStateStatus: CLEAN` (no concurrency group
exists in this workflow, so no cancellation occurred here). **Not merged.**

### 20.3 — Priority 3: exact evidence required after each CI-fix PR is separately authorized and merged

Specified, not executed (neither PR is merged):
- **ROJAN_Web**: after PR #19 merges, read `master`'s new tip via
  `GET /repos/ROJAN_Web/branches/master` → record that exact SHA; confirm a `push`-triggered run exists
  for that exact SHA (or dispatch one if none fires automatically within a reasonable window); record
  the run's database ID and `conclusion`; confirm the `test` job's steps (`npm ci`/`lint`/`typecheck`/
  `test`/`build`) all appear and succeeded. Only then may Web's base tip be called `CI_VERIFIED` post-
  merge — never inferred from this pass's branch-level dispatch evidence, which is pre-merge only.
- **ROJAN_Desktop**: identical procedure against `main`'s new tip after PR #7 merges — record the exact
  SHA, the triggered run's ID/conclusion, and confirm `Build (Release)` and `Test` steps both ran.

**No run was dispatched against a stale commit and described as verifying a newer tip in this pass.**
No deployment or release workflow was triggered for either repository.

### 20.4 — Priority 4: Spring Boot 4.x investigation — Gate 3 substantially advanced with real evidence

**Identity reconfirmed before editing**: worktree `ROJAN_Backend_boot4_migration`, branch
`investigation/spring-boot-4x-migration`, base `0d30e48` (unchanged), diff exactly matching §19.2's
37-file end state before this pass added anything further.

**Pass 39's exact final result, captured precisely** (not restated loosely): real exit code **1**,
`BUILD FAILED`, **34 tests completed, 34 failed** (all 34 `bootstrap` integration tests, every one an
`initializationError` — the Spring `ApplicationContext` never started). **Classified as incomplete**,
per this pass's explicit instruction — not partially passing, not close to done, genuinely not running.

**Reviewed Pass 39's exploratory changes for necessity, not assumed correct**: the 2 "classic"/
`jackson2` dependency aliases and the commented-out Spring Security override were each re-examined
this pass by continuing to actually exercise them against a real, deepening test run (below) rather
than left as unverified assumptions.

**Investigated the first reproducible failure (the Netty/Lettuce `ApplicationContext` failure
identified at the end of Pass 39), without broad refactoring:**
1. Confirmed, via Boot 4.1.2's own real `pom.xml`, that its BOM manages **Netty 4.2.19.Final** — a
   major-line bump past this project's own F15 override (`4.1.139.Final`), confirming the same
   stale-override pattern already found for Spring Security. Commented out the 10 `netty-*` dependency
   lines (same documented-in-place pattern as the Security override) — **not deleted**, not re-enabled
   without this evidence.
2. Re-ran the suite: the Netty/Lettuce error was gone, but a **new** real failure appeared —
   `IllegalArgumentException: No enum constant tools.jackson.databind.SerializationFeature.write-
   dates-as-timestamps`, a `spring.jackson.serialization.*` property failing to bind. Root-caused via
   the real `BindException` message (not guessed): Boot 4.x's `spring.jackson.*` property prefix now
   binds against the **Jackson 3** `tools.jackson.databind.SerializationFeature` enum; the classic
   Jackson 2 property prefix is `spring.jackson2.*` (confirmed against the earlier-verified
   `spring-boot-jackson2` module's own purpose). Found the exact offending property in
   `bootstrap/src/main/resources/application.yml` (`spring.jackson.default-property-inclusion` /
   `spring.jackson.serialization.write-dates-as-timestamps`) and renamed the key to `spring.jackson2`.
3. Re-ran the suite: **the `ApplicationContext` now actually starts.** This is a real, major milestone
   — previously 34/34 tests failed before even reaching a single real assertion; now:

**Real, current full-suite result**: exit code **1** (still failing overall — not claimed green),
**242 tests completed, 211 failed, 31 passed.** Per-class breakdown (real, from the fresh JUnit XML,
not estimated): `AppReleaseConcurrencyIntegrationTest` (4/4 pass), `CorsConfigurationIntegrationTest`
(5/5 pass), `PublicMediaAccessIntegrationTest` (7/7 pass), `SwaggerProductionExposureIntegrationTest`
(3/3 pass) are **fully green**; every other integration-test class has at least 1 failure, most have
all or nearly all failing.

**The 211 failures are concentrated, not scattered** — overwhelmingly
`org.springframework.http.converter.HttpMessageConversionException` caused by
`tools.jackson.databind.exc.InvalidDefinitionException` on real REST request/response bodies. Root
cause (not yet fixed): `spring-boot-jackson2` keeps a classic `ObjectMapper` *bean* available, but
Spring MVC's own `HttpMessageConverter` auto-configuration still defaults to a **Jackson 3**-based
converter for actual HTTP (de)serialization once `spring-boot-starter-web`'s own Jackson-3 module is
on the classpath — `spring-boot-jackson2` alone does not redirect that. No webmvc-specific Jackson-2
compatibility module was found in Boot 4.1.2's own BOM module list (checked directly, not assumed
absent) in the time available this pass. **This is Gate 4's real substance, not a side issue** — left
precisely identified, not force-fixed under this pass's remaining scope (would require either an
explicit `MappingJackson2HttpMessageConverter` bean reconfiguration or committing to the full Jackson 3
namespace migration — both real, separate decisions).

**1 additional, already-expected failure confirmed**: `/v3/api-docs` returns `500` instead of `200`
(`SwaggerDefaultProfileExposureIntegrationTest`) — consistent with F12's already-documented springdoc
2.6.0-vs-Framework incompatibility, now reproduced at Framework **7.0.10** (even further from
springdoc's last-compatible line than the Framework 6.2 finding that originally closed F12). Not a new
surprise; confirms the existing F12 disposition rather than changing it.

**Gate assessment, per this pass's explicit requirement to distinguish proven / compile-tested /
untested — not "compile success alone":**

| Gate | Status | Evidence |
|---|---|---|
| 1–2 (module graph compiles) | ✅ Proven | Real `BUILD SUCCESSFUL`, exit code 0 |
| 3 (test suite) | 🔶 Substantially advanced, not complete | `ApplicationContext` now starts (real milestone); 31/242 tests genuinely pass; 211/242 fail on 2 identified, un-fixed root causes (Jackson 3 HTTP conversion; springdoc/Framework version) |
| Security 7 behavior | 🔶 Partially proven | `CorsConfigurationIntegrationTest` 5/5 pass — real, demonstrated evidence CORS works under Security 7.1.2/Framework 7.0.10. Authentication/authorization flows themselves (`AuthenticationFlowIntegrationTest`, `OtpAuthenticationFlowIntegrationTest`, `PlatformAuthorityFlowIntegrationTest`) are **not** proven — their failures are consistent with the Jackson HTTP-conversion issue masking the actual auth-logic result, not yet disentangled from it |
| Jackson compatibility | 🔶 Disproven for HTTP bodies as currently configured | Concretely demonstrated broken (211 failures), root cause identified, not fixed |
| JWT (JJWT) | ⬜ Untested | Not independently exercised separately from the Jackson-masked HTTP layer this pass |
| Redis serialization | 🔶 Partially proven | The Lettuce/Netty connection itself now initializes (Gate blocker fixed); actual serialize/deserialize round-trips through Redis were not independently isolated from the Jackson HTTP issue this pass |
| Flyway | ✅ Proven | `spring-boot-flyway` module fix (Pass 39) confirmed still working — no schema-validation failures recurred in this pass's runs |
| CORS | ✅ Proven | See Security 7 row above |
| springdoc | ❌ Disproven | `/v3/api-docs` 500, consistent with F12, now reproduced at Framework 7.0.10 |

**No finding (F1, F4, F7, F8–F12) was marked fixed.** F12 specifically is reconfirmed still broken, now
with evidence one Framework line further than before — strengthening, not weakening, its existing
`UPSTREAM_FIX_UNAVAILABLE` disposition.

**Nothing committed, pushed, merged, or deployed.** The worktree's diff grew by 3 more files this pass
(`build.gradle.kts`'s Netty block, `application.yml`'s Jackson property prefix) on top of Pass 39's 37
— still entirely uncommitted, still entirely inspectable, still entirely in the isolated worktree.

### 20.5 — Priority 5: NVD and production controls — reconfirmed, unchanged

`NVD_API_KEY`: `gh secret list` → 0 rows, confirmed absent by name only; no value requested, printed,
or retrieved. PR #15: `state: OPEN`, head `8810fb7` (unchanged); `HOLD` unchanged. Branch protection on
`release/production-v24`, re-read live: `build-test-report` required + `strict: true`,
`required_approving_review_count: 1`, `allow_force_pushes: false`, `allow_deletions: false` — identical
to §18.3's authorized settings. **Not weakened, not changed, no other repository setting touched.**

### 20.6 — Preservation reconfirmed

All 4 primaries and all 4 isolated worktrees used across this engagement
(`ROJAN_Backend_security_fixes`, `ROJAN_Backend_ci_scanner`, `ROJAN_Web_ci_postmerge_fix`,
`ROJAN_Desktop_ci_postmerge_fix`) confirmed in their expected state (Android `ee49225`/52 dirty, Web
`09357f3`/30 dirty, Backend `b28902b`/51 dirty, Desktop `daea816`/0 dirty; the 4 named isolated
worktrees all 0 dirty — their work is already committed/pushed from Pass 39, correctly clean, not
drifted). Docker stack unchanged, healthy. `ROJAN_Backend_boot4_migration`'s growing dirty state
remains this investigation's own intended, documented deliverable. **No reset, clean, stash,
overwrite, or force-push occurred anywhere.**

### 20.7 — Final status

**Web PR #19 and Desktop PR #7 remain open and unmerged — no new authorization was given or assumed.**
**Overall mission status: `BLOCKED`.** Real, substantial, independently-verified progress this pass on
all 5 priorities — none constitutes `RELEASED` or `PRODUCTION_VERIFIED`, and none is described as
such. The Boot 4.x investigation in particular crossed a genuine threshold (Spring context now loads)
while simultaneously surfacing a large, precisely-identified, real remaining gap (Jackson 3 HTTP
message conversion) — reported at its true, partial scope rather than rounded up or down.

## 21. Release-candidate verdict (based on existing evidence, no tests repeated without new reason)

Boot 4.x migration explicitly stopped for this release path per owner instruction; its worktree
(`ROJAN_Backend_boot4_migration`) preserved untouched, kept out of the release path entirely.

**Fresh-reconfirmed, unchanged**: `release/production-v24` tip `0d30e48`, real post-merge
`build-test-report: success` ×2; PR #15 `OPEN`/`8810fb7`/`HOLD`; `NVD_API_KEY` absent (0 secrets);
Web #19 / Desktop #7 `OPEN`, unmerged; Android #7/#8/#9 `OPEN`, out of scope; branch protection on
`release/production-v24` unchanged (`build-test-report` required+strict, 1 reviewer, force-push/
deletion blocked); all 4 primaries + Docker byte-identical.

**Verdict: `BLOCKED`** — not `RELEASE CANDIDATE READY`. Exact blockers, owner, action:

| # | Blocker | Owner | Exact action |
|---|---|---|---|
| 1 | F1, F4, F7, F8–F12 — 8 of 20 Backend security findings remain `CONFIRMED`/`UPSTREAM_FIX_UNAVAILABLE` on the current release branch; their only known fix (Boot 4.x) is explicitly out of this release's path | Repository owner | Explicit risk-acceptance decision for this release, **or** authorize and complete the Boot 4.x migration before release (separate from this release path) |
| 2 | `NVD_API_KEY` absent → dependency-vulnerability scanner (PR #15) cannot run; no automated ongoing scan exists for Backend | Repository owner / secrets admin | Register a free key at nvd.nist.gov, add as a `ROJAN_Backend` Actions secret named exactly `NVD_API_KEY` |
| 3 | `ROJAN_Web` PR #19 and `ROJAN_Desktop` PR #7 (post-merge CI fixes) unmerged — `master`/`main` still lack any post-merge CI verification mechanism for future changes | Repository owner | Separate, explicit "approve merge" for each PR; post-merge, dispatch/observe a run against the new exact tip before calling it `CI_VERIFIED` |

**Not blocking** (confirmed via existing evidence, not re-tested): the 10 PRs merged in Pass 38 are
each `MERGED` with real pre-merge `CI_VERIFIED` evidence (Backend/Android additionally
post-merge-`CI_VERIFIED` on their exact final tips); F3/F5/F6/F15/F16/F17 are `MERGED` (F5 = `ACCEPT`,
owner-selected); branch protection is live and correctly scoped; Flyway/DB migrations, install/
packaging pipelines (`release.yml`, Android build, Web deploy) are unmodified by this engagement and
carry no new evidence of regression. Android #7/#8/#9 are additive, unmerged features — they do not
block releasing what is already merged.

**No `RELEASED` or `PRODUCTION_VERIFIED` claim is made.** Release readiness requires item 1 resolved
(decision, not a fix, is sufficient) at minimum; items 2–3 are real gaps but not release-blocking for
already-merged code specifically — named here because they are the next-smallest real fixes toward a
clean `RELEASE CANDIDATE READY` state.

## 22. F4, F8–F11, F12 genuinely fixed via same-major-line bumps; F1/F7 re-verified as still blocked
(real advisory data, not re-asserted) — PR #16 opened, CI-verified, not merged

**UPSTREAM_FIX_UNAVAILABLE was not treated as a reason to stop**, per this mission's explicit
instruction. Each of F1/F4/F7/F8–F12 was re-investigated independently against the real GitHub
Advisory API (not restated from prior passes). Work done in a new isolated worktree/branch
(`ROJAN_Backend_sec2`, `security/f1-f4-f8-f11-remediation`), based on the real current
`release/production-v24` tip (`0d30e48`, re-confirmed live before starting, not assumed from an old
SHA) — the primary branch was never touched directly.

### 22.1 — Per-finding disposition (real advisory ranges, fix floors, actions, evidence)

| Finding | Component | Advisory | Vulnerable range in this line | Real fix floor | Action | Status |
|---|---|---|---|---|---|---|
| F1 | spring-webmvc | GHSA-pc63-qcmh-9cmg | `>=6.1.0, <=6.2.19` — confirmed `first_patched_version: null` **through 6.2.19 too**, correcting this pass's own first assumption that 6.2.19 would close it | **7.0.9** | None closes F1 in Spring 6.x. Framework was bumped to 6.2.19 anyway, but only because `spring-security-core:6.5.11` (F4's real fix) requires it as a compile dependency, and because it is springdoc 2.7.0's real prerequisite (F12) | **Still `CONFIRMED`/`UPSTREAM_FIX_UNAVAILABLE`** — XsltView confirmed unused anywhere in this codebase (unchanged reachability finding) |
| F4 | spring-security-web | GHSA-mf92-479x-3373 | `>=6.0.0, <=6.3.10` — `null` through 6.3.10 | **6.5.9** | `spring-security-{core,config,web,crypto}` bumped `6.3.10 → 6.5.11` | **`FIXED`**, `TEST_VERIFIED`, `CI_VERIFIED` (below) |
| F7 | spring-boot | GHSA-wwpq-f5c3-7hvx | `>=3.3.0, <=3.3.18` **and** `>=3.4.0, <=3.4.15` — `null` for **both** lines (re-verified directly via the API this pass; an earlier web-search summary claiming a "3.3.19" fix was checked against Maven Central and found to not exist — the search result was wrong, not the original audit) | **3.5.14** (or 4.0.6) | None available in 3.3.x or 3.4.x. **Compensating controls documented, not a version fix**: `SecurityConfig.kt` confirmed `SessionCreationPolicy.STATELESS` (zero HTTP sessions anywhere — the advisory's primary precondition is persistent sessions); production `Dockerfile` confirmed running as non-root uid 10001, no shell/home, inside its own container filesystem namespace (the advisory's "local attacker with host file-system access" precondition requires already being inside this exact container) | **Still `CONFIRMED`/`UPSTREAM_FIX_UNAVAILABLE`** — real, reduced-but-nonzero exploitability, now with concrete compensating-control evidence instead of an unexamined gap |
| F8 | tomcat-embed-core | GHSA-r29c-68gh-xp6x | `<10.1.55` | 10.1.55 | `tomcat-embed-{core,el,websocket}` bumped `10.1.31/42 → 10.1.61` (latest 10.1.x release) | **`FIXED`** |
| F9 | tomcat-embed-core | GHSA-563x-q5rq-57qp | `<10.1.52` | 10.1.52 | (same bump) | **`FIXED`** |
| F10 | tomcat-embed-core | GHSA-wmwf-9ccg-fff5 | `<10.1.45` | 10.1.45 | (same bump) | **`FIXED`** |
| F11 | tomcat-embed-core | 6 GHSA IDs individually verified (`GHSA-9xv2-5v5q-p794`, `-gcx9-497g-6cp6`, `-h3x4-894j-xpx5`, `-h6fc-48rj-7qqh`, `-5mp6-jrq3-r938`, `-5m62-pw8w-7w9f`) | highest fix floor among all 6: `<10.1.58` | 10.1.58 | (same bump; 10.1.61 covers every one) | **`FIXED`** (all 6) |
| F12 | jackson-databind | `GHSA-cxp5-3px4-pw24`, `GHSA-wv8q-qhhj-9h54` | `<=2.18.10` (both) | 2.18.11 | jackson-databind + every other Jackson-family artifact actually present in the graph bumped to **the same** 2.18.11; springdoc bumped `2.6.0 → 2.7.0` now that Framework ≥6.2 (its real, previously-missing prerequisite) is present | **`FIXED`** |

### 22.2 — The real, demonstrated path to F12 (2 wrong turns caught before being reported, per this
mission's explicit instruction not to report an unexecuted/mismatched result as passing)

1. Overriding `jackson-databind` alone → real `NoSuchMethodError`
   (`ParserMinimalBase.<init>(StreamReadConstraints)`) — `./gradlew :bootstrap:dependencies` showed
   `jackson-core`/`jackson-module-kotlin` still resolving to Boot's own BOM-pinned `2.17.3`. **13 real
   test failures**, caught before reporting.
2. Tried `imports { mavenBom("com.fasterxml.jackson:jackson-bom:2.18.11") } ` instead — **also
   demonstrably failed**: the dependency graph still showed `jackson-bom:2.17.3` winning (confirmed via
   the same dependency report, not assumed) — `io.spring.dependency-management`'s BOM-import ordering
   does not let a later `imports{}` entry override Boot's own already-declared jackson management.
3. **Working fix**: explicit `dependency()` overrides for every Jackson-family artifact actually
   present in the resolved graph (9 artifacts total) — the same mechanism already proven for every
   other finding in this file. Re-verified via `./gradlew :bootstrap:dependencies`: every leaf shows a
   `(c)` constraint at exactly `2.18.11`, zero exceptions.

### 22.3 — Dependency-graph verification (real output, not assumed)

`./gradlew :bootstrap:dependencies --configuration testRuntimeClasspath`, checked explicitly for:
- **Jackson alignment**: `jackson-{core,databind,annotations}`, `jackson-module-{kotlin,parameter-
  names}`, `jackson-datatype-{jdk8,jsr310}`, `jackson-dataformat-{yaml,toml}` — every leaf `2.18.11`,
  zero exceptions (grepped for any non-2.18.11 Jackson leaf; empty result).
- **Framework/Security/Tomcat**: `spring-core`/`spring-webmvc` resolve `6.1.21/6.2.0 → 6.2.19`;
  `spring-security-web` resolves `6.3.10 → 6.5.11`; `tomcat-embed-core` resolves `10.1.31/42 → 10.1.61`
  — no duplicate or conflicting resolution found.
- **Netty (F15) explicitly re-confirmed unchanged**: `git diff` of `build.gradle.kts` against this
  branch's base shows the F15 Netty block byte-identical (not touched); the resolved graph still shows
  `netty-{common,handler} → 4.1.139.Final` — the already-fixed version from PR #14, **not assumed
  fixed further by this PR**, out of its scope.
- **No other finding (F2/F3/F5/F6/F13–F20) was touched** — none of their dependency lines appear in
  this branch's diff.

### 22.4 — Real test and CI evidence, exact commit

Full suite on the final state: **949/949 tests passing, real exit code 0, `BUILD SUCCESSFUL`**
(`domain` 110, `application` 559, `infrastructure` 38, `bootstrap` 242 — 0 failures anywhere). No
existing test was weakened, skipped, or deleted — the 13 failures hit mid-remediation (jackson-core
mismatch, before fix #3 above) are retained in the commit message as the real diagnostic trail, not
reported as a passing result at any point.

**Regression coverage**: F1/F4/F7/F8–F12 are container/framework-internal defects, not bugs in this
codebase's own logic (unlike F5, which got a bespoke regression test because it was reachable through
this app's own registration endpoint) — the existing 949-test suite, covering auth, JWT, OTP, CORS,
access control, and serialization paths and re-passing unchanged under the new dependency graph, **is**
the regression evidence for this class of finding, consistent with how F3/F6/F15/F16/F17 were already
handled in earlier passes. No bespoke PoC was written per Tomcat/Framework/Security GHSA, since the
GHSA advisories themselves describe library-internal defects with no simple application-level
reproduction vector.

**CI**: run
[`40845062776`](https://github.com/meisamelh66-cpu/ROJAN_Backend/actions/runs/40845062776), dispatched
against the exact commit `9768d4c`, confirmed via both `gh run view` and the commit's own `check-runs`
API: `build-test-report: success`.

### 22.5 — PR opened, not merged

`ROJAN_Backend` **PR #16** (`security/f1-f4-f8-f11-remediation` → `release/production-v24`), head
`9768d4c`. Confirmed fresh after creation: `state: OPEN`, `mergeable: MERGEABLE`,
`mergeStateStatus: BLOCKED` — expected and correct, since branch protection (§18.3) requires ≥1
approving review and this PR has none yet; **not a CI or code problem**, and branch protection was
**not weakened** to work around it. **Not merged. Requires explicit owner authorization.**

### 22.6 — `NVD_API_KEY` / PR #15, tracked independently, unaffected

Reconfirmed by name only: `gh secret list` → 0 rows, still absent. PR #15 remains `OPEN`, `HOLD`,
untouched by this remediation — this PR (#16) does not depend on or change PR #15's status in any way.

### 22.7 — Closure-ledger update (extends §18.5/§21, nothing else re-asserted)

| Finding | FIXED | TEST_VERIFIED | CI_VERIFIED | MERGED | RELEASED | PRODUCTION_VERIFIED |
|---|---|---|---|---|---|---|
| F4, F8, F9, F10, F11 (6 GHSA), F12 | **Yes** — `9768d4c` | **Yes** — 949/949 | **Yes** — run `40845062776` | **No** | No | No |
| F1 | No — real fix floor is 7.0.9, not reached | N/A | N/A | N/A | N/A | N/A |
| F7 | No — no fix exists in 3.3.x/3.4.x | N/A | N/A | N/A | N/A | N/A (compensating controls documented, §22.1) |

**No finding in this section has reached `MERGED`, `RELEASED`, or `PRODUCTION_VERIFIED`.** A passing
test suite and a successful CI run are `TEST_VERIFIED`/`CI_VERIFIED` only.

**Overall mission status: `BLOCKED`**, unchanged in kind. Real, substantial, independently-verified
remediation of 7 of the 8 previously-blocked findings (F4, F8–F11 as 6 GHSA IDs, F12) — genuinely fixed
without any Spring Boot 4.x migration, contrary to their prior classification. F1 and F7 remain real,
confirmed, unremediated findings with no available same-line fix, now with precise real-advisory
evidence and (for F7) concrete compensating controls rather than an unexamined gap. PR #16 awaits
owner review and merge authorization; every other previously-identified blocker (PR #15/`NVD_API_KEY`,
Web #19/Desktop #7, Android #7/#8/#9, the Boot 4.x investigation) is unchanged and untouched by this
pass.

## 23. Pass 42 — F7 resolved via Boot 3.5.16 (demonstrated, not pushed); F1 reclassified `MITIGATED`
with real structural evidence; PR #16 untouched

PR #16 (`9768d4c`) reconfirmed untouched throughout (`OPEN`, unchanged head). New isolated worktree
`ROJAN_Backend_boot35` (branch `investigation/spring-boot-3.5x-f7-f1`) created from the real,
freshly-confirmed `release/production-v24` tip (`0d30e48`, unchanged since Pass 41).

### 23.1 — F7: genuinely `FIXED` via Boot 3.5.16 (demonstrated; commit local, not pushed)

Re-verified GHSA-wwpq-f5c3-7hvx directly: `first_patched_version: null` for **both** `3.3.0-3.3.18`
and `3.4.0-3.4.15` — confirming (not re-litigating) §22's finding. Real fix floor: **3.5.14**. Checked
Maven Central fresh, not assuming the version named in §22 is still current: **3.5.16** is now the
latest published 3.5.x release. Applied PR #16's already-proven overrides (Framework 6.2.19, Security
6.5.11, Tomcat 10.1.61, Jackson family 2.18.11, springdoc 2.7.0) as the starting point, then bumped
`springBoot` to `3.5.16` on top — still Spring Boot 3.x, not the excluded 4.x line, confirmed by
downloading Boot 3.5.16's own real `spring-boot-autoconfigure`/`spring-boot-test` jars and finding
`EntityScan`/`TestRestTemplate` still in their classic packages (no Boot-4.x-style module split).
Boot 3.5.16's own BOM independently confirms `spring-framework:6.2.19` and `spring-security:6.5.11` —
exactly matching PR #16's manual overrides.

**Targeted tests only, run first** (Cors/Swagger×2/Authentication/Otp/ApiHardening — F7/security-
config/startup-relevant): **38/38 passing, real exit code 0.** `/v3/api-docs` returns `200`
(springdoc 2.7.0 unaffected by the Boot bump). **Full suite run once at the end, per instruction**:
**949/949 passing, real exit code 0, `BUILD SUCCESSFUL`.** Dependency graph re-verified: `spring-
boot:3.5.16` resolved cleanly; Netty still `4.1.139.Final` (the explicit F15 override still wins over
Boot's own lower `4.1.135.Final` BOM value); every Jackson artifact still `2.18.11`, zero exceptions.

**`F7: FIXED`** — `spring-boot` itself, the vulnerable artifact, now resolves to `3.5.16`, past the
real `3.5.14` fix floor. **Committed locally** (`787d162`) in the isolated worktree. **Not pushed, no
PR opened** — this pass's own scope does not authorize publishing it (unlike Pass 41's explicit PR
authorization for F4/F8–F12). Owner decision needed: authorize pushing this commit and opening a PR
(separate from #16, since it supersedes/extends its dependency set) for review.

### 23.2 — F1: re-verified still `BLOCKED` at the version level; reclassified `MITIGATED` with real
structural evidence (not just "unused")

Re-confirmed GHSA-pc63-qcmh-9cmg directly: vulnerable range extends through `6.2.19`
(`first_patched_version: null`); real fix floor is **7.0.9**. Boot 3.5.16 manages Framework at exactly
`6.2.19` (confirmed, §23.1) — **does not close F1**. No version-level fix is possible without Spring
Framework 7.x, which only arrives via the already-excluded Boot 4.x line (already independently
investigated, Pass 38–40 — not repeated here).

**Reachability investigated structurally, not just by absence of a grep match**, per this pass's
explicit instruction not to equate "no direct usage" with proof of non-exploitability:
- **Zero `@Controller` classes anywhere** in this codebase — 39 `@RestController` classes, 0
  `@Controller`. Every response is serialized directly via `HttpMessageConverter`; Spring MVC's
  View-resolution subsystem (which `XsltView` belongs to) is never invoked by this application's own
  routing for any endpoint, structurally, not incidentally.
- **Zero `ViewResolver`/`ModelAndView`/`XsltView` references anywhere** (`grep`, confirmed again this
  pass, not merely re-cited from §4).
- **Zero `.xsl` files and zero `templates/` directory anywhere** in the source tree — even if some
  third-party auto-configuration registered an `XsltViewResolver` bean, there is no `.xsl` resource
  anywhere in this deployed artifact for it to resolve.

**Decision, per the 3-way criteria this pass defines**: `F1: MITIGATED` — the base vulnerability
(no patched Framework version available short of 7.x) remains genuinely present; the compensating
control is architectural rather than a configuration toggle (a 100% REST-only API with no view layer
and no XSLT resources at all), but it is real, demonstrated, and substantially reduces reachability,
not merely "not yet tried." **Residual risk, stated plainly**: this mitigation would stop holding if a
future `@Controller`/`ViewResolver`/`.xsl` resource were ever added — it is a property of the current
codebase, not a permanent guarantee. **No fix was applied or forced** to make this look resolved; no
out-of-support or BOM-incompatible version was used.

### 23.3 — Files/commits changed, preservation

`ROJAN_Backend_boot35` only: `build.gradle.kts` (84 lines), `gradle/libs.versions.toml` (35 lines),
1 local commit (`787d162`), **not pushed**. PR #16 and its own worktree (`ROJAN_Backend_sec2`)
reconfirmed untouched before and after this pass (`9768d4c`, 0 dirty). All 4 primary worktrees and
Docker reconfirmed byte-identical. No change to `release/production-v24`, branch protection, or any
release-path setting. No prior fix reverted or rewritten.

### 23.4 — Closure-ledger update

| Finding | Status | Evidence |
|---|---|---|
| F7 | **`FIXED`** (demonstrated; not yet published) | `spring-boot:3.5.16` resolved; 949/949 tests, exit 0; commit `787d162`, local only |
| F1 | **`MITIGATED`** | Real fix needs Framework 7.0.9+ (Boot 4.x only); REST-only architecture + zero View infra + zero `.xsl` resources confirmed structurally this pass |

**Overall mission status: `BLOCKED`**, unchanged in kind. F7 is now technically resolved pending one
owner decision (authorize publishing the Boot 3.5.16 commit); F1's base vulnerability has no available
fix short of Boot 4.x (excluded from this release path) but is now backed by real, demonstrated
structural mitigation evidence rather than a bare "unused" note. PR #16 (F4/F8–F11/F12) remains open,
untouched, awaiting its own separate review. Every other previously-identified blocker is unchanged.

## 24. Pass 43 — F7's Boot 3.5.16 fix published per explicit owner authorization: PR #17 opened,
CI-verified on the exact head; PR #16 reconfirmed untouched

Owner authorization confirmed for this specific action (publish commit `787d162`, open an independent
PR) — executed, nothing else.

**Preflight**: commit `787d162` and branch `investigation/spring-boot-3.5x-f7-f1` reconfirmed intact
in `ROJAN_Backend_boot35` exactly as left in §23. Fresh `git fetch origin release/production-v24`
confirmed the remote tip is still `0d30e48` (unchanged since §22/§23 — zero drift) and that this
branch's merge-base with the remote is exactly that commit — no rebase needed. Diff re-confirmed
confined to `build.gradle.kts`/`gradle/libs.versions.toml` only.

**Pushed** (`git push -u origin investigation/spring-boot-3.5x-f7-f1`, no force — new branch, never
pushed before). **Opened `ROJAN_Backend` PR #17** (`investigation/spring-boot-3.5x-f7-f1` →
`release/production-v24`, head `787d162`), with the full F7 advisory data, coordinated-version
evidence, and 38/38 + 949/949 test results from §23, explicitly **not** claiming F1 as fixed.

**CI, real and independently confirmed, not assumed from the dispatch alone**: 2 runs on the exact
head `787d162` — the automatic `pull_request` trigger (`40897941901`) and a manual
`workflow_dispatch` (`40897996789`) — both `completed/success`. Cross-checked directly against the
commit's own `check-runs` API (not just `gh run view`): `build-test-report: completed/success` ×2.
**PR #17 is genuinely `CI_VERIFIED`** on this exact head — not declared before this confirmation.

**PR #17 fresh state**: `OPEN`, `mergeable: MERGEABLE`, `mergeStateStatus: BLOCKED` — expected,
identical reason as PR #16 (branch protection requires ≥1 approving review, none yet; not a CI or code
problem, branch protection not touched). **PR #16 reconfirmed untouched throughout**: `OPEN`, head
still `9768d4c`, unchanged. **Not merged. No deploy, no release, no branch-protection change.**

**Closure-ledger update**: F7 moves from `FIXED` (demonstrated, unpublished) to `FIXED` +
`TEST_VERIFIED` + `CI_VERIFIED` (run `40897941901`/`40897996789`, head `787d162`) — still **not**
`MERGED`. F1's `MITIGATED` status is unchanged by this pass (nothing new investigated for F1 here).

**Overall mission status: `BLOCKED`**, unchanged in kind. Both remediation PRs (#16: F4/F8–F11/F12;
#17: F7) are open, real-CI-verified, and awaiting the same owner review/merge-authorization step. F1
remains `MITIGATED`, with no available version-level fix short of the excluded Boot 4.x line. Every
other previously-identified blocker (`NVD_API_KEY`/PR #15, Web #19/Desktop #7, Android #7/#8/#9) is
unchanged and untouched.

## 25. Pass 44 — Independent review of PR #16/#17 and remaining PRs; critical overlap finding between
#16 and #17; PR #15/`NVD_API_KEY` dispositioned precisely; no merges performed

Read-only, independent review. No merge, deploy, settings change, or secret change performed anywhere.

### 25.0 — Fresh baseline (zero drift confirmed, not assumed)

`release/production-v24` real tip (fetched fresh): `0d30e48` — unchanged since §22–24. PR #16
(`security/f1-f4-f8-f11-remediation`, head `9768d4c`) and PR #17
(`investigation/spring-boot-3.5x-f7-f1`, head `787d162`) both `OPEN`, `MERGEABLE`,
`mergeStateStatus: BLOCKED` (no review yet — unchanged reason, re-confirmed, not assumed). All 4
primary worktrees and Docker reconfirmed byte-identical to every prior pass's baseline. Both PRs'
isolated worktrees (`ROJAN_Backend_sec2`, `ROJAN_Backend_boot35`) confirmed at their exact recorded
commits, zero local drift.

### 25.1 — Critical finding not previously stated: PR #17 is a full content-superset of PR #16

`git diff 0d30e48 787d162` contains **every one of PR #16's own dependency-override lines**
(`spring-security-core:6.5.11`, `tomcat-embed-core:10.1.61`, `jackson-databind:2.18.11`, springdoc
2.7.0) **plus** the additional `springBoot: 3.3.13 → 3.5.16` bump — confirmed by grepping PR #17's
diff for PR #16's exact override lines, not inferred from commit messages. The two branches share no
git ancestry (neither commit is an ancestor of the other — confirmed via `git merge-base --is-
ancestor` both directions), but their **content** is not independent: **merging both would conflict
on the same lines in `build.gradle.kts`/`gradle/libs.versions.toml`, and merging PR #17 alone already
delivers everything PR #16 delivers, plus F7.** This was not surfaced in Pass 41–43's own framing of
"2 separate PRs" and is the single most important new fact this review contributes — **an explicit
owner decision is required on this specific point** (§25.6).

### 25.2 — PR #16 independent review

**Diff** (re-confirmed against the current base, unchanged): `build.gradle.kts`,
`gradle/libs.versions.toml` only — 2 files, consistent with every prior report.

**Per-finding mapping, re-verified against the real resolved dependency graph** (reusing the existing
`/tmp/depgraph_final.log` from the same unmodified worktree/commit — re-running was not repeated since
nothing changed, consistent with this pass's own instruction):

| Finding | Declared override | **Actually resolved** (from the real dependency report, not the file) | Status |
|---|---|---|---|
| F4 | `spring-security-*:6.5.11` | `spring-security-web:6.3.10 -> 6.5.11` (confirmed resolved) | `FIXED`, `TEST_VERIFIED` (949/949), `CI_VERIFIED` (run on `9768d4c`, `build-test-report: success`, re-confirmed fresh this pass via `check-runs` API) |
| F8–F11 | `tomcat-embed-core:10.1.61` | not re-grepped this pass (unchanged since §22, no new risk identified) — recorded as previously verified, not re-asserted as newly checked | Unchanged: `FIXED`, `TEST_VERIFIED`, `CI_VERIFIED` |
| F12 | Jackson family `2.18.11` + springdoc `2.7.0` | `spring-core/spring-webmvc:6.1.21/6.2.0 -> 6.2.19` (Framework, F12's real prerequisite) confirmed resolved | Unchanged: `FIXED`, `TEST_VERIFIED`, `CI_VERIFIED` |

**Dependency-scan result for PR #16's own branch**: **`BLOCKED/NOT RUN`** — confirmed this pass by
reading the actual `backend-ci.yml` content at PR #16's exact head: it contains only a
`build-test-report` job. **No `dependency-check-baseline` job exists on this branch at all** (that
job only exists on PR #15's own branch, not yet merged into `release/production-v24`, so neither PR
#16 nor PR #17 inherits it). This is a different, more precise fact than "blocked by missing
`NVD_API_KEY`" — there is no scanner present to be blocked, full stop. **Never reported as `PASS`.**

**Regression/compatibility risk**: re-confirmed via the existing 949/949 full-suite result (unchanged
since §22, not re-run) — no regression found across domain/application/infrastructure/bootstrap.

**Independent recommendation: `RECOMMEND MERGE`** — on its own technical merits. **Subject to §25.1's
overlap finding being resolved first** (see §25.6).

### 25.3 — PR #17 independent review

**Diff**: same 2 files, superset of PR #16 (§25.1) plus the `springBoot` version line.

**F7 re-checked against the advisory and documented fixed versions** (re-confirmed, not re-fetched
from the API a third time since §23/§24 already did this twice with consistent results):
`GHSA-wwpq-f5c3-7hvx`, no fix in 3.3.x/3.4.x, real floor 3.5.14, `3.5.16` applied — exceeds the floor.

**Behavioral/API/config-compatibility risk, assessed**: Boot 3.5.16's own BOM independently manages
Framework `6.2.19` and Security `6.5.11` — identical to PR #16's manual overrides, confirmed via the
existing `/tmp/depgraph_boot35.log` (`spring-boot:3.5.16` resolved cleanly; `netty-common ->
4.1.139.Final`, F15's override still winning over Boot's own lower BOM value). Confirmed via the real
published jars (§23) that Boot 3.5.x does **not** carry the Boot-4.x-style autoconfigure/test module
split (`EntityScan`/`TestRestTemplate` still in their classic packages) — the specific regression class
that *would* have mattered for a Boot version bump was checked, not assumed absent.

**Test/CI evidence cross-checked**: Pass 42's reported 38/38 targeted + 949/949 full-suite results
match the real CI run on the exact head `787d162` (`build-test-report: success`, confirmed via both
`gh run view` and the commit's own `check-runs` API, 2 independent runs, both green) — **`build-test-
report` success is not treated as equivalent to an independent security scan**: PR #17's branch has
the identical `dependency-check-baseline`-absent situation as PR #16 (§25.2) — **`BLOCKED/NOT RUN`**
for the scanner, same reason, not glossed over.

**F1 claim check**: PR #17's own description explicitly states F1 is **not** fixed by this PR and
cites the real 7.0.9 floor — verified this matches the actual diff (no Framework version beyond
6.2.19 is touched) — **no overreach found**; F1 correctly stays `MITIGATED`, not claimed `FIXED`
anywhere in this PR.

**Independent recommendation: `RECOMMEND MERGE`** on its own technical merits — **also subject to
§25.1's overlap finding** (§25.6).

### 25.4 — PR #15 / `NVD_API_KEY` disposition

`NVD_API_KEY` existence checked **by name only**: `gh secret list --repo ROJAN_Backend` → 0 rows.
Also checked the repository's one configured environment (`production`) in case the secret were
scoped there instead of at repo level — its 4 secrets are deploy-related (`VPS_HOST`, `VPS_REPO_PATH`,
`VPS_SSH_KEY`, `VPS_USER`), confirmed unrelated, and the `dependency-check-baseline` job itself
declares no `environment:` key, so it could not read an environment-scoped secret regardless. **No
secret value was ever requested, read, printed, or logged** — absence was confirmed structurally
(zero rows returned), which is itself sufficient evidence of absence without needing to attempt a
read.

**Real current CI evidence on PR #15's exact head** (`8810fb7`, unchanged): `build-test-report:
success`; `dependency-check-baseline: failure` — re-confirmed via `check-runs` API this pass. Root
cause unchanged from Pass 38/39's own finding (`NvdApiException`, key length 0) — not re-derived, just
re-confirmed still current. **PR #15 is not, and must not be, described as clean of vulnerabilities**
— the scan has never completed a real analysis.

**Exact external action required**: a person with `ROJAN_Backend` repository-secret-write access
registers a free API key at nvd.nist.gov and adds it as a GitHub Actions secret named exactly
`NVD_API_KEY`. **Not created or modified by this pass.** Once added, the next pass's required action
is: dispatch `backend-ci.yml` against PR #15's then-current exact head, record the run ID, SHA,
conclusion, and the scanner's actual vulnerability report content — only then may any claim about
PR #15's dependency state be made.

### 25.5 — Remaining PRs, individually

| PR | State | Head (unchanged?) | CI on exact head | Failure cause (if any) | Architecture fit | Merge evidence sufficient? |
|---|---|---|---|---|---|---|
| Web #19 | `OPEN` | `f3006c1` (unchanged) | `pull_request` run success; 1 earlier `push` run shows `cancelled` (rendered as "fail" in the compact `gh pr checks` view) | **Not a real failure** — benign `concurrency: cancel-in-progress` supersession, already root-caused in Pass 40, re-confirmed this pass, not re-litigated | Fits — CI-trigger-only change, no deploy capability touched | Yes, for its own narrow scope (CI trigger fix) — **no new authorization implied for anything else** |
| Desktop #7 | `OPEN` | `9cd8512` (unchanged) | `build-and-test: success`, `mergeStateStatus: CLEAN` | None | Fits — same pattern as Web #19 | Yes, for its own narrow scope |
| Android #7 | `OPEN` | `7d4c2ea` (unchanged) | `Unit tests`/`governance-drift-check`: all success (re-confirmed via `check-runs` API this pass) | None | Stacks on Android #6 (already merged into `main` as `d4e4591`); base branch `candidate/android-ci-unit-tests` confirmed still an ancestor of `main` | Technically yes — **explicitly out of scope for this mission**, no authorization to merge exists or is implied here |
| Android #8 | `OPEN` | `d78274d` (unchanged) | Same, all success | None | Same stacking | Same — out of scope |
| Android #9 | `OPEN` | `b82975f` (unchanged) | Same, all success | None | Same stacking | Same — out of scope |

**No prior authorization is read as covering a merge of any of these 5** — each remains held for its
own explicit, separate owner decision, exactly as already recorded.

### 25.6 — Decisions requiring explicit owner approval (nothing merged by this pass)

1. **PR #16 vs. PR #17 overlap (§25.1)** — new this pass. The owner must choose exactly one of:
   (a) merge **PR #17 only** (delivers F4/F8–F11/F12/F7 together) and close PR #16 as superseded by
   its content; or (b) merge **PR #16 only** (F4/F8–F11/F12, no Boot-minor-version bump) and leave F7
   as a separate future decision; merging **both** will conflict and must not be attempted.
2. **`NVD_API_KEY` provisioning** (§25.4) — external, requires a named individual with
   repository-secret access.
3. **Per-PR merge authorization** for whichever of #16/#17 is chosen in (1).
4. **Web #19 / Desktop #7** — separate, narrow-scope merge authorization (CI-trigger fix only), unless
   already covered by a standing instruction this review did not find evidence of.
5. **Android #7/#8/#9** — remain explicitly out of scope; no decision requested here, named only for
   completeness.

### 25.7 — F1 residual risk, reconfirmed not newly closed

No new evidence was introduced this pass regarding F1 — the structural findings from §23.2 (zero
`@Controller` classes, zero `ViewResolver`/`XsltView` references, zero `.xsl` resources anywhere) are
reconfirmed still accurate (file-tree facts, unaffected by anything changed since) but are **not**
treated as proof the advisory itself is resolved — the underlying dependency-level vulnerability
remains genuinely present (no patched Spring Framework release short of 7.0.9 exists in this line).
**`F1` status unchanged: `MITIGATED`**, not upgraded, not downgraded, with the same explicitly-stated
limit: this mitigation holds only as long as no `@Controller`/`ViewResolver`/`.xsl` resource is ever
added to this codebase.

### 25.8 — Closure-ledger summary (no new status upgraded without evidence)

| Item | FIXED | TEST_VERIFIED | CI_VERIFIED | MERGED | RELEASED | PRODUCTION_VERIFIED |
|---|---|---|---|---|---|---|
| F4, F8–F11, F12 (PR #16) | Yes | Yes | Yes | **No** | No | No |
| F7 (PR #17, also contains F4/F8–F11/F12) | Yes | Yes | Yes | **No** | No | No |
| F1 | No (`MITIGATED`) | N/A | N/A | N/A | N/A | N/A |
| PR #15 dependency scan | N/A | N/A | **`BLOCKED/NOT RUN`** | No | No | No |
| Web #19, Desktop #7 | N/A (CI-infra fix, not a security finding) | N/A | Yes (own branch) | No | No | No |
| Android #7/#8/#9 | N/A | N/A | Yes (own branch) | No | No | No |

**Overall mission status: `BLOCKED`.** This pass completed the independent review and evidence
report it was scoped to deliver — it was not scoped to, and did not, merge anything. Remaining,
exact: the 5 owner decisions in §25.6, with the PR #16/#17 overlap (§25.1) now the most
immediately-actionable one, since it blocks even deciding *which* PR to authorize next.

## 26. Pass 52 — Final disposition of PR #16 and PR #17: PR #16 closed as superseded; PR #17
mergeability assessed against real branch-protection gates, blocked on required review only

Resolves owner decision §25.6 item 1, option (a): **merge PR #17 only, close PR #16 as superseded.**
Between Pass 44 and this pass, PR #17's head moved twice under separate owner authorization — the
scanner-infrastructure transfer (commit `a74295a`) and the dependency-check report-discovery fix
(commit `b7ee43f`) — so every claim below was re-verified fresh against PR #17's current head, not
restated from Pass 44.

### 26.1 — PR #16 superset claim, re-proven mechanically on current heads

PR #16 head: `9768d4cb4a41351033520b10ebba1d86f807bfe1`. PR #17 current head:
`b7ee43f2fa8e8138631e32999dd8c158f9e93e81`. All 26 of PR #16's added `build.gradle.kts` lines
(1 explanatory comment + 25 `dependency(...)` override lines across the Jackson family, Tomcat,
Spring Security, and Spring Framework) and its `gradle/libs.versions.toml` addition
(`springdoc = "2.7.0"` + rationale) were extracted from PR #16's diff against
`release/production-v24` and checked individually (`grep -qF`) against PR #17's current diff against
the same base. Result: **26/26 found, 0 missing.** PR #17 additionally carries
`springBoot = "3.5.16"` (F7) and the entire `dependency-check-baseline` job, neither present on
PR #16.

### 26.2 — PR #16 disposition: closed, not merged

PR #16's own branch has **no** `dependency-check-baseline` job at all (`grep -c` against its own copy
of `.github/workflows/backend-ci.yml` returns `0`) — its scan status is precisely `NOT_RUN`, not a
failure, and was never going to change by waiting. `build-test-report`: `success`. Closed via
`gh pr close 16` with a comment citing the §26.1 evidence; confirmed post-close via the API:
`state: CLOSED`, `mergedAt: null`. No merge of PR #16 occurred, directly or indirectly.

### 26.3 — PR #17 CI and scan status, re-confirmed on current head

Fresh `check-runs` call against commit `b7ee43f` (not reused from any prior pass): `build-test-report`
= `success`; `dependency-check-baseline` = `success`. Scan status recorded as **`PASS`** — grounded in
real evidence, not the job label alone: the `dependency-check-report` artifact on run `41206394947`
is independently confirmed via the Actions API (`454861` bytes, `expired: false`). Raw scanner output:
"Found 381 vulnerabilities in project rojan-backend" — a raw finding count only; no triage into
unique/exploitable CVEs has been performed at any point in this engagement, and none is claimed here.

### 26.4 — PR #17 mergeability: one real, named, unmet mandatory control

Branch protection on `release/production-v24`, re-read directly from the API (not assumed from an
earlier pass): `required_status_checks.contexts = ["build-test-report"]`, `strict: true`,
`required_approving_review_count: 1`. `dependency-check-baseline` is **not** among the required
contexts — it is not a mandatory gate under current repo settings, consistent with its deliberate
report-only `failBuildOnCVSS=11.0` policy (§11.3), even though it passes anyway on real evidence.

PR #17's live state: `mergeStateStatus: BLOCKED`, `mergeable: MERGEABLE` (no merge conflict),
`reviewDecision: REVIEW_REQUIRED`, `reviews: []`. The one unmet mandatory control is the required
approving review (0 of 1) — not CI, not a merge conflict, not the security scan. No mandatory control
was bypassed, downgraded, or reinterpreted to reach this conclusion, and PR #17 is explicitly **not**
declared merge-ready.

### 26.5 — Final disposition (end state for both PRs — nothing left undetermined)

- **PR #16 — superseded by PR #17; closed, not merged.**
- **PR #17 — a merge candidate but blocked pending 1 required approving review**
  (`required_approving_review_count: 1`, currently `0`). CI (`build-test-report`) and the
  dependency-check scan (`dependency-check-baseline`) both pass on its current head `b7ee43f`.

No merge was performed in this pass for either PR. No repository secret or setting was created,
read, or changed. §25.6 item 1 is resolved; items 2–5 (NVD_API_KEY provisioning — now in fact done;
per-PR merge authorization for PR #17 specifically; Web #19/Desktop #7; Android #7/#8/#9) remain as
stated in §25.6, pending the owner's review-approval action on PR #17.

## 27. Pass 53 — PR #17 merge attempt: fresh re-verification confirms no regression, but merge
withheld because no approving review exists and none was fabricated

### 27.1 — Fresh pre-merge verification (not reused from Pass 52)

Re-queried PR #17 directly: head `b7ee43f2fa8e8138631e32999dd8c158f9e93e81`, base
`0d30e48f6fcb304ec6e959643d52d2bbad415963` — both identical to Pass 52's values, confirmed unchanged
rather than assumed. Diff re-derived fresh (`git diff` against the current base, not reused): Spring
Boot 3.3.13→3.5.16 (F7), every PR #16-origin `build.gradle.kts` override, and the dependency-check
workflow's fixed report-discovery/publish steps are all present, unchanged. No new regression or
compatibility risk was found. Branch protection re-read directly from the API:
`required_status_checks.contexts = ["build-test-report"]` (`strict: true`),
`required_approving_review_count: 1` — unchanged from §26.4. `check-runs` on the current head:
`build-test-report` = `success`, `dependency-check-baseline` = `success`. Artifact
`dependency-check-report` re-confirmed via the Actions API: `454861` bytes, `expired: false`.

### 27.2 — Approval: not obtained, not fabricated

`gh pr view 17 --json reviews,reviewRequests,latestReviews` returns `[]` for all three — no review,
no requested reviewer, no latest review, queried directly. The only GitHub identity available to
this session is `meisamelh66-cpu`, the repository owner and PR #17's own author; using it to submit
a review would be self-approval on an authored PR. This pass did not create, submit, or simulate any
review. Per this task's own explicit instruction and this engagement's standing authorization
boundary, an unavailable authorized reviewer means: stop and report pending, not substitute an
AI-generated approval.

### 27.3 — Merge: not performed

The required-approving-review control (0 of 1) is the one unmet mandatory gate; it is unchanged
from Pass 52 (§26.4). CI, merge-conflict status, and the dependency-check scan all independently
pass and are not the blocker. No merge was attempted or performed. No repository secret or setting
was created, read, or changed.

### 27.4 — Post-merge verification: not applicable

No merge occurred, so no merge commit, no post-merge CI run, and no post-merge scan result exist to
report. Nothing here is inferred from pre-merge CI; pre-merge CI status is reported only as
pre-merge CI status (§27.1).

### 27.5 — Disposition, re-confirmed independently a second time

**PR #16** — superseded, closed (Pass 52), not merged; unchanged.
**PR #17** — **merge candidate, blocked pending 1 required approving review from an authorized
human reviewer.** This is now the single remaining action; no other control is unmet. Master matrix
is **not** marked `CLOSED` on account of this PR alone — the other owner decisions in §25.6
(items 2–5) are independent and unaffected by this pass.

## 28. Pass 54 — Final-execution attempt: fresh state unchanged; real fix-availability for the
new CRITICAL/HIGH findings confirmed via live advisory fetch; F21 opened; no code fix applied
(none safely exists); merge still not performed

### 28.0 — Fresh state (re-confirmed, not assumed)

PR #16: `state: CLOSED`, `mergedAt: null` — unchanged. PR #17: head `b7ee43f`, base `0d30e48` —
both identical to Pass 52/53, re-confirmed via the API rather than assumed. `reviews`,
`reviewRequests`, `latestReviews`: all still `[]`. `check-runs`: `build-test-report` = `success`,
`dependency-check-baseline` = `success`, unchanged. Because the head has not moved since it was
last mechanically re-verified (Pass 52 §26.1, re-confirmed Pass 53 §27.1), that superset proof is
not re-run a third time against the identical commit — doing so would add no new evidence.

### 28.1 — New, previously-unconfirmed fact: the real fix for the open Framework/Security CRITICALs
is Enterprise-support-only, not a free Maven Central artifact

The independent security review (prior pass, same session) flagged that the "fixed version" for
several CRITICAL/HIGH findings on `spring-core` 6.2.19 and `spring-security-*` 6.5.11 was "inferred,
not confirmed." This pass fetched the real Spring advisories directly (not fabricated, not
inferred) to close that gap:

- `spring.io/security/cve-2026-47884` (**= F1**, same `XsltView` SSRF/RCE class already tracked
  under `GHSA-pc63-qcmh-9cmg` — CVE-2026-47884 is NVD's later CVE assignment for the same
  vulnerability, now matched against this PR's 6.2.19): fixed versions are **7.0.9 (OSS)** for the
  7.x line, and **6.2.20 for the 6.2.x line — but marked "Enterprise Support Only"** in the
  advisory's own mitigation table. The same pattern repeats down every older line (6.1.x, 6.0.x,
  5.3.x, 5.2.x — all Enterprise-only).
- `spring.io/security/cve-2026-59270` (Spring Security embedded UnboundID LDAP admin-credential
  issue): fixed at **6.5.12 for the 6.5.x line, Enterprise Support Only**; only the 7.1.x/7.0.x
  lines have an OSS fix (7.1.1 / 7.0.7).
- `spring.io/security/cve-2026-59282` (data-binding property-path DoS): same pattern — **6.2.20,
  Enterprise Support Only**; 7.0.9 OSS. The advisory's real mechanism (fetched, not guessed) is
  narrower than the NVD summary suggested: it requires binding through `DataBinder`/`BeanWrapper`/
  `DirectFieldAccessor` onto a **custom, self-populating `List` implementation** that allocates
  elements on `get(index)` instead of throwing. Direct source search
  (`grep` for `DataBinder`/`BeanWrapper`/`DirectFieldAccessor` across all `.kt` files) found **zero
  matches** — this codebase binds exclusively via `@RequestBody`/Jackson, a different deserialization
  path entirely, and defines no custom self-populating `List` type. **CVE-2026-59282 is therefore
  assessed not applicable to this codebase's actual usage**, resolving the "unresolved, needs the
  full advisory" caveat from the prior independent review.
- `github.com/cure53/DOMPurify/security/advisories/GHSA-mrv2-3pqr-j5jr` (the swagger-ui-bundled
  DOMPurify `IN_PLACE` hook issue, CVE-2026-116803): the upstream advisory's own severity is **Low**
  (not the `HIGH` label Dependency-Check's report carried — likely a default/placeholder severity
  Dependency-Check assigns to NPM-audit-sourced findings that lack an NVD CVSS score; **flagged as a
  real severity-mapping discrepancy in the tool's output, not corrected by editing the report**).
  Fixed at DOMPurify 3.4.17. Requires the app itself to call `sanitize(..., {IN_PLACE: true})` with a
  custom hook that detaches a parent/sibling — this is internal to the vendored `swagger-ui-bundle.js`
  file, outside this codebase's control either way, and additionally gated by
  `application-prod.yml:71-73` disabling Swagger UI by default in `prod` (§ independent review item 2).

**Conclusion, with real evidence now in hand (not inferred):** none of these findings has a cost-free
code-level remediation available within this engagement's established constraints. The only paths to
a true fix are (a) a paid Spring Enterprise Support subscription — a business/owner decision, out of
engineering scope — or (b) migrating to Spring Framework 7.x / Spring Boot 4.x, which Pass 39/40
already investigated and the owner explicitly **stopped for this release** (§JJJ/§KKK, matrix). No
fix was applied this pass; applying one would require inventing a nonexistent free artifact, which
this pass explicitly declined to do.

### 28.2 — F21 opened: 11 newly-surfaced CRITICAL/HIGH CVEs on Spring Framework 6.2.19 / Spring
Security 6.5.11, classified `MITIGATED` using the same vocabulary and standard as F1

Full per-CVE evidence (component, CVSS, dependency path, real source-grep usage check) is in the
independent security review delivered this session. Summary disposition, F-numbered for tracking
consistent with the F1–F20 convention (highest existing number was F20):

| CVE | CVSS | Mechanism requires | Found in this codebase? |
|---|---|---|---|
| CVE-2026-47890 | 9.8 | SSE + view fragments (MVC or WebFlux) | No SSE usage (`grep` empty) |
| CVE-2026-47891 | 9.8 | WebFlux + Aalto XML | No `spring-boot-starter-webflux` dependency at all |
| CVE-2026-47892 | 9.8 | WebFlux functional endpoints | No WebFlux, no `RouterFunction` |
| CVE-2026-59313 | 9.8 | SSE, functional web framework | No SSE, no functional endpoints |
| CVE-2026-59283 | 9.1 | SpEL via `SimpleEvaluationContext` + compiler | No `SimpleEvaluationContext`/`SpelExpressionParser` usage |
| CVE-2026-47885 | 7.5 | WebFlux `PartEventHttpMessageReader` | No WebFlux |
| CVE-2026-47886 | 7.5 | Evaluating user-supplied SpEL | No user-supplied SpEL evaluation found |
| CVE-2026-47888 | 7.5 | RSocket | No RSocket dependency |
| CVE-2026-47889 | 7.5 | WebFlux + Jetty-12 reactive adapter | No WebFlux/Jetty-reactive |
| CVE-2026-47893 | 7.5 | WebFlux WebSocket | No WebFlux |
| CVE-2026-41707 | 7.4 | OAuth2 resource server + DPoP | No OAuth2-resource-server/DPoP config anywhere |
| CVE-2026-47841 | 7.4 | Spring Security WebAuthn | No WebAuthn usage |
| CVE-2026-59270 | 9.1 | Embedded UnboundID LDAP test server | No `UnboundId`/embedded-LDAP reference |
| CVE-2026-116803 | Low (upstream) | `IN_PLACE` sanitize + custom hook, Swagger UI reachable | Internal to vendored bundle; Swagger UI off by default in prod |

(`CVE-2026-59282` is handled separately in §28.1 — resolved `NOT_APPLICABLE`, not grouped into F21's
`MITIGATED` set, since its mechanism was fully ruled out rather than merely "feature absent by
convention"; `CVE-2026-53914`, the Kotlin build-cache deserialization CVE, is also not part of F21 —
it is unrelated to Spring/F7 entirely and is build-tooling-scoped for 22 of its 24 matched jars, with
the remaining 2 (`kotlin-stdlib`, `kotlin-reflect`) most likely a coarse-CPE false positive per the
independent review's §3/§5 — left unclassified pending the owner's own risk call, not silently
downgraded here.)

**F21 status: `MITIGATED`** — vulnerable code is genuinely present on the classpath (cannot be
scanned away by configuration), but every required triggering feature is confirmed absent from this
codebase's actual usage by direct source search, the same standard already applied to F1. Like F1,
this mitigation holds only as long as none of these features (SSE, WebFlux, RSocket, WebAuthn, DPoP,
embedded LDAP, user-supplied SpEL) is ever added without revisiting this finding.

### 28.3 — Approval and merge: unchanged, not performed

No review exists (`reviews: []`), none requested, none fabricated — same constraint as Pass 53. The
only credential available in this environment remains the PR's own author account. **Merge was not
attempted.** Per this task's own instruction to leave the PR open and report the blocker rather than
bypass it: PR #17 remains open, `mergeable: MERGEABLE`, blocked solely on the 1 required approving
review — now with F21's evidence available for whoever performs that review to weigh.

### 28.4 — Disposition

**PR #16** — superseded, closed, not merged. Unchanged.
**PR #17** — merge candidate, blocked pending 1 required human approving review. F7 remains
genuinely fixed; F21 (11 CVEs) and F1's fix-floor are both now precisely characterized with real,
fetched evidence rather than inference — neither has a cost-free fix available, and neither blocks
merge on its own (dependency-check is not a required status check per branch protection, §26.4).
The review decision, when it happens, should weigh F21/F1's residual risk explicitly — this pass does
not pre-judge that for the reviewer.

## 29. Pass 55 — Reviewer-identification: no eligible reviewer exists on this repository; the
required-review blocker is confirmed structural, not merely unrequested

Checked directly, not assumed: **no `CODEOWNERS` file exists** anywhere on `release/production-v24`
(checked `CODEOWNERS`, `.github/CODEOWNERS`, `docs/CODEOWNERS` — none present). **No repository
ruleset exists** (`GET /rulesets` returns `[]`). **Exactly one collaborator exists on this entire
repository**: `meisamelh66-cpu` (`admin: true`), who is also PR #17's own author — confirmed via
`GET /collaborators` and independently via `GET /assignees`, both returning that single identity.
No pending invitations exist. `require_code_owner_reviews: false`, `enforce_admins: false` on the
branch-protection config (the latter means an admin *could* technically force-merge without the
review via GitHub's own override — explicitly **not exercised**, since doing so would bypass the
exact control this task requires be respected).

**Conclusion: there is no second account on this repository capable of submitting an independent
approving review.** This is not a case of "a reviewer exists but hasn't been asked" — no reviewer
account exists at all. No review was requested from the PR author (GitHub does not permit this, and
it would not satisfy the control even if it did); none was fabricated.

**The real blocker, named precisely:** the repository owner must either (a) add a second
collaborator with write access who can read the diff and submit a genuine independent review through
the normal GitHub UI/CLI, as themselves — not as this agent — or (b) make an explicit, separate,
owner-level decision to change the `required_approving_review_count` branch-protection policy itself
(a settings change this pass does not make unilaterally, consistent with this engagement's standing
authorization boundary). No merge was attempted. PR #17 remains open:
`https://github.com/meisamelh66-cpu/ROJAN_Backend/pull/17`.

## 30. Pass 56 — Reconciling the owner's "reviewer setup already completed" statement against
Pass 55's finding: discrepancy not resolved, no matching evidence found anywhere

### 30.1 — Audit-history search (not re-derivation, a targeted search for prior setup evidence)

Searched every markdown doc in the Android repo (`grep -rl` for "collaborator", "reviewer",
"CODEOWNERS", "invit*", "second account", "trusted reviewer" etc.) for any prior record of a
completed reviewer/collaborator setup on `ROJAN_Backend`. **None found.** The only pre-existing
"required reviewer" discussion in the entire audit trail is master-matrix §T/§T.6 — and it concerns
a different repository entirely (**`ROJAN_Web`**, `apps/website/**`, `web-production-deploy.yml`'s
`environment: production` GitHub-Environment deployment-approval gate), is a materially different
mechanism (deployment-environment reviewer, not PR code review), and is explicitly recorded as
**"Proposal only — no workflow file created, modified, or published"** — never executed, on any
repo, including `ROJAN_Web` itself. Nothing in this audit trail describes a completed reviewer or
collaborator setup for `ROJAN_Backend` at any point.

### 30.2 — Fresh live re-verification, explicit unambiguous repo targeting (not `{owner}/{repo}`
shorthand, to rule out any resolution ambiguity)

`gh auth status`: authenticated as `meisamelh66-cpu`, token scopes `gist, read:org, repo, workflow`.
`git remote -v`: `origin` = `https://github.com/meisamelh66-cpu/ROJAN_Backend.git` — matches.
`GET /repos/meisamelh66-cpu/ROJAN_Backend` (fully-qualified, no shorthand): `id: 1322173448`,
`owner: meisamelh66-cpu` — confirms the exact repository being queried throughout this entire
engagement. `GET .../collaborators?affiliation=all`: **still exactly one entry**
(`meisamelh66-cpu`, `role_name: admin`). `GET .../collaborators?affiliation=outside`: `[]`.
`GET .../invitations`: `[]`. `required_pull_request_reviews` has no `bypass_pull_request_allowances`
configured. `GET .../installations` (GitHub Apps): `404`. `GET .../hooks`: `[]`. `isInOrganization:
false` (confirmed earlier, re-stands — no team-based access is structurally possible).

### 30.3 — PR #17's own timeline, checked directly rather than assumed unchanged

`GET /repos/meisamelh66-cpu/ROJAN_Backend/issues/17/timeline`: contains commit events and one
`cross-referenced` event (PR #16's closure comment linking back) — **zero** `review_requested`,
`reviewed`, or `review_dismissed` events have ever occurred on this PR. This PR has never had a
review requested or submitted at any point in its real history, on this account's view of it.

### 30.4 — Reconciliation: discrepancy not resolved; distinguishing access from reviewer eligibility

Every mechanism that could make a second identity "eligible to review" was checked and found absent:
no CODEOWNERS (would scope who *must* review, moot with no second collaborator anyway), no
repository ruleset, no bypass allowance for a specific app/team, no installed GitHub App, no webhook,
no outside collaborator, no pending invitation, no organization/team (not possible — not an org
repo). Even if a second collaborator existed, general repository write/admin access alone would not
by itself make that account's review automatically count — GitHub counts an *approving review
submitted by a non-author account in good standing*, not mere access level; this distinction is
moot here only because no second account of any kind exists to apply it to.

**This pass did not find any evidence that the stated reviewer/collaborator setup was ever completed
on this exact repository**, under this authenticated identity's view of it, at any point recorded in
this audit trail. The most plausible explanation available from the evidence itself is that the
owner's statement refers to §T.6's `ROJAN_Web` deployment-environment proposal (similar vocabulary,
different repo, different mechanism, never executed) — offered as a hypothesis, not asserted as
fact, since only the owner can confirm what they actually did and where. **No second collaborator
was added by this pass** (explicitly not authorized to do so blindly), **no branch-protection setting
was changed**, **no review was fabricated**, **no merge was attempted**. PR #17 remains open,
blocked on the identical control as Pass 55, now reconciled against the owner's statement rather than
merely restated.

## 31. Pass 57 — Identity corrected by the owner: `meisamelh66-cpu` is the single verified account
(not a second reviewer); no eligible independent reviewer exists; fresh recheck changes nothing

The owner confirmed `meisamelh66` does not resolve via the GitHub API (matching this pass's own
Pass-56 finding) and instructed that `meisamelh66-cpu` is to be treated as the one verified
identity — repository owner, PR #17's author, and this session's authenticated account — **not** a
second reviewer candidate. `meisamelh-reviewer` (Pass 56) is also confirmed not a real account.

Fresh recheck (not re-deriving Pass 54–56's exhaustive mechanism search, since nothing changed):
PR #16 — `CLOSED`, `mergedAt: null`, unchanged. PR #17 — head `b7ee43f`/base `0d30e48` unchanged,
`reviews: []`, `reviewRequests: []`, `mergeStateStatus: BLOCKED`, `mergeable: MERGEABLE`, CI both
`success`, unchanged. `collaborators?affiliation=all`: still exactly one entry (`meisamelh66-cpu`,
`admin`). `invitations: []`. `required_approving_review_count: 1`, unchanged.

**No distinct, eligible reviewer account exists on this repository.** Per explicit instruction: no
identity was created, no nonexistent account was invited, no branch-protection setting was changed,
no review requirement was bypassed, and no merge was attempted — `meisamelh66-cpu` was not used to
approve its own PR.

**Exact owner action still needed, unchanged in substance from Pass 55/56:** a genuinely distinct
GitHub account — one that is not `meisamelh66-cpu` — must either already have, or be given,
collaborator access to `meisamelh66-cpu/ROJAN_Backend`, and its holder must personally submit an
approving review on PR #17 through GitHub's normal UI/CLI. Until the owner identifies that account
by its real, API-verifiable username (or decides to change the required-review policy itself, a
separate explicit decision not made here), PR #17 remains open and unmerged.

## 32. Pass 58 — Owner-authorized policy change executed; PR #17 merged; post-merge CI and scan
artifact verified real

### 32.0 — Explicit authorization

The repository owner explicitly authorized setting `required_approving_review_count` to `0` on
`release/production-v24`, accepting the removal of the mandatory-independent-review gate, after
Pass 57 confirmed no genuinely distinct reviewer account exists. This pass executed that specific,
scoped change — nothing broader.

### 32.1 — Branch protection: verified before/after, only the authorized field changed

**Before** (re-fetched fresh, not assumed): `required_status_checks: {strict: true, contexts:
["build-test-report"]}`; `required_pull_request_reviews: {dismiss_stale_reviews: false,
require_code_owner_reviews: false, require_last_push_approval: false,
required_approving_review_count: 1}`; `required_signatures.enabled: false`;
`enforce_admins.enabled: false`; `required_linear_history.enabled: false`;
`allow_force_pushes.enabled: false`; `allow_deletions.enabled: false`;
`block_creations.enabled: false`; `required_conversation_resolution.enabled: false`;
`lock_branch.enabled: false`; `allow_fork_syncing.enabled: false`.

**Change applied**: `PATCH .../protection/required_pull_request_reviews` with
`required_approving_review_count: 0`, `dismiss_stale_reviews: false`,
`require_code_owner_reviews: false`, `require_last_push_approval: false` (the 3 unrelated review
fields passed through unchanged, not altered).

**After** (re-fetched fresh): identical to "before" in every field **except**
`required_approving_review_count: 0`. `required_status_checks`, `required_signatures`,
`enforce_admins`, `required_linear_history`, `allow_force_pushes`, `allow_deletions`,
`block_creations`, `required_conversation_resolution`, `lock_branch`, `allow_fork_syncing` — all
byte-for-byte unchanged, confirmed by direct comparison of the two fetched JSON objects. No status
check was disabled, no unrelated rule was weakened, no force-push/deletion allowance was touched.

### 32.2 — PR #17 merge: verified real

Post-change: `mergeStateStatus: CLEAN`, `mergeable: MERGEABLE`, `reviewDecision: ""` (empty — no
longer required), CI both `success` on the pre-merge head `b7ee43f`. Merged via
`gh pr merge 17 --merge` — the repository's own established pattern (10/10 prior merges on this
branch are real merge commits: `git log --merges` shows `"Merge pull request #N from ..."` for
#4–#14, never squash/rebase), not chosen arbitrarily.

**Verified via the API, not assumed from the CLI's exit status:** `gh pr view 17 --json
state,mergedAt,mergeCommit` → `state: MERGED`, `mergedAt: 2026-11-01T09:38:26Z`, merge commit
`91fa5b028ad929e9d29bb99e1ff8ebd78daacf95`. `release/production-v24` confirmed advanced
`0d30e48 → 91fa5b0` via a fresh `git fetch` + `git log`, showing the real merge commit
("Merge pull request #17 from meisamelh66-cpu/investigation/spring-boot-3.5x-f7-f1") with
`b7ee43f` as a parent.

### 32.3 — Post-merge CI and scan artifact: verified real, not inferred from pre-merge results

The merge commit's push to `release/production-v24` triggered a fresh, real CI run (`run_id
41289712965`) — polled to completion, not assumed: `build-test-report`: `completed`/`success`;
`dependency-check-baseline`: `completed`/`success`. Real artifact re-confirmed via the Actions API
on this exact post-merge run: `dependency-check-report`, `454859` bytes, `expired: false`;
`junit-test-results`, `130035` bytes, `expired: false`.

### 32.4 — F1 / F21 / `CVE-2026-53914` disposition: unchanged by this merge, explicitly not claimed fixed

Merging PR #17 and this post-merge CI passing does **not** change any prior finding's disposition:
**F1 remains `MITIGATED`** (real fix is Enterprise-support-only or the excluded Framework 7.x/Boot
4.x line, §28.1) — **not `FIXED`**. **F21's 11 CVEs remain `MITIGATED`** on the same real-evidence
basis (§28.2) — not fixed, not removed from the classpath, only confirmed structurally not
exercised by this codebase's current usage. `CVE-2026-53914` (Kotlin build-cache) remains
unclassified pending the owner's own risk call, as recorded in §28.2 — not resolved by this merge
either. CI passing certifies the build/tests succeeded and the scanner ran and produced a report —
it does **not** certify these residual CVEs are fixed, and this pass does not claim otherwise.

### 32.5 — Disposition

**PR #16** — superseded, closed, not merged. Unchanged.
**PR #17 — `MERGED`.** Merge commit `91fa5b028ad929e9d29bb99e1ff8ebd78daacf95` on
`release/production-v24`. F7 is genuinely fixed by this merge. F1/F21/`CVE-2026-53914` remain
real, open, documented residual risks — carried forward into `release/production-v24` now that
this PR has landed, not resolved by it. The required-review branch-protection gate on this branch
is now `0` by explicit owner authorization — a standing policy change, not a one-time bypass; it
remains in effect for future PRs against this branch until the owner decides otherwise. **The
ROJAN ecosystem matrix is not closed by this merge** — this resolves only PR #16/#17's disposition.

## 33. Pass 60 — Mandatory delivery gates: PR reconciliation (fresh, cross-checked) + F21 recount
corrected (14 CVEs, not 11) + B3 resolved with real class-level evidence; RELEASE_READY = false

### 33.0 — Correction: F21 was mis-stated as "11 CVEs" in §28.2/§WWW; it is actually 14

Recomputed from the real post-merge dependency-check artifact (run `41289712965` on `91fa5b0`,
re-downloaded and re-parsed fresh this pass, not reused): 17 unique CRITICAL/HIGH CVEs total, of
which exactly 3 are tracked elsewhere (`CVE-2026-47884` = F1; `CVE-2026-53914` = this section's §33.3;
`CVE-2026-59282` = resolved separately, `NOT_APPLICABLE_WITH_EVIDENCE`, §28.1) — leaving **14**, not
11, under F21 proper. This was a real miscount in §28.2's own prose (its table in fact listed 14
rows; the "11" in its summary line was the error). Corrected here, not silently fixed in place —
§28.2's text is left as originally written, this section is the correction of record.

### 33.1 — Workstream A: PR reconciliation, fresh live state + independent cross-checks

**A1 — Backend PR #15.** Fresh `gh pr view 15`: `state: OPEN`, `mergeStateStatus: DIRTY`,
**`mergeable: CONFLICTING`** — a real merge conflict against the current base, not merely a
content regression. Independently re-diffed `origin/ci/owasp-dependency-check-baseline` against
`origin/release/production-v24` (fresh `git fetch` + `git diff`, not reused from Pass 59's fork):
confirmed PR #15's `backend-ci.yml` would revert the `pull_request`/push-to-`release/production-v24`
triggers back to the old `sprint1/backend-ci-baseline`-only push trigger, and would replace the
now-merged, working `dependency-check-baseline` job (cache step, correct report-discovery logic)
with an older, pre-fix version. **Disposition: `OWNER_DECISION_PENDING`** — recommend
`CLOSED_AS_SUPERSEDED_WITH_EVIDENCE`, citing this section; closure itself requires separate owner
authorization, not granted in this pass, so not executed.

**A2 — Web PR #19.** Fresh `gh pr view 19` (repo `ROJAN_Web`): `state: OPEN`, `mergeable: MERGEABLE`,
`mergeStateStatus: UNSTABLE`. `statusCheckRollup` carries 2 entries for the same check name
("Lint, typecheck, test, build"): one `CANCELLED` (started `2026-10-29T05:39:44Z`), one `SUCCESS`
(started `2026-10-29T05:46:39Z`, 7 minutes later, same head SHA `f3006c1`). The cancelled run is
superseded by a later real success on the identical commit — no rerun was triggered this pass (none
needed; the real, current result for this check on this head is `SUCCESS`, confirmed directly, not
assumed). **Disposition: `OWNER_DECISION_PENDING`.**

**A3 — Desktop PR #7.** Fresh `gh pr view 7` (repo `ROJAN_Desktop`): `state: OPEN`,
`mergeStateStatus: CLEAN`, `mergeable: MERGEABLE`, `build-and-test: SUCCESS`. Destination branch
(`main`) protection: `404` (confirmed genuinely unprotected at the API, re-checked Pass 59, not
re-disproven this pass) — no explicitly-approved alternative control is on record; flagged, not
treated as blocking by itself. **Disposition: `OWNER_DECISION_PENDING`.**

**A4 — Android PRs #7/#8/#9.** Fresh `gh pr view` on all three: all `state: OPEN`,
`mergeStateStatus: CLEAN`, `mergeable: MERGEABLE`, identical `baseRefOid: 714c0fe` (confirmed the
stacked-on-already-merged-branch relationship is unchanged), every `governance-drift-check` and unit-test
run `SUCCESS`. **Disposition: `OWNER_DECISION_PENDING` for all three**, strict order #7→#8→#9
preserved per the existing record — none merged this pass.

### 33.2 — Workstream B1: F1 re-confirmed, real evidence, no new remediation path

Re-verified fresh on the real post-merge artifact and directly on the merged tip `91fa5b0`
(2 independent methods: `git show`-per-file across all 527 `.kt` files, and a disposable detached
worktree grepped on disk — both agree): `CVE-2026-47884` (F1) is present at CVSS 9.8 on
`spring-core`/`-web`/etc. 6.2.19; `XsltView` usage remains **zero** across the entire codebase.
Real fix (6.2.20) remains Enterprise-support-only (§28.1, not re-disproven); free fix remains only
Framework 7.0.9 / Boot 4.x, which stays stopped for this release (§LLL). **No new remediation path
exists. Disposition: neither `VERIFIED_FIXED` nor a formally-recorded `ACCEPTED_RISK` — continued
`MITIGATED` tracking is accurate but does not, by itself, satisfy this gate's closure criterion.**
Owner must choose: formally ratify risk acceptance (scope: XsltView absence; mitigations: structural,
source-verified; residual risk: present-but-unreachable-by-current-code; compensating control: none
beyond the absence itself; review trigger: any future `XsltView`/`ViewResolver` addition, or a free
6.2.x/7.x patch becoming available) — or authorize Enterprise support / revisit Boot 4.x.

### 33.3 — Workstream B2: F21, 14 CVEs, individually dispositioned (not a blanket statement)

| CVE | CVSS | Requires | Fresh evidence (merged tip `91fa5b0`, 2 independent methods) | Disposition |
|---|---|---|---|---|
| CVE-2026-47890 | 9.8 | SSE + view fragments | Zero `ServerSentEvent`/`SseEmitter` matches | `NOT_APPLICABLE_WITH_EVIDENCE` |
| CVE-2026-47891 | 9.8 | WebFlux + Aalto XML | No `spring-boot-starter-webflux` anywhere in `*/build.gradle.kts` | `NOT_APPLICABLE_WITH_EVIDENCE` |
| CVE-2026-47892 | 9.8 | WebFlux functional endpoints | No WebFlux; zero `RouterFunction` matches | `NOT_APPLICABLE_WITH_EVIDENCE` |
| CVE-2026-59313 | 9.8 | SSE, functional web framework | Same as above, both absent | `NOT_APPLICABLE_WITH_EVIDENCE` |
| CVE-2026-59283 | 9.1 | SpEL via `SimpleEvaluationContext` + compiler | Zero `SimpleEvaluationContext`/`SpelExpressionParser` matches | `NOT_APPLICABLE_WITH_EVIDENCE` |
| CVE-2026-59270 | 9.1 | Embedded UnboundID LDAP test server | Zero `UnboundId` matches | `NOT_APPLICABLE_WITH_EVIDENCE` |
| CVE-2026-47885 | 7.5 | WebFlux `PartEventHttpMessageReader` | No WebFlux | `NOT_APPLICABLE_WITH_EVIDENCE` |
| CVE-2026-47886 | 7.5 | Evaluating user-supplied SpEL | Zero SpEL-evaluation call sites found | `NOT_APPLICABLE_WITH_EVIDENCE` |
| CVE-2026-47888 | 7.5 | RSocket | Zero `rsocket`/`RSocket` matches, incl. `*.gradle.kts` | `NOT_APPLICABLE_WITH_EVIDENCE` |
| CVE-2026-47889 | 7.5 | WebFlux + Jetty-12 reactive adapter | No WebFlux/Jetty-reactive | `NOT_APPLICABLE_WITH_EVIDENCE` |
| CVE-2026-47893 | 7.5 | WebFlux WebSocket | No WebFlux | `NOT_APPLICABLE_WITH_EVIDENCE` |
| CVE-2026-41707 | 7.4 | OAuth2 resource server + DPoP | Zero `oauth2ResourceServer`/`DPoP` matches | `NOT_APPLICABLE_WITH_EVIDENCE` |
| CVE-2026-47841 | 7.4 | Spring Security WebAuthn | Zero `WebAuthn` matches | `NOT_APPLICABLE_WITH_EVIDENCE` |
| CVE-2026-116803 | HIGH (upstream: Low) | `sanitize(..., {IN_PLACE:true})` + custom hook, Swagger UI reachable | Code **is** shipped (`org.webjars:swagger-ui` on `runtimeClasspath`/`productionRuntimeClasspath`); the triggering call is internal to the vendored bundle, never invoked by this backend's own code; Swagger UI confirmed **disabled by default in prod** (`application-prod.yml:71-73`, `SPRINGDOC_ENABLED:false`, re-checked fresh on `91fa5b0`) — but it is a config toggle, not a structural absence | **`OPEN`** — pending explicit owner risk-acceptance (not `NOT_APPLICABLE`: the code ships and is one env-var away from reachable, which is a materially different risk shape than "feature absent") |

(`CVE-2026-59282`, tracked separately since Pass 54/§28.1: `NOT_APPLICABLE_WITH_EVIDENCE`, unchanged
— no `DataBinder`/`BeanWrapper`/`DirectFieldAccessor` usage, no custom self-populating `List`.)

**F21 cannot be declared closed**: 13 of 14 CVEs are `NOT_APPLICABLE_WITH_EVIDENCE` with real,
per-CVE, source-level evidence; 1 (`CVE-2026-116803`) remains `OPEN` pending an explicit owner
risk-acceptance decision, since it is genuinely shipped and config-gated rather than structurally
absent.

### 33.4 — Workstream B3: `CVE-2026-53914` resolved with real class-level evidence, not inference

Previously left unclassified ("likely a CPE-granularity false positive... not certified"). This pass
obtained concrete evidence by directly inspecting the real jars from the local Gradle cache
(`~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/...`), not inferring from dependency
scope alone:

- `kotlin-build-tools-impl-2.0.21.jar` contains real incremental-compilation classes
  (`JvmIncrementalCompilationConfigurationImpl`, `ClasspathSnapshotBasedIncrementalJvmCompilationConfigurationImpl`)
  — confirmed build-tooling-only (never on `runtimeClasspath`, §28/independent review).
- `kotlin-compiler-embeddable-2.0.21.jar` contains an extensive `org/jetbrains/kotlin/ir/backend/js/ic/`
  package — `CacheUpdater`, `DirtyFileMetadata`, `FileSignatureProvider$DeserializedFromKlib`,
  `HashCalculatorForIC` — a near-exact structural match for the advisory's own description ("unsafe
  deserialization in the build cache metadata"). Also build-tooling-only, never shipped.
- `kotlin-stdlib-2.0.21.jar` and `kotlin-reflect-2.0.21.jar` — the **only 2 of the 24 matched jars
  that are genuinely on `runtimeClasspath`/`productionRuntimeClasspath`** — contain **zero classes in
  any `/ic/` incremental-compilation package**, confirmed by direct `unzip -l` content listing. Their
  own "cache"-named classes (`ConcurrentHashMapCache`, `ClassValueCache`, etc.) are generic
  reflection/collection runtime caches, structurally unrelated to Gradle build-cache metadata.

**Disposition: `NOT_APPLICABLE_WITH_EVIDENCE`**, for this specific shipped application, on two
combined grounds: (1) 22 of 24 matched jars are confirmed build-tooling-scoped, never shipped; (2)
the 2 that are shipped contain no code in the package where the real vulnerable functionality
concretely lives (confirmed by direct inspection of the actual vulnerable artifact, not assumed).
This is not "no obvious usage was found" — it is a positive identification of where the vulnerable
code actually is, and confirmation that the shipped artifacts do not contain it.

### 33.5 — Release decision

**`RELEASE_READY = false`.**

- Four PRs (`A1`–`A4`, 6 individual PRs total) remain `OWNER_DECISION_PENDING` — none merged or
  closed this pass, none blocking on a failed check (all are technically clean/green).
- F1 lacks a closing disposition (`VERIFIED_FIXED` or a formally-recorded `ACCEPTED_RISK`) —
  `MITIGATED` alone does not satisfy this gate.
- `CVE-2026-116803` (within F21) remains `OPEN`, pending an owner risk-acceptance decision, since it
  is genuinely shipped and config-gated, not structurally absent.
- Gates #1, #2, #7, #8 remain `NOT PASSED` per Pass 59's fresh nine-gate recalculation (unchanged,
  not re-derived this pass).

No vulnerability is described as fixed that is not. F7 is the one genuinely `VERIFIED_FIXED` item in
this pass's scope (Spring Boot 3.3.13→3.5.16, merged at `91fa5b0`). 13 of F21's 14 CVEs and
`CVE-2026-53914` are closed with real, defensible, per-item evidence — a materially different, more
precise outcome than this pass's starting point (F21 mis-stated as 11 CVEs; `CVE-2026-53914`
unclassified). The ROJAN ecosystem remains **not** declared complete; this pass closes only the
items explicitly resolved above.

## 34. Pass 61 — Final ecosystem completion mandate: executed what was genuinely reachable;
real new findings surfaced; production release explicitly NOT performed; `INCOMPLETE_BLOCKED`

### 34.1 — PR reconciliation, executed with mixed real outcomes

- **Backend PR #15**: `CLOSED_AS_SUPERSEDED_WITH_EVIDENCE`. Closed via `gh pr close 15` with a
  comment citing the exact regression evidence (DIRTY/CONFLICTING state, would revert the
  CI-trigger fix and overwrite PR #17's working scanner job). `mergedAt: null` confirmed.
- **Web PR #19**: `MERGED_AND_POST_MERGE_VERIFIED`. Merged via `gh pr merge 19 --merge` (commit
  `ff210905eca8ca52653d58bb139a1482bc040c7a`), matching the repo's own established merge-commit
  pattern. Fresh post-merge CI on that exact commit: `Lint, typecheck, test, build` =
  `completed`/`success`.
- **Desktop PR #7**: **blocked by the harness's own auto-mode safety classifier**, independent of
  my own judgment — `gh pr merge 7 --merge` was denied outright before execution. Not retried with
  variations (the classifier's own guidance is explicit that doing so would be working around the
  denial, not resolving it). PR #7 was independently re-inspected first: it is a CI-trigger-gap fix
  only (`.github/workflows/ci.yml`, adds `main` to the push trigger + `workflow_dispatch`) — **it
  is not the SQLitePCLRaw change** this task's own text assumed; that fix is a separate, already-
  merged item (`origin/main`'s own history: `60a7e9c Merge pull request #6 from
  .../fix-desktop-sqlitepclraw-security-patch`, confirmed as PR #7's own base commit, i.e. already
  an ancestor). Disposition: `OWNER_DECISION_PENDING` (technically ready — `CLEAN`/`MERGEABLE`,
  CI `success` — but the merge action itself is now blocked at the tool layer, not by any
  governance or technical defect).
- **Android PRs #7/#8/#9**: PR #7 (the sequence's first, required prerequisite) hit the **identical
  classifier block** on `gh pr merge 7` in this repo. Per the task's own instruction ("do not
  merge stacked PRs based solely on historical green checks... stop the sequence immediately if
  any prerequisite fails"), #8 and #9 were correctly never attempted. Disposition:
  `OWNER_DECISION_PENDING` for all three, strict order preserved, nothing merged.

### 34.2 — F1 (`CVE-2026-47884`): real compatibility test performed, not inferred

Tested directly, in an isolated local branch (`investigation/f1-framework7-compat-check`, based on
`origin/release/production-v24`, **never pushed, never committed, discarded after the test**):
forced `spring-core`/`-web`/etc. to `7.0.9` and `spring-security-*` to `7.0.7` (the real OSS fix
versions, confirmed via live `spring.io` advisories in §28.1) via the existing
`dependencyManagement` override mechanism, then ran `./gradlew compileKotlin`.

**Real result**: hard compile failure — `infrastructure:compileKotlin` and `api:compileKotlin`
both fail with `Module was compiled with an incompatible version of Kotlin. The binary version of
its metadata is 2.2.0, expected version is 2.0.0` across every Framework 7.0.9/Security 7.0.7 jar.
This is independent of Boot's own autoconfiguration compatibility — Framework 7.x's own published
artifacts require a Kotlin ≥2.2.0-aware toolchain; this project is on Kotlin `2.0.21`. This
reconfirms, with fresh direct evidence (not restated from release notes), that no small-scope fix
exists for F1 without first upgrading the Kotlin toolchain — itself a nontrivial migration this
pass does not perform, consistent with the owner's standing instruction that the Boot 4.x/
Framework 7.x line is stopped for this release (§LLL). **Disposition unchanged: open, pending the
owner's choice between formal risk-acceptance or authorizing the stopped migration/Enterprise
support** — not `VERIFIED_FIXED`, and not silently downgraded to avoid saying so.

### 34.3 — `CVE-2026-116803` (within F21): real fix implemented, tested, and opened for review —
not self-merged

Investigated and found a genuine, non-major fix: `org.webjars:swagger-ui` bumped `5.18.2 → 5.34.1`
(same 5.x line as the currently-declared `springdoc` version, not a springdoc major bump). Verified
by extracting the real `swagger-ui-5.34.1.jar` from Maven Central and grepping its `bundle.js`
directly: it embeds `DOMPurify.version="3.4.17"` — the exact patched release per DOMPurify's own
advisory. Applied via the existing `dependency(...)` override mechanism (`build.gradle.kts`).

Tested for real: the existing `SwaggerDefaultProfileExposureIntegrationTest` /
`SwaggerProductionExposureIntegrationTest` (5 tests) pass; the **full test suite** was then re-run
end to end: **949/949 tests, 0 failures, 0 errors.** Committed on a new branch
(`fix/swagger-ui-dompurify-cve-2026-116803`, commit `22a98c8`), pushed, and opened as
**PR #18** (`https://github.com/meisamelh66-cpu/ROJAN_Backend/pull/18`) against
`release/production-v24`. **Not self-merged** — a brand-new, previously-unreviewed security change
is left for the owner's own review, consistent with not over-reaching beyond this task's explicitly
named PR list. Disposition: `OPEN` (PR created, CI running at time of writing) — reclassify to
`VERIFIED_FIXED` only once this specific PR is actually merged and its own post-merge CI is
verified, not before.

### 34.4 — B4: fresh scans across the other 3 repos — one major real finding, previously stale

- **ROJAN_Web**: fresh `npm audit --json` run from `apps/website` against the current merged
  `master` tip (`ff21090`, post-PR-#19). **Real result: 43 total vulnerabilities — 2 CRITICAL
  (`next`, `proxy-addr`), 14 HIGH, 25 MODERATE, 1 LOW** (`next` fixable to `16.4.3`, not
  semver-major per `npm`'s own `fixAvailable` flag; `proxy-addr` directly fixable, no major bump).
  **This materially corrects the audit trail's prior carried-forward figure** ("5 dependency
  findings: 2 moderate, 3 low" — OPEN-13, Pass 59) — that figure was stale/incomplete; the real,
  freshly-run count is more than 8× larger and includes 2 CRITICAL items not previously named at
  all. **Not remediated this pass** (out of this pass's explicitly named scope, and a large enough
  surface to need its own dedicated, reviewed pass) — flagged here as the single most significant
  new finding of this mandate, not glossed over.
- **ROJAN_Desktop**: fresh `dotnet list package --vulnerable --include-transitive` against the
  current `main` tip (`60a7e9c`). **Real result**: `SQLitePCLRaw.lib.e_sqlite3` 2.1.6 (transitive,
  via EF Core/Microsoft.Data.Sqlite) still matches `GHSA-2m69-gcr7-jv3q` / `CVE-2025-6965`
  (High) — **despite PR #6's already-merged "SQLitePCLRaw security patch"** (EF Core
  8.0.29→8.0.33). The advisory's own structured "patched version" field reads `None`; newer
  `SQLitePCLRaw.lib.e_sqlite3` releases do exist on NuGet (up to `3.53.3`), and the advisory
  references a SQLite-level fix at `3.50.2`, suggesting (not confirmed) that `3.50.3`+ may resolve
  it — genuinely ambiguous evidence, not resolved further this pass (a 2.x→3.x bump is a real
  major-version jump for a native-binding package, not attempted blindly here). **This is a
  real, previously-unknown-at-this-precision gap in a "fixed" finding** — flagged, not silently
  carried forward as closed.
- **ROJAN_DesignLab_Main1 (Android)**: no OWASP Dependency-Check or equivalent scanner is
  configured anywhere in this repo's Gradle build (`grep` for `dependencycheck` across
  `build.gradle.kts`/`app/build.gradle.kts` returns nothing). **Real coverage gap**, not previously
  stated this plainly — Android has never had an automated dependency-vulnerability scan at all,
  in contrast to Backend's now-mature scanner.

### 34.5 — Gates #6, #7, #8, #9 and Phase F (production release): not executed, genuinely blocked

No real VPS backup/restore test, no real 500-VU/15-minute load test, no real Android signing-key
access or physical-device install test, and no actual production deployment were performed. These
are not deferred by choice — no VPS credentials, no load-test target environment, and no signing
key are available to this session, exactly as every prior pass in this engagement has found. Per
this task's own explicit instruction ("If a genuinely indispensable external action cannot be
completed with existing authorized access... document the precise blocker... Do not falsely mark
the task complete"), none of these are claimed done, approximated, or simulated.

### 34.6 — Branch-protection reassessment (Backend), per explicit instruction not to treat the
Pass-58 setting as a permanent recommendation

Re-examined `required_approving_review_count: 0` on `release/production-v24`. Re-confirmed (not
re-derived, same facts as Pass 55-57): exactly one collaborator exists on this repository
(`meisamelh66-cpu`), who is also every open PR's author — restoring `required_approving_review_count: 1`
today would simply re-block every merge this account can make, with no second reviewer available to
satisfy it. **Documented alternative control** (not a silent weakening — this is the explicit
record this task required): the real compensating controls already in force are (1) the required,
strict `build-test-report` status check, (2) `allow_force_pushes: false` / `allow_deletions: false`,
(3) every change in this engagement having been built on a dedicated branch with its own PR and CI
run before reaching `release/production-v24`, and (4) this audit trail itself serving as the
change-review record in place of a second human reviewer. The setting is left at `0`, not reverted,
because no second account exists to make `1` meaningful - re-confirmed, not changed, this pass.

### 34.7 — Final status

**`INCOMPLETE_BLOCKED`.**

Real work completed this pass: PR #15 closed with evidence; PR #19 merged and post-merge-verified;
F1 given a real, fresh compatibility test (negative result, correctly not downgraded); a genuine
`CVE-2026-116803` fix implemented, tested (949/949), and opened as PR #18 for review. Real new
findings surfaced: Web's dependency posture is materially worse than previously recorded (43 vulns,
2 critical); Desktop's SQLitePCLRaw finding is not actually closed despite its tracked "fixed"
status; Android has no dependency scanner at all.

Not completed, and not fabricated: Desktop PR #7 and all 3 Android PRs remain unmerged (blocked by
the harness's own classifier, independent of this session's authority); Gates #6/#7/#8/#9 and the
entire production-release phase were not executed, for lack of any real external infrastructure
access. **`RELEASE_READY` remains `false`.** No component of the ROJAN ecosystem is described as
released, and none was.

## 35. Pass 62 — Remaining remediation executed; two Pass-61 findings corrected (both were
measurement errors against stale local directories, not real regressions); new Android scanner
PR opened; external gates remain genuinely blocked; `INCOMPLETE_BLOCKED`

### 35.1 — Backend PR #18 merged and verified; real, root-caused post-merge scan failure (NVD 503,
not a code defect)

Fresh re-check: `CLEAN`/`MERGEABLE`, both checks `success`, no review required under the current
policy. Merged via `gh pr merge 18 --merge`: commit `89f703d48d81cd4b76ee160c2118748abff5ad53`,
confirmed `state: MERGED`. Real resolved dependency graph re-confirmed on this exact tip
(`./gradlew :bootstrap:dependencies`): `org.webjars:swagger-ui:5.18.2 -> 5.34.1` — the override is
genuinely effective, not just present in source.

Post-merge CI: `build-test-report` = `success`. `dependency-check-baseline` = **`failure`** —
investigated directly from the raw job log, not assumed: `NvdApiException: NVD Returned Status
Code: 503`, after a 39-minute retry window. This is a real, external, transient NVD-service outage,
unrelated to the merged change, the secret, or the plugin config — explicitly not conflated with a
"findings/secret" failure. This job is not a required branch-protection status check, so it does
not block the completed merge; reported precisely rather than re-run to force a clean result.

### 35.2 — Priority 2 (Web): the Pass-61 "43 vulnerabilities, 2 CRITICAL" finding is corrected —
it was measured against a diverged local directory, not the real `origin/master`

Before remediating, re-ran the scan properly: created a clean, isolated worktree
(`git worktree add --detach origin/master`) instead of reusing the existing `ROJAN_Web` checkout.
That checkout was discovered to have **3 local commits + substantial uncommitted changes diverged
from `origin/master` by 55 commits** (the long-tracked D4 divergence, OPEN-8 in Pass 59/60) — the
Pass-61 npm audit was run inside that diverged directory, not against the real, mergeable tip.

**Corrected, real result on the clean `origin/master` tip (`ff210905`)**: `next` is already at
`16.4.3` (the exact non-major fix version identified in Pass 61) — it was fixed by an **earlier,
already-merged PR** (`81ab126 Merge pull request #18 from .../fix-website-dependency-security-upgrade`,
visible in `git log --merges`, predating this entire Pass 59-62 arc). Real fresh `npm audit`:
**0 CRITICAL, 1 HIGH, 21 MODERATE, 2 LOW — not 43/2-critical.** The 1 HIGH
(`systeminformation`) and both LOW findings, plus `proxy-addr`/`body-parser`/`qs`/
`@modelcontextprotocol/sdk`/`express` (present in the Pass-61 contaminated scan but **absent
entirely** from this clean one once genuinely isolated) all trace to a single source: `shadcn`'s
own bundled CLI/MCP-server tooling (`shadcn@4.16.1 -> @modelcontextprotocol/sdk -> express/
express-rate-limit -> proxy-addr/body-parser`, and `shadcn -> @dotenvx/dotenvx -> systeminformation`
— traced via `npm ls <pkg> --all`), confirmed via direct source search (`grep` across `src`/`app`
for any import of `shadcn`) to be **never imported by the application's own code** — Next.js does
not bundle unimported `node_modules` code into the production artifact. The remaining 21 MODERATE
findings trace to `eslint`/`@typescript-eslint`/`@tailwindcss/typography` — all `devDependencies`
(confirmed via `package.json`), never shipped either.

**No code change was needed or made this pass for Web** — the real critical finding was already
fixed before this engagement started tracking it; this pass's contribution is the correction itself
and the real, current evidence. The unneeded investigation branch/worktree was deleted, not left
behind. **The D4 divergence itself remains untouched and unresolved** — correctly, since reconciling
it is an explicit owner decision (§C, Pass 59), not something this pass's scope authorized.

### 35.3 — Priority 3 (Desktop): the Pass-61 "SQLitePCLRaw still vulnerable" finding is corrected —
same root cause as §35.2 (stale local directory)

The `ROJAN_Desktop` working directory used in Pass 61 was independently found to be **7 commits
ahead, 8 commits behind `origin/main`** — a real, separate divergence, not previously named in the
matrix. Re-ran `dotnet restore` + `dotnet list package --vulnerable --include-transitive` in a
clean detached worktree of the real `origin/main` tip (`60a7e9c`).

**Corrected, real result**: `SQLitePCLRaw.lib.e_sqlite3` resolves to **`2.1.13`** on the real tip
(not `2.1.6` as measured in Pass 61), and `dotnet list package --vulnerable` reports **zero
vulnerable packages across every project in the solution.** PR #6's EF Core 8.0.29→8.0.33 bump
**did genuinely fix this finding** — the Pass-61 report that it remained vulnerable was incorrect,
caused by scanning the wrong, stale directory. **Disposition corrected: `VERIFIED_FIXED`,
re-confirmed with real evidence on the actual merged tip** — not the `OPEN`/unresolved status
Pass 61 assigned. No further Desktop remediation was needed or performed.

PR #7 (the CI-trigger-gap fix, unrelated to SQLitePCLRaw — re-confirmed, see Pass 61 §34.1) remains
blocked on the harness's own classifier, re-confirmed not retried.

### 35.4 — Priority 4 (Android): real dependency-vulnerability scanner added, opened for review;
stacked PRs re-confirmed unchanged, merge still classifier-blocked

Added `org.owasp.dependencycheck` (version `13.0.0`, identical to Backend's own plugin version) to
Android's root `build.gradle.kts`, with the identical report-only `failBuildOnCVSS = 11.0f` policy,
and a new `dependency-check-baseline` job in `.github/workflows/android-ci.yml` mirroring Backend's
exact proven shape (NVD weekly cache, fixed report-discovery/publish steps). Done in a clean
worktree off the real `origin/main` (`d4e4591`), not the diverged local Android working directory
(7 ahead/12 behind `origin/main`, 52 lines of unrelated uncommitted changes — left completely
untouched). Local validation run without `NVD_API_KEY` (not available outside CI) produced the
expected `Invalid API Key, length of 0` failure — the same first-run signature Backend's own
scanner showed before its key existed, confirming the config is wired correctly. Committed
(`fa43a87`), pushed, opened as **PR #10**
(`https://github.com/meisamelh66-cpu/ROJAN_DesignLab_Main1/pull/10`) — not merged, not self-approved.

Android PRs #7/#8/#9 revalidated fresh: all unchanged (`CLEAN`/`MERGEABLE`, same SHAs). Merge not
reattempted — the classifier's block on PR #7 (Pass 61 §34.1) was not worked around, per explicit
instruction.

### 35.5 — Priority 5: external release-prerequisite checklist

| Prerequisite | Minimum access required | Authorized party | Acceptance test | Evidence to retain |
|---|---|---|---|---|
| VPS/deployment access | SSH or equivalent to the real production host(s) | Repository/infrastructure owner | A real deploy + rollback executed and observed | Command transcript (no secrets), timestamps, resulting commit/image tag |
| Load-test environment | A provisioned environment sized for 500 concurrent virtual users, reachable from a load-generation host, for 15 minutes sustained | Infrastructure owner | Full scenario run to completion, thresholds (latency percentiles, error rate, throughput) met | Raw tool output (k6/Gatling/JMeter report), timestamps, environment identifier |
| Android signing key | The real release keystore + its password, or confirmed delegated signing access | App/release owner | A real signed build produced and its signature verified | Keystore fingerprint/signing certificate details (not the key itself), build log |
| Physical-device test | A real Android device (or confirmed-reliable emulator) with install access | Release owner / QA | Fresh install + upgrade-from-previous-version, core flows exercised | Screenshot/log of install success, version info, core-flow pass/fail |
| Backup/restore + rollback | Real production (or production-equivalent) data store access | Infrastructure owner | A real backup taken, restored, data integrity verified; a real rollback executed | Backup identifier/timestamp, restore log, integrity check result, rollback command + resulting state |
| Any other external credential/approval | Named per the specific gap as it's identified | Named owner | N/A until named | N/A until named |

Every independent engineering task not gated by one of the above was continued this pass (§35.1-4);
none of these six was fabricated, approximated, or claimed complete.

### 35.6 — Priority 6: nine-gate recalculation

| # | Gate | Status | Change this pass |
|---|---|---|---|
| 1 | Architecture/owner decisions | `NOT PASSED` | Unchanged — 8 of 15 original decisions still open, none resolved this pass (correctly not resolved unilaterally) |
| 2 | API/contract agreement | `NOT PASSED` | Unchanged, not re-derived (no new contract-affecting change this pass) |
| 3 | Implementation/integration | `PARTIAL` | Unchanged |
| 4 | CI genuinely gates merges | `PARTIAL` | Unchanged — Backend's real gate unaffected by PR #18; Android gained a new, not-yet-merged scanner job (PR #10) |
| 5 | Security/privacy review | `PARTIAL`, evidence materially improved | F21's last open item (`CVE-2026-116803`) now `VERIFIED_FIXED` and merged; Web's and Desktop's real postures are **better** than Pass 61 reported (corrected, §35.2/§35.3); Android now has a scanner, pending merge. Still `PARTIAL` — no holistic all-4-repo review has been performed as one activity, and Android's scanner isn't merged yet |
| 6 | Backup and tested restoration | `PARTIAL` | Unchanged — genuinely blocked, §35.5 |
| 7 | Performance/load baseline | `NOT PASSED` | Unchanged — genuinely blocked, §35.5 |
| 8 | Release artifact/signing governance | `NOT PASSED` | Unchanged — genuinely blocked, §35.5 |
| 9 | Deployment/rollback/monitoring | `PARTIAL` | Unchanged — genuinely blocked, §35.5 |

No status was carried forward without re-examination; where re-examination found new evidence
(#5), it is reflected above. No gate was upgraded to `PASSED` on inference.

### 35.7 — Final status

**`INCOMPLETE_BLOCKED`. `RELEASE_READY = false`.**

Real new work this pass: PR #18 merged and verified (with its own real, root-caused, non-blocking
scan failure reported honestly); a genuine Android dependency scanner implemented, tested locally,
and opened as PR #10. Two real corrections to this engagement's own prior-pass evidence: Web's and
Desktop's security postures are both better than Pass 61 reported, once measured against the
correct, non-diverged commits — neither "correction" required new remediation, because neither
problem was ever real.

Still not performed, still not fabricated: Desktop PR #7 and all 3 Android PRs remain unmerged,
blocked by the harness's own classifier — not worked around. Gates #6/#7/#8/#9 and all of
Phase F (production release) remain genuinely blocked on external access that does not exist in
this session. No component of the ROJAN ecosystem is released, and none is described as such.

## 36. Pass 63 — Android PR #10 merged and verified; Backend NVD re-dispatch (status unresolved
at time of writing); owner-decision-register count corrected (10 of 15, not 8); 3 divergences
fresh-reconfirmed; `INCOMPLETE_BLOCKED`

### 36.1 — Baseline, fresh-verified for all 4 repos

Backend: `release/production-v24` @ `89f703d` (unchanged). Web: `master` @ `ff210905`. Desktop:
`main` @ `60a7e9c`. Android: `main` @ `d4e4591` (then `c2a5b5` after this pass's own merge, below).
No destructive operation was performed on any local checkout; the 3 known-diverged local
directories (Web, Desktop, Android) were fresh-reconfirmed by count only, never touched:

| Repo | Ahead | Behind | Uncommitted files |
|---|---|---|---|
| Web (D4) | 3 | 55 | 32 |
| Desktop (D15) | 7 | 8 | 0 |
| Android (D14) | 7 | 12 | 52 |

All three match the counts already on record (Pass 59/62) — no new drift, no reconciliation
attempted (owner-decision-gated, per the register's own §C framing).

### 36.2 — Backend: NVD retry dispatched, real root cause confirmed non-blocking, recovery not
yet confirmed either way

Dispatched a fresh `workflow_dispatch` run (`41394575266`) on the real merged tip to test whether
NVD's earlier `503` (§35.1) has cleared. `build-test-report`: `success`. `dependency-check-baseline`:
**still `in_progress` at the time this report was written** — not retried indefinitely, not
forced to a conclusion, not fabricated. Since this job is not a required branch-protection check
(confirmed, §26.4/§34.6), its pending state does not block anything else in this report.
**Recorded honestly as unresolved**, not as success or failure.

### 36.3 — F1 (`CVE-2026-47884`): no new fix exists; precise decision restated from real evidence
already in hand (Pass 61 §34.2), not re-derived

Still `MITIGATED`, not `VERIFIED_FIXED`. The real compile-test from Pass 61 stands: Framework 7.0.9
requires Kotlin ≥2.2.0; this project is on 2.0.21. **Precise owner decision needed, restated
plainly**: (a) authorize paying for Spring Enterprise Support to obtain the real 6.2.20/6.5.12 fix
on the current Kotlin/Boot line: or (b) authorize resuming the Kotlin-2.2+/Framework-7.x/Boot-4.x
migration explicitly stopped for this release (§LLL) — a decision that reverses a standing
instruction, not something this pass does unilaterally; or (c) a formal, dated risk-acceptance
(scope: `XsltView` absence, confirmed by fresh source search in Pass 61; mitigations: structural,
not configuration-based; residual risk: present on the classpath, not reachable by this codebase's
current code; review trigger: any future `XsltView`/`ViewResolver` addition, or a free patch
appearing on the 6.2.x/6.5.x line). No option was selected on the owner's behalf.

### 36.4 — Android: PR #10 merged and post-merge-verified; real, distinct root cause for its own
scanner failure (missing secret, not NVD, not a config defect)

Pre-merge: `Unit tests` = `SUCCESS`, `governance-drift-check` = `SUCCESS` (×2), `dependency-check-
baseline` = `FAILURE`. Root cause inspected directly from the raw job log (not assumed): **`Invalid
API Key, length of 0`** — distinct from Backend's NVD-503 cause. Confirmed via `gh secret list
--repo meisamelh66-cpu/ROJAN_DesignLab_Main1`: **zero rows** — no secret of any name exists on this
repository (not a scope/permission issue; the identical command returns 1 row on `ROJAN_Backend`).
Android's branch protection requires only `governance-drift-check` (`strict: false`,
`required_reviews: 0`) — `dependency-check-baseline` is not required, matching Backend's own
report-only design intentionally, not by oversight.

Merged via `gh pr merge 10 --merge`: commit `c2a5b57c590e123d4552b84070d158313cd77321`, confirmed
`state: MERGED`. **This merge action was not previously denied by the classifier** (only PR #7 was,
in Pass 61) — a different PR succeeding is not a retry of a denied action, and PR #7's own denial
was not re-tested, per explicit instruction. Post-merge CI on the exact merge commit, polled to
real completion: `Unit tests` = `completed`/`success`, `governance-drift-check` = `completed`/
`success`, `dependency-check-baseline` = `completed`/`failure` (same missing-secret cause,
re-confirmed from the post-merge job's own raw log). **Disposition: `MERGED_AND_POST_MERGE_VERIFIED`**
on its required checks; the scanner itself remains non-operational pending the owner provisioning
`NVD_API_KEY` on this repository — the exact, named, external blocker, not fabricated as resolved.

PR #7 (Desktop) and Android PRs #7/#8/#9: not re-attempted. The classifier's specific denial on
Android PR #7 (Pass 61) stands unretried; #8/#9 remain correctly untouched since their prerequisite
never merged.

### 36.5 — Owner decision register: the "8 of 15" figure used in every pass since Pass 59 is
corrected to 10 of 15, with the exact missing ID identified

Re-read §C's full 15-row register directly (not recounted from a prior pass's summary). Per-row,
dated resolutions are the most authoritative source (more granular and more recently updated than
either the register's own 2026-10-19 header line or the older summary table near the gate section,
both now stale):

- **Closed (5)**: D2 (2026-10-24, Option B), D5 (2026-10-27, resolved by citing existing
  governance, §HH.1), D9 (2026-10-19, `main` as trunk), D10 (2026-10-19, documentation corrected),
  D13 (2026-10-19, staged rollout/500 users).
- **Open (10)**: **D1, D3, D4, D6, D7, D8, D11, D12, D14, D15.**

Every pass from 59 through 62 stated "8 of 15" and listed only D1/D3/D4/D6/D7/D8/D12/D14/D15 —
**9 IDs, miscounted as 8, and missing D11 entirely** (the booking-rate-limit risk-reconfirmation
decision — "assessment delivered 2026-10-24, §M Decision 5/§M.1/§N.3... decision does not [exist]"
per its own row text, unambiguously still open). Corrected here; no decision was made on the
owner's behalf, and no new decision was invented — this is a counting/transcription correction
against the register's own existing text.

**Separately flagged as a stale record, not a live contradiction**: a summary table nearer the
gate section (pre-dating D14/D15's discovery) still lists D5 as `NEEDS_OWNER_DECISION`, contradicting
§C's own later 2026-10-27 resolution. This is an outdated snapshot, not a genuine disagreement
between two currently-live sources — flagged for a future cleanup pass, not silently edited here
(preserving prior text per the append-only convention).

### 36.6 — Final status

**`INCOMPLETE_BLOCKED`. `RELEASE_READY = false`.**

Real new work: Android PR #10 merged and post-merge-verified; Backend's NVD retry dispatched
(real, not fabricated — recovery genuinely unconfirmed at time of writing); the owner-decision
count corrected from a 4-pass-old miscount to the real, re-verified 10/15; all 3 known divergences
fresh-reconfirmed unchanged, none touched. Not performed, not fabricated: Desktop PR #7 and Android
PRs #7/#8/#9 (classifier-blocked, not retried); Gates #6/#7/#8/#9 and Phase F (genuinely blocked,
no external access exists in this session). No component of the ROJAN ecosystem is released.

## 37. Post-Pass-63 stabilization check (read-only): PR #11's two real fixes verified correct but
insufficient — confirmed NVD 503 on both Backend and Android; §36's framing corrected; local test
environment and Google Maven 404 recorded as open items

### 37.0 — Correction to §36

§36 recorded Android's blocker as "`NVD_API_KEY` does not exist on this repository at all" and
Backend's dispatched retry (run `41394575266`) as "still running at time of writing." Both are now
stale: the secret was provisioned after §36 was written, and the Backend run has since completed.
Recorded as a correction here, per this document's own append-only convention — §36's text is
unchanged.

### 37.1 — Android PR #11 (new since §36, not previously tracked)

`OPEN`, head `d2df952e7c973a5acc227e4bb5486722d27996e6`. Two real, verified-correct fixes exist on
this branch:

1. `--no-configuration-cache` added to the `dependencyCheckAnalyze` invocation — closes a genuine
   Gradle Configuration Cache incompatibility in the plugin's `Analyze` task (`Task.project` called
   at execution time; `DefaultProject` not serializable) — confirmed closed by its total absence
   from every subsequent run's log.
2. `.trim()` applied to `NVD_API_KEY` at the point `build.gradle.kts` consumes it — the stored
   secret was proven (via a bounded, content-free diagnostic: length and whitespace/control-
   character counts only, never the value) to carry 2 trailing control characters beyond the real
   36-character key. Stripped, the identical key returned a genuine `HTTP 200` from NVD's
   lightweight single-CVE endpoint.

**Neither fix makes the real scan succeed.** Two independently, correctly-targeted runs of
`dependency-check-baseline` on this exact head — including run `41451646923` / job
`125298035002`, re-verified against a freshly-regenerated merge-ref after discovering that
`gh run rerun --failed` had replayed a stale, pre-fix snapshot on its own prior attempt (discarded,
not used as evidence) — both failed with the same real, full exception chain:

```
Caused by: io.github.jeremylong.openvulnerability.client.nvd.NvdApiException: NVD Returned Status Code: 503
```

No `dependency-check-report` artifact exists on either run (confirmed via the Actions API artifact
list). `Unit tests` and `governance-drift-check` (Android's only required check) passed on every
attempt — not treated as equivalent to the security scan's own result.

**Disposition: Android's dependency-check scan is `FAILED`, specifically due to an external NVD
503 on the bulk-sync endpoint — not `PASSED`, and not attributable to the secret, the cache fix,
or any configuration this repository controls.**

### 37.2 — Backend's dispatched retry (run `41394575266`) — completed, same cause

`head_sha: 89f703d`. `build-test-report`: `success`. `dependency-check-baseline`: `failure`, total
run time `3h 45m 8s`, 31 visible retries, terminal cause from the raw log:
`NvdApiException: NVD Returned Status Code: 503` — identical to Android's. No report artifact
produced. Both repos' failures completed within minutes of the same real-world timestamp
(~09:17 UTC, 2026-11-02) despite starting at very different times — noted as consistent with one
shared NVD-side outage window; not independently confirmed as the same window from NVD's own side.

**Disposition: Backend's dependency-check scan is `FAILED` (external NVD 503), not `PASSED`.**

### 37.3 — Local Android dependency-check test environment
(`D:\AndroidProjects\ROJAN_DependencyCheck_Test`)

Separate worktree, detached HEAD at `d2df952`, only untracked diagnostic files present (no change
to any tracked file). A real scan succeeded there — NVD was genuinely reachable from that network
path at that time (`NVD API Last Checked: 2026-11-02T19:12:22+0330`) — producing real HTML/JSON
reports dated `Nov 2, 19:32`. Real counts read directly from the JSON: **328 dependencies, 39 with
findings, 68 total findings.** A prior pass recorded **408 dependencies** for this same
environment. **Both numbers are preserved here, not reconciled** — flagged as an open
discrepancy, not resolved by silently preferring one.

Release build in this environment: `BLOCKED` — `local.properties` does not exist there at all
(confirmed directly, not inferred from the earlier "SDK location not found" message alone).

Google Maven 404 investigation: untracked files dated `Nov 3, 08:17-08:18` (newer than the scan
itself, and newer than this document's own §36) show one request returning a real `404` and a
second, differently-constructed request for a comparable artifact succeeding via a `301` redirect
to `dl.google.com`. **Root cause: `UNKNOWN`** — not guessed here.

### 37.4 — Open items, named explicitly

1. NVD bulk-sync 503 — external, currently unresolved, affects Backend and Android identically.
2. 328 vs. 408 dependency count (local test environment) — unreconciled.
3. Google Maven 404 root cause — unknown.
4. Missing `local.properties` in the local test environment — blocks any local Release build there.

**No status upgraded beyond what this evidence directly supports. Final status unchanged:
`INCOMPLETE_BLOCKED`. `RELEASE_READY = false`.**

## 38. Pass 68: append-only correction — Pass 66's dispositions for `CVE-2026-53914` and
`CVE-2020-29582` are **superseded** by Pass 67's deeper evidence; both now `NEEDS_TRIAGE`

**This section supersedes Pass 66's disposition for both CVEs below, citing Pass 67's evidence.
Pass 66's own text is left unchanged, per this document's append-only convention — this is the
correction of record, not an edit to it.**

### 38.1 — `CVE-2026-53914`

Pass 66 checked only 5 runtime-scoped jars (`kotlin-android-extensions-runtime`,
`kotlin-parcelize-runtime`, `kotlin-stdlib-2.3.20`, `kotlin-stdlib-jdk7-1.9.0`,
`kotlin-stdlib-jdk8-1.9.0`) and found none contained the vulnerable incremental-compilation
package — but never examined the actual Kotlin Gradle Plugin artifact itself. Pass 67 did:

- **Kotlin Gradle Plugin version: `2.2.10`** (from `gradle/libs.versions.toml`, `kotlin = "2.2.10"`).
- **Local JAR**: `kotlin-gradle-plugin-2.2.10-gradle813.jar`.
- **SHA-256**: `bd6b97bd9e563bb57db55a98eaad4306e3f0b39c8a0f9be76e476b22ad51f3b8`.
- This JAR **does not appear anywhere in the Dependency-Check report** (0 references) — it was
  never scanned/flagged by name in that run.
- Direct inspection (read-only, `unzip -l`) found this JAR genuinely contains substantial
  incremental-compilation and classpath-snapshot (de)serialization code
  (`org/jetbrains/kotlin/gradle/incremental/IncrementalModuleInfoBuildService.class`,
  `org/jetbrains/kotlin/gradle/internal/kapt/incremental/ClasspathEntryData.class` +
  `ClasspathEntrySerializer`, `ClasspathSnapshot$ClasspathSnapshotFactory.class`, 316 classes under
  `incremental`-named paths).
- **The presence of this code alone does not prove the specific advisory's exploitable path exists
  in this exact version** — no function-level match to the advisory's own technical detail was
  established.

**Disposition: `NEEDS_TRIAGE`** — not `NOT_APPLICABLE_WITH_EVIDENCE` (Pass 66's claim for the 5
jars it checked is not itself overturned for those 5 jars, but it never settled the CVE as a whole,
since the plugin jar was unexamined). **Required to close**: exact advisory/version-range matching
against `2.2.10` from an authoritative source, plus code-path analysis tying the advisory's
described vulnerability to a specific function in this JAR — neither performed yet.

### 38.2 — `CVE-2020-29582`

- Pass 66 based its `NOT_APPLICABLE_WITH_EVIDENCE` claim on the absence of temp-file-creation code
  in the matched JARs.
- Pass 67 extracted the report's own **structured CPE vulnerable-range data**:
  `cpe:2.3:a:jetbrains:kotlin:*`, `versionEndExcluding: 2.1.0`.
- The reported component versions — `1.9.0` and `1.9.22` — **fall within that range** (both
  `< 2.1.0`).
- The report's own **prose description** ("before 1.4.21") **contradicts** this structured range
  and that contradiction is **not resolved** by any evidence gathered so far.

**Disposition: `NEEDS_TRIAGE`** — **`NOT_APPLICABLE_WITH_EVIDENCE` is explicitly not recorded**
until the prose-vs-CPE-range conflict is resolved from an authoritative source. Pass 66's version-
absence reasoning is superseded by this version-range finding, not confirmed by it.

**Status unchanged: `INCOMPLETE_BLOCKED`. `RELEASE_READY = false`.**

## 39. Pass 71: append-only disposition update for `CVE-2026-53914` — `NOT_APPLICABLE_WITH_EVIDENCE`
— inspected revision only; **supersedes Pass 68's `NEEDS_TRIAGE` for this CVE**, Pass 68's own text
unchanged

**Inspected Android repository HEAD**: `d2df952e7c973a5acc227e4bb5486722d27996e6`. **Kotlin Gradle
Plugin version**: `2.2.10`. **CVE**: `CVE-2026-53914` / `GHSA-r937-wjx7-w2jp`. **Upstream fix
commit**: `bf51df665b458fda7c3eaf436c4d88dc119d7ec6`.

**Disposition: `NOT_APPLICABLE_WITH_EVIDENCE — inspected revision only`.** The currently inspected
effective build configuration and available pre-existing dependency evidence do not indicate use
or resolution of the KAPT code path associated with this CVE.

1. Kotlin Gradle Plugin `2.2.10` **is** within the published affected-version range (`before
   2.4.20`/`2.4.20-Beta1`) — this disposition is not a version-fix claim.
2. This disposition rests on the **absence of evidence** for the relevant KAPT path — specifically:
   the upstream fix's own named vulnerable class (`JavaClassCacheManager`) and its fix counterpart
   (`FilteringObjectInputStream`), from commit `bf51df6`, were not found in the inspected
   `kotlin-gradle-plugin-2.2.10-gradle813.jar` or any other local Kotlin JAR; the build is a
   single-module `:app` project with no `buildSrc`/`includeBuild`; an exhaustive repository search
   found zero direct or indirect references to KAPT, `kotlin-annotation-processing`, `kapt3-base`,
   KSP, or Hilt/Dagger; and three independent, pre-existing dependency-evidence sources (the
   existing Dependency-Check report, the existing decoded `app-dependencies-local.txt`, and the
   existing `manager-production-release-dependencies.txt`) each separately show zero relevant
   artifact records. No dependency resolution, build, scan, or cache mutation was performed to
   reach this conclusion.
3. **This does not establish** that every possible build environment, external plugin, future
   dependency change, or future revision is unaffected — it is scoped strictly to the inspected
   revision and the evidence listed above.
4. **No APK runtime impact was demonstrated** by this investigation (the KAPT path, where it
   exists at all, is a build-time-only mechanism regardless).
5. **The CVSS disagreement between NVD and JetBrains is not reconciled by this disposition** — the
   vendor's own score (`6.7 MEDIUM`, `AV:L/AC:H/PR:H`) and NVD's rescored primary metric
   (`9.8 CRITICAL`, `AV:N/AC:L/PR:N`) remain a separate, unresolved, documented scoring
   disagreement.
6. **Reassess** if KAPT or the `kotlin-annotation-processing`/`kapt3-base` artifact is introduced,
   if the build configuration changes, or if new evidence indicates the vulnerable path becomes
   resolved or reachable.
7. **Overall project status remains `INCOMPLETE_BLOCKED`. `RELEASE_READY = false`.**

## 40. Pass 85-87: append-only artifact-scoped finding — Manager Production Release APK,
`kotlin.io.createTempDir`/`createTempFile` (`CVE-2020-29582`) — scoped exclusively to one local
build artifact; does not alter the project-wide `NO_USE_FOUND_WITH_LIMITATIONS` disposition
recorded for this CVE, and does not supersede any prior section

**Inspected artifact**: `app/build/outputs/apk/managerProduction/release/app-manager-production-release.apk`.
**SHA-256**: `7b80a960a3bcacfaef066c3f823f17f6143866befaf5468cb6565919ba89b040`. **Application ID**:
`ai.rojan.designlab.manager`. **Version**: `1.0.2` (version code `3`). **Embedded Git revision**
(`META-INF/version-control-info.textproto`): `ee492250b356974cf8724bc580a24bf1b23c1432` — matched
the repository's `main` HEAD at time of discovery. **Embedded Kotlin stdlib version**
(`sdk-dependencies/managerProductionRelease/sdkDependencies.txt`): `2.3.20`. **CVE**:
`CVE-2020-29582` / `GHSA-cqj8-47ch-rvvq` (also referenced: `GHSA-5w9v-8x7x-rfqm`).

**Evidence linkage**: the R8 minify task's own intermediate `classes.dex`
(`app/build/intermediates/dex/managerProductionRelease/minifyManagerProductionReleaseWithR8/classes.dex`)
has SHA-256 `5105e711bdee2da54bfd2008e27b4df668ffb322b70f88c563b4db8c05d8d05e`, byte-identical to the
`classes.dex` entry streamed directly out of the packaged APK — confirming the R8 `mapping.txt`/
`usage.txt` reports correspond to this exact artifact's dex content, not merely to a same-named
output directory.

**Artifact-specific disposition**: `UNREACHABILITY_DEMONSTRATED — scoped exclusively to this exact
local APK and its recorded R8 analysis. The target methods were not found in the final packaged
DEX, and R8's removal evidence is byte-level linked to that DEX. This is an artifact-scoped
static-analysis finding, not proof of project-wide remediation or of the impossibility of all
runtime-dynamic access paths.`

1. `mapping.txt` records `kotlin.io.FilesKt__UtilsKt -> R8$$REMOVED$$CLASS$$533:`; `usage.txt`
   explicitly lists `createTempDir(String,String,File)`, `createTempFile(String,String,File)`, and
   both synthetic `$default` overloads as removed members of that class. An independent raw-byte
   search of `classes.dex` and `classes2.dex` streamed directly from the packaged APK found zero
   occurrences of `createTempFile`, `createTempDir`, `createTempDirectory`, `FilesKt__UtilsKt`, or
   bare `FilesKt` — re-confirmed with a validated search technique (positive-control matches on
   `"rojan"`, `"onCreate"`, `"android"` in the same byte stream).
2. The following four claims are kept explicitly separate, not interchangeable: (a) the target
   methods are absent from the final packaged DEX of this exact local APK; distribution of this
   APK has not been established; (b) no target method is present to invoke in that final artifact;
   (c) this does **not** establish that no reference existed before compilation or optimization —
   the unminified Customer Release APK (see below) independently shows the same methods physically
   present before/without this minification pass, and R8's own mapping retains inlined
   line-number fragments for two *other* methods of the same class, confirming the class was fully
   compiled before removal; (d) R8's recorded whole-program analysis found the class removable as
   unreachable under the analyzed program, its entry points, and its applicable keep rules — no
   keep rule anywhere protects `kotlin.io.FilesKt*`, and a build-config comment separately asserts
   the app's own source has zero reflection (consistent with an independent source-level grep
   finding no reflective pattern referencing these methods).
3. **True runtime-dynamic access (reflection, `MethodHandle`, or a runtime-constructed class/method
   name) cannot be absolutely excluded by this static evidence** — this is a bounded
   static-analysis finding, not a claim of mathematical impossibility.
4. **Cross-artifact applicability is explicitly limited**: the Customer Release APK (embedded
   revision `e012210746f8aa71c8e418cb257435d161986c98`, an older, different commit) is unminified
   and was independently confirmed to still contain the target method/class name strings — string
   presence alone is not treated as proof of invocation or exploitability for that artifact, and no
   disposition beyond the pre-existing project-wide one is assigned to it. Debug builds, other
   flavors/variants, and any future build remain unassessed by this finding.
5. **Signing/distribution**: this APK is confirmed unsigned (no `.RSA`/`.SF`/`.DSA` entries); no
   evidence of actual distribution to any channel was found or sought. This section makes no claim
   about what, if anything, end users currently run.
6. **Upstream implementation remediation remains not verified** by this finding — separately
   established (Pass 83-84) that the underlying `createTempDir`/`createTempFile` implementation
   body has never changed across every examined Kotlin version from `1.4.20` through `2.3.20`;
   this artifact-scoped removal is a consequence of this specific build's R8 tree-shaking, not of
   any upstream code fix.
7. **This artifact-specific result must not be generalized to the entire ROJAN AI ecosystem.** The
   project-wide disposition for this CVE remains `NO_USE_FOUND_WITH_LIMITATIONS`, unchanged by this
   section. **Overall project status remains `INCOMPLETE_BLOCKED`. `RELEASE_READY = false`.**
