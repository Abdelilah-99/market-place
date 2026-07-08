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

run_maven() {
  local dir="$1" goal="${2:-verify}"
  local mvn_cmd="mvn"
  [[ -x "${dir}/mvnw" ]] && mvn_cmd="./mvnw"
  log "Maven ${goal}: ${dir}"
  (
    cd "${dir}"
    timeout 600 ${mvn_cmd} -B -Dmaven.compile.fork=false clean "${goal}" || {
      [[ $? -eq 124 ]] && { log "ERROR: Maven timed out"; exit 1; }
      exit $?
    }
  )
}

run_gradle() {
  log "Gradle build: $1"
  (
    cd "$1"
    chmod +x ./gradlew
    timeout 600 ./gradlew clean build -Dorg.gradle.workers.max=1 || {
      [[ $? -eq 124 ]] && { log "ERROR: Gradle timed out"; exit 1; }
      exit $?
    }
  )
}

run_frontend() {
  log "Frontend build"
  (
    cd frontend
    npm ci --prefer-offline --no-audit
    if [[ ! -f "node_modules/lightningcss-linux-x64-gnu/lightningcss.linux-x64-gnu.node" ]]; then
      npm install --no-save --include=optional lightningcss-linux-x64-gnu@1.30.2
    fi
    npx playwright install chromium
    npm run test -- --watch=false --browsers=chromium
    npm run build -- --optimization --aot --stats-json
  )
}

any_changed() {
  local pattern="$1"
  for f in "${changed_files[@]}"; do
    case "${f}" in ${pattern}) return 0 ;; esac
  done
  return 1
}

# ---------------------------------------------------------------------------
# Change detection
# ---------------------------------------------------------------------------

base_commit=""
force_full_build=false
if [[ -n "${CI_STATE_DIR:-}" && -f "${CI_STATE_DIR}/last_successful_commit" ]]; then
  base_commit="$(tr -d '[:space:]' < "${CI_STATE_DIR}/last_successful_commit")"
  if [[ -n "${base_commit}" ]] && ! git cat-file -e "${base_commit}^{commit}" 2>/dev/null; then
    log "Stored last successful commit is not available locally: ${base_commit}. Running full build."
    base_commit=""
    force_full_build=true
  fi
fi

if [[ "${force_full_build}" != true && -z "${base_commit}" ]] && git rev-parse --verify HEAD^ >/dev/null 2>&1; then
  base_commit="$(git rev-parse HEAD^)"
fi

# ---------------------------------------------------------------------------
# Determine what to build
# ---------------------------------------------------------------------------

run_shared=false run_eureka=false run_gateway=false run_payments=false
run_products=false run_media=false run_users=false run_frontend=false
changed_files=()
if [[ -n "${base_commit}" ]]; then
  log "Detecting changes since ${base_commit}."
  mapfile -t changed_files < <(
    git diff --name-only --diff-filter=ACMRT "${base_commit}"...HEAD
    git diff --name-only --diff-filter=ACMRT HEAD
  )
else
  log "No valid base commit found."
fi

if [[ ${#changed_files[@]} -eq 0 ]]; then
  log "No changes detected — running full build."
  run_shared=true run_eureka=true run_gateway=true run_payments=true
  run_products=true run_media=true run_users=true run_frontend=true
else
  if any_changed "scripts/ci/build_and_test.sh"; then
    log "CI build script changed — running full build."
    run_shared=true run_eureka=true run_gateway=true run_payments=true
    run_products=true run_media=true run_users=true run_frontend=true
  fi
  any_changed "shared/*"           && run_shared=true run_eureka=true run_gateway=true run_products=true run_media=true run_users=true
  any_changed "eureka-server/*"    && run_eureka=true  run_shared=true
  any_changed "gateway/*"          && run_gateway=true run_shared=true
  any_changed "payments-service/*" && run_payments=true
  any_changed "products-service/*" && run_products=true run_shared=true
  any_changed "media-service/*"    && run_media=true   run_shared=true
  any_changed "users-service/*"    && run_users=true   run_shared=true
  any_changed "frontend/*"         && run_frontend=true
fi

# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------

if [[ "${run_shared}${run_eureka}${run_gateway}${run_payments}${run_products}${run_media}${run_users}" == *true* ]]; then
  ensure_java_21
  rm -rf ~/.m2/repository/com/example/shared 2>/dev/null || true
fi

[[ "${run_shared}"   == true ]] && run_maven "shared" "install"         || log "Skipping shared"
[[ "${run_eureka}"   == true ]] && run_maven "eureka-server/eureka"      || log "Skipping eureka-server"
[[ "${run_gateway}"  == true ]] && run_maven "gateway/gateway"           || log "Skipping gateway"
[[ "${run_payments}" == true ]] && run_maven "payments-service"          || log "Skipping payments-service"
[[ "${run_products}" == true ]] && run_maven "products-service/products" || log "Skipping products-service"
[[ "${run_media}"    == true ]] && run_maven "media-service/media"       || log "Skipping media-service"
[[ "${run_users}"    == true ]] && run_gradle "users-service/service"    || log "Skipping users-service"
[[ "${run_frontend}" == true ]] && run_frontend                          || log "Skipping frontend"

log "Build completed successfully."
