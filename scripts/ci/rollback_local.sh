#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <git-commit-sha>"
  exit 1
fi

TARGET_COMMIT="$1"

git cat-file -e "${TARGET_COMMIT}^{commit}" || {
    echo "[ERROR] Invalid commit: ${TARGET_COMMIT}"
    exit 1
}

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TEMP_DIR}"' EXIT

echo "[CD] Rolling back to commit ${TARGET_COMMIT}"

cd "${ROOT_DIR}"
git archive "${TARGET_COMMIT}" | tar -x -C "${TEMP_DIR}"

if ! docker network inspect shared-net >/dev/null 2>&1; then
  docker network create shared-net
fi

compose_up() {
  local compose_file="${TEMP_DIR}/$1/docker-compose.yaml"
  if [[ ! -f "${compose_file}" ]]; then
    echo "[ERROR] Compose file not found: ${compose_file}"
    return 1
  fi
  echo "[CD] Deploying $1"
  docker compose -f "${compose_file}" up -d --build
}

compose_up "eureka-server"
compose_up "redis"
compose_up "kafka"
compose_up "products-service"
compose_up "media-service"
compose_up "users-service"
compose_up "gateway"

echo "[CD] Rollback completed successfully."