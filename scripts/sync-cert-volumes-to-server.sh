#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

SSH_KEY="${SSH_KEY:-${HOME}/.ssh/oracle.key}"
SSH_TARGET="${SSH_TARGET:-opc@84.8.222.12}"
CERT_DIR="${CERT_DIR:-}"
REMOTE_DIR="${REMOTE_DIR:-/tmp/market-place-cert-volumes}"
HELPER_IMAGE="${HELPER_IMAGE:-alpine:3.20}"
P12_PASSWORD="${P12_PASSWORD:-${PSSWD:-}}"
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
  --cert-dir PATH     Optional bundled cert directory. By default, files are
                      collected from each service's certs directory.
  --remote-dir PATH   Remote staging directory. Default: /tmp/market-place-cert-volumes
  --p12-password PWD  Password for local PKCS12 files when deriving missing
                      ca.crt or Prometheus client PEM files.
  --no-clean          Do not clean target Docker volumes before copying files.
  --keep-remote-stage Keep uploaded files in the remote staging directory.
                      By default, the directory is removed after volume sync.
  -h, --help          Show this help.

Remote Docker volumes prepared:
  gateway-certs
  users-service-certs
  products-service-certs
  media-service-certs
  payments-service-certs
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
    --p12-password)
      P12_PASSWORD="$2"
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

find_first_file() {
  local label="$1"
  shift

  local candidate
  for candidate in "$@"; do
    if [[ -n "${candidate}" && -f "${candidate}" ]]; then
      printf '%s\n' "${candidate}"
      return 0
    fi
  done

  echo "[remote-cert-volumes] Missing required file for ${label}. Checked:" >&2
  for candidate in "$@"; do
    [[ -n "${candidate}" ]] && echo "  - ${candidate}" >&2
  done
  exit 1
}

