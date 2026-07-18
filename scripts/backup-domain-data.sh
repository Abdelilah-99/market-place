#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="${MARKETO_BACKUP_DIR:-}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"

if [[ -z "${BACKUP_DIR}" ]]; then
  echo "[backup] Set MARKETO_BACKUP_DIR to a mounted external/block-storage path." >&2
  exit 1
fi
mkdir -p "${BACKUP_DIR}"

root_source="$(findmnt -n -o SOURCE --target / 2>/dev/null || true)"
backup_source="$(findmnt -n -o SOURCE --target "${BACKUP_DIR}" 2>/dev/null || true)"
if [[ "${ALLOW_LOCAL_BACKUPS:-false}" != "true" && -n "${root_source}" && "${root_source}" == "${backup_source}" ]]; then
  echo "[backup] Refusing to store backups on the 30 GB root filesystem." >&2
  echo "[backup] Mount external storage at ${BACKUP_DIR}, or explicitly set ALLOW_LOCAL_BACKUPS=true." >&2
  exit 1
fi

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
destination="${BACKUP_DIR}/${timestamp}"
mkdir -p "${destination}"

backup_mongo() {
  local container="$1"
  if docker inspect -f '{{.State.Running}}' "${container}" 2>/dev/null | grep -q true; then
    echo "[backup] Exporting ${container}"
    docker exec "${container}" mongodump --archive --gzip > "${destination}/${container}.archive.gz"
  else
    echo "[backup] Skipping stopped/missing container: ${container}"
  fi
}

backup_mongo user-mongodb
backup_mongo products-service-mongodb
backup_mongo media-service-mongodb
backup_mongo payments-service-mongodb

if docker inspect -f '{{.State.Running}}' media-service 2>/dev/null | grep -q true; then
  echo "[backup] Exporting media files"
  docker exec media-service tar -C /storage -czf - . > "${destination}/media-storage.tar.gz"
fi

find "${BACKUP_DIR}" -mindepth 1 -maxdepth 1 -type d -mtime "+${RETENTION_DAYS}" -print -exec rm -rf {} +
echo "[backup] Completed: ${destination}"
