#!/usr/bin/env bash

# Safe Docker cleanup shared by deployment scripts. This intentionally never
# prunes volumes because they contain databases, media, monitoring, Jenkins,
# and certificate data.
docker_cleanup() {
  if [[ "${CI_DOCKER_CLEANUP:-true}" != "true" ]]; then
    echo "[CD] Docker cleanup disabled."
    return
  fi

  local prune_until="${CI_DOCKER_PRUNE_UNTIL:-24h}"

  echo "[CD] Docker disk usage before cleanup:"
  docker system df || true
  echo "[CD] Pruning unused containers, networks, images, and builder cache older than ${prune_until}."
  docker container prune -f >/dev/null || true
  docker network prune -f >/dev/null || true

  if [[ "${CI_DOCKER_PRUNE_ALL_IMAGES:-true}" == "true" ]]; then
    docker image prune -a -f --filter "until=${prune_until}" >/dev/null || true
  else
    docker image prune -f >/dev/null || true
  fi

  docker builder prune -f --filter "until=${prune_until}" >/dev/null || true
  echo "[CD] Docker disk usage after cleanup:"
  docker system df || true
}

# Remove every unused Docker artifact while preserving all named volumes and
# images backing running containers. Used when the Docker filesystem is close
# to full and before rollback, where age-based pruning cannot reclaim layers
# created by the deployment that just failed.
docker_cleanup_aggressive() {
  if [[ "${CI_DOCKER_CLEANUP:-true}" != "true" ]]; then
    return
  fi

  echo "[CD] Running low-disk Docker cleanup (volumes are preserved)."
  docker container prune -f >/dev/null || true
  docker network prune -f >/dev/null || true
  docker image prune -a -f >/dev/null || true
  docker builder prune -a -f >/dev/null || true
  docker system df || true
}

docker_ensure_space() {
  local minimum_gb="${CI_DOCKER_MIN_FREE_GB:-3}"
  local docker_root available_kb required_kb
  docker_root="$(docker info --format '{{.DockerRootDir}}' 2>/dev/null || true)"
  [[ -n "${docker_root}" ]] || docker_root="/var/lib/docker"

  available_kb="$(df -Pk "${docker_root}" 2>/dev/null | awk 'NR == 2 {print $4}' || true)"
  if [[ ! "${available_kb}" =~ ^[0-9]+$ ]]; then
    echo "[CD] Could not determine free space for ${docker_root}; running standard cleanup."
    docker_cleanup
    return
  fi

  required_kb=$((minimum_gb * 1024 * 1024))
  echo "[CD] Docker filesystem has $((available_kb / 1024 / 1024)) GiB free; ${minimum_gb} GiB required."
  if (( available_kb < required_kb )); then
    docker_cleanup_aggressive
    available_kb="$(df -Pk "${docker_root}" | awk 'NR == 2 {print $4}')"
    if (( available_kb < required_kb )); then
      echo "[ERROR] Docker filesystem still has only $((available_kb / 1024 / 1024)) GiB free after cleanup."
      echo "[ERROR] Free host disk space before deploying; database/media volumes were not pruned."
      return 1
    fi
  fi
}

# Release transient layers immediately after each Compose build. The normal
# cleanup keeps recent cache for speed, but small hosts cannot retain every
# service's build layers until the full deployment finishes.
docker_build_cleanup() {
  if [[ "${CI_DOCKER_CLEANUP:-true}" != "true" ]]; then
    return
  fi

  echo "[CD] Releasing transient Docker build data."
  docker image prune -f >/dev/null || true
  docker builder prune -a -f >/dev/null || true
}