find_optional_file() {
  shift

  local candidate
  for candidate in "$@"; do
    if [[ -n "${candidate}" && -f "${candidate}" ]]; then
      printf '%s\n' "${candidate}"
      return 0
    fi
  done
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

if [[ -z "${P12_PASSWORD}" && -f "${ROOT_DIR}/scripts/.env.scripts" ]]; then
  # shellcheck disable=SC1091
  . "${ROOT_DIR}/scripts/.env.scripts"
  P12_PASSWORD="${P12_PASSWORD:-${PSSWD:-}}"
fi

if [[ -n "${CERT_DIR}" ]]; then
  CERT_DIR="$(cd "${CERT_DIR}" && pwd)"
fi

SETUP_SCRIPT="${ROOT_DIR}/scripts/setup-cert-volumes.sh"
require_file "${SETUP_SCRIPT}"

TRUSTSTORE_SRC="$(find_first_file "truststore.p12" \
  "${CERT_DIR}/truststore.p12" \
  "${ROOT_DIR}/gateway/certs/truststore.p12" \
  "${ROOT_DIR}/users-service/certs/truststore.p12" \
  "${ROOT_DIR}/products-service/certs/truststore.p12" \
  "${ROOT_DIR}/media-service/certs/truststore.p12" \
  "${ROOT_DIR}/eureka-server/certs/truststore.p12" \
  "${ROOT_DIR}/scripts/certs/truststore.p12" \
  "${ROOT_DIR}/certs/truststore.p12")"

CA_SRC="$(find_optional_file "ca.crt" \
  "${CERT_DIR}/ca.crt" \
  "${ROOT_DIR}/prometheus/certs/ca.crt" \
  "${ROOT_DIR}/scripts/certs/ca.crt" \
  "${ROOT_DIR}/certs/ca.crt")"

GATEWAY_CERT_SRC="$(find_first_file "gateway.p12" \
  "${CERT_DIR}/gateway.p12" \
  "${ROOT_DIR}/gateway/certs/gateway.p12" \
  "${ROOT_DIR}/scripts/certs/gateway.p12" \
  "${ROOT_DIR}/certs/gateway.p12")"

USERS_CERT_SRC="$(find_first_file "users-service.p12" \
  "${CERT_DIR}/users-service.p12" \
  "${ROOT_DIR}/users-service/certs/users-service.p12" \
  "${ROOT_DIR}/scripts/certs/users-service.p12" \
  "${ROOT_DIR}/certs/users-service.p12")"

PRODUCTS_CERT_SRC="$(find_first_file "products-service.p12" \
  "${CERT_DIR}/products-service.p12" \
  "${ROOT_DIR}/products-service/certs/products-service.p12" \
  "${ROOT_DIR}/scripts/certs/products-service.p12" \
  "${ROOT_DIR}/certs/products-service.p12")"

MEDIA_CERT_SRC="$(find_first_file "media-service.p12" \
  "${CERT_DIR}/media-service.p12" \
  "${ROOT_DIR}/media-service/certs/media-service.p12" \
  "${ROOT_DIR}/scripts/certs/media-service.p12" \
  "${ROOT_DIR}/certs/media-service.p12")"

PAYMENTS_CERT_SRC="$(find_first_file "payments-service.p12" \
  "${CERT_DIR}/payments-service.p12" \
  "${ROOT_DIR}/payments-service/certs/payments-service.p12" \
  "${ROOT_DIR}/scripts/certs/payments-service.p12" \
  "${ROOT_DIR}/certs/payments-service.p12")"

EUREKA_CERT_SRC="$(find_first_file "eureka-server.p12" \
  "${CERT_DIR}/eureka-server.p12" \
  "${ROOT_DIR}/eureka-server/certs/eureka-server.p12" \
  "${ROOT_DIR}/scripts/certs/eureka-server.p12" \
  "${ROOT_DIR}/certs/eureka-server.p12")"

PROMETHEUS_CERT_SRC="$(find_optional_file "prometheus scraper certificate" \
  "${CERT_DIR}/prometheus.crt" \
  "${ROOT_DIR}/prometheus/certs/client.crt" \
  "${ROOT_DIR}/scripts/certs/prometheus.crt" \
  "${ROOT_DIR}/certs/prometheus.crt")"

PROMETHEUS_KEY_SRC="$(find_optional_file "prometheus scraper key" \
  "${CERT_DIR}/prometheus.key" \
  "${ROOT_DIR}/prometheus/certs/client.key" \
  "${ROOT_DIR}/scripts/certs/prometheus.key" \
  "${ROOT_DIR}/certs/prometheus.key")"

STAGE_DIR="$(mktemp -d)"
cleanup() {
  rm -rf "${STAGE_DIR}"
}
trap cleanup EXIT

mkdir -p "${STAGE_DIR}/certs"

install -m 600 "${TRUSTSTORE_SRC}" "${STAGE_DIR}/certs/truststore.p12"
if [[ -n "${CA_SRC}" ]]; then
  install -m 644 "${CA_SRC}" "${STAGE_DIR}/certs/ca.crt"
else
  if [[ -z "${P12_PASSWORD}" ]]; then
    echo "[remote-cert-volumes] ca.crt is missing and no PKCS12 password is available to export it from ${TRUSTSTORE_SRC}." >&2
    echo "[remote-cert-volumes] Set P12_PASSWORD, pass --p12-password, or create scripts/.env.scripts with PSSWD=..." >&2
    exit 1
  fi

  require_cmd keytool
  echo "[remote-cert-volumes] Exporting ca.crt from ${TRUSTSTORE_SRC}"
  keytool -exportcert \
    -rfc \
    -alias ca-cert \
    -keystore "${TRUSTSTORE_SRC}" \
    -storetype PKCS12 \
    -storepass "${P12_PASSWORD}" \
    -file "${STAGE_DIR}/certs/ca.crt" >/dev/null
  chmod 644 "${STAGE_DIR}/certs/ca.crt"
fi

install -m 600 "${GATEWAY_CERT_SRC}" "${STAGE_DIR}/certs/gateway.p12"
install -m 600 "${USERS_CERT_SRC}" "${STAGE_DIR}/certs/users-service.p12"
install -m 600 "${PRODUCTS_CERT_SRC}" "${STAGE_DIR}/certs/products-service.p12"
install -m 600 "${MEDIA_CERT_SRC}" "${STAGE_DIR}/certs/media-service.p12"
install -m 600 "${PAYMENTS_CERT_SRC}" "${STAGE_DIR}/certs/payments-service.p12"
install -m 600 "${EUREKA_CERT_SRC}" "${STAGE_DIR}/certs/eureka-server.p12"

if [[ -n "${PROMETHEUS_CERT_SRC}" && -n "${PROMETHEUS_KEY_SRC}" ]]; then
  install -m 600 "${PROMETHEUS_CERT_SRC}" "${STAGE_DIR}/certs/prometheus.crt"
  install -m 600 "${PROMETHEUS_KEY_SRC}" "${STAGE_DIR}/certs/prometheus.key"
elif [[ -n "${PROMETHEUS_CERT_SRC}" || -n "${PROMETHEUS_KEY_SRC}" ]]; then
  echo "[remote-cert-volumes] Found only one Prometheus PEM file; both client.crt and client.key are required." >&2
  exit 1
else
  if [[ -z "${P12_PASSWORD}" ]]; then
    echo "[remote-cert-volumes] Prometheus PEM files are missing and no PKCS12 password is available to derive them from ${GATEWAY_CERT_SRC}." >&2
    echo "[remote-cert-volumes] Set P12_PASSWORD, pass --p12-password, or create scripts/.env.scripts with PSSWD=..." >&2
    exit 1
  fi

  require_cmd openssl
  echo "[remote-cert-volumes] Deriving Prometheus client cert/key from ${GATEWAY_CERT_SRC}"
  openssl pkcs12 \
    -in "${GATEWAY_CERT_SRC}" \
    -clcerts \
    -nokeys \
    -passin "pass:${P12_PASSWORD}" \
    -out "${STAGE_DIR}/certs/prometheus.crt" >/dev/null 2>&1
  openssl pkcs12 \
    -in "${GATEWAY_CERT_SRC}" \
    -nocerts \
    -nodes \
    -passin "pass:${P12_PASSWORD}" \
    -out "${STAGE_DIR}/certs/prometheus.key" >/dev/null 2>&1
  chmod 600 "${STAGE_DIR}/certs/prometheus.crt" "${STAGE_DIR}/certs/prometheus.key"
fi

copy_if_exists "${CERT_DIR}/jwt-private.pem" "${STAGE_DIR}/certs/jwt-private.pem"
copy_if_exists "${CERT_DIR}/jwt-public.pem" "${STAGE_DIR}/certs/jwt-public.pem"
copy_if_exists "${CERT_DIR}/private.pem" "${STAGE_DIR}/certs/private.pem"
copy_if_exists "${CERT_DIR}/public.pem" "${STAGE_DIR}/certs/public.pem"
copy_if_exists "${ROOT_DIR}/users-service/certs/jwt-private.pem" "${STAGE_DIR}/certs/jwt-private.pem"
copy_if_exists "${ROOT_DIR}/gateway/certs/jwt-public.pem" "${STAGE_DIR}/certs/jwt-public.pem"

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
