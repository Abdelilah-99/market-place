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
