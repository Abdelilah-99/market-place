# SonarQube and SonarCloud Guide

This guide explains what SonarQube does, how its main concepts fit together, and how static analysis is configured in the Buy01 repository. This project uses **SonarCloud**, the hosted SonarQube service, rather than a self-hosted SonarQube Server. The analysis concepts are the same, but SonarCloud is operated by SonarSource and requires an organization in addition to a project key.

## What SonarQube Does

SonarQube performs static analysis: it examines source code and generated analysis data without running the application in production. It helps detect:

- **Bugs**: code likely to behave incorrectly.
- **Vulnerabilities**: security weaknesses for which the analyzer can identify the unsafe flow with sufficient confidence.
- **Security hotspots**: security-sensitive code that requires a human review. A hotspot is not automatically a vulnerability.
- **Code smells**: maintainability problems that make code harder or more expensive to change.
- **Duplications**: repeated code blocks that may increase maintenance cost.
- **Coverage**: the proportion of executable code exercised by tests. Sonar does not generate coverage; it imports reports created by tools such as JaCoCo.

Each issue has a rule, severity, location, explanation, and remediation guidance. Rules are grouped into a **Quality Profile** for each language. A **Quality Gate** evaluates project-level conditions and decides whether an analysis passes.

Sonar analysis complements compilation, unit tests, integration tests, dependency scanning, dynamic security testing, and code review. It does not replace them.

## Important Terminology

| Term | Meaning |
| --- | --- |
| Scanner | The Maven or Gradle integration that analyzes files and uploads a report. |
| Project | A separately analyzed codebase in SonarCloud, identified by a unique project key. |
| Organization | The SonarCloud container that owns projects, members, profiles, and gates. |
| Analysis | One uploaded snapshot of source, metrics, issues, and test/coverage metadata. |
| Main branch | The long-lived baseline branch used for overall project history. |
| New code | Code added or changed during the configured comparison period. |
| Quality Profile | The set of static-analysis rules enabled for a language. |
| Quality Gate | Pass/fail conditions applied after analysis. |
| Measure | A numeric value such as coverage, duplicated-line density, or issue count. |
| Issue | A rule violation found at a particular location. |
| False positive | An issue that is technically incorrect and is marked as such after review. |
| Won't fix / accepted | A valid issue whose risk is consciously accepted according to team policy. |

Sonar's recommended **Clean as You Code** approach focuses the gate on new code. This prevents newly changed code from introducing debt while older issues can be handled separately. The actual new-code definition and gate thresholds are configured in the SonarCloud UI, not in this repository.

## Repository Architecture

Buy01 treats every backend module as a separate SonarCloud project. This gives every service its own dashboard, history, Quality Gate, and permissions.

| Source directory | Build tool | Project key | Display name | Analysis goal |
| --- | --- | --- | --- | --- |
| `shared` | Maven | `buy01-shared` | Buy01 Shared | `install` |
| `eureka-server/eureka` | Maven | `buy01-eureka-server` | Buy01 Eureka Server | `compile` |
| `gateway/gateway` | Maven | `buy01-gateway` | Buy01 Gateway | `compile` |
| `payments-service` | Maven | `buy01-payments-service` | Buy01 Payments Service | `verify` |
| `products-service/products` | Maven | `buy01-products-service` | Buy01 Products Service | `verify` |
| `media-service/media` | Maven | `buy01-media-service` | Buy01 Media Service | `verify` |
| `users-service/service` | Gradle | `buy01-users-service` | Buy01 Users Service | `test jacocoTestReport sonar` |

The source of truth for this list is `scripts/ci/sonarqube_analysis.sh`. A project can be created on its first analysis when the token has permission; otherwise it must be created in SonarCloud first.

## CI Flow

The relevant Jenkins order is:

```text
Checkout -> Build and Test -> SonarCloud Analysis -> Deploy
```

The `Jenkinsfile` defines:

```groovy
SONAR_HOST_URL     = 'https://sonarcloud.io'
SONAR_ORGANIZATION = 'abdelilah-99'
```

During the analysis stage Jenkins injects the secret-text credential named `sonarqube-token` as `SONAR_TOKEN`, then runs:

```bash
bash scripts/ci/sonarqube_analysis.sh
```

The token must never be committed, written to a log, or placed in a checked-in configuration file. Jenkins limits its exposure to the credential block.

The analysis script waits for every project's Quality Gate. If any scan or gate fails, the script ultimately returns a non-zero status, Jenkins marks the stage as failed, and deployment is skipped. Since deployment has not begun, the deployment rollback block is not involved.

## Analysis Script in Detail

### Shell safety and paths

The script starts with `set -euo pipefail`, resolves the repository root relative to its own location, and validates all required environment variables:

```bash
: "${SONAR_HOST_URL:?Missing SONAR_HOST_URL}"
: "${SONAR_ORGANIZATION:?Missing SONAR_ORGANIZATION}"
: "${SONAR_TOKEN:?Missing SONAR_TOKEN}"
```

