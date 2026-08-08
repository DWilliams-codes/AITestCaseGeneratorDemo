# Dependency Risk and Immutable-Reference Register

## React Router advisory closure

The former temporary acceptance for `GHSA-qwww-vcr4-c8h2` is closed. On
2026-08-05, official npm registry metadata showed that `react-router@8.3.0`
exists while `react-router-dom@8.3.0` does not. The SPA imports directly from
and locks exact `react-router@8.3.0`; `react-router-dom` and the advisory
allowlist were removed.

The reviewed lockfile was generated, never hand-edited, with:

```text
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
```

`frontend/scripts/audit-policy.mjs` now fails closed for process failures,
signals, unexpected exits, invalid JSON, report errors, unsupported report
versions, missing metadata/vulnerability sections, or any high/critical
vulnerability. There is no package/advisory exception path.

## Temporary Tomcat WebSocket chat-example acceptance

**Advisory:** `CVE-2026-66299`
**Exact affected PURL:** `pkg:maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.57`
**Owner:** TestForge AI maintainers
**Accepted:** 2026-08-07
**Expires:** 2026-08-22T00:00:00Z

### Applicability and acceptance

Applicability is supported by the exact affected PURL, the official Apache
examples-only advisory, and unchanged embedded-application packaging
configuration. The historical 2026-08-06 TestForge JAR observation found no
Tomcat example or WebSocket chat resources. This is a temporary, exact-PURL and
exact-CVE acceptance only; it is not a CVSS threshold reduction or a general
Tomcat suppression.

### Evidence and compensating controls

- Apache advisory and download evidence: [Tomcat 10 security notices](https://tomcat.apache.org/security-10.html) and [Tomcat 10 downloads](https://tomcat.apache.org/download-10.cgi).
- Local gaps: Maven verification and fresh candidate packaging were unavailable
  locally because Maven is not on PATH.
- Existing CI closes the Maven verify, OWASP security-profile, candidate
  container-build, and container-scan gaps. The seven exact-SHA CI jobs are
  publication gates after implementation completion, not completion prerequisites.
- Existing-artifact observation (not fresh candidate or CI evidence):
  `backend/target/testforge-backend.jar` dated 2026-08-06 contains
  `BOOT-INF/lib/tomcat-embed-core-10.1.57.jar`; `jar tf` found no Tomcat example
  web application or chat-example resources. TF-008 does not change packaging
  source.
- Compensating controls: the suppression matches exactly one Maven PURL and one
  CVE, has a hard expiry, fails the build when unused, keeps CVSS 7 and every
  other dependency scan in force, and the embedded application does not ship
  the affected example.

### Removal and expiry policy

Remove this acceptance and suppression immediately on Tomcat 10.1.58 or later.
If the exact dependency is no longer present, the unused-suppression rule fails
closed. If the date expires first, the suppression no longer applies and the
security build must fail closed until the dependency is upgraded or a newly
approved acceptance replaces it.

## Verified immutable references

All values below were resolved on 2026-08-05 from the named publisher's
official GitHub tag/ref API or the official Docker Hub Registry v2 manifest API.
Version comments/tags remain beside immutable values for update readability.

| Reference | Immutable value | Verification source |
| --- | --- | --- |
| `actions/checkout` v5 | `fbc6f3992d24b796d5a048ff273f7fcc4a7b6c09` | GitHub `actions/checkout` v5 ref |
| `actions/setup-python` v6 | `ece7cb06caefa5fff74198d8649806c4678c61a1` | GitHub `actions/setup-python` v6 ref |
| `actions/setup-java` v5 | `b6effb05e454b25005698d916606bdc6ffcbf961` | GitHub `actions/setup-java` v5 ref |
| `actions/setup-node` v6 | `249970729cb0ef3589644e2896645e5dc5ba9c38` | GitHub `actions/setup-node` v6 ref |
| `actions/cache` v5 | `caa296126883cff596d87d8935842f9db880ef25` | GitHub `actions/cache` v5 ref |
| `aquasecurity/trivy-action` v0.36.0 | `ed142fd0673e97e23eac54620cfb913e5ce36c25` | GitHub `aquasecurity/trivy-action` v0.36.0 ref |
| `postgres:18.4-trixie` | `sha256:d129b9577d274bb96cbd44d902bdeb1b935c89247d161241e9154cba64e13df4` | Docker Hub manifest |
| `node:24.12.0-alpine` | `sha256:c921b97d4b74f51744057454b306b418cf693865e73b8100559189605f6955b8` | Docker Hub manifest |
| `nginxinc/nginx-unprivileged:1.29-alpine3.23` | `sha256:0c79d56aee561a1d81c63f00eee5fb5fe29279560cdc55e91425133104c7fbe6` | Docker Hub manifest |
| `maven:3.9.11-eclipse-temurin-21-alpine` | `sha256:922927df2c662cdd47ddb116443d6bec4696cfae3de1a0ddac8fcc7b87ce61ae` | Docker Hub manifest |
| `eclipse-temurin:21-jre-alpine-3.23` | `sha256:3f08b13888f595cc49edabea7250ba69499ba25602b267da591720769400e08c` | Docker Hub manifest |

Tags and version comments are descriptive only; execution resolves the commit
or digest. Future updates must repeat publisher-source verification and update
this table with the review date. SBOM generation, artifact signing, and an
automated reviewed digest-update workflow remain deferred work, not accepted
exceptions.
