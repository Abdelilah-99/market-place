#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"
STATE_DIR="${ROOT_DIR}/.deploy-state"
FRONTEND_PID_FILE="${STATE_DIR}/frontend.pid"
FRONTEND_LOG_FILE="${STATE_DIR}/frontend.log"

mkdir -p "${STATE_DIR}"

compose_up() {
  local compose_file="$1"
  
  if [[ ! -f "${compose_file}" ]]; then
    echo "[ERROR] Compose file not found: ${compose_file}"
    return 1
  fi
  
  echo "[CD] Applying ${compose_file}"
  
  # Try docker compose (v2) first, fall back to docker-compose (v1)
  if command -v docker &> /dev/null && docker compose --version &>/dev/null 2>&1; then
    docker compose -f "${compose_file}" up -d --build
  elif command -v docker-compose &> /dev/null; then
    docker-compose -f "${compose_file}" up -d --build
  else
    echo "[ERROR] Neither 'docker compose' nor 'docker-compose' found"
    return 1
  fi
}

if ! docker network inspect shared-net >/dev/null 2>&1; then
  echo "[CD] Creating shared Docker network: shared-net"
  docker network create shared-net
fi

compose_up "eureka-server/docker-compose.yaml"
compose_up "redis/docker-compose.yaml"
compose_up "kafka/docker-compose.yaml"
compose_up "products-service/docker-compose.yaml"
compose_up "media-service/docker-compose.yaml"
compose_up "users-service/docker-compose.yaml"
compose_up "gateway/docker-compose.yaml"

start_frontend() {
  local frontend_dir="${ROOT_DIR}/frontend"

  if [[ -f "${FRONTEND_PID_FILE}" ]]; then
    local existing_pid
    existing_pid="$(cat "${FRONTEND_PID_FILE}")"
    if kill -0 "${existing_pid}" >/dev/null 2>&1; then
      echo "[CD] Frontend is already running with PID ${existing_pid}."
      return
    fi
  fi

  echo "[CD] Starting frontend on port 4200"
  (
    cd "${frontend_dir}"
    nohup npm run start -- --host 0.0.0.0 --port 4200 > "${FRONTEND_LOG_FILE}" 2>&1 &
    echo $! > "${FRONTEND_PID_FILE}"
  )

  echo "[CD] Waiting for frontend to become ready"
  for _ in {1..60}; do
    if curl -fsS "http://127.0.0.1:4200" >/dev/null 2>&1; then
      echo "[CD] Frontend is ready on port 4200"
      return
    fi
    sleep 1
  done

  echo "[CD] Frontend failed to start. Last log lines:"
  tail -n 50 "${FRONTEND_LOG_FILE}" || true
  exit 1
}

start_frontend

echo "[CD] Deployment completed successfully."
