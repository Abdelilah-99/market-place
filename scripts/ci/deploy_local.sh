#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

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
ensure_volume "users-service-certs"
ensure_volume "gateway-certs"

sync_volume() {
  local src="$1"
  local volume="$2"

  if [[ ! -d "${src}" ]]; then
    echo "[WARN] Missing certificate directory: ${src}"
    return
  fi

  echo "[CD] Syncing ${src} to Docker volume ${volume}"
  docker run --rm \
    -v "${ROOT_DIR}/${src}:/src:ro" \
    -v "${volume}:/dst" \
    alpine:3.20 \
    sh -c 'rm -rf /dst/* && cp -a /src/. /dst/ && chmod -R go-rwx /dst'
}

sync_volume "certs" "eureka-server-certs"
sync_volume "products-service/certs" "products-service-certs"
sync_volume "media-service/certs" "media-service-certs"
sync_volume "users-service/certs" "users-service-certs"
sync_volume "gateway/certs" "gateway-certs"

compose_up "eureka-server"
compose_up "redis"
compose_up "opensearch"
compose_up "kafka"
compose_up "products-service"
compose_up "media-service"
compose_up "users-service"
compose_up "gateway"
compose_up "frontend"

echo "[CD] Deployment completed successfully."
