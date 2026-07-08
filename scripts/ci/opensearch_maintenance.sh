#!/usr/bin/env bash
set -euo pipefail

OPENSEARCH_CONTAINER="${OPENSEARCH_CONTAINER:-opensearch}"
OPENSEARCH_URL="${OPENSEARCH_URL:-http://localhost:9200}"
MAX_ATTEMPTS="${OPENSEARCH_HEALTH_ATTEMPTS:-30}"
SLEEP_SECONDS="${OPENSEARCH_HEALTH_SLEEP:-5}"

log() {
  echo "[opensearch-maintenance] $*"
}

container_running() {
  docker inspect -f '{{.State.Running}}' "${OPENSEARCH_CONTAINER}" 2>/dev/null | grep -q '^true$'
}

opensearch_curl() {
  docker exec "${OPENSEARCH_CONTAINER}" curl -fsS "$@"
}

wait_for_opensearch() {
  if ! container_running; then
    log "Container ${OPENSEARCH_CONTAINER} is not running; skipping maintenance."
    return 1
  fi

  for attempt in $(seq 1 "${MAX_ATTEMPTS}"); do
    if opensearch_curl "${OPENSEARCH_URL}/_cluster/health" >/dev/null 2>&1; then
      return 0
    fi

    log "Waiting for OpenSearch health endpoint (${attempt}/${MAX_ATTEMPTS})..."
    sleep "${SLEEP_SECONDS}"
  done

  log "OpenSearch did not become ready; skipping index block cleanup."
  return 1
}

log "Docker disk usage before OpenSearch maintenance:"
docker system df || true

if wait_for_opensearch; then
  log "Clearing flood-stage read-only blocks from all indices."
  opensearch_curl \
    -X PUT "${OPENSEARCH_URL}/_all/_settings" \
    -H 'Content-Type: application/json' \
    -d '{"index.blocks.read_only_allow_delete": null}' >/dev/null || {
      log "Could not clear read-only index blocks. Host disk may still be above the flood-stage watermark."
    }

  log "OpenSearch allocation/disk view:"
  opensearch_curl "${OPENSEARCH_URL}/_cat/allocation?v" || true
fi

log "Docker disk usage after OpenSearch maintenance:"
docker system df || true
