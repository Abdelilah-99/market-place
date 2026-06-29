#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

export MAVEN_OPTS="${MAVEN_OPTS:--Xmx1g -Xms256m}"
export GRADLE_OPTS="${GRADLE_OPTS:--Xmx1g -Xms256m -Dorg.gradle.parallel=false -Dorg.gradle.workers.max=1}"

: "${SONAR_HOST_URL:?Missing SONAR_HOST_URL}"
: "${SONAR_ORGANIZATION:?Missing SONAR_ORGANIZATION}"
: "${SONAR_TOKEN:?Missing SONAR_TOKEN}"

log() { echo "[CI][SonarCloud] $*"; }

ensure_java_21() {
  local java_major
  java_major="$(java -version 2>&1 | awk -F '[\".]' '/version/ {print $2; exit}')"
  [[ "${java_major}" == "21" ]] && return

  local candidates=(
    /usr/lib/jvm/java-21-openjdk-amd64
    /usr/lib/jvm/java-1.21.0-openjdk-amd64
    /usr/lib/jvm/openjdk-21
  )
  for candidate in "${candidates[@]}"; do
    if [[ -x "${candidate}/bin/java" ]]; then
      export JAVA_HOME="${candidate}"
      export PATH="${JAVA_HOME}/bin:${PATH}"
      return
    fi
  done

  log "Java 21 required (current: ${java_major}). Install JDK 21 or set JAVA_HOME."
  exit 1
}

run_maven_sonar() {
  local dir="$1"
  local project_key="$2"
  local project_name="$3"
  local build_goal="${4:-compile}"
  local mvn_cmd="mvn"

  [[ -x "${dir}/mvnw" ]] && mvn_cmd="./mvnw"

  log "Analyzing ${project_name}"
  (
    cd "${dir}"
    timeout 900 "${mvn_cmd}" -B -DskipTests -Dmaven.compile.fork=false "${build_goal}" sonar:sonar \
      -Dsonar.host.url="${SONAR_HOST_URL}" \
      -Dsonar.token="${SONAR_TOKEN}" \
      -Dsonar.organization="${SONAR_ORGANIZATION}" \
      -Dsonar.projectKey="${project_key}" \
      -Dsonar.projectName="${project_name}" \
      -Dsonar.qualitygate.wait=true \
      -Dsonar.qualitygate.timeout=300 || {
        [[ $? -eq 124 ]] && { log "ERROR: SonarCloud analysis timed out for ${dir}"; exit 1; }
        exit $?
      }
  )
}

run_gradle_sonar() {
  local dir="$1"
  local project_key="$2"
  local project_name="$3"

  log "Analyzing ${project_name}"
  (
    cd "${dir}"
    chmod +x ./gradlew
    timeout 900 ./gradlew sonar -Dorg.gradle.workers.max=1 \
      -Dsonar.host.url="${SONAR_HOST_URL}" \
      -Dsonar.token="${SONAR_TOKEN}" \
      -Dsonar.organization="${SONAR_ORGANIZATION}" \
      -Dsonar.projectKey="${project_key}" \
      -Dsonar.projectName="${project_name}" \
      -Dsonar.qualitygate.wait=true \
      -Dsonar.qualitygate.timeout=300 || {
        [[ $? -eq 124 ]] && { log "ERROR: SonarCloud analysis timed out for ${dir}"; exit 1; }
        exit $?
      }
  )
}

ensure_java_21

run_maven_sonar "shared" "buy01-shared" "Buy01 Shared" "install"
run_maven_sonar "eureka-server/eureka" "buy01-eureka-server" "Buy01 Eureka Server"
run_maven_sonar "gateway/gateway" "buy01-gateway" "Buy01 Gateway"
run_maven_sonar "products-service/products" "buy01-products-service" "Buy01 Products Service" "verify"
run_maven_sonar "media-service/media" "buy01-media-service" "Buy01 Media Service" "verify"
run_gradle_sonar "users-service/service" "buy01-users-service" "Buy01 Users Service"

log "SonarCloud analysis completed successfully."
