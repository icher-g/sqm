# v0.5.1 dependency security audit

Audit date: 2026-11-07.

## Scope and method

Reviewed all 43 tracked Maven POMs. Resolved the default reactor's dependency trees and plugin dependencies with Maven Dependency Plugin 3.11.0, plus the root `central-release` profile's plugin dependencies. Queried each distinct Maven coordinate and version against the public [OSV batch API](https://google.github.io/osv.dev/post-v1-querybatch/), including transitive, test, provided, and isolated plugin dependencies. No findings were suppressed.

The baseline contained 491 distinct artifact/version pairs, of which 71 had OSV package-query advisories. After remediation, 387 pairs remain, of which three have OSV package-query advisories. These counts describe affected coordinates, not unique CVEs. Resolution includes inherited plugins even when the repository never invokes them.

The initial OSV-only assessment missed findings reported by IntelliJ/Mend. Some advisories have no Maven package mapping, so querying a Maven coordinate does not establish absence of vulnerabilities. The user's IDE findings were therefore checked against upstream advisories and release notes as a second source. The playground npm lockfile was also audited separately. No claim is made that the OSV count represents every advisory in every database.

## Remediation

| Area | Result |
| --- | --- |
| Web hosts | Spring Boot 4.0.9 / Spring Framework 7, confined to playground REST and REST middleware |
| SQL JSON API | Jackson 2.21.8; core API remains on Jackson 2 |
| Web JSON | Jackson 3.1.8, explicitly overriding Boot's older patch; Jackson 2 HTTP defaults retained through Boot's compatibility property |
| JDBC | PostgreSQL 42.7.14; SQL Server 12.8.2.jre11; Oracle 23.26.4.0.0 |
| Logging and utility libraries | Logback 1.6.6, Log4j 2.25.5, JSON Smart 2.6.1, Commons Compress 1.28.0, Guava 33.8.0-jre, Commons Lang 3.18.0 |
| ASM | All five ASM artifacts aligned to 9.11.1 in project and affected plugin dependency graphs; Maven Core's provided dependency tree is included |
| JSON assertion support | Replaced unmaintained Android JSON with org.json 20260814 in REST tests |
| Build and release tooling | Updated Maven plugins and explicitly patched their isolated dependency classpaths, including Commons IO/BeanUtils, Plexus, HTTP Components, and Bouncy Castle |
| Maven Release SCM | Native Git provider; excluded the unused JGit provider and its legacy EdDSA dependency |
| Tests | JUnit 6.0.3; Boot 4 test starters and explicit TestRestTemplate configuration |
| Playground frontend | Vite 7.3.7, Vitest 5.1.0, Monaco 0.57.0, DOMPurify 3.4.17, and refreshed transitive dependencies; npm audit reports zero findings |

Spring's BOM is imported only in the two web host modules. Project dependency management does not govern plugin classpaths, so affected plugins also carry explicit dependency overrides in the parent POM.

The Jackson BOMs precede Boot's BOM in the web hosts to prevent Boot selecting older patches. The parent also manages Jackson 3 and Logback so integration-test consumers of the REST adapter retain those fixes.

### Findings cross-checked from IntelliJ

- [ASM's release notes](https://asm.ow2.io/versions.html) cover the fixes in 9.11 and the regression fixes in 9.11.1. This addresses the ASM advisories surfaced through Maven Core/Sisu and other ASM consumers, including CVE-2026-110260, CVE-2026-110258, CVE-2026-110263, and CVE-2026-110257.
- Oracle's [July 2026 CPU](https://www.oracle.com/security-alerts/cpujul2026verbose.html) lists JDBC issues affecting the old 23.8 driver, including CVE-2026-47045, CVE-2026-47060, and CVE-2026-47061. The driver now uses the October patch, 23.26.4.0.0.
- The [Jackson advisory for CVE-2026-110879](https://github.com/FasterXML/jackson-databind/security/advisories/GHSA-m8xf-2cr7-77ph) specifies fixes in 2.21.8 and 3.1.8. Both old lines were present in the IDE findings; both are now aligned, including plugin classpaths.
- [Logback releases](https://github.com/qos-ch/logback/releases) document the security corrections after 1.5.38; both Core and Classic now use 1.6.6.
- [JSON Smart's fix](https://github.com/netplex/json-smart-v2/pull/305) addresses CVE-2026-113602; the selected release is 2.6.1.
- [CVE-2026-110974](https://nvd.nist.gov/vuln/detail/CVE-2026-110974) affects Android JSON's unlimited nesting. The unmaintained artifact is excluded and replaced with maintained org.json.

### Monaco's bundled sanitizer

Monaco 0.57.0 vendors DOMPurify 3.4.15 inside its distributed JavaScript, as recorded in [the upstream issue](https://github.com/microsoft/monaco-editor/issues/5517). An npm override alone does not replace this copy. The playground now configures `@monaco-editor/react` with the locally bundled editor and worker, and Vite replaces Monaco's relative sanitizer import with DOMPurify 3.4.17. The dependency override also aligns the npm dependency. The production bundle was inspected for the fixed version and absence of the old 3.4.15/3.4.16 versions.

This removes the default CDN editor loading path. Editor assets are now served with the application, increasing its local bundle size. Frontend tooling requires Node 22.12+ on the 22.x line, Node 24, or Node 26+.

## Remaining upstream findings

Maven Site Plugin 3.22.0 bundles Jetty 9.4.58.v20250814 for its optional `site:run` preview server:

| Artifact | Advisories |
| --- | --- |
| jetty-http | [GHSA-355h-qmc2-wpwf](https://github.com/advisories/GHSA-355h-qmc2-wpwf), [GHSA-qh8g-58pp-2wxh](https://github.com/advisories/GHSA-qh8g-58pp-2wxh) |
| jetty-security | [GHSA-2fvj-hgj9-j2gr](https://github.com/advisories/GHSA-2fvj-hgj9-j2gr) |
| jetty-server | [GHSA-7p3p-8qv8-m2vh](https://github.com/advisories/GHSA-7p3p-8qv8-m2vh) |

These dependencies are confined to Maven Site's plugin realm. The repository's build, test, and release workflows do not invoke the preview server, and these artifacts are not application dependencies. This establishes the current workflow's applicability assessment; the raw scan still reports them. Do not expose `mvn site:run` as a service.

Jetty 9 is end of life, and the fixed Jetty 9 releases are not available as public Maven Central updates. Overriding Jetty with version 12 would break Site's existing API usage. Track the [upstream Maven Site migration](https://github.com/apache/maven-site-plugin/issues/1298) and update the plugin once a compatible public release is available. See also [Jetty's supported downloads](https://jetty.org/download.html).

## Validation

Both web hosts passed their full test suites on Boot 4, including HTTP status, JSON, security, request size, CORS, rate limiting, and error contract checks. The HTTP 413 assertion now uses Spring 7's canonical `CONTENT_TOO_LARGE` enum name; the wire status remains 413.

The full `mvn -B -ntp install` reactor passed: 5,565 tests, zero failures, errors, or skips, including source and Javadoc packaging. Live database engine execution remains a separate CI check.

After the IntelliJ findings were addressed, the full reactor passed again with Javadoc generation skipped: 5,565 tests, zero failures, errors, or skips. The middleware integration-test consumer was retested after its transitive Jackson/Logback alignment. Frontend validation passed all 30 tests and the production build. The new editor initialization has 100% statement, line, and function coverage. `npm audit` reports zero findings, with no audit suppressions.

The Central bundle build also passed with release revision `0.5.1`, `central.skipUpload=true`, `gpg.skip=true`, and tests skipped after the successful reactor run. It generated the local release bundle without uploading artifacts. Signing and server-side Central validation remain release workflow checks.
