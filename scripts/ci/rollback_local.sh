#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <git-commit-sha>"
  exit 1
fi

TARGET_COMMIT="$1"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TEMP_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "${TEMP_DIR}"
}
trap cleanup EXIT

cd "${ROOT_DIR}"
echo "[CD] Rolling back deployment to commit ${TARGET_COMMIT}"

git archive "${TARGET_COMMIT}" | tar -x -C "${TEMP_DIR}"

if ! docker network inspect shared-net >/dev/null 2>&1; then
  docker network create shared-net
fi

compose_up() {
  local compose_file="$1"
  local full_path="${TEMP_DIR}/${compose_file}"
  
  if [[ ! -f "${full_path}" ]]; then
    echo "[ERROR] Compose file not found: ${full_path}"
    return 1
  fi
  
  # Try docker compose (v2) first, fall back to docker-compose (v1)
  if command -v docker &> /dev/null && docker compose --version &>/dev/null 2>&1; then
    docker compose -f "${full_path}" up -d --build
  elif command -v docker-compose &> /dev/null; then
    docker-compose -f "${full_path}" up -d --build
  else
    echo "[ERROR] Neither 'docker compose' nor 'docker-compose' found"
    return 1
  fi
}

compose_up "eureka-server/docker-compose.yaml"
compose_up "redis/docker-compose.yaml"
compose_up "kafka/docker-compose.yaml"
compose_up "products-service/docker-compose.yaml"
compose_up "media-service/docker-compose.yaml"
compose_up "users-service/docker-compose.yaml"
compose_up "gateway/docker-compose.yaml"

echo "[CD] Rollback completed successfully."
