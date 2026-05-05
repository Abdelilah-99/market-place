#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

ensure_java_21() {
  local java_major
  java_major="$(java -version 2>&1 | awk -F '[\".]' '/version/ {print $2; exit}')"

  if [[ "${java_major}" == "21" ]]; then
    echo "[CI] Java 21 detected."
    return
  fi

  local candidates=(
    "/usr/lib/jvm/java-21-openjdk-amd64"
    "/usr/lib/jvm/java-1.21.0-openjdk-amd64"
    "/usr/lib/jvm/openjdk-21"
  )

  for candidate in "${candidates[@]}"; do
    if [[ -x "${candidate}/bin/java" ]]; then
      export JAVA_HOME="${candidate}"
      export PATH="${JAVA_HOME}/bin:${PATH}"
      echo "[CI] Switched JAVA_HOME to ${JAVA_HOME}"
      java -version
      return
    fi
  done

  echo "[CI] Java 21 is required. Current java version is ${java_major}."
  echo "[CI] Install JDK 21 or set JAVA_HOME to a Java 21 installation."
  exit 1
}

run_maven_module() {
  local module_dir="$1"
  local maven_goal="${2:-verify}"
  echo "[CI] Running Maven (${maven_goal}) for module: ${module_dir}"

  if [[ -x "${module_dir}/mvnw" ]]; then
    (
      cd "${module_dir}"
      ./mvnw -B clean "${maven_goal}"
    )
  else
    (
      cd "${module_dir}"
      mvn -B clean "${maven_goal}"
    )
  fi
}

run_gradle_module() {
  local module_dir="$1"
  echo "[CI] Testing Gradle module: ${module_dir}"

  (
    cd "${module_dir}"
    chmod +x ./gradlew
    export DISCOVERY="${DISCOVERY:-http://localhost:8761/eureka}"
    export MONGODB_USERNAME="${MONGODB_USERNAME:-admin}"
    export MONGODB_PWD="${MONGODB_PWD:-admin}"
    export MONGODB_HOST="${MONGODB_HOST:-localhost}"
    export MONGODB_PORT="${MONGODB_PORT:-27017}"
    export MONGO_DB="${MONGO_DB:-users_db}"
    export MONGODB_AUTH="${MONGODB_AUTH:-admin}"
    export JWT_EXP="${JWT_EXP:-3600000}"
    export SPRING_KAFKA_BOOTSTRAP_SERVERS="${SPRING_KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"
    ./gradlew clean test
  )
}

run_frontend_tests() {
  local module_dir="frontend"
  echo "[CI] Testing Angular frontend (${module_dir})"

  (
    cd "${module_dir}"
    npm ci
    if [[ ! -f "node_modules/lightningcss-linux-x64-gnu/lightningcss.linux-x64-gnu.node" ]]; then
      echo "[CI] Reinstalling missing lightningcss Linux native binary."
      npm install --no-save --include=optional lightningcss-linux-x64-gnu@1.30.2
    fi
    if find src -type f \( -name "*.spec.ts" -o -name "*.test.ts" \) | grep -q .; then
      npm run test -- --watch=false
    else
      echo "[CI] No frontend unit tests found; skipping ng test."
    fi
    npm run build
  )
}

detect_base_commit() {
  local base_commit=""

  if [[ -n "${CI_STATE_DIR:-}" && -f "${CI_STATE_DIR}/last_successful_commit" ]]; then
    base_commit="$(tr -d '[:space:]' < "${CI_STATE_DIR}/last_successful_commit")"
  fi

  if [[ -z "${base_commit}" ]]; then
    if git rev-parse --verify HEAD^ >/dev/null 2>&1; then
      base_commit="$(git rev-parse HEAD^)"
    fi
  fi

  printf '%s' "${base_commit}"
}

changed_files_since() {
  local base_commit="$1"

  if [[ -z "${base_commit}" ]]; then
    git ls-files
    return
  fi

  git diff --name-only --diff-filter=ACMRT "${base_commit}"...HEAD
}

path_matches_any() {
  local path="$1"
  shift

  local pattern
  for pattern in "$@"; do
    case "${path}" in
      ${pattern})
        return 0
        ;;
    esac
  done

  return 1
}

should_run_changes() {
  local pattern="$1"
  local changed_file

  for changed_file in "${changed_files[@]}"; do
    [[ -z "${changed_file}" ]] && continue
    if path_matches_any "${changed_file}" "${pattern}"; then
      return 0
    fi
  done

  return 1
}

base_commit="$(detect_base_commit)"
if [[ -n "${base_commit}" ]]; then
  echo "[CI] Diff base commit: ${base_commit}"
else
  echo "[CI] No diff base commit available; running full build."
fi

mapfile -t changed_files < <(changed_files_since "${base_commit}")

run_shared=false
run_eureka=false
run_gateway=false
run_products=false
run_media=false
run_users=false
run_frontend=false

if [[ ${#changed_files[@]} -eq 0 ]]; then
  run_shared=true
  run_eureka=true
  run_gateway=true
  run_products=true
  run_media=true
  run_users=true
  run_frontend=true
else
  if should_run_changes "shared/*"; then
    run_shared=true
    run_eureka=true
    run_gateway=true
    run_products=true
    run_media=true
    run_users=true
  fi

  if should_run_changes "eureka-server/*"; then
    run_eureka=true
  fi

  if should_run_changes "gateway/*"; then
    run_gateway=true
  fi

  if should_run_changes "products-service/*"; then
    run_products=true
  fi

  if should_run_changes "media-service/*"; then
    run_media=true
  fi

  if should_run_changes "users-service/*"; then
    run_users=true
  fi

  if should_run_changes "frontend/*"; then
    run_frontend=true
  fi
fi

if [[ "${run_shared}" == true || "${run_eureka}" == true || "${run_gateway}" == true || "${run_products}" == true || "${run_media}" == true || "${run_users}" == true ]]; then
  ensure_java_21
fi

if [[ "${run_shared}" == true ]]; then
  echo "[CI] Building shared library"
  run_maven_module "shared" "install"
else
  echo "[CI] Skipping shared library build"
fi

if [[ "${run_eureka}" == true ]]; then
  run_maven_module "eureka-server/eureka"
else
  echo "[CI] Skipping eureka-server build"
fi

if [[ "${run_gateway}" == true ]]; then
  run_maven_module "gateway/gateway"
else
  echo "[CI] Skipping gateway build"
fi

if [[ "${run_products}" == true ]]; then
  run_maven_module "products-service/products"
else
  echo "[CI] Skipping products-service build"
fi

if [[ "${run_media}" == true ]]; then
  run_maven_module "media-service/media"
else
  echo "[CI] Skipping media-service build"
fi

if [[ "${run_users}" == true ]]; then
  run_gradle_module "users-service/service"
else
  echo "[CI] Skipping users-service build"
fi

if [[ "${run_frontend}" == true ]]; then
  run_frontend_tests
else
  echo "[CI] Skipping frontend build and tests"
fi

echo "[CI] Build and test completed successfully."
