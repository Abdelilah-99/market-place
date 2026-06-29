# SonarCloud CI Analysis

This document explains how `scripts/ci/sonarqube_analysis.sh` works and how it is used by Jenkins.

## Jenkins Entry Point

The `Jenkinsfile` runs SonarCloud after the normal build and test stage:

```groovy
stage('SonarCloud Analysis') {
  steps {
    withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
      sh 'bash scripts/ci/sonarqube_analysis.sh'
    }
  }
}
```

Jenkins provides:

- `SONAR_HOST_URL`: `https://sonarcloud.io`
- `SONAR_ORGANIZATION`: `abdelilah-99`
- `SONAR_TOKEN`: loaded from the Jenkins secret text credential `sonarqube-token`

The token is not stored in Git. Jenkins injects it only while the SonarCloud stage is running.

## Script Safety Settings

The script starts with:

```bash
set -euo pipefail
```

This means:

- `-e`: stop immediately when a command fails
- `-u`: fail when an unset variable is used
- `-o pipefail`: fail a pipeline if any command inside it fails

This is important because a failed SonarCloud scan or failed Quality Gate must eventually stop Jenkins before deployment.

## Working Directory

The script calculates the repository root from its own location:

```bash
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"
```

This allows Jenkins to call the script from anywhere and still scan the correct service directories.

## Memory Defaults

The script sets default memory options for Maven and Gradle:

```bash
export MAVEN_OPTS="${MAVEN_OPTS:--Xmx1g -Xms256m}"
export GRADLE_OPTS="${GRADLE_OPTS:--Xmx1g -Xms256m -Dorg.gradle.parallel=false -Dorg.gradle.workers.max=1}"
```

If Jenkins already defines `MAVEN_OPTS` or `GRADLE_OPTS`, those values are kept. Otherwise, these defaults reduce memory pressure on the EC2/Jenkins host.

## Required Environment Variables

The script requires these values:

```bash
: "${SONAR_HOST_URL:?Missing SONAR_HOST_URL}"
: "${SONAR_ORGANIZATION:?Missing SONAR_ORGANIZATION}"
: "${SONAR_TOKEN:?Missing SONAR_TOKEN}"
```

If any value is missing, the script fails immediately with a clear error.

## Java 21 Check

All backend services are Java 21, so the script verifies Java before scanning:

```bash
ensure_java_21
```

If the current `java` is not Java 21, it searches common Java 21 install paths and updates `JAVA_HOME`/`PATH`. If Java 21 cannot be found, the script exits.

## Maven Services

Maven services are scanned through `run_maven_sonar`:

```bash
run_maven_sonar "gateway/gateway" "buy01-gateway" "Buy01 Gateway"
```

Arguments are:

1. Service directory
2. SonarCloud project key
3. SonarCloud project display name
4. Optional Maven build goal, defaulting to `compile`

The Maven command runs:

```bash
./mvnw -B -DskipTests -Dmaven.compile.fork=false <goal> sonar:sonar
```

Important flags:

- `-B`: batch mode for CI
- `-DskipTests`: do not rerun tests during analysis, because `build_and_test.sh` already ran them
- `-Dmaven.compile.fork=false`: avoids extra compiler JVMs and lowers memory usage
- `<goal>`: compiles/builds enough bytecode for SonarCloud analysis
- `sonar:sonar`: sends the analysis to SonarCloud

## Gradle Users Service

The users service uses Gradle, so it is scanned through `run_gradle_sonar`:

```bash
run_gradle_sonar "users-service/service" "buy01-users-service" "Buy01 Users Service"
```

This runs:

```bash
./gradlew sonar -Dorg.gradle.workers.max=1
```

The `users-service/service/build.gradle` file includes the Sonar plugin:

```gradle
id 'org.sonarqube' version '6.2.0.5505'
```

Without this plugin, Gradle would not have the `sonar` task.

## Services Scanned

The script currently scans:

| Path | Project Key | Project Name |
| --- | --- | --- |
| `shared` | `buy01-shared` | `Buy01 Shared` |
| `eureka-server/eureka` | `buy01-eureka-server` | `Buy01 Eureka Server` |
| `gateway/gateway` | `buy01-gateway` | `Buy01 Gateway` |
| `products-service/products` | `buy01-products-service` | `Buy01 Products Service` |
| `media-service/media` | `buy01-media-service` | `Buy01 Media Service` |
| `users-service/service` | `buy01-users-service` | `Buy01 Users Service` |

SonarCloud creates these projects on first analysis if the token has permission, or updates them if they already exist.

## Quality Gate Behavior

Every service scan includes:

```bash
-Dsonar.qualitygate.wait=true
-Dsonar.qualitygate.timeout=300
```

This makes Maven/Gradle wait for SonarCloud's Quality Gate result.

If a service Quality Gate fails, that service command exits with an error. The script records the failed service and continues scanning the remaining services.

At the end, if any service failed analysis or failed its Quality Gate, the script prints a summary and exits with status `1`. Jenkins then stops before deployment.

The timeout is 300 seconds. If SonarCloud does not return the Quality Gate result in time, the command fails.

## Failure Handling

The script intentionally does not stop at the first failed Quality Gate. This allows one Jenkins run to update SonarCloud results for all backend services:

```text
Buy01 Shared
Buy01 Eureka Server
Buy01 Gateway
Buy01 Products Service
Buy01 Media Service
Buy01 Users Service
```

If one or more services fail, the script exits only after all scans have been attempted.

The `Deploy` stage is skipped when SonarCloud fails. Jenkins does not run rollback for a SonarCloud failure, because no new deployment happened. Rollback is handled only inside the `Deploy` stage if deployment itself fails.

## Command Timeouts

Each service scan is wrapped with:

```bash
timeout 900 ...
```

That gives each service up to 15 minutes for analysis. If a scan hangs or takes too long, the script fails with a clear timeout message.

## Adding a New Maven Service

Add one line at the bottom of the script:

```bash
run_maven_sonar "path/to/service" "sonar-project-key" "Sonar Project Name"
```

If the service needs a different Maven goal:

```bash
run_maven_sonar "path/to/service" "sonar-project-key" "Sonar Project Name" "verify"
```

Use `install` only when another service depends on that artifact.

## Adding a New Gradle Service

Add the Sonar plugin to the service `build.gradle`:

```gradle
id 'org.sonarqube' version '6.2.0.5505'
```

Then add one line to the script:

```bash
run_gradle_sonar "path/to/service" "sonar-project-key" "Sonar Project Name"
```

## Local Verification

You can verify script syntax without contacting SonarCloud:

```bash
bash -n scripts/ci/sonarqube_analysis.sh
```

You can verify the users service has the Gradle Sonar task:

```bash
cd users-service/service
./gradlew tasks --all
```

The real analysis should run in Jenkins because Jenkins has the `sonarqube-token` credential.
