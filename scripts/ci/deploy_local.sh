#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

# shellcheck source=scripts/ci/docker_cleanup.sh
source "${ROOT_DIR}/scripts/ci/docker_cleanup.sh"

compose_up() {
  local dir="$1"
  local rebuild="${2:-true}"
  local compose_args=(up -d)
  local project_args=()
  # The Jenkins workspace is named "marketo", while this stack may also be
  # started from a checkout named "market-place". Without a stable project
  # name, Compose treats the same fixed-name monitoring containers as foreign
  # and fails with "container name is already in use".
  if [[ "${dir}" == "." ]]; then
    project_args=(--project-name market-place)
  fi
  if [[ "${rebuild}" == "true" ]]; then
    compose_args+=(--build --force-recreate)
  fi
  if [[ "${rebuild}" == "true" ]]; then
    docker_ensure_space
  fi

  echo "[CD] Deploying ${dir} (rebuild=${rebuild})"
  if [[ "$dir" == "opensearch" && -f "${dir}/.env.opensearch" ]]; then
    if ! (cd "${dir}" && docker compose "${project_args[@]}" --env-file .env.opensearch "${compose_args[@]}"); then
      docker_cleanup_aggressive
      return 1
    fi
  else
    if ! (cd "${dir}" && docker compose "${project_args[@]}" "${compose_args[@]}"); then
      docker_cleanup_aggressive
      return 1
    fi
  fi

  docker_build_cleanup
}

changed_since_last_success() {
  local pattern="$1"
  [[ "${FULL_DEPLOY}" == "true" ]] && return 0
  git diff --quiet "${DEPLOY_BASE}"...HEAD -- "${pattern}" || return 0
  return 1
}

deploy_if_changed() {
  local dir="$1"
  shift
  local container_name
  case "${dir}" in
    eureka-server) container_name="eureka-server" ;;
    products-service) container_name="products-service" ;;
    media-service) container_name="media-service" ;;
    users-service) container_name="users-service" ;;
    payments-service) container_name="payments-service" ;;
    gateway) container_name="gateway" ;;
    frontend) container_name="buy01-frontend" ;;
    *) container_name="${dir}" ;;
  esac

  if ! docker container inspect "${container_name}" >/dev/null 2>&1; then
    echo "[CD] ${container_name} is missing; deploying it regardless of change detection."
    compose_up "${dir}" true
    return
  fi

  local pattern
  for pattern in "$@"; do
    if changed_since_last_success "${pattern}"; then
      compose_up "${dir}" true
      return
    fi
  done
  echo "[CD] Skipping unchanged service: ${dir}"
}

detach_legacy_runtime_caches() {
  local dirs=(products-service media-service users-service gateway)
  local dir
  echo "[CD] Detaching obsolete Maven/Gradle runtime cache mounts."
  for dir in "${dirs[@]}"; do
    # Reuse the currently deployed image; do not consume space by building yet.
    # A missing image/container is harmless and will be handled by deployment.
    if [[ -f "${dir}/docker-compose.yaml" ]]; then
      (cd "${dir}" && docker compose up -d --no-build) || true
    fi
  done
  docker_cleanup_legacy_build_cache_volumes
}

if ! docker network inspect shared-net >/dev/null 2>&1; then
  echo "[CD] Creating shared Docker network: shared-net"
  docker network create shared-net
fi

ensure_volume() {
  local volume="$1"
  if ! docker volume inspect "${volume}" >/dev/null 2>&1; then
    echo "[CD] Creating Docker volume: ${volume}"
    docker volume create "${volume}" >/dev/null
  fi
}

ensure_volume "media-service_mongo_data"
ensure_volume "media-service_media_storage"
ensure_volume "eureka-server-certs"
ensure_volume "products-service-certs"
ensure_volume "media-service-certs"
ensure_volume "payments-service-certs"
ensure_volume "users-service-certs"
ensure_volume "gateway-certs"
ensure_volume "prometheus-certs"

if [[ -f "certs/truststore.p12" ]]; then
  echo "[CD] Syncing certificate and key Docker volumes."
  bash scripts/setup-cert-volumes.sh \
    --cert-dir certs \
    --jwt-private users-service/certs/jwt-private.pem \
    --jwt-public gateway/certs/jwt-public.pem \
    --clean
else
  echo "[CD] Skipping certificate volume sync; no local cert bundle found."
  echo "[CD] Expected cert volumes to be prepared on the Docker host before deploy."
fi

docker_cleanup
detach_legacy_runtime_caches

DEPLOY_BASE=""
FULL_DEPLOY=false
if [[ -n "${LAST_SUCCESSFUL_COMMIT_FILE:-}" && -f "${LAST_SUCCESSFUL_COMMIT_FILE}" ]]; then
  DEPLOY_BASE="$(tr -d '[:space:]' < "${LAST_SUCCESSFUL_COMMIT_FILE}")"
fi
if [[ -z "${DEPLOY_BASE}" ]] || ! git cat-file -e "${DEPLOY_BASE}^{commit}" 2>/dev/null; then
  echo "[CD] No valid previous successful commit; running a full deployment."
  FULL_DEPLOY=true
else
  echo "[CD] Deploying only changes since ${DEPLOY_BASE}."
  if ! git diff --quiet "${DEPLOY_BASE}"...HEAD -- \
      scripts/generate-certs.sh scripts/setup-cert-volumes.sh scripts/sync-cert-volumes-to-server.sh; then
    echo "[CD] Certificate lifecycle changed; restarting all certificate consumers."
    FULL_DEPLOY=true
  fi
fi

# Infrastructure is started without forced recreation. This is cheap when it
# is already running and avoids repeatedly pulling/rebuilding unchanged images.
compose_up "." false
compose_up "redis" false
compose_up "opensearch" false
bash scripts/ci/opensearch_maintenance.sh
compose_up "kafka" false

deploy_if_changed "eureka-server" "eureka-server" "shared"
deploy_if_changed "products-service" "products-service" "shared"
deploy_if_changed "media-service" "media-service" "shared"
deploy_if_changed "users-service" "users-service" "shared"
deploy_if_changed "payments-service" "payments-service"
deploy_if_changed "gateway" "gateway" "shared"
deploy_if_changed "frontend" "frontend"

docker_cleanup
docker_cleanup_legacy_build_cache_volumes

echo "[CD] Deployment completed successfully."
