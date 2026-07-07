#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

SSH_KEY="${SSH_KEY:-${HOME}/.ssh/oracle.key}"
SSH_TARGET="${SSH_TARGET:-opc@84.8.222.12}"
CERT_DIR="${CERT_DIR:-${ROOT_DIR}/scripts/certs}"
REMOTE_DIR="${REMOTE_DIR:-/tmp/market-place-cert-volumes}"
HELPER_IMAGE="${HELPER_IMAGE:-alpine:3.20}"
CLEAN=true
KEEP_REMOTE_STAGE=false

usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

Uploads certificate/key material to a remote Docker host and creates/syncs the
Docker cert volumes there by running scripts/setup-cert-volumes.sh remotely.

Default server:
  ssh -i ~/.ssh/oracle.key opc@84.8.222.12

Options:
  --ssh-key PATH      SSH private key. Default: ${HOME}/.ssh/oracle.key
  --target USER@HOST  SSH target. Default: opc@84.8.222.12
  --cert-dir PATH     Local cert directory. Default: ${ROOT_DIR}/scripts/certs
  --remote-dir PATH   Remote staging directory. Default: /tmp/market-place-cert-volumes
  --no-clean          Do not clean target Docker volumes before copying files.
  --keep-remote-stage Keep uploaded files in the remote staging directory.
                      By default, the directory is removed after volume sync.
  -h, --help          Show this help.

Remote Docker volumes prepared:
  gateway-certs
  users-service-certs
  products-service-certs
  media-service-certs
  eureka-server-certs
  prometheus-certs
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --ssh-key)
      SSH_KEY="$2"
      shift 2
      ;;
    --target)
      SSH_TARGET="$2"
      shift 2
      ;;
    --cert-dir)
      CERT_DIR="$2"
      shift 2
      ;;
    --remote-dir)
      REMOTE_DIR="$2"
      shift 2
      ;;
    --no-clean)
      CLEAN=false
      shift
      ;;
    --keep-remote-stage)
      KEEP_REMOTE_STAGE=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[remote-cert-volumes] Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

require_cmd() {
  local cmd="$1"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    echo "[remote-cert-volumes] Missing required command: ${cmd}" >&2
    exit 1
  fi
}

require_file() {
  local path="$1"
  if [[ ! -f "${path}" ]]; then
    echo "[remote-cert-volumes] Missing required file: ${path}" >&2
    exit 1
  fi
}

copy_if_exists() {
  local src="$1"
  local dest="$2"

  if [[ -f "${src}" ]]; then
    install -m 600 "${src}" "${dest}"
  fi
}

require_cmd ssh
require_cmd scp
require_file "${SSH_KEY}"

CERT_DIR="$(cd "${CERT_DIR}" && pwd)"
SETUP_SCRIPT="${ROOT_DIR}/scripts/setup-cert-volumes.sh"
require_file "${SETUP_SCRIPT}"

require_file "${CERT_DIR}/truststore.p12"
require_file "${CERT_DIR}/ca.crt"
require_file "${CERT_DIR}/gateway.p12"
require_file "${CERT_DIR}/users-service.p12"
require_file "${CERT_DIR}/products-service.p12"
require_file "${CERT_DIR}/media-service.p12"
require_file "${CERT_DIR}/eureka-server.p12"
require_file "${CERT_DIR}/prometheus.crt"
require_file "${CERT_DIR}/prometheus.key"

STAGE_DIR="$(mktemp -d)"
cleanup() {
  rm -rf "${STAGE_DIR}"
}
trap cleanup EXIT

mkdir -p "${STAGE_DIR}/certs"

install -m 600 "${CERT_DIR}/truststore.p12" "${STAGE_DIR}/certs/truststore.p12"
install -m 644 "${CERT_DIR}/ca.crt" "${STAGE_DIR}/certs/ca.crt"
install -m 600 "${CERT_DIR}/gateway.p12" "${STAGE_DIR}/certs/gateway.p12"
install -m 600 "${CERT_DIR}/users-service.p12" "${STAGE_DIR}/certs/users-service.p12"
install -m 600 "${CERT_DIR}/products-service.p12" "${STAGE_DIR}/certs/products-service.p12"
install -m 600 "${CERT_DIR}/media-service.p12" "${STAGE_DIR}/certs/media-service.p12"
install -m 600 "${CERT_DIR}/eureka-server.p12" "${STAGE_DIR}/certs/eureka-server.p12"
install -m 600 "${CERT_DIR}/prometheus.crt" "${STAGE_DIR}/certs/prometheus.crt"
install -m 600 "${CERT_DIR}/prometheus.key" "${STAGE_DIR}/certs/prometheus.key"

copy_if_exists "${CERT_DIR}/jwt-private.pem" "${STAGE_DIR}/certs/jwt-private.pem"
copy_if_exists "${CERT_DIR}/jwt-public.pem" "${STAGE_DIR}/certs/jwt-public.pem"
copy_if_exists "${CERT_DIR}/private.pem" "${STAGE_DIR}/certs/private.pem"
copy_if_exists "${CERT_DIR}/public.pem" "${STAGE_DIR}/certs/public.pem"

install -m 755 "${SETUP_SCRIPT}" "${STAGE_DIR}/setup-cert-volumes.sh"

SSH_ARGS=(-i "${SSH_KEY}" -o IdentitiesOnly=yes)
CLEAN_ARG=("--clean")
if [[ "${CLEAN}" != "true" ]]; then
  CLEAN_ARG=()
fi

echo "[remote-cert-volumes] Preparing remote staging directory: ${SSH_TARGET}:${REMOTE_DIR}"
ssh "${SSH_ARGS[@]}" "${SSH_TARGET}" "rm -rf '${REMOTE_DIR}' && mkdir -p '${REMOTE_DIR}'"

echo "[remote-cert-volumes] Uploading staged certificate files"
scp "${SSH_ARGS[@]}" -r "${STAGE_DIR}/." "${SSH_TARGET}:${REMOTE_DIR}/"

echo "[remote-cert-volumes] Syncing Docker cert volumes on ${SSH_TARGET}"
ssh "${SSH_ARGS[@]}" "${SSH_TARGET}" \
  "HELPER_IMAGE='${HELPER_IMAGE}' bash '${REMOTE_DIR}/setup-cert-volumes.sh' --cert-dir '${REMOTE_DIR}/certs' ${CLEAN_ARG[*]}"

if [[ "${KEEP_REMOTE_STAGE}" != "true" ]]; then
  echo "[remote-cert-volumes] Removing remote staging directory"
  ssh "${SSH_ARGS[@]}" "${SSH_TARGET}" "rm -rf '${REMOTE_DIR}'"
fi

echo "[remote-cert-volumes] Remote certificate volumes are ready."
