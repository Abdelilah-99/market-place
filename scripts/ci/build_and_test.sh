#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

export MAVEN_OPTS="${MAVEN_OPTS:--Xmx1g -Xms256m}"
export GRADLE_OPTS="${GRADLE_OPTS:--Xmx1g -Xms256m -Dorg.gradle.parallel=false -Dorg.gradle.workers.max=1}"
CI_CACHE_DIR="${CI_STATE_DIR:-${ROOT_DIR}/.jenkins-state}"
CI_MAVEN_REPO="${CI_MAVEN_REPO:-${CI_CACHE_DIR}/m2/repository}"
CI_GRADLE_USER_HOME="${CI_GRADLE_USER_HOME:-${CI_CACHE_DIR}/gradle}"

mkdir -p "${CI_MAVEN_REPO}" "${CI_GRADLE_USER_HOME}"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

log() { echo "[CI] $*"; }

ensure_java_21() {
  local java_major="unavailable"
  if command -v java >/dev/null 2>&1; then
    java_major="$(java -version 2>&1 | awk -F '[\".]' '/version/ {print $2; exit}' || true)"
  fi
  [[ "${java_major}" == "21" ]] && return

  local candidates=(
    /opt/java/openjdk
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
  local mvn_cmd="${ROOT_DIR}/shared/mvnw"
  [[ -x "${dir}/mvnw" ]] && mvn_cmd="./mvnw"
  log "Maven ${goal}: ${dir}"
  (
    cd "${dir}"
    set +e
    timeout 600 ${mvn_cmd} -B -Dmaven.repo.local="${CI_MAVEN_REPO}" -Dmaven.compile.fork=false clean "${goal}"
    local status=$?
    set -e
    [[ ${status} -eq 124 ]] && log "ERROR: Maven timed out"
    exit "${status}"
  )
}

run_gradle() {
  log "Gradle build: $1"
  (
    cd "$1"
    chmod +x ./gradlew
    set +e
    timeout 600 ./gradlew clean build \
      -Dorg.gradle.workers.max=1 \
      -Dmaven.repo.local="${CI_MAVEN_REPO}" \
      --gradle-user-home "${CI_GRADLE_USER_HOME}"
    local status=$?
    set -e
    [[ ${status} -eq 124 ]] && log "ERROR: Gradle timed out"
    exit "${status}"
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
  rm -rf "${CI_MAVEN_REPO}/com/example/shared" 2>/dev/null || true
fi

if [[ "${run_shared}" == true ]]; then run_maven "shared" "install"; else log "Skipping shared"; fi
if [[ "${run_eureka}" == true ]]; then run_maven "eureka-server/eureka"; else log "Skipping eureka-server"; fi
if [[ "${run_gateway}" == true ]]; then run_maven "gateway/gateway"; else log "Skipping gateway"; fi
if [[ "${run_payments}" == true ]]; then run_maven "payments-service"; else log "Skipping payments-service"; fi
if [[ "${run_products}" == true ]]; then run_maven "products-service/products"; else log "Skipping products-service"; fi
if [[ "${run_media}" == true ]]; then run_maven "media-service/media"; else log "Skipping media-service"; fi
if [[ "${run_users}" == true ]]; then run_gradle "users-service/service"; else log "Skipping users-service"; fi
if [[ "${run_frontend}" == true ]]; then run_frontend; else log "Skipping frontend"; fi

log "Build completed successfully."
