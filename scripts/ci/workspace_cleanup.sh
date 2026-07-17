#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

echo "[CI] Cleaning transient workspace build outputs."

# Test reports are archived by Jenkins before this script runs. Keep the
# persistent .jenkins-state dependency caches and deployment commit marker.
find shared eureka-server gateway payments-service products-service media-service \
  -type d -name target -prune -exec rm -rf {} + 2>/dev/null || true
rm -rf users-service/service/build frontend/node_modules frontend/dist frontend/coverage

# Log files have already been archived. Retain only the current deployment
# marker and reusable dependency caches under .jenkins-state.
rm -rf "${CI_STATE_DIR:-${ROOT_DIR}/.jenkins-state}/logs"

echo "[CI] Workspace usage after cleanup:"
du -sh "${ROOT_DIR}" 2>/dev/null || true