It supplies conservative Maven and Gradle heap defaults for the Jenkins host. Existing `MAVEN_OPTS` and `GRADLE_OPTS` values take precedence.

### Java requirement

The script requires Java 21. It first checks the active `java`; if necessary, it searches common JDK 21 locations and updates `JAVA_HOME` and `PATH`. Analysis stops if no Java 21 installation is available.

### Maven analysis

Maven modules are analyzed with this effective command:

```bash
./mvnw -B -DskipTests -Dmaven.compile.fork=false <goal> sonar:sonar \
  -Dsonar.host.url="$SONAR_HOST_URL" \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.organization="$SONAR_ORGANIZATION" \
  -Dsonar.projectKey="<project-key>" \
  -Dsonar.projectName="<project-name>" \
  -Dsonar.projectVersion="$SONAR_PROJECT_VERSION" \
  -Dsonar.qualitygate.wait=true \
  -Dsonar.qualitygate.timeout=300
```

- `-B` enables non-interactive batch output.
- `-DskipTests` avoids unnecessarily rerunning tests in the Maven invocation. Depending on the selected goal, test compilation and JaCoCo report generation can still occur.
- `-Dmaven.compile.fork=false` reduces extra JVM processes and memory usage.
- `compile` produces bytecode required for accurate Java analysis.
- `verify` runs the Maven lifecycle through verification and creates JaCoCo XML where that plugin is configured.
- `install` also places the shared artifact in the local Maven repository for modules that depend on it.
- `sonar:sonar` invokes the SonarScanner for Maven and uploads the analysis.
- `sonar.projectVersion` defaults to the current Git commit and advances the baseline used by SonarCloud's **previous version** new-code definition. It can be overridden with `SONAR_PROJECT_VERSION` when required.

The service's own wrapper is used when it is executable; otherwise the script falls back to `shared/mvnw`.

### Gradle analysis

The users service runs:

```bash
./gradlew test jacocoTestReport sonar -Dorg.gradle.workers.max=1 <Sonar properties>
```

Its `build.gradle` applies `org.sonarqube` version `6.2.0.5505`, enables the JaCoCo XML report, and tells Sonar where to find it:

```gradle
sonar {
    properties {
        property 'sonar.coverage.jacoco.xmlReportPaths',
                 'build/reports/jacoco/test/jacocoTestReport.xml'
        property 'sonar.coverage.exclusions', '**/Search/**'
    }
}
```

`test` executes the tests, `jacocoTestReport` writes the coverage XML, and `sonar` imports that XML and uploads the analysis. The worker limit reduces memory pressure.

### Failure collection and timeouts

Each project command has two timing controls:

- `timeout 900` limits the complete service command to 15 minutes.
- `sonar.qualitygate.timeout=300` gives the scanner up to five minutes to receive the server-side gate result.

The script records a failed project and continues analyzing the remaining projects. After all projects have been attempted, it prints every failure and exits with status `1`. This produces more useful feedback than stopping after the first project.

## Test Coverage

SonarCloud does not execute tests and does not calculate Java coverage itself. The flow is:

```text
Tests run -> JaCoCo records executed bytecode -> JaCoCo writes XML -> Scanner imports XML -> SonarCloud displays coverage
```

JaCoCo is configured in the payments, products, and media Maven projects and in the users Gradle project. Maven XML reports are normally produced during the `verify` phase under `target/site/jacoco/jacoco.xml`; the Gradle report is under `build/reports/jacoco/test/jacocoTestReport.xml`.

A successful scan with `0%` or missing coverage usually means that no XML report existed when the scanner ran, the report path was wrong, tests were skipped before coverage data was recorded, or the analyzed classes did not match those in the report. Coverage exclusions remove matching files only from coverage calculations; source exclusions would remove files from analysis entirely.

Coverage is evidence that code executed during tests, not proof that assertions are correct or that all behavior is tested. Branch coverage is especially useful for condition-heavy code.

## Quality Profiles, Gates, and Issue Review

Quality Profiles determine which rules the analyzers apply. Prefer inheriting Sonar's maintained profiles and adding a small, documented set of project-specific changes. Disabling a rule globally should be rare; first decide whether the issue is genuinely incorrect, whether a narrow exclusion is appropriate, or whether the code should change.

Quality Gates operate on measures after analysis. Typical gates enforce reliability, security, maintainability, coverage, and duplication on new code. Exact Buy01 conditions must be checked in **SonarCloud -> Organization -> Quality Gates**, because they are server-side configuration and can change without a Git commit.

When reviewing an issue:

1. Read the rule description and the highlighted data/control flow.
2. Confirm whether the behavior is reachable and whether framework behavior changes the result.
3. Fix the root cause and add or improve a test when useful.
4. Mark an issue false positive only when the rule is factually wrong in this context.
5. Accept an issue only with a documented risk decision and the appropriate permissions.

