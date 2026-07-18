#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SYSTEMD_DIR="${SYSTEMD_DIR:-/etc/systemd/system}"
SERVICE_TEMPLATE="${ROOT_DIR}/ops/systemd/marketo-cleanup.service.in"
TIMER_SOURCE="${ROOT_DIR}/ops/systemd/marketo-cleanup.timer"

if [[ ! -f "${SERVICE_TEMPLATE}" || ! -f "${TIMER_SOURCE}" ]]; then
  echo "[timer] systemd templates are missing" >&2
  exit 1
fi

escaped_root="${ROOT_DIR//&/\\&}"
escaped_root="${escaped_root//|/\\|}"

echo "[timer] Installing daily cleanup for ${ROOT_DIR}"
sed "s|@REPOSITORY@|${escaped_root}|g" "${SERVICE_TEMPLATE}" \
  | sudo tee "${SYSTEMD_DIR}/marketo-cleanup.service" >/dev/null
sudo install -m 0644 "${TIMER_SOURCE}" "${SYSTEMD_DIR}/marketo-cleanup.timer"
sudo systemctl daemon-reload
sudo systemctl enable --now marketo-cleanup.timer

echo "[timer] Installed successfully"
sudo systemctl list-timers marketo-cleanup.timer --no-pager
