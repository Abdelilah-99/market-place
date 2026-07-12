#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CERT_DIR="${CERT_DIR:-${ROOT_DIR}/scripts/certs}"
HELPER_IMAGE="${HELPER_IMAGE:-alpine:3.20}"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--cert-dir PATH] [--service NAME] [--jwt-private PATH] [--jwt-public PATH] [--clean]

Copies generated certificates into the external Docker volumes used by the
service docker-compose files. If JWT keys are not provided or found, the script
generates a matching RSA pair for users-service and gateway.

Options:
  --cert-dir PATH     Directory containing *.p12, ca.crt, and service certs.
                      Default: ${ROOT_DIR}/scripts/certs
  --service NAME      Sync only one service certificate volume. Supported names:
                      gateway, users-service, products-service, media-service,
                      payments-service, eureka-server, prometheus. Repeatable.
  --jwt-private PATH  JWT private key to copy into users-service-certs as jwt-private.pem.
                      Also read from JWT_PRIVATE_KEY_PATH or JWT_PRIVATE_KEY.
  --jwt-public PATH   JWT public key to copy into gateway-certs as jwt-public.pem.
                      Also read from JWT_PUBLIC_KEY_PATH or JWT_PUBLIC_KEY.
                      If omitted but a private key is available, it is derived.
  --clean             Remove existing files from each cert volume before copying.
  HELPER_IMAGE        Override the Docker helper image used for copying.
                      Default: alpine:3.20
  -h, --help          Show this help.

Expected mTLS files:
  truststore.p12
  ca.crt
  gateway.p12
  users-service.p12
  products-service.p12
  media-service.p12
  payments-service.p12
  eureka-server.p12
  prometheus.crt
  prometheus.key
EOF
}

JWT_PRIVATE_SRC="${JWT_PRIVATE_KEY_PATH:-${JWT_PRIVATE_KEY:-}}"
JWT_PUBLIC_SRC="${JWT_PUBLIC_KEY_PATH:-${JWT_PUBLIC_KEY:-}}"
CLEAN=false
SERVICES=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --cert-dir)
      CERT_DIR="$2"
      shift 2
      ;;
    --jwt-private)
      JWT_PRIVATE_SRC="$2"
      shift 2
      ;;
    --service)
      SERVICES+=("$2")
      shift 2
      ;;
    --jwt-public)
      JWT_PUBLIC_SRC="$2"
      shift 2
      ;;
    --clean)
      CLEAN=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[cert-volumes] Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

CERT_DIR="$(cd "${CERT_DIR}" && pwd)"

require_file() {
  local path="$1"
  if [[ ! -f "${path}" ]]; then
    echo "[cert-volumes] Missing required file: ${path}" >&2
    exit 1
  fi
}

require_cmd() {
  local cmd="$1"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    echo "[cert-volumes] Missing required command: ${cmd}" >&2
    exit 1
  fi
}

require_cmd docker
require_cmd openssl

if [[ ${#SERVICES[@]} -eq 0 ]]; then
  SERVICES=(gateway users-service products-service media-service payments-service eureka-server prometheus)
fi

is_selected() {
  local wanted="$1"
  local service
  for service in "${SERVICES[@]}"; do
    [[ "${service}" == "${wanted}" ]] && return 0
  done
  return 1
}

for service in "${SERVICES[@]}"; do
  case "${service}" in
    gateway|users-service|products-service|media-service|payments-service|eureka-server)
      require_file "${CERT_DIR}/truststore.p12"
      require_file "${CERT_DIR}/ca.crt"
      require_file "${CERT_DIR}/${service}.p12"
      ;;
    prometheus)
      require_file "${CERT_DIR}/ca.crt"
      require_file "${CERT_DIR}/prometheus.crt"
      require_file "${CERT_DIR}/prometheus.key"
      ;;
    *)
      echo "[cert-volumes] Unsupported service: ${service}" >&2
      exit 2
      ;;
  esac
done

ensure_volume() {
  local volume="$1"
  if ! docker volume inspect "${volume}" >/dev/null 2>&1; then
    echo "[cert-volumes] Creating Docker volume: ${volume}"
    docker volume create "${volume}" >/dev/null
  fi
}