Security hotspots require explicit review. Mark them safe only after verifying the relevant authentication, authorization, validation, encryption, or configuration controls.

## Initial SonarCloud and Jenkins Setup

An administrator performs these steps once:

1. Create or select the SonarCloud organization `abdelilah-99`.
2. Create the seven projects listed above, or allow provisioning during the first scan.
3. Generate a SonarCloud token for a dedicated CI identity with analysis permission for those projects.
4. In Jenkins, add the token as a **Secret text** credential with ID `sonarqube-token`.
5. Assign the intended Quality Gate and Quality Profiles in SonarCloud.
6. Run the pipeline and verify that every project dashboard receives an analysis.

Use the least privilege needed for analysis. Rotate the token periodically and immediately after suspected disclosure. A rotated token needs to be updated only in Jenkins.

## Running Analysis Locally

Local analysis uploads real results and should use a personal token, never the Jenkins token:

```bash
export SONAR_HOST_URL='https://sonarcloud.io'
export SONAR_ORGANIZATION='abdelilah-99'
read -rsp 'Sonar token: ' SONAR_TOKEN && export SONAR_TOKEN
bash scripts/ci/sonarqube_analysis.sh
unset SONAR_TOKEN
```

This analyzes all seven projects and waits for all gates. Be aware that running from a local branch without CI branch/PR parameters may update the branch selected by the scanner and SonarCloud configuration. For routine development, use normal tests and leave authoritative analysis to Jenkins.

Safe checks that do not upload analysis are:

```bash
bash -n scripts/ci/sonarqube_analysis.sh
cd users-service/service && ./gradlew tasks --all
```

## Adding a Service

For a Maven service:

1. Ensure it builds with Java 21 and produces compiled classes.
2. Configure JaCoCo XML if coverage is required.
3. Add a unique project key and a `run_analysis ... run_maven_sonar ...` entry near the end of `scripts/ci/sonarqube_analysis.sh`.
4. Choose `compile`, `verify`, or `install` based on lifecycle and dependency needs.
5. Create/authorize the project in SonarCloud and assign its gate/profile.
6. Update the service table in this document.

For a Gradle service, also apply the `org.sonarqube` plugin, enable JaCoCo XML, configure its report path, and add a `run_gradle_sonar` entry.

Project keys are stable identifiers. Avoid renaming one casually, because a new key can create a separate project and lose continuity with existing history.

## Troubleshooting

### Missing or invalid token

Messages such as `Not authorized`, `Insufficient privileges`, or `Missing SONAR_TOKEN` indicate a missing, expired, revoked, or underprivileged credential. Verify the Jenkins credential ID, token ownership, organization membership, and project analysis permission. Do not print the token while debugging.

### Java bytecode or Java version errors

Java analysis needs compiled bytecode. Run the appropriate compile/verify goal and check `sonar.java.binaries` only for an unusual layout. If the scanner reports an unsupported runtime, confirm `java -version` and `JAVA_HOME` both point to JDK 21 in the Jenkins agent.

### Coverage is absent

Confirm the JaCoCo XML exists before the scanner starts, is non-empty, and uses the expected path. Review scanner output for `JaCoCo XML Report Importer` messages. HTML reports cannot substitute for XML imports.

### Quality Gate failure

Open the project's latest analysis in SonarCloud, inspect the failed gate condition, and filter issues/measures to **New Code**. Fix the code or tests; do not remove the wait flag merely to let deployment continue. If a condition is inappropriate, change it through an explicit team decision in the Quality Gate configuration.

### Timeout

A five-minute gate timeout can mean SonarCloud processing is delayed; a 15-minute command timeout can also indicate slow dependency resolution, compilation, tests, or networking. Check the timestamped Jenkins log to distinguish scanner upload, compute-engine waiting, and build work before increasing a limit.

### Analysis succeeds locally but fails in Jenkins

Compare Java, wrapper versions, environment variables, permissions, checked-out revision, available memory, and network/proxy access. Jenkins uses constrained Gradle workers and default 1 GiB heaps, which may expose resource problems not seen locally.

## Files That Control Analysis

| File | Responsibility |
| --- | --- |
| `Jenkinsfile` | Pipeline order, host URL, organization, and secure token injection. |
| `scripts/ci/sonarqube_analysis.sh` | Project list, scanner commands, keys, names, timeouts, and gate waiting. |
| `users-service/service/build.gradle` | Gradle Sonar plugin and users-service JaCoCo import/exclusion settings. |
| `payments-service/pom.xml` | Payments JaCoCo generation. |
| `products-service/products/pom.xml` | Products JaCoCo generation. |
| `media-service/media/pom.xml` | Media JaCoCo generation. |

Most rule activation, gate thresholds, new-code definitions, issue status, and permissions live in SonarCloud. If an investigation depends on one of those values, verify it in the SonarCloud UI rather than assuming it from this repository.
