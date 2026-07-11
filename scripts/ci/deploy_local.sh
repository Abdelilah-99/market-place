#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

# shellcheck source=scripts/ci/docker_cleanup.sh
source "${ROOT_DIR}/scripts/ci/docker_cleanup.sh"

compose_up() {
  local dir="$1"
  if [[ "$dir" == "kafka" ]]; then
    echo "[CD] Deploying ${dir}"
    (cd "${dir}" && docker compose down && docker compose up -d --build --force-recreate)
  elif [[ "$dir" == "opensearch" && -f "${dir}/.env.opensearch" ]]; then
    echo "[CD] Deploying ${dir}"
    (cd "${dir}" && docker compose --env-file .env.opensearch up -d --build --force-recreate)
  else
    echo "[CD] Deploying ${dir}"
    (cd "${dir}" && docker compose up -d --build --force-recreate)
  fi

  docker_build_cleanup
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

compose_up "eureka-server"
compose_up "redis"
compose_up "opensearch"
bash scripts/ci/opensearch_maintenance.sh
compose_up "kafka"
compose_up "products-service"
compose_up "media-service"
compose_up "users-service"
compose_up "payments-service"
compose_up "gateway"
compose_up "frontend"

docker_cleanup

echo "[CD] Deployment completed successfully."