copy_into_volume() {
  local volume="$1"
  local service_cert="$2"
  shift 2
  local optional_sources=("$@")

  ensure_volume "${volume}"

  echo "[cert-volumes] Syncing ${volume}"

  local stage_dir
  stage_dir="$(mktemp -d)"
  trap 'rm -rf "${stage_dir}"' RETURN

  install -m 600 "${CERT_DIR}/${service_cert}.p12" "${stage_dir}/${service_cert}.p12"
  install -m 600 "${CERT_DIR}/truststore.p12" "${stage_dir}/truststore.p12"
  install -m 644 "${CERT_DIR}/ca.crt" "${stage_dir}/ca.crt"

  local source_pair
  local source
  local dest
  for source_pair in "${optional_sources[@]}"; do
    source="${source_pair%%:*}"
    dest="${source_pair#*:}"

    if [[ -n "${source}" && -f "${source}" ]]; then
      install -m 600 "${source}" "${stage_dir}/${dest}"
      echo "[cert-volumes]   copied optional ${dest}"
    fi
  done

  local sync_script='set -eu
if [ "${CLEAN_VOLUME}" = "true" ]; then
  find /target -mindepth 1 -maxdepth 1 -exec rm -rf {} +
fi
cp -a /source/. /target/
chmod 600 /target/* || true
chmod 644 /target/ca.crt || true
'

  docker run --rm \
    -e "CLEAN_VOLUME=${CLEAN}" \
    -v "${volume}:/target" \
    -v "${stage_dir}:/source:ro" \
    "${HELPER_IMAGE}" \
    sh -c "${sync_script}"

  rm -rf "${stage_dir}"
  trap - RETURN
}

copy_prometheus_volume() {
  local volume="prometheus-certs"

  ensure_volume "${volume}"

  echo "[cert-volumes] Syncing ${volume}"

  local stage_dir
  stage_dir="$(mktemp -d)"
  trap 'rm -rf "${stage_dir}"' RETURN

  install -m 644 "${CERT_DIR}/ca.crt" "${stage_dir}/ca.crt"
  install -m 600 "${CERT_DIR}/prometheus.crt" "${stage_dir}/client.crt"
  install -m 600 "${CERT_DIR}/prometheus.key" "${stage_dir}/client.key"

  local sync_script='set -eu
if [ "${CLEAN_VOLUME}" = "true" ]; then
  find /target -mindepth 1 -maxdepth 1 -exec rm -rf {} +
fi
cp -a /source/. /target/
chmod 600 /target/* || true
chmod 644 /target/ca.crt || true
'

  docker run --rm \
    -e "CLEAN_VOLUME=${CLEAN}" \
    -v "${volume}:/target" \
    -v "${stage_dir}:/source:ro" \
    "${HELPER_IMAGE}" \
    sh -c "${sync_script}"

  rm -rf "${stage_dir}"
  trap - RETURN
}

resolve_jwt_sources() {
  if [[ -z "${JWT_PRIVATE_SRC}" ]]; then
    for candidate in \
      "${CERT_DIR}/jwt-private.pem" \
      "${CERT_DIR}/private.pem" \
      "${ROOT_DIR}/users-service/certs/jwt-private.pem"; do
      if [[ -f "${candidate}" ]]; then
        JWT_PRIVATE_SRC="${candidate}"
        break
      fi
    done
  fi

  if [[ -z "${JWT_PUBLIC_SRC}" ]]; then
    for candidate in \
      "${CERT_DIR}/jwt-public.pem" \
      "${CERT_DIR}/public.pem" \
      "${ROOT_DIR}/gateway/certs/jwt-public.pem"; do
      if [[ -f "${candidate}" ]]; then
        JWT_PUBLIC_SRC="${candidate}"
        break
      fi
    done
  fi
}

prepare_jwt_keys() {
  resolve_jwt_sources

  if [[ -n "${JWT_PRIVATE_SRC}" && ! -f "${JWT_PRIVATE_SRC}" ]]; then
    echo "[cert-volumes] JWT private key was provided but does not exist: ${JWT_PRIVATE_SRC}" >&2
    exit 1
  fi

  if [[ -n "${JWT_PUBLIC_SRC}" && ! -f "${JWT_PUBLIC_SRC}" ]]; then
    echo "[cert-volumes] JWT public key was provided but does not exist: ${JWT_PUBLIC_SRC}" >&2
    exit 1
  fi

  if [[ -z "${JWT_PRIVATE_SRC}" && -z "${JWT_PUBLIC_SRC}" ]]; then
    JWT_PRIVATE_SRC="${CERT_DIR}/jwt-private.pem"
    JWT_PUBLIC_SRC="${CERT_DIR}/jwt-public.pem"

    echo "[cert-volumes] Generating JWT RSA key pair in ${CERT_DIR}"
    openssl genpkey \
      -algorithm RSA \
      -pkeyopt rsa_keygen_bits:2048 \
      -out "${JWT_PRIVATE_SRC}" >/dev/null 2>&1
    openssl rsa \
      -pubout \
      -in "${JWT_PRIVATE_SRC}" \
      -out "${JWT_PUBLIC_SRC}" >/dev/null 2>&1
    chmod 600 "${JWT_PRIVATE_SRC}" "${JWT_PUBLIC_SRC}"
    return
  fi

  if [[ -n "${JWT_PRIVATE_SRC}" && -z "${JWT_PUBLIC_SRC}" ]]; then
    JWT_PUBLIC_SRC="${CERT_DIR}/jwt-public.pem"

    echo "[cert-volumes] Deriving JWT public key from ${JWT_PRIVATE_SRC}"
    openssl rsa \
      -pubout \
      -in "${JWT_PRIVATE_SRC}" \
      -out "${JWT_PUBLIC_SRC}" >/dev/null 2>&1
    chmod 600 "${JWT_PUBLIC_SRC}"
    return
  fi

  if [[ -z "${JWT_PRIVATE_SRC}" && -n "${JWT_PUBLIC_SRC}" ]]; then
    echo "[cert-volumes] Gateway JWT public key exists, but users-service JWT private key is missing." >&2
    echo "[cert-volumes] Provide --jwt-private or remove the stale public key so a matching pair can be generated." >&2
    exit 1
  fi
}

if is_selected gateway || is_selected users-service; then
  prepare_jwt_keys
  echo "[cert-volumes] users-service JWT private key: ${JWT_PRIVATE_SRC}"
  echo "[cert-volumes] gateway JWT public key: ${JWT_PUBLIC_SRC}"
fi

is_selected gateway && copy_into_volume "gateway-certs" "gateway" "${JWT_PUBLIC_SRC}:jwt-public.pem"
is_selected users-service && copy_into_volume "users-service-certs" "users-service" "${JWT_PRIVATE_SRC}:jwt-private.pem"
is_selected products-service && copy_into_volume "products-service-certs" "products-service"
is_selected media-service && copy_into_volume "media-service-certs" "media-service"
is_selected payments-service && copy_into_volume "payments-service-certs" "payments-service"
is_selected eureka-server && copy_into_volume "eureka-server-certs" "eureka-server"
is_selected prometheus && copy_prometheus_volume

echo "[cert-volumes] Certificate volumes are ready."
