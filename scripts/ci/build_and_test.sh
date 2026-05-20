#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

export MAVEN_OPTS="${MAVEN_OPTS:--Xmx1g -Xms256m}"
export GRADLE_OPTS="${GRADLE_OPTS:--Xmx1g -Xms256m -Dorg.gradle.parallel=false -Dorg.gradle.workers.max=1}"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

log() { echo "[CI] $*"; }

monitor_resources() {
  log "System resources:"
  free -h 2>/dev/null | grep "Mem:" || true
  df -h /  2>/dev/null | tail -1    || true
  nproc    2>/dev/null | xargs echo "CPUs:" || true
}

ensure_java_21() {
  local java_major
  java_major="$(java -version 2>&1 | awk -F '[\".]' '/version/ {print $2; exit}')"
  [[ "${java_major}" == "21" ]] && { log "Java 21 detected."; return; }

  local candidates=(
    /usr/lib/jvm/java-21-openjdk-amd64
    /usr/lib/jvm/java-1.21.0-openjdk-amd64
    /usr/lib/jvm/openjdk-21
  )
  for candidate in "${candidates[@]}"; do
    if [[ -x "${candidate}/bin/java" ]]; then
      export JAVA_HOME="${candidate}"
      export PATH="${JAVA_HOME}/bin:${PATH}"
      log "Switched JAVA_HOME → ${JAVA_HOME}"
      java -version
      return
    fi
  done

  log "Java 21 required (current: ${java_major}). Install JDK 21 or set JAVA_HOME."
  exit 1
}

run_maven() {
  local dir="$1" goal="${2:-verify}"
  log "Maven ${goal}: ${dir}"
  local mvn_cmd="mvn"
  [[ -x "${dir}/mvnw" ]] && mvn_cmd="./mvnw"

  (
    cd "${dir}"
    timeout 600 ${mvn_cmd} -B \
      -Dmaven.compile.fork=false \
      -Dmaven.test.skip=true \
      clean "${goal}" || {
        [[ $? -eq 124 ]] && { log "ERROR: Maven timed out (600s)"; exit 1; }
        exit $?
      }
  )
}

run_gradle() {
  local dir="$1"
  log "Gradle build: ${dir}"
  (
    cd "${dir}"
    chmod +x ./gradlew
    timeout 600 ./gradlew clean build -Dorg.gradle.workers.max=1 || {
      [[ $? -eq 124 ]] && { log "ERROR: Gradle timed out (600s)"; exit 1; }
      exit $?
    }
  )
}

run_frontend() {
  log "Angular frontend build"
  (
    cd frontend
    npm ci --prefer-offline --no-audit

    if [[ ! -f "node_modules/lightningcss-linux-x64-gnu/lightningcss.linux-x64-gnu.node" ]]; then
      log "Installing missing lightningcss native binary."
      npm install --no-save --include=optional lightningcss-linux-x64-gnu@1.30.2
    fi

    npm run build -- --optimization --aot --stats-json
  )
}

# ---------------------------------------------------------------------------
# Change detection
# ---------------------------------------------------------------------------

detect_base_commit() {
  if [[ -n "${CI_STATE_DIR:-}" && -f "${CI_STATE_DIR}/last_successful_commit" ]]; then
    tr -d '[:space:]' < "${CI_STATE_DIR}/last_successful_commit"
    return
  fi
  git rev-parse --verify HEAD^ 2>/dev/null && git rev-parse HEAD^ || true
}

changed_files_since() {
  local base="$1"
  if [[ -z "${base}" ]]; then git ls-files; return; fi
  git diff --name-only --diff-filter=ACMRT "${base}"...HEAD
}

any_changed() {
  local pattern="$1"
  for f in "${changed_files[@]}"; do
    [[ -z "${f}" ]] && continue
    case "${f}" in ${pattern}) return 0 ;; esac
  done
  return 1
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

log "=========================================="
log "BUILD STARTED"
monitor_resources
log "=========================================="

base_commit="$(detect_base_commit)"
if [[ -n "${base_commit}" ]]; then
  log "Diff base: ${base_commit}"
else
  log "No base commit — running full build."
fi

mapfile -t changed_files < <(changed_files_since "${base_commit}")

# Determine what to build
run_shared=false run_eureka=false run_gateway=false
run_products=false run_media=false run_users=false run_frontend=false

if [[ -z "${base_commit}" ]]; then
  log "No base commit — running full build."
  run_shared=true run_eureka=true run_gateway=true
  run_products=true run_media=true run_users=true run_frontend=true
else
  log "Diff base: ${base_commit}"
  mapfile -t changed_files < <(changed_files_since "${base_commit}")

  if [[ ${#changed_files[@]} -eq 0 ]]; then
    log "No changes detected — skipping all builds."
  else
    any_changed "shared/*"           && run_shared=true run_eureka=true run_gateway=true run_products=true run_media=true run_users=true
    any_changed "eureka-server/*"    && run_eureka=true   run_shared=true
    any_changed "gateway/*"          && run_gateway=true  run_shared=true
    any_changed "products-service/*" && run_products=true run_shared=true
    any_changed "media-service/*"    && run_media=true    run_shared=true
    any_changed "users-service/*"    && run_users=true    run_shared=true
    any_changed "frontend/*"         && run_frontend=true
  fi
fi

# Ensure Java 21 before any JVM builds
if [[ "${run_shared}${run_eureka}${run_gateway}${run_products}${run_media}${run_users}" == *true* ]]; then
  ensure_java_21
  log "Clearing stale Maven cache for com.example:shared"
  rm -rf ~/.m2/repository/com/example/shared 2>/dev/null || true
fi

[[ "${run_shared}"   == true ]] && run_maven "shared" "install"           || log "Skipping shared"
[[ "${run_eureka}"   == true ]] && run_maven "eureka-server/eureka"        || log "Skipping eureka-server"
[[ "${run_gateway}"  == true ]] && run_maven "gateway/gateway"             || log "Skipping gateway"
[[ "${run_products}" == true ]] && run_maven "products-service/products"   || log "Skipping products-service"
[[ "${run_media}"    == true ]] && run_maven "media-service/media"         || log "Skipping media-service"
[[ "${run_users}"    == true ]] && run_gradle "users-service/service"      || log "Skipping users-service"
[[ "${run_frontend}" == true ]] && run_frontend                            || log "Skipping frontend"

log "=========================================="
log "BUILD COMPLETED SUCCESSFULLY"
monitor_resources
log "=========================================="