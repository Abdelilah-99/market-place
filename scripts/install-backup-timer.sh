#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 /mounted/external/backup-directory" >&2
  exit 2
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="$1"
SYSTEMD_DIR="${SYSTEMD_DIR:-/etc/systemd/system}"

mkdir -p "${BACKUP_DIR}"
root_source="$(findmnt -n -o SOURCE --target /)"
backup_source="$(findmnt -n -o SOURCE --target "${BACKUP_DIR}")"
if [[ "${root_source}" == "${backup_source}" ]]; then
  echo "[backup] ${BACKUP_DIR} is on the root filesystem; mount external storage first." >&2
  exit 1
fi

escaped_root="${ROOT_DIR//&/\\&}"
escaped_root="${escaped_root//|/\\|}"
escaped_backup="${BACKUP_DIR//\\/\\\\}"
escaped_backup="${escaped_backup//\"/\\\"}"

sed "s|@REPOSITORY@|${escaped_root}|g" "${ROOT_DIR}/ops/systemd/marketo-backup.service.in" \
  | sudo tee "${SYSTEMD_DIR}/marketo-backup.service" >/dev/null
sudo install -m 0644 "${ROOT_DIR}/ops/systemd/marketo-backup.timer" \
  "${SYSTEMD_DIR}/marketo-backup.timer"
printf 'MARKETO_BACKUP_DIR="%s"\nBACKUP_RETENTION_DAYS=7\n' "${escaped_backup}" \
  | sudo tee /etc/marketo-backup.env >/dev/null
sudo chmod 0600 /etc/marketo-backup.env
sudo systemctl daemon-reload
sudo systemctl enable --now marketo-backup.timer
sudo systemctl list-timers marketo-backup.timer --no-pager
